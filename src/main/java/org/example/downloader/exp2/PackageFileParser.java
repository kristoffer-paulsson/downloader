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

public class PackageFileParser implements Iterator<Map<String, StringBuilder>>, AutoCloseable {
    private final BufferedReader reader;
    private Map<String, StringBuilder> currentPackage;
    private String nextLine;
    private String currentField;

    public PackageFileParser(String filePath) throws IOException {
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
    public Map<String, StringBuilder> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more packages to parse");
        }
        Map<String, StringBuilder> data = currentPackage;
        currentPackage = new HashMap<>();
        currentField = null;
        try {
            nextLine = reader.readLine(); // Move to the next line
        } catch (IOException e) {
            e.printStackTrace();
            nextLine = null;
        }
        return data;
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

    @Override
    public void close() throws IOException {
        reader.close();
    }

    // Example usage
    public static void main(String[] args) {
        String[] filePaths = {
            "package-cache/dists/bookworm/main/binary-amd64/Packages.gz",
            "package-cache/dists/bookworm/contrib/binary-amd64/Packages.gz",
            "package-cache/dists/bookworm/non-free/binary-amd64/Packages.gz",
            "package-cache/dists/bookworm/non-free-firmware/binary-amd64/Packages.gz",
        };
        Set<String> fieldsToExtract = new HashSet<>();

        for (String filePath : filePaths) {
            try (PackageFileParser parser = new PackageFileParser(filePath)) {
                while (parser.hasNext()) {
                    Map<String, StringBuilder> packageData = parser.next();
                    fieldsToExtract.addAll(packageData.keySet());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Print the unique fields found in all packages
        System.out.println("Unique fields found in all packages:");
        for (String field : fieldsToExtract) {
            System.out.println(field);
        }
    }
}
