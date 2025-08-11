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
    private static final String PACKAGE_META_URL = "http://deb.debian.org/debian/dists/%s/%s";
    private static final String PACKAGE_META_REPO = "dists/%s/%s";

    private static final String[] META = {
            "ChangeLog",
            "InRelease",
            "Release",
            "Release.gpg"
    };

    private static final String REPO_URL = "https://deb.debian.org/debian/";
    private static final String RELEASE = "bookworm";
    private static final String[] COMPONENTS = {"main", "contrib", "non-free", "non-free-firmware"};
    private static final String ARCH = "amd64";
    private static final String[] ICON_SIZES = {"48x48", "64x64", "128x128", "48x48@2", "64x64@2", "128x128@2"};

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

    public void downloadFile(String filePath, List<Pair<BasePackageImpl, DownloadHelper.Download>> metadataTasks) {
        //String comp = component.getComp();
        //String url = String.format(PACKAGE_URL, dde.getDistribution().getDist(), comp, dde.getArchitecture().getArch());

        try {
            Path outputFile = dde.getDownloadDir().resolve(filePath);
            Files.createDirectories(dde.getDownloadDir());
            Files.createDirectories(outputFile.getParent());

            URL realUrl = URI.create(REPO_URL + filePath).toURL();
            long byteSize = DownloadHelper.queryUrlFileDownloadSize(realUrl);

            totalBytes.addAndGet(byteSize);

            metadataTasks.add(new Pair<>(
                    new BasePackageImpl(filePath, String.valueOf(byteSize), "n/a"),
                    new DownloadHelper.Download(realUrl, outputFile)
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void prepareMetadataTasks() {
        List<Pair<BasePackageImpl, DownloadHelper.Download>> metadataTasks = new ArrayList<>();

        try {
            // Download InRelease
            downloadFile("dists/" + dde.getDistribution().getDist() + "/InRelease", metadataTasks);
            downloadFile("dists/" + dde.getDistribution().getDist() + "/Release", metadataTasks);
            downloadFile("dists/" + dde.getDistribution().getDist() + "/Release.gpg", metadataTasks);

            // Download index files for each component
            for (String component : DebianComponent.toStringList()) {
                // Packages.gz (already present, but included for completeness)
                String packagesUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/binary-" + dde.getArchitecture().getArch() + "/Packages.gz";
                downloadFile(packagesUrl, metadataTasks);

                String packagesAllUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/binary-all/Packages.gz";
                downloadFile(packagesAllUrl, metadataTasks);

                String releaseAllUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/binary-all/Release";
                downloadFile(releaseAllUrl, metadataTasks);

                // Translation-en.gz
                String translationUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/i18n/Translation-en.xz";
                downloadFile(translationUrl, metadataTasks);

                // Contents-amd64.gz
                String contentsUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/Contents-" + dde.getArchitecture().getArch() + ".gz";
                downloadFile(contentsUrl, metadataTasks);

                String contentsAllUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/Contents-all.gz";
                downloadFile(contentsAllUrl, metadataTasks);

                // AppStream for GUI support
                String componentsUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/dep11/Components-" + dde.getArchitecture().getArch() + ".yml.gz";
                downloadFile(componentsUrl, metadataTasks);

                String cidIndexUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/dep11/CID-Index-" + dde.getArchitecture().getArch() + ".json.gz";
                downloadFile(cidIndexUrl, metadataTasks);

                for (String size : ICON_SIZES) {
                    String iconsUrl = "dists/" + dde.getDistribution().getDist() + "/" + component + "/dep11/icons-" + size + ".tar.gz";
                    downloadFile(iconsUrl, metadataTasks);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        /*Iterator<DebianComponent> components = Arrays.stream(DebianComponent.values()).iterator();
        Iterator<String> metaFiles = Arrays.stream(META).iterator();

        components.forEachRemaining((component) -> {
            String comp = component.getComp();
            String url = String.format(PACKAGE_URL, dde.getDistribution().getDist(), comp, dde.getArchitecture().getArch());

            try {
                Path outputFile = repositoryFile(dde, component);
                Files.createDirectories(dde.getDownloadDir());
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

        metaFiles.forEachRemaining((fileName) -> {
            String url = String.format(PACKAGE_META_URL, dde.getDistribution().getDist(), fileName);

            try {
                Path outputFile = repositoryFile(dde, fileName);
                Files.createDirectories(dde.getDownloadDir());
                Files.createDirectories(outputFile.getParent());

                URL realUrl = URI.create(url).toURL();
                long byteSize = DownloadHelper.queryUrlFileDownloadSize(realUrl);

                totalBytes.addAndGet(byteSize);

                metadataTasks.add(new Pair<>(
                        new BasePackageImpl(fileName, String.valueOf(byteSize), "n/a"),
                        new DownloadHelper.Download(realUrl, outputFile)
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });*/
        this.metadataTasks = metadataTasks.iterator();
    }

    public static Path repositoryFile(DebianDownloadEnvironment dde, DebianComponent comp) {
        return dde.getDownloadDir().resolve(String.format(PACKAGE_REPO, dde.getDistribution().getDist(), comp.getComp(), dde.getArchitecture().getArch()));
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
