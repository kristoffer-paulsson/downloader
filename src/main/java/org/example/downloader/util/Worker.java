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
package org.example.downloader.util;

import org.example.downloader.*;

import java.io.IOException;
import java.nio.file.Files;

public abstract class Worker<E extends BasePackage> extends AbstractWorker {

    protected final E basePackage;
    protected final DownloadHelper.Download downloadTask;

    public Worker(E basePackage, DownloadHelper.Download downloadTask, DownloadLogger logger) {
        super(logger);
        this.basePackage = basePackage;
        this.downloadTask = downloadTask;
    }

    protected abstract void doWhenTimedOut() throws IOException;

    protected abstract void doWhenDownloadHaltedUnexpectedly() throws IOException;

    protected abstract void doWhenVerifiedSuccessful() throws IOException;

    protected abstract void doWhenVerifiedFailed() throws IOException;

    protected abstract void doSomethingUnknownError() throws IOException;

    @Override
    public void run() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Download already in progress for " + downloadTask);
            return;
        }

        long downloadFullByteSize = basePackage.getByteSize();

        try {
            long bytesDownloaded = DownloadHelper.continueDownload(downloadTask, workLogger);
            long currentByteSize = Files.size(downloadTask.getFilePath());


            if(downloadTask.hasTimedOut()) {
                logger.info("Download of " + basePackage.uniqueKey() + " halted due to time out for some reason, continue another time please.");
                doWhenTimedOut();
            } else if(currentByteSize == downloadFullByteSize) {
                logger.info("Download of " + basePackage.uniqueKey() + " completed.");

                boolean digestVerified = Sha256Helper.verifySha256Digest(
                        downloadTask.getFilePath(),
                        basePackage.getSha256Digest()
                );

                if(digestVerified) {
                    logger.info("Download of " + basePackage.uniqueKey() + " sha256 digest verified, download file is intact.");
                    doWhenVerifiedSuccessful();
                } else {
                    logger.warning("Download of " + basePackage.uniqueKey() + " file failed sha256 verification");
                    doWhenVerifiedFailed();
                }
            } else if (bytesDownloaded > 0) {
                logger.info("Download of " + basePackage.uniqueKey() + " incomplete due to manual stop, continue another time please.");
                doWhenDownloadHaltedUnexpectedly();
            } else {
                logger.severe("Download file " + basePackage.uniqueKey() + " marked as complete but file size differ, investigate!");
                doSomethingUnknownError();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            isRunning.set(false);
        }
    }


    protected void stopProcessImpl() {
        downloadTask.stop();
        logger.info("Download stopped for " + basePackage.uniqueKey());
    }

    public float getSpeed() { return downloadTask.getSpeed(); }
    public float getTime() { return downloadTask.getTime(); }
    public long getCurrentProcessSize() {
        synchronized (downloadTask) {
            return downloadTask.totalBytesDownloaded();
        }
    }

    public boolean isRunning() { return isRunning.get() && !isCompleted(); }
    public boolean isCompleted() { return downloadTask.isComplete(); }
}