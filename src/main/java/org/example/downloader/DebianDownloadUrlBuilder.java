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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DebianDownloadUrlBuilder {
    private String packageName;
    private String version;
    private String distribution; // e.g., bookworm, sid
    private String component; // e.g., main, contrib, non-free
    private String architecture; // e.g., amd64, arm64, or all
    private String mirrorBaseUrl; // e.g., http://deb.debian.org/debian
    private static final Set<String> VALID_COMPONENTS = new HashSet<>(Arrays.asList("main", "contrib", "non-free", "non-free-firmware"));
    private static final Set<String> VALID_ARCHITECTURES = new HashSet<>(Arrays.asList(
            "amd64", "arm64", "armel", "armhf", "i386", "mips64el", "mipsel", "ppc64el", "riscv64", "s390x", "all"
    ));

    // Constructor
    public DebianDownloadUrlBuilder(String packageName, String version, String distribution,
                                    String component, String architecture, String mirrorBaseUrl) {
        this.packageName = packageName;
        this.version = version;
        this.distribution = distribution;
        this.component = component;
        this.architecture = architecture;
        this.mirrorBaseUrl = mirrorBaseUrl != null ? mirrorBaseUrl : "http://deb.debian.org/debian";
    }

    // Builder pattern
    public static class Builder {
        private String packageName;
        private String version;
        private String distribution;
        private String component;
        private String architecture;
        private String mirrorBaseUrl;

        public Builder packageName(String packageName) { this.packageName = packageName; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder distribution(String distribution) { this.distribution = distribution; return this; }
        public Builder component(String component) { this.component = component; return this; }
        public Builder architecture(String architecture) { this.architecture = architecture; return this; }
        public Builder mirrorBaseUrl(String mirrorBaseUrl) { this.mirrorBaseUrl = mirrorBaseUrl; return this; }

        public DebianDownloadUrlBuilder build() {
            return new DebianDownloadUrlBuilder(packageName, version, distribution, component, architecture, mirrorBaseUrl);
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
    }

    // Build the download URL for a specific .deb package
    public String buildDownloadUrl() throws IllegalArgumentException {
        validateInputs();

        // Normalize inputs
        String normalizedPackageName = packageName.trim().toLowerCase();
        String normalizedVersion = version.trim();
        String normalizedDistribution = distribution.trim().toLowerCase();
        String normalizedComponent = component.trim().toLowerCase();
        String normalizedArchitecture = architecture.trim().toLowerCase();
        String normalizedBaseUrl = mirrorBaseUrl.trim().endsWith("/") ? mirrorBaseUrl.trim() : mirrorBaseUrl.trim() + "/";

        // Determine source initial (e.g., '0' for 0ad-data, 'liba' for libapp)
        String sourceInitial = normalizedPackageName.startsWith("lib") ?
                "lib" + normalizedPackageName.charAt(3) :
                normalizedPackageName.matches("^[0-9].*") ?
                        normalizedPackageName.charAt(0) + "" :
                        normalizedPackageName.charAt(0) + "";
        // Simplified: assumes package name matches source name
        String sourceName = normalizedPackageName.startsWith("0ad-data-") ? "0ad-data" : normalizedPackageName;

        // Format: {baseUrl}/pool/{component}/{sourceInitial}/{sourceName}/{package}_{version}_{architecture}.deb
        return String.format("%spool/%s/%s/%s/%s_%s_%s.deb",
                normalizedBaseUrl, normalizedComponent, sourceInitial, sourceName,
                normalizedPackageName, normalizedVersion, normalizedArchitecture);
    }

    // Fetch a mirror dynamically (simplified example)
    public static String fetchFastestMirror(String country, String distribution, String architecture) {
        try {
            URL url = new URL("https://www.debian.org/mirror/list-full");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("http") && line.contains(country) && line.contains(architecture)) {
                    if (line.contains("/debian/")) {
                        return line.split("/debian/")[0].replaceAll(".*(http[s]?://[^\"]+)", "$1");
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error fetching mirror: " + e.getMessage());
        }
        return "http://deb.debian.org/debian";
    }

    // Example usage
    public static void main(String[] args) {
        try {
            // Example for 0ad-data-common (architecture-independent)
            DebianDownloadUrlBuilder builder = new Builder()
                    .packageName("0ad")
                    .version("0.0.26-3")
                    .distribution("bookworm")
                    .component("main")
                    .architecture("amd64")
                    .mirrorBaseUrl("http://deb.debian.org/debian")
                    .build();
            System.out.println("Download URL: " + builder.buildDownloadUrl());

            // Example with dynamic mirror selection
            String fastestMirror = fetchFastestMirror("us", "bookworm", "all");
            builder = new Builder()
                    .packageName("0ad-data-common")
                    .version("0.0.26-1")
                    .distribution("bookworm")
                    .component("main")
                    .architecture("all")
                    .mirrorBaseUrl(fastestMirror)
                    .build();
            System.out.println("Download URL with fastest mirror: " + builder.buildDownloadUrl());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}