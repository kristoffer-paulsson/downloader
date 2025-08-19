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
    // Multiple regex patterns for different w_download formats
    private static final Pattern[] DOWNLOAD_PATTERNS = {
            // Standard: w_download <url> <checksum> <filename>
            Pattern.compile("w_download\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+))\\s+([^\\s]+)(?:\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+)))?(?:\\s+.*)?"),
            // w_download_to: w_download_to <path> <url> <checksum> <filename>
            Pattern.compile("w_download_to\\s+[^\\s]+\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+))\\s+([^\\s]+)(?:\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+)))?(?:\\s+.*)?"),
            // Minimal: w_download <url> <checksum>
            Pattern.compile("w_download\\s+(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+))\\s+([^\\s]+)(?:\\s*$|\\s+[^\"'].*)")
    };
    private static final Pattern VERB_PATTERN = Pattern.compile("w_metadata\\s+([^\\s]+)\\s+([^\\s]+)");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(\\w+)=(?:\"([^\"]+)\"|'([^']+)'|([^\\s]+))");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("w_call\\s+([^\\s]+)");
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\b(for|while)\\b.*\\bdo\\b");
    private static final Pattern END_LOOP_PATTERN = Pattern.compile("\\bdone\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*#");

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

            // Step 2: Extract verb categories, variables, and URL data
            Map<String, String> verbCategories = extractVerbCategories(winetricksFile);
            Map<String, Map<String, String>> contextVariables = extractContextVariables(winetricksFile, verbCategories.keySet());
            System.out.println("Parsed " + verbCategories.size() + " verbs.");
            System.out.println("Parsed variables for " + contextVariables.size() + " contexts.");
            if (!downloadAll && !listAll) {
                validateArguments(targetVerbs, targetCategories, verbCategories);
            }
            Set<String[]> urlData = extractURLsAndChecksums(winetricksFile, targetVerbs, targetCategories, verbCategories, contextVariables, downloadAll || listAll);
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

    // Extract variables with context (global, per-verb, and in loops)
    private static Map<String, Map<String, String>> extractContextVariables(String winetricksFile, Set<String> verbs) throws IOException {
        Map<String, Map<String, String>> contextVariables = new HashMap<>();
        Map<String, String> globalVariables = new HashMap<>();
        Map<String, String> currentVerbVariables = new HashMap<>();
        String currentVerb = null;
        int loopDepth = 0;
        List<String> loopVars = new ArrayList<>(); // Track loop iteration variables

        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments
                if (COMMENT_PATTERN.matcher(line).find()) {
                    continue;
                }

                // Check for verb definition
                Matcher verbMatcher = VERB_PATTERN.matcher(line);
                if (verbMatcher.find()) {
                    if (currentVerb != null && !currentVerbVariables.isEmpty()) {
                        contextVariables.put(currentVerb, new HashMap<>(currentVerbVariables));
                        currentVerbVariables.clear();
                    }
                    currentVerb = verbMatcher.group(1);
                    loopDepth = 0;
                    loopVars.clear();
                    continue;
                }

                // Track loop depth and variables
                Matcher loopMatcher = LOOP_PATTERN.matcher(line);
                if (loopMatcher.find()) {
                    loopDepth++;
                    // Extract loop variable (e.g., "for lang in en fr ...")
                    String[] parts = line.split("\\s+");
                    if (parts.length > 2 && parts[0].equals("for")) {
                        loopVars.add(parts[1]); // e.g., "lang"
                    }
                    continue;
                } else if (END_LOOP_PATTERN.matcher(line).find()) {
                    loopDepth = Math.max(0, loopDepth - 1);
                    if (!loopVars.isEmpty()) {
                        loopVars.remove(loopVars.size() - 1);
                    }
                    continue;
                }

                // Check for variable assignments
                Matcher varMatcher = VARIABLE_PATTERN.matcher(line);
                if (varMatcher.find()) {
                    String varName = varMatcher.group(1);
                    String varValue = varMatcher.group(2) != null ? varMatcher.group(2) :
                            varMatcher.group(3) != null ? varMatcher.group(3) : varMatcher.group(4);
                    if (currentVerb != null) {
                        if (loopDepth > 0) {
                            contextVariables.computeIfAbsent(currentVerb + "_loop_" + loopDepth, k -> new HashMap<>()).put(varName, varValue);
                            // Handle loop iteration variables (e.g., lang=en, fr)
                            if (!loopVars.isEmpty() && varName.equals(loopVars.get(loopVars.size() - 1))) {
                                for (String val : varValue.split("\\s+")) {
                                    contextVariables.computeIfAbsent(currentVerb + "_loop_" + loopDepth, k -> new HashMap<>()).put(varName, val);
                                }
                            }
                        } else {
                            currentVerbVariables.put(varName, varValue);
                        }
                    } else {
                        globalVariables.put(varName, varValue);
                    }
                }
            }
            // Save variables for the last verb
            if (currentVerb != null && !currentVerbVariables.isEmpty()) {
                contextVariables.put(currentVerb, new HashMap<>(currentVerbVariables));
            }
        }
        contextVariables.put("global", globalVariables);
        return contextVariables;
    }

    // Extract URLs, filenames, and checksums
    private static Set<String[]> extractURLsAndChecksums(String winetricksFile, Set<String> targetVerbs,
                                                         Set<String> targetCategories, Map<String, String> verbCategories,
                                                         Map<String, Map<String, String>> contextVariables, boolean processAll) throws IOException {
        Set<String[]> urlData = new HashSet<>();
        String currentVerb = null;
        int downloadCount = 0;
        int loopDepth = 0;
        String lastComment = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments but track the last one for dynamic URL replacement
                if (COMMENT_PATTERN.matcher(line).find()) {
                    lastComment = line.trim();
                    continue;
                }

                // Check for verb definition
                Matcher verbMatcher = VERB_PATTERN.matcher(line);
                if (verbMatcher.find()) {
                    currentVerb = verbMatcher.group(1);
                    loopDepth = 0;
                    lastComment = null;
                    continue;
                }

                // Track loop depth
                if (LOOP_PATTERN.matcher(line).find()) {
                    loopDepth++;
                    continue;
                } else if (END_LOOP_PATTERN.matcher(line).find()) {
                    loopDepth = Math.max(0, loopDepth - 1);
                    continue;
                }

                // Check for function calls (e.g., w_call)
                Matcher funcMatcher = FUNCTION_PATTERN.matcher(line);
                if (funcMatcher.find() && currentVerb != null) {
                    String calledVerb = funcMatcher.group(1);
                    urlData.addAll(extractFunctionURLs(winetricksFile, calledVerb, currentVerb, targetVerbs, targetCategories, verbCategories, contextVariables, processAll));
                }

                // Check for download calls
                for (Pattern pattern : DOWNLOAD_PATTERNS) {
                    Matcher downloadMatcher = pattern.matcher(line);
                    if (downloadMatcher.find()) {
                        downloadCount++;
                        if (currentVerb != null) {
                            boolean verbMatch = processAll || targetVerbs.isEmpty() || targetVerbs.contains(currentVerb);
                            boolean categoryMatch = processAll || targetCategories.isEmpty() || targetCategories.contains(verbCategories.getOrDefault(currentVerb, ""));
                            if (verbMatch && categoryMatch) {
                                String rawUrl = downloadMatcher.group(1) != null ? downloadMatcher.group(1) :
                                        downloadMatcher.group(2) != null ? downloadMatcher.group(2) : downloadMatcher.group(3);
                                String checksum = downloadMatcher.group(4);
                                String filename = null;
                                if (downloadMatcher.groupCount() >= 5 && downloadMatcher.group(5) != null) {
                                    filename = downloadMatcher.group(5);
                                } else if (downloadMatcher.groupCount() >= 6 && downloadMatcher.group(6) != null) {
                                    filename = downloadMatcher.group(6);
                                } else if (downloadMatcher.groupCount() >= 7 && downloadMatcher.group(7) != null) {
                                    filename = downloadMatcher.group(7);
                                } else {
                                    filename = extractFilenameFromURL(rawUrl);
                                }

                                // Handle dynamic URL replacement (e.g., dotnet11sp1)
                                String resolvedUrl = resolveVariables(rawUrl, contextVariables, currentVerb, loopDepth);
                                if (resolvedUrl == null || resolvedUrl.equals("downloads")) {
                                    if (lastComment != null && lastComment.contains("instead we change the link")) {
                                        // Try to find the replacement URL in the next few lines
                                        String replacementUrl = findReplacementURL(winetricksFile, reader, currentVerb);
                                        if (replacementUrl != null) {
                                            resolvedUrl = resolveVariables(replacementUrl, contextVariables, currentVerb, loopDepth);
                                        }
                                    }
                                    if (resolvedUrl == null || resolvedUrl.equals("downloads")) {
                                        System.err.println("Skipping unresolved or invalid URL for verb " + currentVerb + ": " + rawUrl + " (line: " + line + ")");
                                        continue;
                                    }
                                }

                                filename = sanitizeFilename(filename, resolvedUrl, currentVerb);
                                urlData.add(new String[]{resolvedUrl, filename, checksum, currentVerb});
                            }
                        }
                        break;
                    }
                }
                lastComment = null;
            }
        }
        System.out.println("Found " + downloadCount + " w_download calls in script.");
        return urlData;
    }

    // Find replacement URL after a commented w_download
    private static String findReplacementURL(String winetricksFile, BufferedReader currentReader, String currentVerb) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            boolean foundVerb = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("w_metadata " + currentVerb + " ")) {
                    foundVerb = true;
                    continue;
                }
                if (foundVerb && !COMMENT_PATTERN.matcher(line).find()) {
                    for (Pattern pattern : DOWNLOAD_PATTERNS) {
                        Matcher downloadMatcher = pattern.matcher(line);
                        if (downloadMatcher.find()) {
                            return downloadMatcher.group(1) != null ? downloadMatcher.group(1) :
                                    downloadMatcher.group(2) != null ? downloadMatcher.group(2) : downloadMatcher.group(3);
                        }
                    }
                }
                if (foundVerb && line.startsWith("w_metadata ")) {
                    break;
                }
            }
        }
        return null;
    }

    // Extract URLs from function calls (w_call)
    private static Set<String[]> extractFunctionURLs(String winetricksFile, String calledVerb, String parentVerb,
                                                     Set<String> targetVerbs, Set<String> targetCategories,
                                                     Map<String, String> verbCategories, Map<String, Map<String, String>> contextVariables,
                                                     boolean processAll) throws IOException {
        Set<String[]> urlData = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            boolean inFunction = false;
            int loopDepth = 0;
            while ((line = reader.readLine()) != null) {
                // Skip comments
                if (COMMENT_PATTERN.matcher(line).find()) {
                    continue;
                }

                if (line.startsWith("w_metadata " + calledVerb + " ")) {
                    inFunction = true;
                    loopDepth = 0;
                    continue;
                }
                if (inFunction && line.startsWith("w_metadata ")) {
                    inFunction = false;
                    continue;
                }
                if (inFunction) {
                    // Track loop depth in function
                    if (LOOP_PATTERN.matcher(line).find()) {
                        loopDepth++;
                        continue;
                    } else if (END_LOOP_PATTERN.matcher(line).find()) {
                        loopDepth = Math.max(0, loopDepth - 1);
                        continue;
                    }

                    for (Pattern pattern : DOWNLOAD_PATTERNS) {
                        Matcher downloadMatcher = pattern.matcher(line);
                        if (downloadMatcher.find()) {
                            boolean verbMatch = processAll || targetVerbs.isEmpty() || targetVerbs.contains(parentVerb);
                            boolean categoryMatch = processAll || targetCategories.isEmpty() || targetCategories.contains(verbCategories.getOrDefault(parentVerb, ""));
                            if (verbMatch && categoryMatch) {
                                String rawUrl = downloadMatcher.group(1) != null ? downloadMatcher.group(1) :
                                        downloadMatcher.group(2) != null ? downloadMatcher.group(2) : downloadMatcher.group(3);
                                String checksum = downloadMatcher.group(4);
                                String filename = null;
                                if (downloadMatcher.groupCount() >= 5 && downloadMatcher.group(5) != null) {
                                    filename = downloadMatcher.group(5);
                                } else if (downloadMatcher.groupCount() >= 6 && downloadMatcher.group(6) != null) {
                                    filename = downloadMatcher.group(6);
                                } else if (downloadMatcher.groupCount() >= 7 && downloadMatcher.group(7) != null) {
                                    filename = downloadMatcher.group(7);
                                } else {
                                    filename = extractFilenameFromURL(rawUrl);
                                }

                                String resolvedUrl = resolveVariables(rawUrl, contextVariables, parentVerb, loopDepth);
                                if (resolvedUrl == null || resolvedUrl.equals("downloads")) {
                                    System.err.println("Skipping unresolved or invalid URL for verb " + parentVerb + " (called from " + calledVerb + "): " + rawUrl + " (line: " + line + ")");
                                    continue;
                                }

                                filename = sanitizeFilename(filename, resolvedUrl, parentVerb);
                                urlData.add(new String[]{resolvedUrl, filename, checksum, parentVerb});
                            }
                        }
                    }
                }
            }
        }
        return urlData;
    }

    // Resolve variables in URLs
    private static String resolveVariables(String rawUrl, Map<String, Map<String, String>> contextVariables, String verb, int loopDepth) {
        String resolvedUrl = rawUrl.replaceAll("[\"']", "");
        // Try loop-specific variables
        for (int i = loopDepth; i > 0; i--) {
            Map<String, String> loopVars = contextVariables.getOrDefault(verb + "_loop_" + i, new HashMap<>());
            for (Map.Entry<String, String> entry : loopVars.entrySet()) {
                resolvedUrl = resolvedUrl.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
                resolvedUrl = resolvedUrl.replace("$" + entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
        }
        // Try verb-specific variables
        Map<String, String> verbVars = contextVariables.getOrDefault(verb, new HashMap<>());
        for (Map.Entry<String, String> entry : verbVars.entrySet()) {
            resolvedUrl = resolvedUrl.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            resolvedUrl = resolvedUrl.replace("$" + entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        // Fall back to global variables
        Map<String, String> globalVars = contextVariables.getOrDefault("global", new HashMap<>());
        for (Map.Entry<String, String> entry : globalVars.entrySet()) {
            resolvedUrl = resolvedUrl.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            resolvedUrl = resolvedUrl.replace("$" + entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        // Handle positional parameters
        resolvedUrl = resolvedUrl.replace("${1}", verb).replace("$1", verb);
        // Fallback for winrar
        if (verb.equals("winrar")) {
            // Try multiple possible filenames
            String[] possibleExeNames = {
                    "winrar-x64-511.exe", "winrar-x32-511.exe",
                    "winrar-x64-621.exe", "winrar-x32-621.exe",
                    // Add more based on Winetricks script or common patterns
            };
            resolvedUrl = resolvedUrl.replace("${_W_winrar_url}", "https://www.rarlab.com/rar");
            for (String exe : possibleExeNames) {
                String testUrl = resolvedUrl.replace("${_W_winrar_exe}", exe);
                try {
                    new URL(testUrl).toURI();
                    return testUrl;
                } catch (Exception ignored) {
                }
            }
        }
        try {
            new URL(resolvedUrl).toURI();
            return resolvedUrl;
        } catch (Exception e) {
            return null;
        }
    }

    // Extract filename from URL as a fallback
    private static String extractFilenameFromURL(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        return lastPart.contains("?") ? lastPart.substring(0, lastPart.indexOf("?")) : lastPart;
    }

    // Sanitize filename
    private static String sanitizeFilename(String filename, String url, String verb) {
        try {
            filename = URLDecoder.decode(filename, "UTF-8");
            filename = filename.replaceAll("[\"']", "").replaceAll("[^a-zA-Z0-9._-]", "_");
            if (filename.contains("$") || filename.equals("_1") || filename.isEmpty()) {
                filename = extractFilenameFromURL(url);
                if (filename.isEmpty()) {
                    filename = verb + "_file";
                }
            }
            return filename.isEmpty() ? verb + "_file" : filename;
        } catch (UnsupportedEncodingException e) {
            filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            return filename.isEmpty() ? verb + "_file" : filename;
        }
    }

    // Download files to the Winetricks cache directory
    private static void downloadFiles(Set<String[]> urlData, Map<String, String> verbCategories) throws IOException {
        Path cachePath = Paths.get(CACHE_DIR);
        Files.createDirectories(cachePath);

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

            String category = verbCategories.getOrDefault(verb, "misc");
            Path categoryPath = cachePath.resolve(category);
            Files.createDirectories(categoryPath);

            Path filePath = categoryPath.resolve(filename);
            System.out.println("Downloading " + url + " to " + filePath);

            int maxRetries = 3;
            int attempt = 0;
            boolean success = false;

            while (attempt < maxRetries && !success) {
                attempt++;
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    long totalSize = conn.getContentLengthLong();

                    long existingSize = Files.exists(filePath) ? Files.size(filePath) : 0;
                    if (existingSize > 0 && existingSize == totalSize && verifyChecksum(filePath, expectedChecksum)) {
                        System.out.println("File already exists and checksum matches: " + filename);
                        success = true;
                        downloadedURLs.add(url);
                        continue;
                    }

                    if (existingSize > 0 && existingSize < totalSize) {
                        conn.setRequestProperty("Range", "bytes=" + existingSize + "-");
                    }

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
                        System.out.println();
                    }

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

    // Verify SHA256 checksum
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