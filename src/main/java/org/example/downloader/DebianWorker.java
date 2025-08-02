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
package org.example.downloader;

import org.example.downloader.util.InversionOfControl;

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

public class DebianWorker implements Runnable {
    private final DebianPackage debianPackage;
    private final InversionOfControl ioc;
    private final ConfigManager configManager;
    private final Logger logger;
    private final String baseUrl;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    private volatile boolean isCompleted;
    private volatile float progress = 0.0f;
    private volatile float speed = 0.0f;
    private volatile long bytesDownloaded = 0;
    private volatile float timeUsed = 0;
    static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    public DebianWorker(DebianPackage debianPackage, InversionOfControl ioc) {
        this.debianPackage = debianPackage;
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.logger = ioc.resolve(WorkLogger.class).getLogger();
        this.baseUrl = ioc.resolve(DebianMirrorCache.class).getNextMirror();
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
        this.isCompleted = false;
    }

    @Override
    public void run() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Download already in progress for " + debianPackage.packageName);
            return;
        }

        try {
            String downloadUrl = debianPackage.buildDownloadUrl(baseUrl);
            Path saveFile = debianPackage.buildSavePath(configManager);
            String savePath = saveFile.toString();
            Files.createDirectories(saveFile.getParent());

            long downloadedSize = 0;
            if (Files.exists(saveFile)) {
                downloadedSize = Files.size(saveFile);

                if(downloadedSize >= debianPackage.getSize()) {
                    isCompleted = true;
                    if(!debianPackage.verifySha256Digest(saveFile)) {
                        Files.deleteIfExists(saveFile);
                        logger.warning("SHA256 digest verification failed for " + debianPackage.packageName + ", file may be corrupted. Deleted partial file.");
                    } else {
                        ioc.resolve(DebianPackageBlockchain.class).logPackage(debianPackage);
                        logger.info("Skipping download for " + debianPackage.packageName + " as it is already fully downloaded and SHA256 digest verified.");
                    }
                    return;
                }

                logger.info("Resuming download for " + debianPackage.packageName + " at " + downloadedSize + " bytes from " + downloadUrl + " to " + savePath);
            } else {
                logger.info("Starting download for " + debianPackage.packageName + " from " + downloadUrl + " to " + savePath);
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
                    throw new IOException("HTTP error code: " + responseCode + " for " + debianPackage.packageName + " at " + downloadUrl);
                }

                long totalSize = connection.getContentLengthLong() + downloadedSize;

                try (InputStream inputStream = connection.getInputStream();
                     RandomAccessFile outputFile = new RandomAccessFile(savePath, "rw")) {
                    outputFile.seek(downloadedSize);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && isRunning.get() && !isPaused.get()) {
                        outputFile.write(buffer, 0, bytesRead);
                        downloadedSize += bytesRead;
                        bytesDownloaded += bytesRead;
                        progress = (float) downloadedSize / totalSize;
                        float currentTimeSlice = System.currentTimeMillis() - connection.getLastModified();
                        timeUsed += currentTimeSlice / 1000.0f;
                        speed = bytesRead / (currentTimeSlice / 1000.0f);
                    }

                    if (isPaused.get()) {
                        logger.info("Download paused for " + debianPackage.packageName + " at " + downloadedSize + " bytes");
                        return;
                    }

                    if (downloadedSize != totalSize) {
                        throw new IOException("Download incomplete: expected " + totalSize + " bytes, got " + downloadedSize);
                    }

                    if (!debianPackage.verifySha256Digest(saveFile)) {
                        throw new IOException("SHA256 digest verification failed for " + debianPackage.packageName);
                    } else {
                        ioc.resolve(DebianPackageBlockchain.class).logPackage(debianPackage);
                        logger.info("SHA256 digest verified for " + debianPackage.packageName);
                    }

                    isCompleted = true;
                    logger.info("Download completed for " + debianPackage.packageName);
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException e) {
            ioc.resolve(DebianMirrorCache.class).reportBadMirror(baseUrl);
            logger.warning("Download timed out for " + debianPackage.packageName + " for mirror " + baseUrl + ": " + e.getMessage());
        } catch (IOException e) {
            ioc.resolve(DebianMirrorCache.class).reportBadMirror(baseUrl);
            logger.severe("Download failed for " + debianPackage.packageName + ": " + e.getMessage());
        } finally {
            isRunning.set(false);
        }
    }

    public void pauseDownload() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            logger.info("Pausing download for " + debianPackage.packageName);
        }
    }

    public void resumeDownload() {
        if (isPaused.get() && !isRunning.get() && !isCompleted) {
            isPaused.set(false);
            Thread thread = new Thread(this);
            thread.start();
            logger.info("Resuming download for " + debianPackage.packageName);
        } else if (isCompleted) {
            logger.info("Download already completed for " + debianPackage.packageName);
        } else if (isRunning.get()) {
            logger.warning("Download already in progress for " + debianPackage.packageName);
        }
    }

    public void stopDownload() {
        isRunning.set(false);
        isPaused.set(true);
        logger.info("Download stopped for " + debianPackage.packageName);
    }

    public float getProgress() { return progress; }
    public float getSpeed() { return speed; }
    public DebianPackage getDebianPackage() { return debianPackage; }
    public String getBaseUrl() { return baseUrl; }
    public float getTimeUsed() { return timeUsed; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public float getAverageSpeed() { return bytesDownloaded / (timeUsed > 0 ? timeUsed : 1); }
    public boolean isDownloading() { return isRunning.get() && !isPaused.get(); }
    public boolean isPaused() { return isPaused.get(); }
    public boolean isCompleted() { return isCompleted; }
    public long getDownloadedSize() {
        try {
            Path saveFile = debianPackage.buildSavePath(configManager);
            return Files.exists(saveFile) ? Files.size(saveFile) : 0;
        } catch (IOException e) {
            logger.warning("Error checking downloaded size for " + debianPackage.packageName);
            return 0;
        }
    }
}