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

import org.example.downloader.DownloadLogger;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.Worker;

import java.io.IOException;
import java.nio.file.Files;

public class JavaWorker extends Worker<JavaPackage> {

    private final BlockChainHelper.Blockchain chain;

    public JavaWorker(
            JavaPackage basePackage,
            DownloadHelper.Download downloadTask,
            BlockChainHelper.Blockchain chain,
            DownloadLogger logger
    ) {
        super(basePackage, downloadTask, logger);
        this.chain = chain;
    }

    @Override
    protected void doWhenTimedOut() throws IOException {
    }

    @Override
    protected void doWhenDownloadHaltedUnexpectedly() {
    }

    @Override
    protected void doWhenVerifiedSuccessful() throws IOException {
        if(chain != null) {
            chain.addRow(basePackage.uniqueKey(), basePackage.getFilename(), basePackage.getSha256Digest());
            logger.warning("Download of " + basePackage.uniqueKey() + " registered to blockchain.");
        }
    }

    @Override
    protected void doWhenVerifiedFailed() throws IOException {
        Files.deleteIfExists(downloadTask.getFilePath());
        logger.warning("File of download " + basePackage.uniqueKey() + " deleted due to failed verification.");
    }

    @Override
    protected void doSomethingUnknownError() throws IOException {
    }
}
