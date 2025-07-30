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
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Worker<E extends BasePackage> implements Runnable {

    public static enum WorkerState {
        NOT_STARTED,
        RUNNING,
        STOPPED,
        COMPLETED;
    }

    private final E basePackage;
    private final DownloadHelper.Download downloadTask;
    private final InversionOfControl ioc;
    private final ConfigManager configManager;
    private final Logger logger;
    private final AtomicBoolean isRunning;
    private volatile float progress = 0.0f;
    private volatile float speed = 0.0f;
    private volatile long bytesDownloaded = 0;
    private volatile float timeUsed = 0;
    static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    public Worker(E basePackage, DownloadHelper.Download downloadTask, InversionOfControl ioc) {
        this.basePackage = basePackage;
        this.downloadTask = downloadTask;
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.logger = ioc.resolve(DownloadLogger.class).getLogger();
        this.isRunning = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Download already in progress for " + downloadTask);
            return;
        }

        try {
            URL downloadUrl = downloadTask.getUrl();
            Path saveFile = downloadTask.getFilePath();
            String savePath = saveFile.toString();
            Files.createDirectories(saveFile.getParent());

            long downloadedSize = 0;
            if (Files.exists(saveFile)) {
                downloadedSize = Files.size(saveFile);

                if(downloadedSize >= basePackage.getSize()) {
                    isCompleted = true;
                    if(!Sha256Helper.verifySha256Digest(saveFile, basePackage.getSha256Digest())) {
                        Files.deleteIfExists(saveFile);
                        logger.warning("SHA256 digest verification failed for " + downloadTask + ", file may be corrupted. Deleted partial file.");
                    } else {
                        ioc.resolve(DebianPackageBlockchain.class).logPackage(downloadTask);
                        logger.info("Skipping download for " + downloadTask + " as it is already fully downloaded and SHA256 digest verified.");
                    }
                    return;
                }

                logger.info("Resuming download for " + downloadTask + " at " + downloadedSize + " bytes from " + downloadUrl + " to " + savePath);
            } else {
                logger.info("Starting download for " + downloadTask + " from " + downloadUrl + " to " + savePath);
            }

            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                if (downloadedSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
                }
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("HTTP error code: " + responseCode + " for " + downloadTask);
                }

                long totalSize = connection.getContentLengthLong() + downloadedSize;

                try (InputStream inputStream = connection.getInputStream();
                     RandomAccessFile outputFile = new RandomAccessFile(savePath, "rw")) {
                    outputFile.seek(downloadedSize);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && isRunning.get()) {
                        outputFile.write(buffer, 0, bytesRead);
                        downloadedSize += bytesRead;
                        bytesDownloaded += bytesRead;
                        progress = (float) downloadedSize / totalSize;
                        float currentTimeSlice = System.currentTimeMillis() - connection.getLastModified();
                        timeUsed += currentTimeSlice / 1000.0f;
                        speed = bytesRead / (currentTimeSlice / 1000.0f);
                    }

                    if (downloadedSize != totalSize) {
                        throw new IOException("Download incomplete: expected " + totalSize + " bytes, got " + downloadedSize);
                    }

                    if (!Sha256Helper.verifySha256Digest(saveFile, basePackage.getSha256Digest())) {
                        throw new IOException("SHA256 digest verification failed for " + downloadTask);
                    } else {
                        ioc.resolve(DebianPackageBlockchain.class).logPackage(downloadTask);
                        logger.info("SHA256 digest verified for " + downloadTask);
                    }

                    isCompleted = true;
                    logger.info("Download completed for " + downloadTask);
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException e) {
            ioc.resolve(DebianMirrorCache.class).reportBadMirror(baseUrl);
            logger.warning("Download timed out for " + downloadTask + " for mirror " + baseUrl + ": " + e.getMessage());
        } catch (IOException e) {
            ioc.resolve(DebianMirrorCache.class).reportBadMirror(baseUrl);
            logger.severe("Download failed for " + downloadTask + ": " + e.getMessage());
        } finally {
            isRunning.set(false);
        }
    }

    public void stopDownload() {
        downloadTask.stop();
        isRunning.set(false);
        logger.info("Download stopped for " + downloadTask);
    }

    public float getProgress() { return progress; }
    public float getSpeed() { return speed; }
    public DownloadHelper.Download getDownloadTask() { return downloadTask; }
    public float getTimeUsed() { return timeUsed; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public float getAverageSpeed() { return bytesDownloaded / (timeUsed > 0 ? timeUsed : 1); }

    public boolean isRunning() { return isRunning.get() && !isCompleted(); }
    public boolean isCompleted() { return downloadTask.isComplete(); }
    public long getDownloadedSize() {
        try {
            Path saveFile = downloadTask.getFilePath();
            return Files.exists(saveFile) ? Files.size(saveFile) : 0;
        } catch (IOException e) {
            logger.warning("Error checking downloaded size for " + downloadTask);
            return 0;
        }
    }
}