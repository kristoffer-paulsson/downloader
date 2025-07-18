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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DebianPackageChunkSplitter {
    // Class to store package info
    static class Package {
        String name;
        long size;

        Package(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }

    public static void main(String[] args) {
        String filePath = "package-cache/dists/bookworm/main/binary-amd64/Packages.gz"; // Replace with your file path
        int numChunks = 5; // Number of chunks required (adjust as needed)
        List<Package> packages = new ArrayList<>();
        long totalSize = 0;

        // Step 1: Read Packages.gz and collect package names and sizes
        try (FileInputStream fis = new FileInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            String line;
            String currentPackage = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Package: ")) {
                    currentPackage = line.substring(9).trim();
                } else if (line.startsWith("Size: ") && currentPackage != null) {
                    try {
                        long size = Long.parseLong(line.substring(6).trim());
                        packages.add(new Package(currentPackage, size));
                        totalSize += size;
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid size format for package: " + currentPackage);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        // Step 2: Validate input
        if (packages.isEmpty()) {
            System.out.println("No packages found.");
            return;
        }
        if (numChunks < 1) {
            System.out.println("Number of chunks must be at least 1.");
            return;
        }
        if (numChunks > packages.size()) {
            System.out.println("Number of chunks cannot exceed number of packages (" + packages.size() + ").");
            return;
        }

        // Step 3: Split packages into exactly numChunks
        List<List<Package>> chunks = new ArrayList<>();
        List<Integer> startIndices = new ArrayList<>();
        List<Integer> endIndices = new ArrayList<>();
        long targetChunkSize = totalSize / numChunks;
        long accumulatedSize = 0;
        int packagesPerChunk = packages.size() / numChunks;
        int extraPackages = packages.size() % numChunks; // Distribute remainder packages

        int currentIndex = 0;
        for (int i = 0; i < numChunks; i++) {
            List<Package> chunk = new ArrayList<>();
            long chunkSize = 0;
            int numPackagesInChunk = packagesPerChunk + (i < extraPackages ? 1 : 0); // Distribute extra packages

            // Adjust chunk to approximate target size while ensuring all packages are included
            while (currentIndex < packages.size() && (chunk.isEmpty() || chunkSize < targetChunkSize || i == numChunks - 1)) {
                Package pkg = packages.get(currentIndex);
                chunk.add(pkg);
                chunkSize += pkg.size;
                currentIndex++;
                // For the last chunk, include all remaining packages
                if (i == numChunks - 1 && currentIndex < packages.size()) {
                    continue;
                }
                // Break if we exceed target size and not the last chunk
                if (chunkSize >= targetChunkSize && !chunk.isEmpty() && i < numChunks - 1) {
                    break;
                }
            }

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
                startIndices.add(currentIndex - chunk.size());
                endIndices.add(currentIndex - 1);
            }
        }

        // Step 4: Output the chunks
        System.out.printf("Total size: %,d bytes (%.2f GB)%n", totalSize, totalSize / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Target chunk size: %,d bytes (%.2f GB)%n", targetChunkSize, targetChunkSize / (1024.0 * 1024.0 * 1024.0));
        System.out.println("Number of chunks: " + chunks.size());
        System.out.println();

        for (int i = 0; i < chunks.size(); i++) {
            List<Package> chunk = chunks.get(i);
            long chunkSize = 0;
            System.out.printf("Chunk %d (Start Index: %d, End Index: %d):%n", (i + 1), startIndices.get(i), endIndices.get(i));
            for (Package pkg : chunk) {
                //System.out.printf("  Package: %s, Size: %,d bytes%n", pkg.name, pkg.size);
                chunkSize += pkg.size;
            }
            System.out.printf("  Total size for chunk %d: %,d bytes (%.2f GB)%n", (i + 1), chunkSize, chunkSize / (1024.0 * 1024.0 * 1024.0));
            System.out.println();
        }
    }
}
