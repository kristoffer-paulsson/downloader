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

import org.example.downloader.WorkLogger;
import org.example.downloader.java.*;
import org.example.downloader.util.*;
import org.example.downloader.deb.Menu;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

        JavaParser.filterPackages(jde).forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
        System.out.println("Total artifact batch count: " + allPackages.size());

        ProgressBar.printProgress(downloadedSize.get(), totalSize.get(), 50, ProgressBar.ANSI_GREEN);

        WorkLogger logger = ioc.resolve(WorkLogger.class);
        Path blockchainFile = Path.of(String.format("%s/%s", jde.getDownloadDir(), "java_download_chain.csv"));
        BlockChainHelper.Blockchain chain = new BlockChainHelper.Blockchain(blockchainFile.toFile());
        BlockchainVerifier verifier = new BlockchainVerifier(chain, logger, false, (r) -> Path.of(
                String.format(
                        "%s/%s",
                        jde.getDownloadDir().toString(),
                        allPackages.get(r.getDigest()).getFilename()
                )
        ));

        WorkerExecutor executor = new WorkerExecutor(verifier, logger);

        Thread indicator = new Thread(() -> {
            String color;

            executor.start();
            while (executor.isRunning()) {
                try {
                    Thread.sleep(10);
                    if(verifier.isBroken()) {
                        color = ProgressBar.ANSI_RED;
                    } else if(!verifier.getBrokenArtifacts().isEmpty()) {
                        color = ProgressBar.ANSI_YELLOW;
                    } else {
                        color = ProgressBar.ANSI_GREEN;
                    }
                    ProgressBar.printProgressMsg(
                            executor.getCurrentTotalBytes(),
                            totalSize.get(),
                            50,
                            color,
                            "Verifying blockchain " + PrintHelper.formatSpeed(executor.getSpeed())
                    );
                } catch (InterruptedException e) {
                    //
                }
            }
            executor.shutdown();
        });

        try {
            indicator.start();
            indicator.join();
            System.out.println();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(
                "Verified " + PrintHelper.formatByteSize(executor.getCurrentTotalBytes()) + " of information in " +
                        PrintHelper.formatTime(executor.getTime()) + " at a speed of " +
                        PrintHelper.formatSpeed(executor.getActiveWorkerCount())
        );

        if(verifier.isBroken()) {
            System.out.println("The blockchain file is broken, it is recommended to delete the file and try again.");
        } else {
            System.out.println("The blockchain file is intact!");
        }

        if(verifier.getBrokenArtifacts().isEmpty()) {
            System.out.println("All downloaded artifacts was verified intact using SHA-256.");
        } else {
            System.out.println("One or several of the artifacts failed SHA-256 verification");
            verifier.getBrokenArtifacts().forEach((r) -> {
                String path = String.format(
                        "%s/%s",
                        jde.getDownloadDir().toString(),
                        allPackages.get(r.getDigest()).getFilename()
                );
                System.out.println("Delete: " + path);
            });
        }

        verifier.getVerifiedArtifacts().forEach((r) -> {
            allPackages.remove(r.getDigest());
        });
        allPackages.forEach((d, p) -> {
            downloadedSize.addAndGet(p.getByteSize());
        });

        System.out.println("Totally " + allPackages.size() + " artifacts yet to download for completion.");
        System.out.println("Approximately up to " + PrintHelper.formatByteSize(downloadedSize.get()) + " of data to download.");

        /*BlockChainHelper.Blockchain chain = BlockChainHelper.continueBlockchain(
                Path.of(String.format("%s/%s", jde.getDownloadDir(), "java_download_chain.csv")),
                (row) -> {
                    try {
                        Path artifactFile = Path.of(String.format("%s/%s", jde.getDownloadDir(), row.getMetadata()));
                        downloadedSize.getAndAdd(Files.size(artifactFile));
                        allPackages.remove(row.getDigest());

                        ProgressBar.printProgressMsg(count.get() - allPackages.size(), count.get(), 50, ProgressBar.ANSI_GREEN, "(Sha-256 verify)");

                        return Sha256Helper.verifySha256Digest(artifactFile, row.getDigest());
                    } catch (IOException e) {
                        return false;
                    }
                }
        );
        System.out.println();

        System.out.println("Packages verified on blockchain: " + (count.get() - allPackages.size()));
        System.out.println("Packages left to download: " + allPackages.size());*/


        /*allPackages.forEach((s, p) -> System.out.println(p.uniqueKey()));

        Thread indicator = new Thread(() -> {
            WorkLogger logger = ioc.resolve(WorkLogger.class);
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
            executor.shutdown();
            chain.close();
        });

        try {
            indicator.start();
            indicator.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }
}
