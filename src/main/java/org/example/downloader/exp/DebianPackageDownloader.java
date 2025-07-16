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

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class DebianPackageDownloader {
    private String packageName;
    private String version;
    private String distribution; // e.g., bookworm
    private String component; // e.g., main
    private String architecture; // e.g., amd64, all
    private String mirrorBaseUrl; // e.g., http://deb.debian.org/debian
    private String destinationDir; // Directory to save files
    private static final Set<String> VALID_COMPONENTS = new HashSet<>(Arrays.asList("main", "contrib", "non-free", "non-free-firmware"));
    private static final Set<String> VALID_ARCHITECTURES = new HashSet<>(Arrays.asList(
            "amd64", "arm64", "armel", "armhf", "i386", "mips64el", "mipsel", "ppc64el", "riscv64", "s390x", "all"
    ));

    // Constructor
    public DebianPackageDownloader(String packageName, String version, String distribution,
                                   String component, String architecture, String mirrorBaseUrl, String destinationDir) {
        this.packageName = packageName;
        this.version = version;
        this.distribution = distribution;
        this.component = component;
        this.architecture = architecture;
        this.mirrorBaseUrl = mirrorBaseUrl != null ? mirrorBaseUrl : "http://deb.debian.org/debian";
        this.destinationDir = destinationDir;
    }

    // Builder pattern
    public static class Builder {
        private String packageName;
        private String version;
        private String distribution;
        private String component;
        private String architecture;
        private String mirrorBaseUrl;
        private String destinationDir;

        public Builder packageName(String packageName) { this.packageName = packageName; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder distribution(String distribution) { this.distribution = distribution; return this; }
        public Builder component(String component) { this.component = component; return this; }
        public Builder architecture(String architecture) { this.architecture = architecture; return this; }
        public Builder mirrorBaseUrl(String mirrorBaseUrl) { this.mirrorBaseUrl = mirrorBaseUrl; return this; }
        public Builder destinationDir(String destinationDir) { this.destinationDir = destinationDir; return this; }

        public DebianPackageDownloader build() {
            return new DebianPackageDownloader(packageName, version, distribution, component, architecture, mirrorBaseUrl, destinationDir);
        }
    }

    // Validate inputs
    private void validateInputs() throws IllegalArgumentException {
        if (packageName == null || packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        if (distribution == null || distribution.trim().isEmpty()) {
            throw new IllegalArgumentException("Distribution cannot be null or empty");
        }
        if (component == null || !VALID_COMPONENTS.contains(component.toLowerCase())) {
            throw new IllegalArgumentException("Invalid component. Supported: " + String.join(", ", VALID_COMPONENTS));
        }
        if (architecture == null || !VALID_ARCHITECTURES.contains(architecture.toLowerCase())) {
            throw new IllegalArgumentException("Invalid architecture. Supported: " + String.join(", ", VALID_ARCHITECTURES));
        }
        if (mirrorBaseUrl == null || !mirrorBaseUrl.matches("^(https?://)[\\w\\d\\-_./]+$")) {
            throw new IllegalArgumentException("Invalid mirror base URL format");
        }
        if (destinationDir == null || destinationDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination directory cannot be null or empty");
        }
        // Ensure destination directory exists
        Path destPath = Paths.get(destinationDir);
        if (!Files.exists(destPath)) {
            try {
                Files.createDirectories(destPath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to create destination directory: " + e.getMessage());
            }
        }
    }

    // Build the download URL for the .deb package
    public String buildDownloadUrl() throws IllegalArgumentException {
        validateInputs();

        String normalizedPackageName = packageName.trim().toLowerCase();
        String normalizedVersion = version.trim();
        String normalizedDistribution = distribution.trim().toLowerCase();
        String normalizedComponent = component.trim().toLowerCase();
        String normalizedArchitecture = architecture.trim().toLowerCase();
        String normalizedBaseUrl = mirrorBaseUrl.trim().endsWith("/") ? mirrorBaseUrl.trim() : mirrorBaseUrl.trim() + "/";

        String sourceInitial = normalizedPackageName.startsWith("lib") ?
                "lib" + normalizedPackageName.charAt(3) :
                normalizedPackageName.matches("^[0-9].*") ?
                        normalizedPackageName.charAt(0) + "" :
                        normalizedPackageName.charAt(0) + "";
        String sourceName = normalizedPackageName.startsWith("0ad-data-") ? "0ad-data" : normalizedPackageName;

        return String.format("%spool/%s/%s/%s/%s_%s_%s.deb",
                normalizedBaseUrl, normalizedComponent, sourceInitial, sourceName,
                normalizedPackageName, normalizedVersion, normalizedArchitecture);
    }

    // Fetch SHA256 checksum from Packages file and save it
    private String fetchAndSaveChecksum() throws IOException {
        String normalizedDistribution = distribution.trim().toLowerCase();
        String normalizedComponent = component.trim().toLowerCase();
        String normalizedArchitecture = architecture.trim().toLowerCase();
        String normalizedBaseUrl = mirrorBaseUrl.trim().endsWith("/") ? mirrorBaseUrl.trim() : mirrorBaseUrl.trim() + "/";

        String packagesUrl = String.format("%sdists/%s/%s/binary-%s/Packages.gz",
                normalizedBaseUrl, normalizedDistribution, normalizedComponent, normalizedArchitecture);
        String packagesUrlUncompressed = String.format("%sdists/%s/%s/binary-%s/Packages",
                normalizedBaseUrl, normalizedDistribution, normalizedComponent, normalizedArchitecture);

        String checksum;
        try {
            URL url = new URL(packagesUrl);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(url.openStream())))) {
                checksum = parsePackagesFile(reader);
            }
        } catch (IOException e) {
            URL url = new URL(packagesUrlUncompressed);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                checksum = parsePackagesFile(reader);
            }
        }

        // Save checksum to file
        String checksumFileName = String.format("%s_%s_%s.sha256", packageName, version, architecture);
        Path checksumPath = Paths.get(destinationDir, checksumFileName);
        Files.write(checksumPath, checksum.getBytes());
        System.out.println("Checksum saved to: " + checksumPath);

        return checksum;
    }

    // Parse Packages file to find SHA256 checksum
    private String parsePackagesFile(BufferedReader reader) throws IOException {
        String normalizedPackageName = packageName.trim().toLowerCase();
        String normalizedVersion = version.trim();
        String currentPackage = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Package: ")) {
                currentPackage = line.substring("Package: ".length()).trim();
            } else if (currentPackage != null && currentPackage.equals(normalizedPackageName) &&
                    line.startsWith("Version: ") && line.substring("Version: ".length()).trim().equals(normalizedVersion)) {
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("SHA256: ")) {
                        return line.substring("SHA256: ".length()).trim();
                    }
                }
            }
        }
        throw new IOException("Checksum not found for package " + normalizedPackageName + " version " + normalizedVersion);
    }

    // Compute SHA256 checksum of a file
    private String computeSha256(Path filePath) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 not supported: " + e.getMessage());
        }
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Download the package
    public Path download() throws IOException {
        validateInputs();

        String downloadUrl = buildDownloadUrl();
        String fileName = String.format("%s_%s_%s.deb", packageName, version, architecture);
        Path destinationPath = Paths.get(destinationDir, fileName);

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(downloadUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(destinationPath.toFile())) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }

        System.out.println("Package downloaded to: " + destinationPath);
        return destinationPath;
    }

    // Verify the package and save verification info
    public boolean verify(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }

        String expectedChecksum = fetchAndSaveChecksum();
        String actualChecksum = computeSha256(filePath);
        boolean isValid = expectedChecksum.equalsIgnoreCase(actualChecksum);

        // Save verification info
        String verifyFileName = String.format("%s_%s_%s.verify", packageName, version, architecture);
        Path verifyPath = Paths.get(destinationDir, verifyFileName);
        String verificationInfo = isValid ?
                "Verification: Success\nExpected SHA256: " + expectedChecksum + "\nActual SHA256: " + actualChecksum :
                "Verification: Failed\nExpected SHA256: " + expectedChecksum + "\nActual SHA256: " + actualChecksum;
        Files.write(verifyPath, verificationInfo.getBytes());
        System.out.println("Verification info saved to: " + verifyPath);

        if (!isValid) {
            throw new IOException("Checksum verification failed for " + filePath.getFileName());
        }

        return isValid;
    }

    // Fetch a mirror dynamically (simplified)
    public static String fetchFastestMirror(String country, String distribution, String architecture) {
        try {
            URL url = new URL("https://www.debian.org/mirror/list-full");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("http") && line.contains(country) && line.contains(architecture)) {
                        if (line.contains("/debian/")) {
                            return line.split("/debian/")[0].replaceAll(".*(http[s]?://[^\"]+)", "$1");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching mirror: " + e.getMessage());
        }
        return "http://deb.debian.org/debian";
    }

    // Example usage
    public static void main(String[] args) {
        try {
            // Example for 0ad-data-common
            DebianPackageDownloader downloader = new Builder()
                    .packageName("0ad-data-common")
                    .version("0.0.26-1")
                    .distribution("bookworm")
                    .component("main")
                    .architecture("all")
                    .mirrorBaseUrl("http://deb.debian.org/debian")
                    .destinationDir("/tmp/debian-packages")
                    .build();

            // Step 1: Download
            Path downloadedFile = downloader.download();

            // Step 2: Verify
            boolean isValid = downloader.verify(downloadedFile);
            System.out.println("Verification result: " + (isValid ? "Success" : "Failed"));

            // Example with dynamic mirror
            String fastestMirror = fetchFastestMirror("us", "bookworm", "all");
            downloader = new Builder()
                    .packageName("0ad-data-common")
                    .version("0.0.26-1")
                    .distribution("bookworm")
                    .component("main")
                    .architecture("all")
                    .mirrorBaseUrl(fastestMirror)
                    .destinationDir("/tmp/debian-packages")
                    .build();

            downloadedFile = downloader.download();
            isValid = downloader.verify(downloadedFile);
            System.out.println("Verification result with fastest mirror: " + (isValid ? "Success" : "Failed"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
