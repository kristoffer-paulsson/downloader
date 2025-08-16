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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyDownloader {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private static final String OUTPUT_DIR = "downloaded-jars";
    private static final String CACHE_DIR = OUTPUT_DIR + "/custom-cache";
    private static final Set<String> PROCESSED_ARTIFACTS = new HashSet<>(); // Track processed artifacts
    private static final Queue<String> POM_QUEUE = new ArrayDeque<>(); // Queue for POM files
    private static final List<String> COMMON_EXTENSIONS = Arrays.asList("jar", "war", "zip", "bundle"); // Fallback extensions
    private static final List<String> HASH_EXTENSIONS = Arrays.asList("md5", "sha1", "asc"); // Hash and signature extensions
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}"); // Matches ${property.name}
    private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)*([-][a-zA-Z0-9]+)*"); // Matches Maven versions

    public static void main(String[] args) throws Exception {
        String inputFile = "pom_list.txt";

        // Create output and cache directories
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        Files.createDirectories(Paths.get(CACHE_DIR));

        // Read initial POM file paths/URLs from input file and add to queue
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                POM_QUEUE.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found: " + inputFile);
            throw e;
        }

        // Process POM files from queue
        while (!POM_QUEUE.isEmpty()) {
            processPomFile(POM_QUEUE.poll());
        }
    }

    private static void processPomFile(String pomPath) {
        try {
            // Cache POM file and get its local path
            Path cachePath = cachePomFile(pomPath);
            if (cachePath == null) {
                System.err.println("Failed to cache POM file: " + pomPath);
                return;
            }

            // Parse POM file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(cachePath.toFile());

            // Get packaging for the artifact
            String packaging = getElementText(doc.getDocumentElement(), "packaging");
            if (packaging == null) {
                packaging = "jar"; // Default to jar if not specified
            }

            // Get POM groupId and version for property resolution
            String pomGroupId = getPomGroupId(doc);
            String pomVersion = getPomVersion(doc);
            if (pomVersion == null) {
                System.err.println("No version found in POM or parent POM: " + pomPath);
                return;
            }

            // Extract properties from <properties> section
            Map<String, String> properties = getProperties(doc);
            properties.put("project.groupId", pomGroupId != null ? pomGroupId : "");
            properties.put("project.version", pomVersion);
            properties.putIfAbsent("java.version", System.getProperty("java.version").split("\\.")[0]); // e.g., "11" from "11.0.2"

            // Get dependencies
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dep = (Element) dependencyNodes.item(i);
                String groupId = getElementText(dep, "groupId");
                String artifactId = getElementText(dep, "artifactId");
                String version = getElementText(dep, "version");
                String scope = getElementText(dep, "scope");

                // Skip invalid dependencies or test/provided scopes
                if (groupId == null || artifactId == null || version == null ||
                        (scope != null && (scope.equals("test") || scope.equals("provided")))) {
                    continue;
                }

                // Resolve properties in groupId and version
                groupId = resolveProperties(groupId, properties);
                version = resolveProperties(version, properties);
                if (groupId == null || version == null) {
                    System.err.println("Unresolved properties in groupId/version for " + groupId + ":" + artifactId + ":" + version);
                    continue;
                }

                // Handle com.sun.tools:tools specially
                if ("com.sun.tools".equals(groupId) && "tools".equals(artifactId)) {
                    System.err.println("Skipping com.sun.tools:tools:" + version + " (not available in Maven Central)");
                    continue;
                }

                // Process dependency
                processDependency(groupId, artifactId, version, packaging);
            }
        } catch (Exception e) {
            System.err.println("Failed to process POM file: " + pomPath + " - " + e.getMessage());
        }
    }

    private static String getPomGroupId(Document doc) {
        // Try to get groupId from the POM itself
        String groupId = getElementText(doc.getDocumentElement(), "groupId");
        if (groupId != null) {
            return groupId;
        }

        // If no groupId, try the parent POM
        NodeList parentNodes = doc.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parent = (Element) parentNodes.item(0);
            return getElementText(parent, "groupId");
        }

        return null;
    }

    private static String getPomVersion(Document doc) {
        // Try to get version from the POM itself
        String version = getElementText(doc.getDocumentElement(), "version");
        if (version != null) {
            return version;
        }

        // If no version, try the parent POM
        NodeList parentNodes = doc.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parent = (Element) parentNodes.item(0);
            return getElementText(parent, "version");
        }

        return null;
    }

    private static Map<String, String> getProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        if (propertiesNodes.getLength() > 0) {
            Element propertiesElement = (Element) propertiesNodes.item(0);
            NodeList propertyNodes = propertiesElement.getChildNodes();
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                if (propertyNodes.item(i) instanceof Element) {
                    Element prop = (Element) propertyNodes.item(i);
                    String name = prop.getTagName();
                    String value = prop.getTextContent().trim();
                    if (!value.isEmpty()) {
                        properties.put(name, value);
                    }
                }
            }
        }
        return properties;
    }

    private static String resolveProperties(String value, Map<String, String> properties) {
        if (!value.contains("${")) {
            return value;
        }

        String resolved = value;
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        while (matcher.find()) {
            String propName = matcher.group(1);
            String propValue = properties.get(propName);
            if (propValue == null) {
                return null; // Unresolved property
            }
            resolved = resolved.replace("${" + propName + "}", propValue);
        }
        return resolved;
    }

    private static void processDependency(String groupId, String artifactId, String version, String parentPackaging) {
        String artifactKey = groupId + ":" + artifactId + ":" + version;
        if (PROCESSED_ARTIFACTS.contains(artifactKey)) {
            return; // Skip already processed artifacts
        }
        PROCESSED_ARTIFACTS.add(artifactKey);

        // Download artifact (try packaging from parent or common extensions)
        downloadArtifact(groupId, artifactId, version, parentPackaging);

        // Add dependency's POM to queue
        String pomUrl = constructPomUrl(groupId, artifactId, version);
        POM_QUEUE.add(pomUrl);
    }

    private static Path cachePomFile(String pomPath) {
        try {
            // Check for com.sun.tools:tools and skip
            if (pomPath.contains("com/sun/tools/") && pomPath.contains("/tools-")) {
                System.err.println("Skipping com.sun.tools:tools POM (not available in Maven Central): " + pomPath);
                return null;
            }

            // Initialize properties for URL resolution
            Map<String, String> properties = new HashMap<>();
            properties.put("java.version", System.getProperty("java.version").split("\\.")[0]); // e.g., "11" from "11.0.2"

            // Resolve properties in pomPath
            String resolvedPomPath = pomPath;
            if (pomPath.contains("${")) {
                resolvedPomPath = resolveProperties(pomPath, properties);
                if (resolvedPomPath == null) {
                    System.err.println("Unresolved properties in POM URL: " + pomPath);
                    return null;
                }
            }

            String pomFileName;
            String groupPath;
            String artifactId;
            String version;

            if (resolvedPomPath.startsWith("http://") || resolvedPomPath.startsWith("https://")) {
                // Extract metadata from URL
                pomFileName = Paths.get(new URL(resolvedPomPath).getPath()).getFileName().toString();
                if (!pomFileName.endsWith(".pom")) {
                    System.err.println("Invalid POM file name (must end with .pom): " + pomFileName);
                    return null;
                }

                // Extract artifactId and version from filename
                String baseName = pomFileName.substring(0, pomFileName.length() - 4); // Remove .pom
                // Find version using Maven version pattern
                Matcher versionMatcher = VERSION_PATTERN.matcher(baseName);
                String candidateVersion = null;
                int versionStart = -1;
                while (versionMatcher.find()) {
                    String foundVersion = versionMatcher.group();
                    int start = versionMatcher.start();
                    // Check if this version forms a valid filename suffix
                    String candidateArtifactId = baseName.substring(0, start > 0 ? start - 1 : 0);
                    if (pomFileName.equals(candidateArtifactId + "-" + foundVersion + ".pom")) {
                        candidateVersion = foundVersion;
                        versionStart = start;
                    }
                }
                if (candidateVersion == null || versionStart == -1) {
                    System.err.println("Failed to parse version from POM filename: " + pomFileName);
                    return null;
                }
                version = candidateVersion;
                artifactId = baseName.substring(0, versionStart > 0 ? versionStart - 1 : 0);

                // Extract groupPath from URL
                String expectedPathEnd = artifactId + "/" + version + "/" + pomFileName;
                int groupPathEnd = resolvedPomPath.lastIndexOf(expectedPathEnd);
                if (groupPathEnd == -1) {
                    System.err.println("Invalid POM URL structure: " + resolvedPomPath);
                    return null;
                }
                groupPath = resolvedPomPath.substring(MAVEN_CENTRAL.length(), groupPathEnd);
            } else {
                // Parse local POM to get metadata
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                try (InputStream tempInputStream = new FileInputStream(pomPath)) {
                    Document doc = builder.parse(tempInputStream);
                    groupPath = getElementText(doc.getDocumentElement(), "groupId");
                    if (groupPath != null) {
                        groupPath = groupPath.replace('.', '/');
                    }
                    artifactId = getElementText(doc.getDocumentElement(), "artifactId");
                    version = getElementText(doc.getDocumentElement(), "version");
                    pomFileName = artifactId + "-" + version + ".pom";
                }
                if (groupPath == null || artifactId == null || version == null) {
                    System.err.println("Invalid POM metadata in: " + pomPath);
                    return null;
                }
            }

            // Construct cache path for POM
            Path cachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, pomFileName);
            Files.createDirectories(cachePath.getParent());

            // Skip if POM is already cached
            if (Files.exists(cachePath)) {
                verifyFile(cachePath, groupPath, artifactId, version, pomFileName);
                return cachePath;
            }

            // Download or copy POM to cache
            try (InputStream pomInputStream = resolvedPomPath.startsWith("http://") || resolvedPomPath.startsWith("https://") ?
                    new URL(resolvedPomPath).openStream() : new FileInputStream(pomPath);
                 FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(Channels.newChannel(pomInputStream), 0, Long.MAX_VALUE);
                System.out.println("Downloaded to cache: " + cachePath);
            }

            // Download hash and signature files for POM
            downloadHashAndSignatureFiles(groupPath, artifactId, version, pomFileName);

            // Verify POM file
            verifyFile(cachePath, groupPath, artifactId, version, pomFileName);

            return cachePath;
        } catch (IOException | javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException e) {
            System.err.println("Failed to cache POM file: " + pomPath + " - " + e.getMessage());
            return null;
        }
    }

    private static void downloadArtifact(String groupId, String artifactId, String version, String packaging) {
        // Try the specified packaging first
        if (tryDownloadArtifact(groupId, artifactId, version, packaging)) {
            return;
        }

        // If packaging fails, try common extensions
        for (String extension : COMMON_EXTENSIONS) {
            if (!extension.equals(packaging) && tryDownloadArtifact(groupId, artifactId, version, extension)) {
                return;
            }
        }

        System.err.println("Failed to download artifact for all attempted extensions: " + groupId + ":" + artifactId + ":" + version);
    }

    private static boolean tryDownloadArtifact(String groupId, String artifactId, String version, String extension) {
        try {
            // Construct artifact URL and cache path
            String groupPath = groupId.replace('.', '/');
            String artifactUrl = MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" +
                    artifactId + "-" + version + "." + extension;
            String fileName = artifactId + "-" + version + "." + extension;
            Path cachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, fileName);

            // Skip if artifact is already in cache
            if (Files.exists(cachePath)) {
                System.out.println("Already in cache: " + cachePath);
                verifyFile(cachePath, groupPath, artifactId, version, fileName);
                return true;
            }

            // Download to cache
            Files.createDirectories(cachePath.getParent());
            URL url = new URL(artifactUrl);
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Downloaded to cache: " + cachePath);
            }

            // Download hash and signature files for artifact
            downloadHashAndSignatureFiles(groupPath, artifactId, version, fileName);

            // Verify artifact file
            verifyFile(cachePath, groupPath, artifactId, version, fileName);

            return true;
        } catch (IOException e) {
            System.err.println("Failed to download " + groupId + ":" + artifactId + ":" + version + " with extension " + extension + ": " + e.getMessage());
            return false;
        }
    }

    private static void downloadHashAndSignatureFiles(String groupPath, String artifactId, String version, String baseFileName) {
        for (String hashExt : HASH_EXTENSIONS) {
            try {
                String hashUrl = MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" +
                        baseFileName + "." + hashExt;
                String hashFileName = baseFileName + "." + hashExt;
                Path hashCachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, hashFileName);

                // Skip if hash/signature file is already in cache
                if (Files.exists(hashCachePath)) {
                    System.out.println("Already in cache: " + hashCachePath);
                    continue;
                }

                // Download hash/signature file
                Files.createDirectories(hashCachePath.getParent());
                URL url = new URL(hashUrl);
                try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                     FileOutputStream fos = new FileOutputStream(hashCachePath.toFile())) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    System.out.println("Downloaded to cache: " + hashCachePath);
                }
            } catch (IOException e) {
                System.err.println("Failed to download " + hashExt + " for " + baseFileName + ": " + e.getMessage());
            }
        }
    }

    private static void verifyFile(Path filePath, String groupPath, String artifactId, String version, String fileName) {
        try {
            // Compute MD5 and SHA1 hashes
            String computedMd5 = computeHash(filePath, "MD5");
            String computedSha1 = computeHash(filePath, "SHA-1");

            // Verify MD5
            Path md5Path = Paths.get(CACHE_DIR, groupPath, artifactId, version, fileName + ".md5");
            if (Files.exists(md5Path)) {
                String expectedMd5 = new String(Files.readAllBytes(md5Path)).trim();
                if (computedMd5.equalsIgnoreCase(expectedMd5)) {
                    System.out.println("Verified MD5 for: " + filePath);
                } else {
                    System.err.println("Verification failed for " + filePath + ".md5: expected " + expectedMd5 + ", got " + computedMd5);
                }
            } else {
                System.out.println("MD5 file not found for: " + filePath + " (skipping verification)");
            }

            // Verify SHA1
            Path sha1Path = Paths.get(CACHE_DIR, groupPath, artifactId, version, fileName + ".sha1");
            if (Files.exists(sha1Path)) {
                String expectedSha1 = new String(Files.readAllBytes(sha1Path)).trim();
                if (computedSha1.equalsIgnoreCase(expectedSha1)) {
                    System.out.println("Verified SHA1 for: " + filePath);
                } else {
                    System.err.println("Verification failed for " + filePath + ".sha1: expected " + expectedSha1 + ", got " + computedSha1);
                }
            } else {
                System.out.println("SHA1 file not found for: " + filePath + " (skipping verification)");
            }

            // Note: Skipping .asc verification (requires external GPG libraries)
            Path ascPath = Paths.get(CACHE_DIR, groupPath, artifactId, version, fileName + ".asc");
            if (Files.exists(ascPath)) {
                System.out.println("ASC file present but not verified: " + ascPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to verify file: " + filePath + " - " + e.getMessage());
        }
    }

    private static String computeHash(Path filePath, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashedBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String constructPomUrl(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', '/');
        return MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" +
                artifactId + "-" + version + ".pom";
    }

    private static String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }
}