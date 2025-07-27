package org.example.downloader.java;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class FileDownloader {
    public static void downloadFile(String urlString, Path outputDir) throws IOException {
        Set<String> visitedUrls = new HashSet<>(); // Track visited URLs to prevent circular redirects
        int maxRedirects = 8;
        int redirectCount = 0;
        URL url = new URL(urlString);

        while (redirectCount <= maxRedirects) {
            // Check for circular redirect
            String currentUrl = url.toString();
            if (!visitedUrls.add(currentUrl)) {
                throw new IOException("Circular redirect detected for URL: " + currentUrl);
            }

            // Set up the connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET"); // Use GET to download the file
            connection.setInstanceFollowRedirects(false); // Handle redirects manually
            connection.setConnectTimeout(5000); // 5 seconds timeout
            connection.setReadTimeout(5000);

            // Get response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Extract filename from URL or Content-Disposition header
                String fileName = extractFileName(currentUrl, connection);
                Path outputPath = outputDir.resolve(fileName);

                // Download the file
                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("File downloaded to: " + outputPath);
                connection.disconnect();
                return; // Download complete
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

                // Resolve the new URL
                url = new URL(url, location);
                connection.disconnect();
                continue; // Follow the redirect
            } else {
                throw new IOException("Failed to download file, HTTP response code: " + responseCode + " for URL: " + url);
            }
        }
        throw new IOException("Unexpected error while downloading file: " + url);
    }

    public static long downloadFileSize(URL url) {
        Set<String> visitedUrls = new HashSet<>(); // Track visited URLs to prevent circular redirects
        int maxRedirects = 8;
        int redirectCount = 0;

        while (redirectCount <= maxRedirects) {
            // Check for circular redirect
            String currentUrl = url.toString();
            if (!visitedUrls.add(currentUrl)) {
                throw new RuntimeException("Circular redirect detected for URL: " + currentUrl);
            }

            // Set up the connection
            try {
                int responseCode;
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD"); // Use GET to download the file
                connection.setInstanceFollowRedirects(false); // Handle redirects manually
                connection.setConnectTimeout(5000); // 5 seconds timeout
                connection.setReadTimeout(5000);

                // Get response code
                responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    return connection.getContentLengthLong();
                } else if (isRedirect(responseCode)) {
                    redirectCount++;
                    if (redirectCount > maxRedirects) {
                        throw new RuntimeException("Maximum redirect limit (" + maxRedirects + ") exceeded for URL: " + url);
                    }

                    // Get the redirect location
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isEmpty()) {
                        throw new RuntimeException("Redirect response code " + responseCode + " but no Location header found for URL: " + url);
                    }

                    // Resolve the new URL
                    url = new URL(url, location);
                    connection.disconnect();
                    continue; // Follow the redirect
                } else {
                    throw new RuntimeException("Failed to download file, HTTP response code: " + responseCode + " for URL: " + url);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error connecting to URL: " + url, e);
            }
        }
        throw new RuntimeException("Unexpected error while downloading file: " + url);
    }

    // Helper method to check if the response code indicates a redirect
    private static boolean isRedirect(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM || // 301
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP || // 302
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||  // 303
                responseCode == 308; // Permanent Redirect
    }

    // Helper method to extract the filename from the URL or Content-Disposition header
    private static String extractFileName(String url, HttpURLConnection connection) {
        // Try to get filename from Content-Disposition header
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        if (contentDisposition != null) {
            String[] parts = contentDisposition.split("filename=");
            if (parts.length > 1) {
                String fileName = parts[1].replaceAll("[\"']", "").trim();
                if (!fileName.isEmpty()) {
                    return fileName;
                }
            }
        }

        // Fallback to extracting filename from URL
        String path = url.substring(url.lastIndexOf('/') + 1);
        return path.isEmpty() ? "downloaded_file" : path;
    }

    public static void main(String[] args) {
        String url = "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz";
        Path outputDir = Paths.get(System.getProperty("user.dir")); // Current working directory

        try {
            // Ensure output directory exists
            Files.createDirectories(outputDir);
            downloadFile(url, outputDir);
        } catch (IOException e) {
            System.err.println("Error downloading file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
