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
package org.example.downloader.ui;

import org.example.downloader.DownloadLogger;
import org.example.downloader.java.*;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.deb.Menu;
import org.example.downloader.util.Sha256Helper;
import org.example.downloader.util.WorkerExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class JavaMenu extends Menu {
    public JavaMenu(InversionOfControl ioc) {
        super(ioc, "Java Downloader CLI");
        ioc.register(JavaDownloadEnvironment.class, () -> new JavaDownloadEnvironment("./"));
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup environment", option -> new JavaForm(ioc.resolve(JavaDownloadEnvironment.class), ioc).runForm());
        registerOption("View environment", option -> reviewConfig(ioc.resolve(JavaDownloadEnvironment.class)));
        registerOption("Download Worker", option -> runDownloadWorker());
    }

    private void reviewConfig(JavaDownloadEnvironment jds) {
        System.out.println("\n=== Current config ===");
        jds.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }

    private void runDownloadWorker() {
        JavaDownloadEnvironment jde = ioc.resolve(JavaDownloadEnvironment.class);
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalSize = new AtomicLong();
        AtomicLong downloadedSize = new AtomicLong();
        HashMap<String, JavaPackage> allPackages = new HashMap<>();
        List<String> chainedPackages = new ArrayList<>();

        JavaParser.filterPackages(jde).forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
        System.out.println("Total batch count: " + allPackages.size());

        ProgressBar.printProgress(downloadedSize.get(), totalSize.get(), 50, ProgressBar.ANSI_GREEN);

        BlockChainHelper.Blockchain chain = BlockChainHelper.continueBlockchain(
                Path.of(String.format("%s/%s", jde.getDownloadDir(), "java_download_chain.csv")),
                (row) -> {
                    try {
                        Path artifactFile = Path.of(String.format("%s/%s", jde.getDownloadDir(), row.getMetadata()));
                        downloadedSize.getAndAdd(Files.size(artifactFile));
                        allPackages.remove(row.getDigest());
                        chainedPackages.add(row.getDigest());

                        ProgressBar.printProgress(downloadedSize.get(), totalSize.get(), 50, ProgressBar.ANSI_GREEN);

                        return Sha256Helper.verifySha256Digest(artifactFile, row.getDigest());
                    } catch (IOException e) {
                        return false;
                    }
                }
        );

        chainedPackages.forEach(allPackages::remove);

        System.out.println("After verification left count: " + allPackages.size());

        Thread indicator = new Thread(() -> {
            DownloadLogger logger = ioc.resolve(DownloadLogger.class);
            WorkerExecutor executor = new WorkerExecutor(new JavaWorkerIterator(jde, allPackages, chain, logger), logger);

            executor.start();
            while (executor.isRunning()) {
                try {
                    Thread.sleep(100);
                    ProgressBar.printProgress(downloadedSize.get() + executor.getCurrentTotalBytes(), totalSize.get(), 50, ProgressBar.ANSI_GREEN);
                } catch (InterruptedException e) {
                    //
                }
            }
            chain.close();
        });

        try {
            indicator.start();
            indicator.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
