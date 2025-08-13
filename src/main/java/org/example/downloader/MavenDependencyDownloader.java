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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class MavenDependencyDownloader {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private static final String OUTPUT_DIR = "downloaded-jars";
    private static final String CACHE_DIR = OUTPUT_DIR + "/custom-cache";
    private static final Set<String> PROCESSED_ARTIFACTS = new HashSet<>(); // Track processed artifacts
    private static final Queue<String> POM_QUEUE = new ArrayDeque<>(); // Queue for POM files
    private static final List<String> COMMON_EXTENSIONS = Arrays.asList("jar", "war", "zip"); // Fallback extensions

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

                // Process dependency
                processDependency(groupId, artifactId, version, packaging);
            }
        } catch (Exception e) {
            System.err.println("Failed to process POM file: " + pomPath + " - " + e.getMessage());
        }
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
            String pomFileName;
            String groupPath;
            String artifactId;
            String version;

            if (pomPath.startsWith("http://") || pomPath.startsWith("https://")) {
                // Extract metadata from URL
                pomFileName = Paths.get(new URL(pomPath).getPath()).getFileName().toString();
                String[] parts = pomFileName.split("-");
                if (parts.length < 2) {
                    System.err.println("Invalid POM file name: " + pomFileName);
                    return null;
                }
                version = parts[parts.length - 1].replace(".pom", "");
                artifactId = parts[parts.length - 2];
                groupPath = pomPath.substring(MAVEN_CENTRAL.length(), pomPath.lastIndexOf(artifactId + "/" + version));
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

            // Construct cache path
            Path cachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, pomFileName);
            Files.createDirectories(cachePath.getParent());

            // Skip if already cached
            if (Files.exists(cachePath)) {
                return cachePath;
            }

            // Download or copy POM to cache
            try (InputStream pomInputStream = pomPath.startsWith("http://") || pomPath.startsWith("https://") ?
                    new URL(pomPath).openStream() : new FileInputStream(pomPath);
                 FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(Channels.newChannel(pomInputStream), 0, Long.MAX_VALUE);
            }
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

            // Skip if already in cache
            if (Files.exists(cachePath)) {
                System.out.println("Already in cache: " + cachePath);
                return true;
            }

            // Download to cache
            Files.createDirectories(cachePath.getParent());
            URL url = new URL(artifactUrl);
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Downloaded to cache: " + cachePath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Failed to download " + groupId + ":" + artifactId + ":" + version + " with extension " + extension + ": " + e.getMessage());
            return false;
        }
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