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
package org.example.downloader.exp;

import org.example.downloader.ConfigManager;
import org.example.downloader.DebianPackage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebianWorker {
    private final DebianPackage debianPackage;
    private final ConfigManager configManager;
    private final AtomicBoolean isRunning;
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds

    public DebianWorker(DebianPackage debianPackage, ConfigManager configManager) {
        this.debianPackage = debianPackage;
        this.configManager = configManager;
        this.isRunning = new AtomicBoolean(false);
    }

    /**
     * Downloads the Debian package with support for partial downloads and continuous operation.
     * @param baseUrl The base URL for the Debian repository
     * @throws IOException If an I/O error occurs during download
     * @throws InterruptedException If the download is interrupted
     */
    public void downloadPackage(String baseUrl) throws IOException, InterruptedException {
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Download already in progress for " + debianPackage.packageName);
        }

        try {
            String downloadUrl = debianPackage.buildDownloadUrl(baseUrl);
            String savePath = debianPackage.buildSavePath(configManager);
            Path saveFile = Paths.get(savePath);
            Files.createDirectories(saveFile.getParent());

            // Check for existing partial download
            long downloadedSize = 0;
            if (Files.exists(saveFile)) {
                downloadedSize = Files.size(saveFile);
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
                    throw new IOException("HTTP error code: " + responseCode);
                }

                // Get total file size
                long totalSize = connection.getContentLengthLong() + downloadedSize;

                try (InputStream inputStream = connection.getInputStream();
                     RandomAccessFile outputFile = new RandomAccessFile(savePath, "rw")) {
                    outputFile.seek(downloadedSize); // Resume from last position

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && isRunning.get()) {
                        outputFile.write(buffer, 0, bytesRead);
                        downloadedSize += bytesRead;
                    }

                    // Verify download completion
                    if (downloadedSize != totalSize) {
                        cleanupFailedDownload(savePath);
                        throw new IOException("Download incomplete: expected " + totalSize + " bytes, got " + downloadedSize);
                    }

                    // Verify SHA256 digest
                    if (!verifyDigest(savePath)) {
                        cleanupFailedDownload(savePath);
                        throw new IOException("SHA256 digest verification failed for " + debianPackage.packageName);
                    }
                }
            } finally {
                connection.disconnect();
            }
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
            return computedDigest.equalsIgnoreCase(debianPackage.sha256digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Stops the current download and cleans up partial files.
     */
    public void stopDownload() {
        isRunning.set(false);
        cleanupFailedDownload(debianPackage.buildSavePath(configManager));
    }

    /**
     * Deletes the specified file if it exists.
     * @param filePath Path to the file to delete
     */
    private void cleanupFailedDownload(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Failed to clean up partial download: " + filePath);
            e.printStackTrace();
        }
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
        return isRunning.get();
    }
}
