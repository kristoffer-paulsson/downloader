package org.example.downloader.deb;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

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

public class DebianIndexDownloader {
    private static final String REPO_URL = "https://deb.debian.org/debian/";
    private static final String RELEASE = "bookworm";
    private static final String[] COMPONENTS = {"main", "contrib", "non-free", "non-free-firmware"};
    private static final String ARCH = "amd64";
    private static final String[] ICON_SIZES = {"48x48", "64x64", "128x128"};

    public static void main(String[] args) {
        /*try {
            // Download InRelease
            downloadFile(REPO_URL + "dists/" + RELEASE + "/InRelease", "dists/" + RELEASE + "/InRelease");

            // Download index files for each component
            for (String component : COMPONENTS) {
                // Packages.gz (already present, but included for completeness)
                String packagesUrl = REPO_URL + "dists/" + RELEASE + "/" + component + "/binary-" + ARCH + "/Packages.gz";
                downloadFile(packagesUrl, "dists/" + RELEASE + "/" + component + "/binary-" + ARCH + "/Packages.gz");

                // Translation-en.gz
                String translationUrl = REPO_URL + "dists/" + RELEASE + "/" + component + "/i18n/Translation-en.gz";
                downloadFile(translationUrl, "dists/" + RELEASE + "/" + component + "/i18n/Translation-en.gz");

                // Contents-amd64.gz
                String contentsUrl = REPO_URL + "dists/" + RELEASE + "/" + component + "/Contents-amd64.gz";
                downloadFile(contentsUrl, "dists/" + RELEASE + "/" + component + "/Contents-amd64.gz");

                // AppStream for GUI support
                String componentsUrl = REPO_URL + "dists/" + RELEASE + "/" + component + "/dep11/Components-amd64.yml.gz";
                downloadFile(componentsUrl, "dists/" + RELEASE + "/" + component + "/dep11/Components-amd64.yml.gz");

                for (String size : ICON_SIZES) {
                    String iconsUrl = REPO_URL + "dists/" + RELEASE + "/" + component + "/dep11/icons-" + size + ".tar.gz";
                    downloadFile(iconsUrl, "dists/" + RELEASE + "/" + component + "/dep11/icons-" + size + ".tar.gz");
                }

                // Verify checksums
                verifyChecksum("dists/" + RELEASE + "/InRelease", "Packages.gz", component, "binary-" + ARCH);
                verifyChecksum("dists/" + RELEASE + "/InRelease", "Translation-en.gz", component, "i18n");
                verifyChecksum("dists/" + RELEASE + "/InRelease", "Contents-amd64.gz", component, "");
                verifyChecksum("dists/" + RELEASE + "/InRelease", "Components-amd64.yml.gz", component, "dep11");
                for (String size : ICON_SIZES) {
                    verifyChecksum("dists/" + RELEASE + "/InRelease", "icons-" + size + ".tar.gz", component, "dep11");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }*/
    }

    // Reuse downloadFile and verifyChecksum from previous code
    // Update verifyChecksum to handle dep11 paths
    private static String getExpectedChecksum(String releaseFile, String targetFile, String component, String section)
            throws IOException {
        String targetPath = component + "/" + (section.isEmpty() ? "" : section + "/") + targetFile;
        try (Scanner scanner = new Scanner(new File(releaseFile))) {
            boolean inSha256Section = false;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("SHA256:")) {
                    inSha256Section = true;
                    continue;
                }
                if (inSha256Section && line.isEmpty()) {
                    inSha256Section = false;
                    continue;
                }
                if (inSha256Section && line.contains(targetPath)) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        return parts[0]; // SHA256 checksum
                    }
                }
            }
        }
        return null;
    }
}