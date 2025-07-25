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
package org.example.downloader.exp2;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class DebianPackageBuilder {
    public static void buildPackagesFile(Collection<DebianPackage> packages, String outputPath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzipOutputStream))) {

            for (DebianPackage pkg : packages) {
                writePackage(writer, pkg);
                writer.newLine(); // Blank line between packages
            }
        }
    }

    private static void writePackage(BufferedWriter writer, DebianPackage pkg) throws IOException {
        // Write fields in a standard order
        writeField(writer, "Package", pkg.packageName());
        writeField(writer, "Source", pkg.source());
        writeField(writer, "Version", pkg.version());
        writeField(writer, "Installed-Size", pkg.installedSize() != 0 ? String.valueOf(pkg.installedSize()) : null);
        writeField(writer, "Maintainer", pkg.maintainer());
        writeField(writer, "Architecture", pkg.architecture());
        writeMultiValueField(writer, "Depends", pkg.depends());
        writeMultiValueField(writer, "Pre-Depends", pkg.preDepends());
        writeMultiValueField(writer, "Suggests", pkg.suggests());
        writeMultiValueField(writer, "Recommends", pkg.recommends());
        writeMultiValueField(writer, "Replaces", pkg.replaces());
        writeMultiValueField(writer, "Breaks", pkg.breaks());
        writeMultiRowField(writer, "Description", pkg.description());
        writeField(writer, "Homepage", pkg.homepage());
        writeField(writer, "Description-md5", pkg.descriptionMd5());
        writeMultiValueField(writer, "Tag", pkg.tag());
        writeField(writer, "Section", pkg.section());
        writeField(writer, "Priority", pkg.priority());
        writeField(writer, "Filename", pkg.filename());
        writeField(writer, "Size", pkg.size() != 0 ? String.valueOf(pkg.size()) : null);
        writeField(writer, "MD5sum", pkg.md5sum());
        writeField(writer, "SHA256", pkg.sha256());
    }

    private static void writeField(BufferedWriter writer, String key, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            writer.write(key + ": " + value);
            writer.newLine();
        }
    }

    private static void writeMultiValueField(BufferedWriter writer, String key, List<String> values) throws IOException {
        if (values != null && !values.isEmpty()) {
            String joined = values.stream()
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
            writer.write(key + ": " + joined);
            writer.newLine();
        }
    }

    private static void writeMultiRowField(BufferedWriter writer, String key, String value) throws IOException {
        if (value != null && !value.isEmpty()) {
            String[] lines = value.split("\n");
            writer.write(key + ": " + lines[0]);
            writer.newLine();
            for (int i = 1; i < lines.length; i++) {
                writer.write(" " + lines[i]);
                writer.newLine();
            }
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Create sample packages (e.g., 0ad, 0ad-data, 0ad-data-common)
        List<DebianPackage> packages = new ArrayList<>();
        packages.add(new DebianPackage(
                "0ad", null, "0.0.26-3", 28591, "Debian Games Team <pkg-games-devel@lists.alioth.debian.org>", "amd64",
                Arrays.asList("0ad-data (>= 0.0.26)", "0ad-data (<= 0.0.26-3)", "0ad-data-common (>= 0.0.26)",
                        "0ad-data-common (<= 0.0.26-3)", "libboost-filesystem1.74.0 (>= 1.74.0)", "libc6 (>= 2.34)",
                        "libcurl3-gnutls (>= 7.32.0)", "libenet7", "libfmt9 (>= 9.1.0+ds1)", "libfreetype6 (>= 2.2.1)",
                        "libgcc-s1 (>= 3.4)", "libgloox18 (>= 1.0.24)", "libicu72 (>= 72.1~rc-1~)",
                        "libminiupnpc17 (>= 1.9.20140610)", "libopenal1 (>= 1.14)", "libpng16-16 (>= 1.6.2-1)",
                        "libsdl2-2.0-0 (>= 2.0.12)", "libsodium23 (>= 1.0.14)", "libstdc++6 (>= 12)",
                        "libvorbisfile3 (>= 1.1.2)", "libwxbase3.2-1 (>= 3.2.1+dfsg)", "libwxgtk-gl3.2-1 (>= 3.2.1+dfsg)",
                        "libwxgtk3.2-1 (>= 3.2.1+dfsg-2)", "libx11-6", "libxml2 (>= 2.9.0)", "zlib1g (>= 1:1.2.0)"),
                Arrays.asList("dpkg (>= 1.15.6~)"), null, null, null, null,
                "Real-time strategy game of ancient warfare\n0 A.D. is a free, open-source, historical Real Time Strategy (RTS) game.",
                "https://play0ad.com/", "d943033bedada21853d2ae54a2578a7b",
                Arrays.asList("game::strategy", "interface::graphical", "interface::x11", "role::program",
                        "uitoolkit::sdl", "uitoolkit::wxwidgets", "use::gameplaying", "x11::application"),
                "games", "optional", "pool/main/0/0ad/0ad_0.0.26-3_amd64.deb", 7891488,
                "4d471183a39a3a11d00cd35bf9f6803d", "3a2118df47bf3f04285649f0455c2fc6fe2dc7f0b237073038aa00af41f0d5f2"
        ));
        packages.add(new DebianPackage(
                "0ad-data", null, "0.0.26-1", 3218736, "Debian Games Team <pkg-games-devel@lists.alioth.debian.org>", "all",
                null, Arrays.asList("dpkg (>= 1.15.6~)"), Arrays.asList("0ad"), null, null, null,
                "Real-time strategy game of ancient warfare (data files)",
                "https://play0ad.com/", "26581e685027d5ae84824362a4ba59ee",
                Arrays.asList("role::app-data"), "games", "optional", "pool/main/0/0ad-data/0ad-data_0.0.26-1_all.deb",
                1377557908, "fc5ed8a20ce1861950c7ed3a5a615be0",
                "53745ae74d05bccf6783400fa98f3932b21729ab9d2e86151aa2c331c3455178"
        ));
        packages.add(new DebianPackage(
                "0ad-data-common", "0ad-data", "0.0.26-1", 2428, "Debian Games Team <pkg-games-devel@lists.alioth.debian.org>", "all",
                Arrays.asList("fonts-dejavu-core | ttf-dejavu-core", "fonts-freefont-ttf | ttf-freefont", "fonts-texgyre | tex-gyre"),
                Arrays.asList("dpkg (>= 1.15.6~)"), Arrays.asList("0ad"), null,
                Arrays.asList("0ad-data (<< 0.0.12-1~)"), Arrays.asList("0ad-data (<< 0.0.12-1~)"),
                "Real-time strategy game of ancient warfare (common data files)",
                "https://play0ad.com/", "8d014b839c4c4e9b6f82c7512d7e3496",
                Arrays.asList("game::strategy", "role::app-data", "role::program", "use::gameplaying"),
                "games", "optional", "pool/main/0/0ad-data/0ad-data-common_0.0.26-1_all.deb", 779908,
                "7ce70dc6e6de01134d2e199499fd3925", "0a40074c844a304688e503dd0c3f8b04e10e40f6f81b8bad260e07c54aa37864"
        ));

        String outputPath = "OutputPackages.gz";
        try {
            buildPackagesFile(packages, outputPath);
            System.out.println("Packages.gz file created at: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
