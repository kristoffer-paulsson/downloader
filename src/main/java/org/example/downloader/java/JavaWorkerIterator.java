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
package org.example.downloader.java;

import org.example.downloader.WorkLogger;
import org.example.downloader.util.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JavaWorkerIterator extends WorkerIterator<JavaPackage> {

    private final JavaDownloadEnvironment jde;
    private final Iterator<JavaPackage> packageIterator;
    private final WorkLogger logger;
    private final BlockChainHelper.Blockchain chain;

    private final AtomicReference<List<DownloadHelper.Download>> incompleteDownloads = new AtomicReference<>(new ArrayList<>());


    public JavaWorkerIterator(JavaDownloadEnvironment jde, WorkLogger logger) {
        this.jde = jde;
        this.packageIterator = JavaParser.filterPackages(jde).iterator();
        this.logger = logger;
        this.chain = null;
    }

    public JavaWorkerIterator(
            JavaDownloadEnvironment jde,
            HashMap<String, JavaPackage> packages,
            BlockChainHelper.Blockchain chain,
            WorkLogger logger
    ) {
        this.jde = jde;
        this.packageIterator = packages.values().iterator();
        this.chain = chain;
        this.logger = logger;
    }

    @Override
    protected JavaWorker createWorker() {
        JavaPackage pkg = packageIterator.next();
        try {
            Path downloadPath = Path.of(String.format("%s/%s", jde.getDownloadDir(), pkg.getFilename()));
            return new JavaWorker(pkg, new DownloadHelper.Download(pkg.getRealUrl(), downloadPath), chain, logger);
        } catch (MalformedURLException | URISyntaxException e) {
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

    public class JavaWorker extends Worker<JavaPackage> {

        private final BlockChainHelper.Blockchain chain;

        public JavaWorker(
                JavaPackage basePackage,
                DownloadHelper.Download downloadTask,
                BlockChainHelper.Blockchain chain,
                WorkLogger logger
        ) {
            super(basePackage, downloadTask, logger);
            this.chain = chain;
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
            System.out.println("Download timed out for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenError() throws IOException {
            incompleteDownloads.get().add(downloadTask);
            System.out.println("Download errored for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenUnexpected() throws IOException {
            incompleteDownloads.get().add(downloadTask);
            System.out.println("Something unexpected for " + basePackage.uniqueKey());
        }
    }
}
