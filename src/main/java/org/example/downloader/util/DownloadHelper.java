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

import org.example.downloader.DownloadLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for downloading files and querying file sizes from URLs.
 * Provides methods to query the total file size and the size of a partial download.
 */
public class DownloadHelper {

    static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    private static HttpURLConnection setupConnection(URL url, String method, String rangeHeader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (method != null) {
            connection.setRequestMethod(method);
        }
        if (rangeHeader != null) {
            connection.setRequestProperty("Range", rangeHeader);
        }
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.connect();
        return connection;
    }

    /**
     * Represents a download task with a URL and file path.
     */
    public static class Download {
        private final URL url;
        private final Path filePath;

        private float startTime;
        private boolean hasExited = false;
        private boolean isComplete = false;
        private boolean timedOut = false;
        private boolean httpError = false;

        private float speed = 0.0f;

        private int currentDownloadSize = 0;

        /**
         * Constructs a Download object with the specified URL and file path.
         *
         * @param url      The URL to download from.
         * @param filePath The path where the downloaded file will be saved.
         */
        public Download(URL url, Path filePath) {
            this.url = url;
            this.filePath = filePath;
        }

        /**
         * Gets the URL of the download.
         *
         * @return The URL to download from.
         */
        public URL getUrl() {
            return url;
        }

        /**
         * Gets the file path where the downloaded file will be saved.
         *
         * @return The path to the file.
         */
        public Path getFilePath() {
            return filePath;
        }

        /**
         * Stops the download process.
         */
        public void stop() {
            if(startTime == 0) {
                throw new IllegalStateException("Download has not started yet.");
            }
            hasExited = true;
        }

        /**
         * Gets the elapsed time since the download started.
         *
         * @return The elapsed time in seconds.
         */
        public float getTime() {
            return (System.currentTimeMillis() - startTime) / 1000.0f;
        }

        /**
         * Gets the current download speed in bytes per second.
         *
         * @return The download speed.
         */
        public float getSpeed() {
            return speed;
        }

        /**
         * Checks if the download is complete or partial.
         *
         * @return true if the download is complete, false otherwise.
         */
        public boolean isComplete() {
            return isComplete;
        }

        /**
         * Checks if the download has timed out.
         *
         * @return true if the download has timed out, false otherwise.
         */
        public boolean hasTimedOut() {
            return timedOut;
        }
        public boolean httpError() {
            return httpError;
        }


        private long totalSize = 0;

        public long totalByteSize() { return totalSize; }

        private long bytesDownloaded = 0;
        private long currentByte = 0;

        public long totalBytesDownloaded() { return bytesDownloaded + currentByte; }
    }

    /**
     * Continues a download from a specified URL, resuming from the last downloaded byte.
     *
     * @param download The Download object containing the URL and file path.
     * @return The number of bytes downloaded during this continuation.
     * @throws RuntimeException if an error occurs while continuing the download.
     */
    public static long continueDownload(Download download, DownloadLogger logger) {
        try {
            if (Files.exists(download.filePath)) {
                download.currentByte = Files.size(download.filePath);
            } else {
                Files.createDirectories(download.filePath.getParent());
                Files.createFile(download.filePath);
            }

            download.startTime = System.currentTimeMillis();
            HttpURLConnection connection = (HttpURLConnection) download.url.openConnection();
            try {
                if (download.currentByte > 0) {
                    connection.setRequestProperty("Range", "bytes=" + download.currentByte + "-");
                }
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(READ_TIMEOUT);

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("HTTP error code: " + responseCode + " for " + download.url);
                }

                download.totalSize = connection.getContentLengthLong() + download.currentByte;

                try (
                        InputStream inputStream = connection.getInputStream();
                        RandomAccessFile outputFile = new RandomAccessFile(download.filePath.toFile(), "rw")
                ) {
                    outputFile.seek(download.currentByte);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1 && !download.hasExited) {
                        outputFile.write(buffer, 0, bytesRead);
                        download.bytesDownloaded += bytesRead;
                        download.speed = download.bytesDownloaded / download.getTime();
                    }
                }
                if(download.bytesDownloaded + download.currentByte == download.totalSize) {
                    download.isComplete = true;
                }
            } catch (SocketTimeoutException e) {
                download.timedOut = true;
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            download.httpError = true;
            logger.getLogger().severe("Unexpected error when downloading " + download.url + ": " + e.getMessage());
        }
        return download.bytesDownloaded;
    }

    public static String downloadSmallData(URL url) {
        try {
            HttpURLConnection connection = setupConnection(url, "GET", null);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    return new String(inputStream.readAllBytes());
                }
            } else {
                throw new IOException("Failed to download data, HTTP response code: " + responseCode);
            }
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout while downloading data: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Error downloading data: " + url, e);
        }
    }

    /**
     * Queries the file size of a URL using a HEAD request.
     *
     * @param url The URL to query.
     * @return The size of the file in bytes.
     * @throws RuntimeException if an error occurs while querying the file size.
     */
    public static long queryUrlFileDownloadSize(URL url) {
        try {
            HttpURLConnection connection = setupConnection(url, "HEAD", null);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                return connection.getContentLengthLong();
            }
            throw new IOException("Failed to query file size, HTTP response code: " + responseCode);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout while querying file size: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Error querying file size: " + url, e);
        }
    }

    /**
     * Queries the partial download size of a URL starting from a specific byte.
     *
     * @param url       The URL to query.
     * @param startByte The byte from which to start the download.
     * @return The size of the file in bytes from the specified byte onward.
     * @throws RuntimeException if an error occurs while querying the partial file size.
     */
    public static long queryUrlPartialDownloadSize(URL url, long startByte) {
        try {
            String range = "bytes=" + startByte + "-";
            HttpURLConnection connection = setupConnection(url, "GET", range);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                return connection.getContentLengthLong();
            }
            throw new IOException("Failed to query partial file size, HTTP response code: " + responseCode);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout while querying partial file size: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Error querying partial file size: " + url, e);
        }
    }

    public static void main(String[] args) {
        try {
            URL url = new URL("https://download.oracle.com/java/19/archive/jdk-19.0.2_linux-aarch64_bin.tar.gz");
            long fileSize = queryUrlFileDownloadSize(url);
            System.out.println("File size: " + fileSize + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            URL url = new URL("https://download.oracle.com/java/19/archive/jdk-19.0.2_linux-aarch64_bin.tar.gz");
            long startByte = 10000000;
            long partialSize = queryUrlPartialDownloadSize(url, startByte);
            System.out.println("Partial file size from byte " + startByte + ": " + partialSize + " bytes");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long queryUrlFileDownloadSizeWithRedirect(URL url) {
        Set<String> visitedUrls = new HashSet<>(); // Track visited URLs to prevent circular redirects
        int maxRedirects = 8;
        int redirectCount = 0;

        try {
            while (redirectCount <= maxRedirects) {
                // Check for circular redirect
                String urlString = url.toString();
                if (!visitedUrls.add(urlString)) {
                    throw new IOException("Circular redirect detected for URL: " + urlString);
                }

                HttpURLConnection connection = setupConnection(url, "HEAD", null);
                connection.setInstanceFollowRedirects(false); // Disable automatic redirect following
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    return connection.getContentLengthLong();
                } else if (isRedirect(responseCode)) {
                    redirectCount++;
                    if (redirectCount > maxRedirects) {
                        throw new IOException("Maximum redirect limit (" + maxRedirects + ") exceeded for URL: " + url);
                    }

                    // Get the redirect location
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) {
                        throw new IOException("Redirect response code " + responseCode + " but no Location header found for URL: " + url);
                    }

                    // Resolve the new URL relative to the current URL
                    url = new URL(url, location);
                    connection.disconnect(); // Close the current connection
                    continue; // Follow the redirect
                } else {
                    throw new IOException("Failed to query file size, HTTP response code: " + responseCode + " for URL: " + url);
                }
            }
        } catch (SocketTimeoutException e) {
            throw new RuntimeException("Timeout while querying file size: " + url, e);
        } catch (IOException e) {
            throw new RuntimeException("Error querying file size: " + url, e);
        }
        //throw new IOException("Unexpected error while querying file size: " + url);
        return -1; // Return -1 if no valid size could be determined
    }

    // Helper method to check if the response code indicates a redirect
    private static boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM || // 301
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP || // 302
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||  // 303
                responseCode == 308; // Permanent Redirect
    }
}
