/**
 * Copyright (c) 2025 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 * <p>
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 * <p>
 * https://opensource.org/licenses/MIT
 * <p>
 * SPDX-License-Identifier: MIT
 * <p>
 * Contributors:
 * Kristoffer Paulsson - initial implementation
 */
package org.example.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebianWorker implements Runnable {
    private final DebianPackage debianPackage;
    private final ConfigManager configManager;
    private final String baseUrl;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    private volatile boolean isCompleted;
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    public DebianWorker(DebianPackage debianPackage, ConfigManager configManager, String baseUrl) {
        this.debianPackage = debianPackage;
        this.configManager = configManager;
        this.baseUrl = baseUrl;
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
        this.isCompleted = false;
    }

    /**
     * Implements the Runnable interface to download the Debian package in a separate thread.
     */
    @Override
    public void run() {
        if (!isRunning.compareAndSet(false, true)) {
            System.err.println("Download already in progress for " + debianPackage.packageName());
            return;
        }

        try {
            String downloadUrl = debianPackage.buildDownloadUrl(baseUrl);
            String savePath = debianPackage.buildSavePath(configManager);
            Path saveFile = Paths.get(savePath);
            Files.createDirectories(saveFile.getParent());

            System.out.println("Starting download for " + debianPackage.packageName() + " from " + downloadUrl + " to " + savePath);

            // Check for existing partial download
            long downloadedSize = 0;
            if (Files.exists(saveFile)) {
                downloadedSize = Files.size(saveFile);

                if(downloadedSize >= debianPackage.getSize()) {
                    isCompleted = true;
                    if(!verifyDigest(savePath)) {
                        Files.deleteIfExists(saveFile);
                        System.err.println("SHA256 digest verification failed for " + debianPackage.packageName() + ", file may be corrupted. Deleted partial file.");
                    } else {
                        System.out.println("Skipping download for " + debianPackage.packageName() + " as it is already fully downloaded and SHA256 digest verified.");
                    }
                    return;
                }

                System.out.println("Resuming download for " + debianPackage.packageName() + " at " + downloadedSize + " bytes");
            }

            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                // Configure for partial download
                if (downloadedSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
                }
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("HTTP error code: " + responseCode + " for " + debianPackage.packageName() + " at " + downloadUrl);
                }

                // Get total file size
                long totalSize = connection.getContentLengthLong() + downloadedSize;

                try (InputStream inputStream = connection.getInputStream();
                     RandomAccessFile outputFile = new RandomAccessFile(savePath, "rw")) {
                    outputFile.seek(downloadedSize); // Resume from last position

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && isRunning.get() && !isPaused.get()) {
                        outputFile.write(buffer, 0, bytesRead);
                        downloadedSize += bytesRead;
                    }

                    // Check if download was paused
                    if (isPaused.get()) {
                        System.out.println("Download paused for " + debianPackage.packageName() + " at " + downloadedSize + " bytes");
                        return;
                    }

                    // Verify download completion
                    if (downloadedSize != totalSize) {
                        throw new IOException("Download incomplete: expected " + totalSize + " bytes, got " + downloadedSize);
                    }

                    // Verify SHA256 digest
                    if (!verifyDigest(savePath)) {
                        throw new IOException("SHA256 digest verification failed for " + debianPackage.packageName());
                    }

                    isCompleted = true;
                    System.out.println("Download completed for " + debianPackage.packageName());
                }
            } finally {
                connection.disconnect();
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Download timed out for " + debianPackage.packageName() + ": " + e.getMessage());
            // Partial file is not deleted to allow resumption later
            // Implement bad mirror handling if needed
        } catch (IOException e) {
            System.err.println("Download failed for " + debianPackage.packageName() + ": " + e.getMessage());
            e.printStackTrace();
            // Partial file is not deleted to allow resumption later
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Verifies the SHA256 digest of the downloaded file.
     * @param filePath Path to the downloaded file
     * @return true if digest matches, false otherwise
     * @throws IOException If an I/O error occurs
     */
    public boolean verifyDigest(String filePath) throws IOException {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    sha256.update(buffer, 0, bytesRead);
                }
            }

            byte[] computedHash = sha256.digest();
            String computedDigest = bytesToHex(computedHash);
            return computedDigest.equalsIgnoreCase(debianPackage.sha256digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Pauses the current download without deleting the partial file.
     */
    public void pauseDownload() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            System.out.println("Pausing download for " + debianPackage.packageName());
        }
    }

    /**
     * Resumes a paused download by starting a new thread.
     */
    public void resumeDownload() {
        if (isPaused.get() && !isRunning.get() && !isCompleted) {
            isPaused.set(false);
            Thread thread = new Thread(this);
            thread.start();
            System.out.println("Resuming download for " + debianPackage.packageName());
        } else if (isCompleted) {
            System.out.println("Download already completed for " + debianPackage.packageName());
        } else if (isRunning.get()) {
            System.out.println("Download already in progress for " + debianPackage.packageName());
        }
    }

    /**
     * Stops the download and keeps the partial file for later resumption.
     */
    public void stopDownload() {
        isRunning.set(false);
        isPaused.set(true); // Treat stop as a pause to preserve partial file
        System.out.println("Download stopped for " + debianPackage.packageName());
    }

    /**
     * Converts a byte array to hexadecimal string.
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Checks if a download is currently in progress.
     * @return true if downloading, false otherwise
     */
    public boolean isDownloading() {
        return isRunning.get() && !isPaused.get();
    }

    /**
     * Checks if the download is paused.
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Checks if the download is completed.
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Gets the current size of the downloaded file.
     * @return The size in bytes, or 0 if the file doesn't exist
     */
    public long getDownloadedSize() {
        try {
            Path saveFile = Paths.get(debianPackage.buildSavePath(configManager));
            return Files.exists(saveFile) ? Files.size(saveFile) : 0;
        } catch (IOException e) {
            System.err.println("Error checking downloaded size for " + debianPackage.packageName());
            return 0;
        }
    }
}
