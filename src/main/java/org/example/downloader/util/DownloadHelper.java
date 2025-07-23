package org.example.downloader.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Utility class for downloading files and querying file sizes from URLs.
 * Provides methods to query the total file size and the size of a partial download.
 */
public class DownloadHelper {

    private static HttpURLConnection setupConnection(URL url, String method, String rangeHeader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (method != null) {
            connection.setRequestMethod(method);
        }
        if (rangeHeader != null) {
            connection.setRequestProperty("Range", rangeHeader);
        }
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        connection.connect();
        return connection;
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
}
