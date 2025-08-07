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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public class DebianMetadataDownloader extends AbstractWorkerIterator<DebianMetadataDownloader.MetadataWorker> {

    private static final String PACKAGE_URL = "http://deb.debian.org/debian/dists/%s/%s/binary-%s/Packages.gz";
    private static final String PACKAGE_REPO = "dists/%s/%s/binary-%s/Packages.gz";

    private final GeneralEnvironment ge;
    private final DebianDownloadEnvironment dde;
    private final WorkLogger workLogger;

    private final AtomicLong totalBytes = new AtomicLong();
    private Iterator<Pair<BasePackageImpl, DownloadHelper.Download>> metadataTasks;
    private final AtomicReference<List<DownloadHelper.Download>> incompleteDownloads = new AtomicReference<>(new ArrayList<>());

    public DebianMetadataDownloader(GeneralEnvironment ge, DebianDownloadEnvironment dde, WorkLogger workLogger) {
        this.ge = ge;
        this.dde = dde;
        this.workLogger = workLogger;
        prepareMetadataTasks();
    }

    @Override
    protected MetadataWorker createWorker() {
        Pair<BasePackageImpl, DownloadHelper.Download> pair = metadataTasks.next();
        return new MetadataWorker(pair.getFirst(), pair.getSecond(), workLogger);
    }

    @Override
    public boolean hasNext() {
        return metadataTasks.hasNext();
    }

    public long getTotalBytes() {
        return totalBytes.get();
    }

    public void prepareMetadataTasks() {
        List<Pair<BasePackageImpl, DownloadHelper.Download>> metadataTasks = new ArrayList<>();
        Iterator<String> components = DebianComponent.toStringList().iterator();

        components.forEachRemaining((comp) ->{
            String url = String.format(PACKAGE_URL, dde.getDistribution().getDist(), comp, dde.getArchitecture().getArch());

            try {
                Path cachePath = dde.getDownloadDir();
                Files.createDirectories(cachePath);

                Path outputFile = cachePath.resolve(String.format(PACKAGE_REPO, dde.getDistribution().getDist(), comp, dde.getArchitecture().getArch()));
                Files.createDirectories(outputFile.getParent());

                URL realUrl = URI.create(url).toURL();
                long byteSize = DownloadHelper.queryUrlFileDownloadSize(realUrl);

                totalBytes.addAndGet(byteSize);

                metadataTasks.add(new Pair<>(
                        new BasePackageImpl("Packages.gz", String.valueOf(byteSize), "n/a"),
                        new DownloadHelper.Download(realUrl, outputFile)
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.metadataTasks = metadataTasks.iterator();
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
        protected boolean verifySha256Digest() throws IOException {
            return Files.exists(downloadTask.getFilePath());
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
