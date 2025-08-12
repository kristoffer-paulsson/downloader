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

import org.example.downloader.deb.DebianWorkerIteratorWithCopy;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.BlockchainVerifier;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.PrintHelper;

import java.util.Optional;


public class DebianExportAction extends DebianVerifyAction {

    public DebianExportAction(InversionOfControl ioc, String name) {
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
                    generateBlockchainFilename()
            );
            chain.start();
            System.out.println("Created new blockchain: " + chain.getBlockchainFile());
        }

        System.out.println("Totally " + allPackages.size() + " artifacts yet to download for completion.");
        System.out.println("Approximately up to " + PrintHelper.formatByteSize(totalSize.get() - downloadedSize.get()) + " of data to download.");

        DebianWorkerIteratorWithCopy debianDownloader = new DebianWorkerIteratorWithCopy(ge, em, allPackages, chain, logger);

        progressWorker(executorHolder, debianDownloader, logger, (eh) -> {
            ProgressBar.printProgressMsg(
                    eh.executor.getCurrentTotalBytes(),
                    totalSize.get() - downloadedSize.get(),
                    50,
                    ProgressBar.ANSI_GREEN,
                    "Downloading " + PrintHelper.formatByteSize(eh.executor.getCurrentTotalBytes())
            );
        });

        if(debianDownloader.getIncompleteDownloads().isEmpty()) {
            System.out.println("No incomplete downloads, finalizing blockchain!");
            chain.finalizeBlockchain();
        } else {
            System.out.println("Number of incomplete downloads are " + debianDownloader.getIncompleteDownloads().size() + ", try to download again.");
        }

        chain.close();
    }

    @Override
    protected boolean prepareResumeBlockchain() {
        Optional<BlockChainHelper.Blockchain> blockchain = BlockChainHelper.resumeBlockchain(
                ge.getChainDir(),
                generateBlockchainFilename()
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
