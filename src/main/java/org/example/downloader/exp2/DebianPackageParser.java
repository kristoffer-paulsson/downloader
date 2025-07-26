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
import java.util.zip.GZIPInputStream;

/**
 * Parses Debian package files in the "Packages.gz" format.
 * This class implements Iterator<DebianPackage> to allow iteration over packages.
 *
 * Enhances
 * Size
 * Pre-Depends
 * Important
 * Cnf-Extra-Commands
 * Essential
 * Cnf-Ignore-Commands
 * Version
 * Gstreamer-Encoders
 * Static-Built-Using
 * Python-Egg-Name
 * Built-Using
 * Protected
 * Architecture
 * Priority
 * Filename
 * Gstreamer-Uri-Sinks
 * Gstreamer-Uri-Sources
 * Cnf-Visible-Pkgname
 * MD5sum
 * Original-Maintainer
 * Gstreamer-Version
 * Ghc-Package
 * Lua-Versions
 * Gstreamer-Decoders
 * Package
 * Depends
 * Go-Import-Path
 * Python-Version
 * Description
 * Build-Ids
 * Recommends
 * X-Cargo-Built-Using
 * Description-md5
 * Source
 * Maintainer
 * Ruby-Versions
 * Suggests
 * Multi-Arch
 * Javascript-Built-Using
 * Postgresql-Catversion
 * Breaks
 * Homepage
 * Efi-Vendor
 * Replaces
 * Conflicts
 * Provides
 * Section
 * Build-Essential
 * SHA256
 * Installed-Size
 * Tag
 * Gstreamer-Elements
 */
public class DebianPackageParser implements Iterator<DebianPackage>, AutoCloseable {
    private final BufferedReader reader;
    private Map<String, StringBuilder> currentPackage;
    private String nextLine;
    private String currentField;

    public DebianPackageParser(String filePath) throws IOException {
        InputStream fileStream = new FileInputStream(filePath);
        GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
        reader = new BufferedReader(new InputStreamReader(gzipStream));
        currentPackage = new HashMap<>();
        currentField = null;
        nextLine = reader.readLine(); // Read the first line
    }

    @Override
    public boolean hasNext() {
        try {
            while (nextLine != null) {
                if (nextLine.trim().isEmpty() && !currentPackage.isEmpty()) {
                    return true; // Ready to yield a package
                }
                processLine();
            }
            // Check for a final package
            if (!currentPackage.isEmpty()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public DebianPackage next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more packages to parse");
        }
        DebianPackage packageRecord = createDebianPackage(currentPackage);
        currentPackage = new HashMap<>();
        currentField = null;
        try {
            nextLine = reader.readLine(); // Move to the next line
        } catch (IOException e) {
            e.printStackTrace();
            nextLine = null;
        }
        return packageRecord;
    }

    private void processLine() throws IOException {
        while (nextLine != null && !nextLine.trim().isEmpty()) {
            if (nextLine.startsWith(" ")) {
                if (currentField != null) {
                    currentPackage.computeIfAbsent(currentField, k -> new StringBuilder())
                            .append("\n")
                            .append(nextLine.trim());
                }
            } else {
                int colonIndex = nextLine.indexOf(":");
                if (colonIndex != -1) {
                    currentField = nextLine.substring(0, colonIndex).trim();
                    String value = nextLine.substring(colonIndex + 1).trim();
                    currentPackage.computeIfAbsent(currentField, k -> new StringBuilder())
                            .append(value);
                }
            }
            nextLine = reader.readLine();
        }
    }

    private DebianPackage createDebianPackage(Map<String, StringBuilder> fields) {
        String packageName = getField(fields, "Package", "");
        String source = getField(fields, "Source", null);
        String version = getField(fields, "Version", "");
        int installedSize = parseIntField(fields, "Installed-Size", 0);
        String maintainer = getField(fields, "Maintainer", "");
        String architecture = getField(fields, "Architecture", "");
        List<String> depends = parseMultiValueField(fields, "Depends");
        List<String> preDepends = parseMultiValueField(fields, "Pre-Depends");
        List<String> suggests = parseMultiValueField(fields, "Suggests");
        List<String> recommends = parseMultiValueField(fields, "Recommends");
        List<String> replaces = parseMultiValueField(fields, "Replaces");
        List<String> breaks = parseMultiValueField(fields, "Breaks");
        String description = getField(fields, "Description", "");
        String homepage = getField(fields, "Homepage", null);
        String descriptionMd5 = getField(fields, "Description-md5", "");
        List<String> tag = parseMultiValueField(fields, "Tag");
        String section = getField(fields, "Section", "");
        String priority = getField(fields, "Priority", "");
        String filename = getField(fields, "Filename", "");
        long size = parseLongField(fields, "Size", 0L);
        String md5sum = getField(fields, "MD5sum", "");
        String sha256 = getField(fields, "SHA256", "");

        return new DebianPackage(
                packageName, source, version, installedSize, maintainer, architecture,
                depends, preDepends, suggests, recommends, replaces, breaks, description,
                homepage, descriptionMd5, tag, section, priority, filename, size, md5sum, sha256
        );
    }

    private String getField(Map<String, StringBuilder> fields, String key, String defaultValue) {
        StringBuilder value = fields.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int parseIntField(Map<String, StringBuilder> fields, String key, int defaultValue) {
        StringBuilder value = fields.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                // Log error if needed
            }
        }
        return defaultValue;
    }

    private long parseLongField(Map<String, StringBuilder> fields, String key, long defaultValue) {
        StringBuilder value = fields.get(key);
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                // Log error if needed
            }
        }
        return defaultValue;
    }

    private List<String> parseMultiValueField(Map<String, StringBuilder> fields, String key) {
        StringBuilder value = fields.get(key);
        if (value == null) {
            return null;
        }
        return Arrays.stream(value.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    // Example usage
    public static void main(String[] args) {
        String filePath = "Packages.gz";
        try (DebianPackageParser parser = new DebianPackageParser(filePath)) {
            int count = 0;
            while (parser.hasNext() && count < 5) { // Limit to 5 packages for demo
                DebianPackage pkg = parser.next();
                System.out.println("Package: " + pkg.packageName());
                System.out.println("Source: " + pkg.source());
                System.out.println("Version: " + pkg.version());
                System.out.println("Installed-Size: " + pkg.installedSize());
                System.out.println("Maintainer: " + pkg.maintainer());
                System.out.println("Architecture: " + pkg.architecture());
                System.out.println("Depends: " + pkg.depends());
                System.out.println("Pre-Depends: " + pkg.preDepends());
                System.out.println("Suggests: " + pkg.suggests());
                System.out.println("Recommends: " + pkg.recommends());
                System.out.println("Replaces: " + pkg.replaces());
                System.out.println("Breaks: " + pkg.breaks());
                System.out.println("Description: " + pkg.description());
                System.out.println("Homepage: " + pkg.homepage());
                System.out.println("Description-md5: " + pkg.descriptionMd5());
                System.out.println("Tag: " + pkg.tag());
                System.out.println("Section: " + pkg.section());
                System.out.println("Priority: " + pkg.priority());
                System.out.println("Filename: " + pkg.filename());
                System.out.println("Size: " + pkg.size());
                System.out.println("MD5sum: " + pkg.md5sum());
                System.out.println("SHA256: " + pkg.sha256());
                System.out.println("---");
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
