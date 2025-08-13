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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MavenDependencyDownloader {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";
    private static final String OUTPUT_DIR = "downloaded-jars";
    private static final String CACHE_DIR = OUTPUT_DIR + "/custom-cache";
    private static final Set<String> PROCESSED_ARTIFACTS = new HashSet<>(); // Track processed artifacts to avoid duplicates

    public static void main(String[] args) throws Exception {
        String inputFile = "pom_list.txt";

        // Create output and cache directories
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        Files.createDirectories(Paths.get(CACHE_DIR));

        // Read POM file paths/URLs from input file
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                processPomFile(line.trim());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Input file not found: " + inputFile);
            throw e;
        }
    }

    private static void processPomFile(String pomPath) {
        try {
            // Read or download POM file
            Path cachePath = cachePomFile(pomPath);
            if (cachePath == null) {
                System.err.println("Failed to cache POM file: " + pomPath);
                return;
            }

            // Parse POM file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(cachePath.toFile());

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

                // Process dependency recursively
                processDependency(groupId, artifactId, version);
            }
        } catch (Exception e) {
            System.err.println("Failed to process POM file: " + pomPath + " - " + e.getMessage());
        }
    }

    private static void processDependency(String groupId, String artifactId, String version) {
        String artifactKey = groupId + ":" + artifactId + ":" + version;
        if (PROCESSED_ARTIFACTS.contains(artifactKey)) {
            return; // Skip already processed artifacts
        }
        PROCESSED_ARTIFACTS.add(artifactKey);

        // Download JAR
        downloadArtifact(groupId, artifactId, version);

        // Download and process dependency's POM recursively
        String pomUrl = constructPomUrl(groupId, artifactId, version);
        Path pomCachePath = cachePomFile(pomUrl);
        if (pomCachePath != null) {
            processPomFile(pomCachePath.toString());
        }
    }

    private static Path cachePomFile(String pomPath) {
        try {
            // Determine if pomPath is a URL or local path
            InputStream pomInputStream;
            String pomFileName;
            if (pomPath.startsWith("http://") || pomPath.startsWith("https://")) {
                URL url = new URL(pomPath);
                pomInputStream = url.openStream();
                pomFileName = Paths.get(url.getPath()).getFileName().toString();
            } else {
                pomInputStream = new FileInputStream(pomPath);
                pomFileName = Paths.get(pomPath).getFileName().toString();
            }

            // Extract groupId, artifactId, version from POM path (assumes Maven URL structure)
            String[] parts = pomFileName.split("-");
            if (parts.length < 2) {
                System.err.println("Invalid POM file name: " + pomFileName);
                return null;
            }
            String version = parts[parts.length - 1].replace(".pom", "");
            String artifactId = parts[parts.length - 2];
            String groupPath = pomPath.contains(MAVEN_CENTRAL) ?
                    pomPath.substring(MAVEN_CENTRAL.length(), pomPath.lastIndexOf(artifactId + "/" + version)) :
                    Paths.get(pomPath).getParent().toString().replace(File.separator, "/");

            // Construct cache path (e.g., custom-cache/groupId/artifactId/version/artifactId-version.pom)
            Path cachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, pomFileName);
            Files.createDirectories(cachePath.getParent());

            // Skip if already cached
            if (Files.exists(cachePath)) {
                return cachePath;
            }

            // Download or copy to cache
            try (pomInputStream; FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(Channels.newChannel(pomInputStream), 0, Long.MAX_VALUE);
            }
            return cachePath;
        } catch (IOException e) {
            System.err.println("Failed to cache POM file: " + pomPath + " - " + e.getMessage());
            return null;
        }
    }

    private static void downloadArtifact(String groupId, String artifactId, String version) {
        try {
            // Construct artifact URL and cache path
            String groupPath = groupId.replace('.', '/');
            String artifactUrl = MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" +
                    artifactId + "-" + version + ".jar";
            String fileName = artifactId + "-" + version + ".jar";
            Path outputPath = Paths.get(OUTPUT_DIR, fileName);
            Path cachePath = Paths.get(CACHE_DIR, groupPath, artifactId, version, fileName);

            // Skip if already in output directory or cache
            if (Files.exists(outputPath) || Files.exists(cachePath)) {
                if (Files.exists(cachePath) && !Files.exists(outputPath)) {
                    Files.copy(cachePath, outputPath);
                    System.out.println("Copied from cache: " + outputPath);
                }
                return;
            }

            // Download to cache
            Files.createDirectories(cachePath.getParent());
            URL url = new URL(artifactUrl);
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(cachePath.toFile())) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            // Copy to output directory
            Files.copy(cachePath, outputPath);
            System.out.println("Downloaded: " + outputPath);
        } catch (IOException e) {
            System.err.println("Failed to download " + groupId + ":" + artifactId + ":" + version + ": " + e.getMessage());
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