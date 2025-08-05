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

import org.example.downloader.java.JavaWorkerIterator;
import org.example.downloader.util.*;

import java.util.Optional;


public class JavaDownloadAction extends JavaVerifyAction {

    public static String FILENAME = "java_download-%s";

    public JavaDownloadAction(InversionOfControl ioc, String name) {
        super(ioc, name);
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

            chain.resume();
        } catch (IllegalStateException e) {
            chain = BlockChainHelper.startBlockchain(
                    ge.getChainDir(),
                    String.format(FILENAME, jde.hashOfConfiguration())
            );
            chain.start();
            System.out.println("Created new blockchain: " + chain.getBlockchainFile());
        }

        System.out.println("Totally " + allPackages.size() + " artifacts yet to download for completion.");
        System.out.println("Approximately up to " + PrintHelper.formatByteSize(totalSize.get() - downloadedSize.get()) + " of data to download.");

        JavaWorkerIterator javaDownloader = new JavaWorkerIterator(jde, allPackages, chain, logger);


        executorHolder.executor = new WorkerExecutor(javaDownloader, logger);
        executorHolder.indicator = new Thread(() -> {

            executorHolder.executor.start();
            while (executorHolder.executor.isRunning()) {
                try {
                    Thread.sleep(10);
                    ProgressBar.printProgressMsg(
                            executorHolder.executor.getCurrentTotalBytes(),
                            totalSize.get() - downloadedSize.get(),
                            50,
                            ProgressBar.ANSI_GREEN,
                            "Downloading " + PrintHelper.formatByteSize(executorHolder.executor.getCurrentTotalBytes())
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

        if(javaDownloader.getIncompleteDownloads().isEmpty()) {
            System.out.println("No incomplete downloads, finalizing blockchain!");
            chain.finalizeBlockchain();
        } else {
            System.out.println("Number of incomplete downloads are " + javaDownloader.getIncompleteDownloads().size() + ", try to download again.");
        }

        chain.close();
    }

    @Override
    protected boolean prepareResumeBlockchain() {
        Optional<BlockChainHelper.Blockchain> blockchain = BlockChainHelper.resumeBlockchain(
                ge.getChainDir(),
                String.format(FILENAME, jde.hashOfConfiguration())
        );
        if(blockchain.isPresent()) {
            chain = blockchain.get();
            System.out.println("Found latest blockchain: " + chain.getBlockchainFile());
            if(chain.isFinalized()) {
                showMessageAndWait("Blockchain is finalized and can only be verified");
                return false;
            }
        } else {
            throw new IllegalStateException("No blockchain available");
        }
        return true;
    }
}
