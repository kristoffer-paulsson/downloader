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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinetricksURLExtractor {
    private static final String WINETRICKS_URL = "https://raw.githubusercontent.com/Winetricks/winetricks/master/src/winetricks";
    private static final String CACHE_DIR = "cache-winetricks";
    // Updated regex to handle more w_download variations
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("w_download(?:_to)?\\s+([^\\s]+)\\s+([^\\s]+)(?:\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+)))?(?:\\s+.*)?");
    private static final Pattern VERB_PATTERN = Pattern.compile("w_metadata\\s+([^\\s]+)\\s+([^\\s]+)");

    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            Set<String> targetVerbs = new HashSet<>();
            Set<String> targetCategories = new HashSet<>();
            boolean downloadAll = false;
            boolean listAll = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--all")) {
                    downloadAll = true;
                } else if (args[i].equals("--list-all")) {
                    listAll = true;
                } else if (args[i].equals("--verbs") && i + 1 < args.length) {
                    String[] verbs = args[++i].split(",");
                    targetVerbs.addAll(Arrays.asList(verbs));
                } else if (args[i].equals("--categories") && i + 1 < args.length) {
                    String[] categories = args[++i].split(",");
                    targetCategories.addAll(Arrays.asList(categories));
                }
            }

            // Step 1: Download the Winetricks script
            String winetricksFile = downloadWinetricksScript();

            // Step 2: Extract verb categories and URL data
            Map<String, String> verbCategories = extractVerbCategories(winetricksFile);
            System.out.println("Parsed " + verbCategories.size() + " verbs.");
            if (!downloadAll && !listAll) {
                validateArguments(targetVerbs, targetCategories, verbCategories);
            }
            Set<String[]> urlData = extractURLsAndChecksums(winetricksFile, targetVerbs, targetCategories, verbCategories, downloadAll || listAll);
            System.out.println("Extracted " + urlData.size() + " URLs.");

            // Step 3: Process based on arguments
            if (urlData.isEmpty()) {
                System.out.println("No matching verbs or categories found.");
            } else if (listAll) {
                // List all URLs
                System.out.println("Extracted URLs:");
                for (String[] data : urlData) {
                    String url = data[0];
                    String filename = data[1];
                    String checksum = data[2];
                    String verb = data[3];
                    String category = verbCategories.getOrDefault(verb, "misc");
                    System.out.printf("Verb: %s, Category: %s, Filename: %s, URL: %s, Checksum: %s%n",
                            verb, category, filename, url, checksum);
                }
            } else {
                // Download files
                downloadFiles(urlData, verbCategories);
                System.out.println("All URLs extracted and files downloaded to " + CACHE_DIR);
            }
        } catch (IOException | SecurityException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Validate verbs and categories
    private static void validateArguments(Set<String> targetVerbs, Set<String> targetCategories, Map<String, String> verbCategories) {
        for (String verb : targetVerbs) {
            if (!verbCategories.containsKey(verb)) {
                System.err.println("Warning: Verb '" + verb + "' not found in Winetricks script.");
            }
        }
        Set<String> validCategories = new HashSet<>(verbCategories.values());
        for (String category : targetCategories) {
            if (!validCategories.contains(category)) {
                System.err.println("Warning: Category '" + category + "' not found in Winetricks script.");
            }
        }
    }

    // Download the Winetricks script
    private static String downloadWinetricksScript() throws IOException {
        String tempFile = "winetricks_temp";
        URL url = new URL(WINETRICKS_URL);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return tempFile;
    }

    // Extract verb categories from w_metadata calls
    private static Map<String, String> extractVerbCategories(String winetricksFile) throws IOException {
        Map<String, String> verbCategories = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = VERB_PATTERN.matcher(line);
                if (matcher.find()) {
                    String verb = matcher.group(1);
                    String category = matcher.group(2);
                    verbCategories.put(verb, category);
                }
            }
        }
        return verbCategories;
    }

    // Extract URLs, filenames, and checksums, filtered by verbs, categories, or all
    private static Set<String[]> extractURLsAndChecksums(String winetricksFile, Set<String> targetVerbs,
                                                         Set<String> targetCategories, Map<String, String> verbCategories, boolean processAll) throws IOException {
        Set<String[]> urlData = new HashSet<>();
        String currentVerb = null;
        int downloadCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Check for verb definition
                Matcher verbMatcher = VERB_PATTERN.matcher(line);
                if (verbMatcher.find()) {
                    currentVerb = verbMatcher.group(1);
                    continue;
                }

                // Check for download calls
                Matcher downloadMatcher = DOWNLOAD_PATTERN.matcher(line);
                if (downloadMatcher.find()) {
                    downloadCount++;
                    if (currentVerb != null) {
                        // Apply verb and category filters unless processing all
                        boolean verbMatch = processAll || targetVerbs.isEmpty() || targetVerbs.contains(currentVerb);
                        boolean categoryMatch = processAll || targetCategories.isEmpty() || targetCategories.contains(verbCategories.getOrDefault(currentVerb, ""));
                        if (verbMatch && categoryMatch) {
                            String url = downloadMatcher.group(1);
                            String checksum = downloadMatcher.group(2);
                            String filename = downloadMatcher.group(3) != null ? downloadMatcher.group(3) :
                                    downloadMatcher.group(4) != null ? downloadMatcher.group(4) :
                                            downloadMatcher.group(5) != null ? downloadMatcher.group(5) : extractFilenameFromURL(url);
                            filename = sanitizeFilename(filename);
                            urlData.add(new String[]{url, filename, checksum, currentVerb});
                        }
                    }
                }
            }
        }
        System.out.println("Found " + downloadCount + " w_download calls in script.");
        return urlData;
    }

    // Extract filename from URL as a fallback
    private static String extractFilenameFromURL(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.contains("?") ? lastPart.substring(0, lastPart.indexOf("?")) : lastPart;
    }

    // Sanitize filename by decoding URL-encoded characters and handling variables
    private static String sanitizeFilename(String filename) {
        try {
            // Decode URL-encoded characters (e.g., %20 to space)
            filename = URLDecoder.decode(filename, "UTF-8");
            // Remove quotes and replace invalid characters
            filename = filename.replaceAll("[\"']", "").replaceAll("[^a-zA-Z0-9._-]", "_");
            // Handle variable placeholders (e.g., ${file1})
            if (filename.contains("$")) {
                filename = filename.replaceAll("\\$\\{[^}]+\\}", "file");
            }
            // Ensure non-empty filename
            return filename.isEmpty() ? "downloaded_file" : filename;
        } catch (UnsupportedEncodingException e) {
            return filename.replaceAll("[^a-zA-Z0-9._-]", "_").isEmpty() ? "downloaded_file" : filename;
        }
    }

    // Download files to the Winetricks cache directory with progress, resume, and checksum verification
    private static void downloadFiles(Set<String[]> urlData, Map<String, String> verbCategories) throws IOException {
        Path cachePath = Paths.get(CACHE_DIR);
        Files.createDirectories(cachePath);

        // Track unique URLs to avoid duplicate downloads
        Set<String> downloadedURLs = new HashSet<>();

        for (String[] data : urlData) {
            String url = data[0];
            String filename = data[1];
            String expectedChecksum = data[2];
            String verb = data[3];

            if (downloadedURLs.contains(url)) {
                System.out.println("Skipping duplicate URL: " + url);
                continue;
            }

            // Get category from verb, default to "misc" if not found
            String category = verbCategories.getOrDefault(verb, "misc");
            Path categoryPath = cachePath.resolve(category);
            Files.createDirectories(categoryPath);

            Path filePath = categoryPath.resolve(filename);
            System.out.println("Downloading " + url + " to " + filePath);

            // Retry logic (up to 3 attempts)
            int maxRetries = 3;
            int attempt = 0;
            boolean success = false;

            while (attempt < maxRetries && !success) {
                attempt++;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    long totalSize = conn.getContentLengthLong();

                    // Check if file exists and is complete
                    long existingSize = Files.exists(filePath) ? Files.size(filePath) : 0;
                    if (existingSize > 0 && existingSize == totalSize && verifyChecksum(filePath, expectedChecksum)) {
                        System.out.println("File already exists and checksum matches: " + filename);
                        success = true;
                        downloadedURLs.add(url);
                        continue;
                    }

                    // Resume download if file is incomplete
                    if (existingSize > 0 && existingSize < totalSize) {
                        conn.setRequestProperty("Range", "bytes=" + existingSize + "-");
                    }

                    // Download with progress feedback
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(filePath.toString(), existingSize > 0)) {
                        byte[] buffer = new byte[8192];
                        long downloaded = existingSize;
                        int bytesRead;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            downloaded += bytesRead;
                            if (totalSize > 0) {
                                int progress = (int) ((downloaded * 100) / totalSize);
                                System.out.print("\rProgress: " + progress + "%");
                            }
                        }
                        System.out.println(); // Newline after progress
                    }

                    // Verify checksum
                    if (!verifyChecksum(filePath, expectedChecksum)) {
                        System.err.println("Checksum mismatch for " + filename + ". Deleting file.");
                        Files.deleteIfExists(filePath);
                    } else {
                        System.out.println("Checksum verified for " + filename);
                        success = true;
                        downloadedURLs.add(url);
                    }
                } catch (IOException e) {
                    System.err.println("Attempt " + attempt + " failed for " + url + ": " + e.getMessage());
                    if (attempt == maxRetries) {
                        System.err.println("Max retries reached for " + url + ". Skipping.");
                    }
                }
            }
        }
    }

    // Verify SHA256 checksum of a file
    private static boolean verifyChecksum(Path filePath, String expectedChecksum) throws IOException {
        if (!Files.exists(filePath)) {
            return false;
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 not supported", e);
        }

        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder computedChecksum = new StringBuilder();
        for (byte b : hashBytes) {
            computedChecksum.append(String.format("%02x", b));
        }

        return computedChecksum.toString().equalsIgnoreCase(expectedChecksum);
    }
}