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


import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianComponent;

import java.util.Arrays;

public class DebianDownloadUrl {
    private DebianPackage debianPackage;
    private String mirrorBaseUrl; // e.g., http://deb.debian.org/debian

    // Constructor
    public DebianDownloadUrl(DebianPackage debianPackage, String mirrorBaseUrl) {
        this.debianPackage = debianPackage;
        this.mirrorBaseUrl = mirrorBaseUrl != null ? mirrorBaseUrl : "http://deb.debian.org/debian";
    }

    // Validate inputs
    private void validateInputs() throws IllegalArgumentException {
        if (debianPackage.packageName == null || debianPackage.packageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Package name cannot be null or empty");
        }
        if (debianPackage.version == null || debianPackage.version.trim().isEmpty()) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        if (debianPackage.distribution == null) {
            throw new IllegalArgumentException("Distribution cannot be null or empty");
        }
        if (debianPackage.component == null) {
            throw new IllegalArgumentException("Invalid component. Supported: " + String.join(", ", Arrays.toString(DebianComponent.values())));
        }
        if (debianPackage.architecture == null) {
            throw new IllegalArgumentException("Invalid architecture. Supported: " + String.join(", ", Arrays.toString(DebianArchitecture.values())));
        }
        if (mirrorBaseUrl == null || !mirrorBaseUrl.matches("^(https?://)[\\w\\d\\-_./]+$")) {
            throw new IllegalArgumentException("Invalid mirror base URL format");
        }
    }

    // Build the download URL for a specific .deb package
    public String buildDownloadUrl() throws IllegalArgumentException {
        validateInputs();

        // Normalize inputs
        String normalizedPackageName = debianPackage.packageName.trim().toLowerCase();
        String normalizedVersion = debianPackage.version.trim();
        String normalizedDistribution = debianPackage.distribution.toString().toLowerCase();
        String normalizedComponent = debianPackage.component.toString().toLowerCase();
        String normalizedArchitecture = debianPackage.architecture.toString().toLowerCase();
        String normalizedBaseUrl = mirrorBaseUrl.trim().endsWith("/") ? mirrorBaseUrl.trim() : mirrorBaseUrl.trim() + "/";

        // Determine source initial (e.g., '0' for 0ad-data, 'liba' for libapp)
        String sourceInitial = normalizedPackageName.startsWith("lib") ?
                "lib" + normalizedPackageName.charAt(3) :
                normalizedPackageName.matches("^[0-9].*") ?
                        normalizedPackageName.charAt(0) + "" :
                        normalizedPackageName.charAt(0) + "";
        // Simplified: assumes package name matches source name
        String sourceName = normalizedPackageName;

        // Format: {baseUrl}/pool/{component}/{sourceInitial}/{sourceName}/{package}_{version}_{architecture}.deb
        return String.format("%spool/%s/%s/%s/%s_%s_%s.deb",
                normalizedBaseUrl, normalizedComponent, sourceInitial, sourceName,
                normalizedPackageName, normalizedVersion, normalizedArchitecture);
    }

    public String buildDownloadUrlAsAll() throws IllegalArgumentException {
        validateInputs();

        // Normalize inputs
        String normalizedPackageName = debianPackage.packageName.trim().toLowerCase();
        String normalizedVersion = debianPackage.version.trim();
        String normalizedDistribution = debianPackage.distribution.toString().toLowerCase();
        String normalizedComponent = debianPackage.component.toString().toLowerCase();
        String normalizedArchitecture = DebianArchitecture.ALL.toString().toLowerCase();
        String normalizedBaseUrl = mirrorBaseUrl.trim().endsWith("/") ? mirrorBaseUrl.trim() : mirrorBaseUrl.trim() + "/";

        // Determine source initial (e.g., '0' for 0ad-data, 'liba' for libapp)
        String sourceInitial = normalizedPackageName.startsWith("lib") ?
                "lib" + normalizedPackageName.charAt(3) :
                normalizedPackageName.matches("^[0-9].*") ?
                        normalizedPackageName.charAt(0) + "" :
                        normalizedPackageName.charAt(0) + "";
        // Simplified: assumes package name matches source name
        String sourceName = normalizedPackageName;

        // Format: {baseUrl}/pool/{component}/{sourceInitial}/{sourceName}/{package}_{version}_{architecture}.deb
        return String.format("%spool/%s/%s/%s/%s_%s_%s.deb",
                normalizedBaseUrl, normalizedComponent, sourceInitial, sourceName,
                normalizedPackageName, normalizedVersion, normalizedArchitecture);
    }
}