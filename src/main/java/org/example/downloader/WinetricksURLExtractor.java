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
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("w_download\\s+(https?://[^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)");
    private static final Pattern VERB_PATTERN = Pattern.compile("w_metadata\\s+([^\\s]+)\\s+([^\\s]+)");

    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            Set<String> targetVerbs = new HashSet<>();
            Set<String> targetCategories = new HashSet<>();
            parseArguments(args, targetVerbs, targetCategories);

            // Step 1: Download the Winetricks script
            String winetricksFile = downloadWinetricksScript();

            // Step 2: Extract verb categories and URL data
            Map<String, String> verbCategories = extractVerbCategories(winetricksFile);
            Set<String[]> urlData = extractURLsAndChecksums(winetricksFile, targetVerbs, targetCategories, verbCategories);

            // Step 3: Download files to Winetricks cache directory
            if (urlData.isEmpty()) {
                System.out.println("No matching verbs or categories found to download.");
            } else {
                downloadFiles(urlData, verbCategories);
                System.out.println("All URLs extracted and files downloaded to " + CACHE_DIR);
            }
        } catch (IOException | SecurityException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Parse command-line arguments for verbs and categories
    private static void parseArguments(String[] args, Set<String> targetVerbs, Set<String> targetCategories) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--verbs") && i + 1 < args.length) {
                String[] verbs = args[++i].split(",");
                targetVerbs.addAll(Arrays.asList(verbs));
            } else if (args[i].equals("--categories") && i + 1 < args.length) {
                String[] categories = args[++i].split(",");
                targetCategories.addAll(Arrays.asList(categories));
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

    // Extract URLs, filenames, and checksums, filtered by verbs and/or categories
    private static Set<String[]> extractURLsAndChecksums(String winetricksFile, Set<String> targetVerbs,
                                                         Set<String> targetCategories, Map<String, String> verbCategories) throws IOException {
        Set<String[]> urlData = new HashSet<>();
        String currentVerb = null;

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
                if (downloadMatcher.find() && currentVerb != null) {
                    // Apply verb and category filters
                    boolean verbMatch = targetVerbs.isEmpty() || targetVerbs.contains(currentVerb);
                    boolean categoryMatch = targetCategories.isEmpty() || targetCategories.contains(verbCategories.getOrDefault(currentVerb, ""));

                    if (verbMatch && categoryMatch) {
                        String url = downloadMatcher.group(1);
                        String checksum = downloadMatcher.group(2);
                        String filename = downloadMatcher.group(3);
                        urlData.add(new String[]{url, filename, checksum, currentVerb});
                    }
                }
            }
        }
        return urlData;
    }

    // Download files to the Winetricks cache directory with progress, resume, and checksum verification
    private static void downloadFiles(Set<String[]> urlData, Map<String, String> verbCategories) throws IOException {
        Path cachePath = Paths.get(CACHE_DIR);
        Files.createDirectories(cachePath);

        for (String[] data : urlData) {
            String url = data[0];
            String filename = data[1];
            String expectedChecksum = data[2];
            String verb = data[3];

            // Get category from verb, default to "misc" if not found
            String category = verbCategories.getOrDefault(verb, "misc");
            Path categoryPath = cachePath.resolve(category);
            Files.createDirectories(categoryPath);

            Path filePath = categoryPath.resolve(filename);
            System.out.println("Downloading " + url + " to " + filePath);

            // Check if file exists and is complete
            long existingSize = Files.exists(filePath) ? Files.size(filePath) : 0;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                long totalSize = conn.getContentLengthLong();

                // Resume download if file is incomplete
                if (existingSize > 0 && existingSize < totalSize) {
                    conn.setRequestProperty("Range", "bytes=" + existingSize + "-");
                } else if (existingSize == totalSize && verifyChecksum(filePath, expectedChecksum)) {
                    System.out.println("File already exists and checksum matches: " + filename);
                    continue;
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
                }
            } catch (IOException e) {
                System.err.println("Failed to download " + url + ": " + e.getMessage());
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
