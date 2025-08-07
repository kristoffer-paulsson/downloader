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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class DebianMetadataDownloader extends AbstractWorkerIterator<DebianMetadataDownloader.MetadataWorker> {

    private static final String MIRROR_URL = "https://www.debian.org/mirror/list-full";
    private static final String PACKAGE_URL = "http://deb.debian.org/debian/dists/%s/%s/binary-%s/Packages.gz";
    private static final String PACKAGE_REPO = "dists/%s/%s/binary-%s/Packages.gz";


    private final GeneralEnvironment ge;
    private final DebianDownloadEnvironment dde;
    private final WorkLogger workLogger;

    private final List<Pair<BasePackageImpl, DownloadHelper.Download>> metadataTasks = new ArrayList<>();

    private final AtomicReference<List<DownloadHelper.Download>> incompleteDownloads = new AtomicReference<>(new ArrayList<>());

    public DebianMetadataDownloader(GeneralEnvironment ge, DebianDownloadEnvironment dde, WorkLogger workLogger) {
        this.ge = ge;
        this.dde = dde;
        this.workLogger = workLogger;

    }

    @Override
    protected MetadataWorker createWorker() {
        if (!lastRow.get().verifyRowHash(lastHash)) {
            brokenChain.compareAndSet(false, true);
            throw new IllegalStateException("Invalid row hash: " + lastRow.get().hash);
        }

        lastHash = lastRow.get().hash;
        return new BlockchainVerifier.VerifyTask(workLogger, lastRow.get(), blockchain.isFinalized(), artifactPath.artifactFile(lastRow.get()));
    }

    @Override
    public boolean hasNext() {
        return stringIterator.hasNext() && !brokenChain.get();
    }

    public void prepareMetadataTasks() throws Exception {
        Iterator<String> components = DebianComponent.toStringList().iterator();

        components.forEachRemaining((comp) ->{
            String url = String.format(PACKAGE_URL, dde.getDistribution(), comp, dde.getArchitecture());

            try {
                Path cachePath = ge.getCacheDir();
                Files.createDirectories(cachePath);

                Path outputFile = cachePath.resolve(String.format(PACKAGE_REPO, dde.getDistribution(), comp, dde.getArchitecture()));
                Files.createDirectories(outputFile.getParent());

                metadataTasks.add(new Pair<>(
                        new BasePackageImpl("Packages.gz", "n/a", "n/a"),
                        new DownloadHelper.Download(URI.create(url).toURL(), outputFile)
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<DownloadHelper.Download> getIncompleteDownloads() {
        return incompleteDownloads.get();
    }

    public class MetadataWorker extends Worker<BasePackageImpl> {

        public MetadataWorker(
                BasePackageImpl basePackage,
                DownloadHelper.Download downloadTask,
                WorkLogger logger
        ) {
            super(basePackage, downloadTask, logger);
        }

        @Override
        protected void doWhenDownloadVerifiedSuccessful() throws IOException {
            logger.warning("Download of " + basePackage.uniqueKey() + " registered to blockchain.");
        }

        @Override
        protected void doWhenDownloadVerifiedFailure() throws IOException {
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
