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
package org.example.downloader.deb;

import org.example.downloader.java.*;
import org.example.downloader.util.AbstractFileParser;
import org.example.downloader.util.MultiIterator;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *     public final String packageName;
 *     public final String version;
 *     public final String architecture;
 *     public final String filename;
 *     public final long size;
 *     public final String sha256digest;
 *     public final String distribution;
 */
public class DebianParser extends AbstractFileParser<DebianPackage> {

    private final DebianDownloadEnvironment dde;

    public DebianParser(DebianDownloadEnvironment dde, String filePath) throws IOException {
        super(filePath);
        this.dde = dde;
    }

    public DebianParser(DebianDownloadEnvironment dde, InputStream fileStream) throws IOException {
        super(fileStream);
        this.dde = dde;
    }

    @Override
    protected DebianPackage parseFieldsAndCreatePackage(Map<String, StringBuilder> packageData) {
        /**
         *     public final String packageName;
         *     public final String version;
         *     public final String architecture;
         *     public final String filename;
         *     public final long size;
         *     public final String sha256digest;
         *     public final String distribution;
         * */

        return new DebianPackage(
                packageData.getOrDefault("Package", new StringBuilder()).toString(),
                packageData.getOrDefault("Version", new StringBuilder()).toString(),
                dde.getArchitecture().getArch(),
                packageData.getOrDefault("Filename", new StringBuilder()).toString(),
                Long.parseLong(packageData.getOrDefault("Size", new StringBuilder()).toString()),
                packageData.getOrDefault("SHA256", new StringBuilder()).toString(),
                dde.getDistribution().getDist()
        );
    }

    public static MultiIterator<DebianPackage> createAllIterator(DebianDownloadEnvironment dde) {
        try {
            return new MultiIterator<>(
                    new DebianParser(dde, DebianMetadataDownloader.repositoryFile(dde, DebianComponent.CONTRIB).toString()),
                    new DebianParser(dde, DebianMetadataDownloader.repositoryFile(dde, DebianComponent.MAIN).toString()),
                    new DebianParser(dde, DebianMetadataDownloader.repositoryFile(dde, DebianComponent.NON_FREE).toString()),
                    new DebianParser(dde, DebianMetadataDownloader.repositoryFile(dde, DebianComponent.NON_FREE_FIRMWARE).toString())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Long totalSizeForAllChunks(DebianDownloadEnvironment dde) {
        AtomicLong totalByteSize = new AtomicLong();
        MultiIterator<DebianPackage> allIterator = createAllIterator(dde);

        allIterator.forEachRemaining((pkg) -> {
            totalByteSize.addAndGet(pkg.getByteSize());
        });

        return totalByteSize.get();
    }

    public static List<ChunkSplit> chunkPackages(DebianDownloadEnvironment dde) {
        AtomicLong totalByteSize = new AtomicLong();
        List<DebianPackage> packages = new ArrayList<>();
        List<ChunkSplit> chunks = new ArrayList<>();

        MultiIterator<DebianPackage> allIterator = createAllIterator(dde);
        allIterator.forEachRemaining((pkg) -> {
            packages.add(pkg);
            totalByteSize.addAndGet(pkg.getByteSize());
        });

        int numChunks = dde.getChunks();

        long targetChunkSize = totalByteSize.get() / numChunks;

        int currentIndex = 0;
        for (int i = 0; i < numChunks; i++) {
            List<DebianPackage> chunk = new ArrayList<>();
            long chunkSize = 0;

            // Adjust chunk to approximate target size while ensuring all packages are included
            while (currentIndex < packages.size() && (chunk.isEmpty() || chunkSize < targetChunkSize || i == numChunks - 1)) {
                DebianPackage pkg = packages.get(currentIndex);
                chunk.add(pkg);
                chunkSize += pkg.getByteSize();
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
            } else {
                // If no packages were added, create an empty chunk
                chunks.add(new ChunkSplit(
                        Collections.emptyList(),
                        i + 1,
                        currentIndex - 1,
                        currentIndex - 1
                ));
            }
        }

        return chunks;
    }
}
