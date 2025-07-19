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

import org.example.downloader.deb.DebianComponent;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;


public class DebianPackageChunkSplitter {

    private final InversionOfControl ioc;
    private final ConfigManager configManager;
    private final String distribution;

    private final Map<DebianComponent, List<DebianPackage>> packages = Map.of();

    DebianPackageChunkSplitter(InversionOfControl ioc) {
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.distribution = configManager.get(ConfigManager.DIST);
    }

    public DebianPackage buildDebianPackage(String packageName, BufferedReader reader) {

        String version = null;
        String filename = null;
        String architecture = null;
        String sha256digest = null;
        long downloadSize = 0;

        String line = packageName;
        try {
            while(!(line = reader.readLine()).trim().isEmpty()) {
                if(line.startsWith("Version: ")) version = line.substring(9).trim();
                if(line.startsWith("Filename: ")) filename = line.substring(10).trim();
                if(line.startsWith("Architecture: ")) architecture = line.substring(14).trim();
                if(line.startsWith("SHA256: ")) sha256digest = line.substring(8).trim();
                if(line.startsWith("Size: ")) downloadSize = Long.parseLong(line.substring(6).trim());;
            }
        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException(e);
        }

        if (packageName != null && version != null) {
            return new DebianPackage(packageName, version, architecture, filename, downloadSize, sha256digest, distribution);
        }
        return null;
    }

    public List<DebianPackage> extractPackagesFromList(File filePath) {
        List<DebianPackage> packages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Package: ")) {
                    packages.add(buildDebianPackage(line.substring(9).trim(), reader));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return packages;
        }

        return packages;
    }

    public List<ChunkSplit> splitPackagesIntoChunks(int numChunks, long totalSize, List<DebianPackage> packages) {
        List<ChunkSplit> chunks = new ArrayList<>();

        long targetChunkSize = totalSize / numChunks;

        int currentIndex = 0;
        for (int i = 0; i < numChunks; i++) {
            List<DebianPackage> chunk = new ArrayList<>();
            long chunkSize = 0;

            // Adjust chunk to approximate target size while ensuring all packages are included
            while (currentIndex < packages.size() && (chunk.isEmpty() || chunkSize < targetChunkSize || i == numChunks - 1)) {
                DebianPackage pkg = packages.get(currentIndex);
                chunk.add(pkg);
                chunkSize += pkg.getSize();
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
                chunks.add(new ChunkSplit(
                        chunk,
                        i + 1,
                        currentIndex - chunk.size(),
                        currentIndex - 1
                ));
            }
        }

        return chunks;
    }

    public long countPackageList(List<DebianPackage> packages) {
        long totalSize = 0;
        Iterator<DebianPackage> packIter = packages.stream().iterator();
        while (packIter.hasNext()) {
            totalSize += packIter.next().getSize();
        }
        return totalSize;
    }

    public List<ChunkSplit> loadAndParseAndChunkSplitPackages(DebianComponent comp) {
        DebianPackagesListCache packageCache = ioc.resolve(DebianPackagesListCache.class);
        Path filePath = packageCache.repositoryPath(comp);
        int numChunks = Integer.parseInt(configManager.get(ConfigManager.CHUNKS));
        List<DebianPackage> packages = extractPackagesFromList(filePath.toFile());
        long totalSize = countPackageList(packages);

        // Step 2: Validate input
        if (packages.isEmpty()) {
            System.out.println("No packages found.");
        }
        if (numChunks < 1) {
            System.out.println("Number of chunks must be at least 1.");
        }
        if (numChunks > packages.size()) {
            System.out.println("Number of chunks cannot exceed number of packages (" + packages.size() + ").");
        }

        return splitPackagesIntoChunks(numChunks, totalSize, packages);
    }

    public DebianWorkerIterator workerIterator(DebianComponent comp) {
        if(!packages.containsKey(comp)) {
            packages.put(comp, loadAndParseAndChunkSplitPackages(comp).get(Integer.parseInt(configManager.get(ConfigManager.PIECE))).packages);
        }

        return new DebianWorkerIterator(ioc, packages.get(comp));
    }

    public DebianWorkerIterator jointWorkerIterator() {
        Arrays.stream(DebianComponent.values()).iterator().forEachRemaining(c -> {
            if(!packages.containsKey(c)) {
                packages.put(c, loadAndParseAndChunkSplitPackages(c).get(Integer.parseInt(configManager.get(ConfigManager.PIECE))).packages);
            }
        });

        List<DebianPackage> all = new ArrayList<>(List.of());
        packages.forEach((c, d) -> all.addAll(d));
        return new DebianWorkerIterator(ioc, all);
    }

    public void printStats() {
        String filePath = "package-cache/dists/bookworm/main/binary-amd64/Packages.gz"; // Replace with your file path
        int numChunks = 7; // Number of chunks required (adjust as needed)
        List<DebianPackage> packages = extractPackagesFromList(Paths.get(filePath).toFile());
        long totalSize = countPackageList(packages);

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

        List<ChunkSplit> chunks = splitPackagesIntoChunks(numChunks, totalSize, packages);

        long targetChunkSize = totalSize / numChunks;

        // Step 4: Output the chunks
        System.out.printf("Total size: %,d bytes (%.2f GB)%n", totalSize, totalSize / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Target chunk size: %,d bytes (%.2f GB)%n", targetChunkSize, targetChunkSize / (1024.0 * 1024.0 * 1024.0));
        System.out.println("Number of chunks: " + chunks.size());
        System.out.println();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkSplit chunk = chunks.get(i);
            List<DebianPackage> pkgs = chunk.packages;
            long chunkSize = 0;
            System.out.printf("Chunk %d (Start Index: %d, End Index: %d):%n", (i + 1), chunk.startIndex, chunk.endIndex);
            for (DebianPackage pkg : pkgs) {
                //System.out.printf("  Package: %s, Size: %,d bytes%n", pkg.name, pkg.size);
                chunkSize += pkg.getSize();
            }
            System.out.printf("  Total size for chunk %d: %,d bytes (%.2f GB)%n", (i + 1), chunkSize, chunkSize / (1024.0 * 1024.0 * 1024.0));
            System.out.println();
        }
    }
}
