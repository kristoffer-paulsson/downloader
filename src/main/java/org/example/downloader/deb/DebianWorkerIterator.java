/**
 * Copyright (c) 2025 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 *
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 *
 *      https://opensource.org/licenses/MIT
 *
 * SPDX-License-Identifier: MIT
 *
 * Contributors:
 *      Kristoffer Paulsson - initial implementation
 */
package org.example.downloader.deb;

import org.example.downloader.GeneralEnvironment;
import org.example.downloader.WorkLogger;
import org.example.downloader.util.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DebianWorkerIterator extends WorkerIterator<DebianPackage> {

    private final DebianDownloadEnvironment dde;
    private final Iterator<DebianPackage> packageIterator;
    private final WorkLogger logger;
    private final BlockChainHelper.Blockchain chain;
    private final DebianMirrorCache mirrors;

    private final AtomicReference<List<DownloadHelper.Download>> incompleteDownloads = new AtomicReference<>(new ArrayList<>());

    public DebianWorkerIterator(
            GeneralEnvironment ge,
            DebianDownloadEnvironment dde,
            HashMap<String, DebianPackage> packages,
            BlockChainHelper.Blockchain chain,
            WorkLogger logger
    ) {
        this.dde = dde;
        this.packageIterator = packages.values().iterator();
        this.chain = chain;
        this.logger = logger;

        this.mirrors = new DebianMirrorCache(ge);
        this.mirrors.loadCachedMirrors(false);
    }

    @Override
    protected DebianWorkerIterator.DebianWorker createWorker() {
        DebianPackage pkg = packageIterator.next();
        try {
            String baseUrl = mirrors.getNextMirror();
            URL downloadURL = URI.create(pkg.buildDownloadUrl(baseUrl)).toURL();
            Path downloadPath = pkg.buildSavePath(dde);

            return new DebianWorkerIterator.DebianWorker(
                    pkg,
                    new DownloadHelper.Download(
                            downloadURL,
                            downloadPath
                    ),
                    chain,
                    mirrors,
                    logger,
                    baseUrl
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return packageIterator.hasNext();
    }

    public List<DownloadHelper.Download> getIncompleteDownloads() {
        return incompleteDownloads.get();
    }

    public class DebianWorker extends Worker<DebianPackage> {

        private final BlockChainHelper.Blockchain chain;
        private final DebianMirrorCache mirrors;
        private final String baseUrl;

        public DebianWorker(
                DebianPackage basePackage,
                DownloadHelper.Download downloadTask,
                BlockChainHelper.Blockchain chain,
                DebianMirrorCache mirrors,
                WorkLogger logger,
                String baseUrl
        ) {
            super(basePackage, downloadTask, logger);
            this.chain = chain;
            this.mirrors = mirrors;
            this.baseUrl = baseUrl;
        }

        @Override
        protected void doWhenDownloadVerifiedSuccessful() throws IOException {
            if(chain != null) {
                chain.addRow(basePackage.uniqueKey(), basePackage.getFilename(), basePackage.getSha256Digest());
                logger.warning("Download of " + basePackage.uniqueKey() + " registered to blockchain.");
            }
        }

        @Override
        protected void doWhenDownloadVerifiedFailure() throws IOException {
            Files.deleteIfExists(downloadTask.getFilePath());
            logger.warning("File of download " + basePackage.uniqueKey() + " deleted due to failed verification.");
        }

        @Override
        protected void doWhenTimedOut() throws IOException {
            incompleteDownloads.get().add(downloadTask);
            mirrors.reportBadMirror(baseUrl);
            System.out.println("Download timed out for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenError() throws IOException {
            incompleteDownloads.get().add(downloadTask);
            mirrors.reportBadMirror(baseUrl);
            System.out.println("Download errored for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenUnexpected() throws IOException {
            incompleteDownloads.get().add(downloadTask);
            mirrors.reportBadMirror(baseUrl);
            System.out.println("Something unexpected for " + basePackage.uniqueKey());
        }
    }
}