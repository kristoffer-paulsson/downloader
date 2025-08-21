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
package org.example.downloader.wtx;

import org.example.downloader.WorkLogger;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.Worker;
import org.example.downloader.util.WorkerIterator;

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

public class WinetricksWorkerIterator extends WorkerIterator<WinetricksPackage> {

    private final WinetricksDownloadEnvironment wde;
    private final Iterator<WinetricksPackage> packageIterator;
    private final WorkLogger logger;
    private final BlockChainHelper.Blockchain chain;

    private final AtomicReference<List<DownloadHelper.Download>> incompleteDownloads = new AtomicReference<>(new ArrayList<>());


    public WinetricksWorkerIterator(WinetricksDownloadEnvironment wde, WorkLogger logger) {
        this.wde = wde;
        this.packageIterator = WinetricksParser.filterPackages(wde).iterator();
        this.logger = logger;
        this.chain = null;
    }

    public WinetricksWorkerIterator(
            WinetricksDownloadEnvironment wde,
            HashMap<String, WinetricksPackage> packages,
            BlockChainHelper.Blockchain chain,
            WorkLogger logger
    ) {
        this.wde = wde;
        this.packageIterator = packages.values().iterator();
        this.chain = chain;
        this.logger = logger;
    }

    @Override
    protected WinetricksWorker createWorker() {
        WinetricksPackage pkg = packageIterator.next();
        try {
            Path downloadPath = wde.getDownloadDir().resolve(pkg.getVerb()).resolve(pkg.getFilename());
            return new WinetricksWorker(pkg, new DownloadHelper.Download(pkg.getRealUrl(), downloadPath), chain, logger);
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

    public class WinetricksWorker extends Worker<WinetricksPackage> {

        private final BlockChainHelper.Blockchain chain;

        public WinetricksWorker(
                WinetricksPackage basePackage,
                DownloadHelper.Download downloadTask,
                BlockChainHelper.Blockchain chain,
                WorkLogger logger
        ) {
            super(basePackage, downloadTask, logger);
            this.chain = chain;
        }

        @Override
        protected void doWhenDownloadVerifiedSuccessful() {
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
        protected void doWhenTimedOut() {
            incompleteDownloads.get().add(downloadTask);
            System.out.println("Download timed out for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenError() {
            incompleteDownloads.get().add(downloadTask);
            System.out.println("Download errored for " + basePackage.uniqueKey());
        }

        @Override
        protected void doWhenUnexpected() {
            incompleteDownloads.get().add(downloadTask);
            System.out.println("Something unexpected for " + basePackage.uniqueKey());
        }
    }
}
