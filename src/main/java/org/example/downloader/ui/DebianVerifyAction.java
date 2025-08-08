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

import org.example.downloader.deb.DebianDownloadEnvironment;
import org.example.downloader.deb.DebianPackage;
import org.example.downloader.deb.DebianParser;
import org.example.downloader.util.*;

import java.nio.file.Path;


public class DebianVerifyAction extends AbstractVerifyAction<DebianDownloadEnvironment, DebianPackage> {

    public static String FILENAME = "debian_%s_%s_%s_%s";

    /*protected GeneralEnvironment ge;
    protected DebianDownloadEnvironment dde;
    protected AtomicInteger count;
    protected AtomicLong totalSize;
    protected AtomicLong downloadedSize;
    protected HashMap<String, JavaPackage> allPackages;
    protected MyObject executorHolder;
    WorkLogger logger;*/

    BlockChainHelper.Blockchain chain;

    public DebianVerifyAction(InversionOfControl ioc, String name) {
        super(ioc, name);
    }

    /*@Override
    protected void setupAction() {
        ge = ioc.resolve(GeneralEnvironment.class);
        dde = ioc.resolve(DebianDownloadEnvironment.class);
        count = new AtomicInteger();
        totalSize = new AtomicLong();
        downloadedSize = new AtomicLong();
        allPackages = new HashMap<>();
        executorHolder = new MyObject();
        logger = ioc.resolve(WorkLogger.class);
    }*/

    @Override
    protected DebianDownloadEnvironment getEnvironmentManager() {
        return ioc.resolve(DebianDownloadEnvironment.class);
    }

    @Override
    public void runAction() {
        setupAction();

        artifactInventory();

        try {
            if(!prepareResumeBlockchain()){
                return;
            }

            BlockchainVerifier verifier = createBlockchainVerifier();

            verifierThread(executorHolder, verifier, logger, totalSize.get());

            postVerificationAnalyze(executorHolder, verifier);

        } catch (IllegalStateException e) {
            System.out.println("Failed to verify blockchain because of " + e.getMessage());
        }

        System.out.println("Totally " + allPackages.size() + " artifacts needs yet to be downloaded for completion.");
    }

    @Override
    protected void loadArtifactInventory() {
        DebianParser.chunkPackages(em).get(em.getPiece()-1).packages.forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
    }

    /*protected void artifactInventory() {
        JavaParser.filterPackages(dde).forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
        System.out.println("Estimated total size: " + PrintHelper.formatByteSize(totalSize.get()));
        System.out.println("Total artifact batch count: " + allPackages.size());
    }*/

    protected String generateBlockchainFilename() {
        return String.format(FILENAME, em.getDistribution().getDist(), em.getArchitecture().getArch(), em.getChunks(), em.getPiece());
    }

    /*protected boolean prepareResumeBlockchain() {
        Optional<BlockChainHelper.Blockchain> blockchain = BlockChainHelper.resumeBlockchain(
                ge.getChainDir(),
                generateBlockchainFilename()
        );
        if(blockchain.isPresent()) {
            chain = blockchain.get();
            System.out.println("Found latest blockchain: " + chain.getBlockchainFile());
            if(!chain.isFinalized()) {
                showMessageAndWait("Blockchain is expected to be finalized");
                return false;
            }
        } else {
            throw new IllegalStateException("No blockchain available");
        }
        return true;
    }*/

    protected String generateArtifactPath(BlockChainHelper.Row r) {
        return allPackages.get(r.getDigest()).buildSavePath(em).toString();
    }

    protected BlockchainVerifier createBlockchainVerifier() {
        return new BlockchainVerifier(chain, logger, (r) -> Path.of(generateArtifactPath(r)));
    }

    /*protected void verifierThread(
            MyObject executorHolder,
            BlockchainVerifier verifier,
            WorkLogger logger,
            long totalSize
    ) {
        executorHolder.executor = new WorkerExecutor(verifier, logger);
        executorHolder.indicator = new Thread(() -> {
            String color;

            executorHolder.executor.start();
            while (executorHolder.executor.isRunning()) {
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
                            executorHolder.executor.getCurrentTotalBytes(),
                            totalSize,
                            50,
                            color,
                            "Verifying blockchain " + PrintHelper.formatSpeed(executorHolder.executor.getSpeed())
                    );
                } catch (InterruptedException e) {
                    //
                }
            }
            executorHolder.executor.shutdown();
        });

        try {
            executorHolder.indicator.start();
            executorHolder.indicator.join();
            System.out.println();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }*/

    /*protected void postVerificationAnalyze(
            MyObject executorHolder,
            BlockchainVerifier verifier
    ) {
        System.out.println(
                "Verified " + PrintHelper.formatByteSize(executorHolder.executor.getCurrentTotalBytes()) + " of information in " +
                        PrintHelper.formatTime(executorHolder.executor.getTime()) + " at a speed of " +
                        PrintHelper.formatSpeed(executorHolder.executor.getActiveWorkerCount())
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
                String path = generateArtifactPath(r);
                System.out.println("Delete: " + path);
            });
        }

        verifier.getVerifiedArtifacts().forEach((r) -> {
            JavaPackage jp = allPackages.remove(r.getDigest());
            downloadedSize.addAndGet(jp.getByteSize());
        });
    }*/

    /*protected static class MyObject {
        WorkerExecutor executor;
        Thread indicator;
    }*/
}
