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
package org.example.downloader.ui;

import org.example.downloader.*;
import org.example.downloader.deb.DebianComponent;
import org.example.downloader.util.Menu;
import org.example.downloader.util.InversionOfControl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PackageMenu extends Menu {
    public PackageMenu(InversionOfControl ioc) {
        super(ioc, "Package lists");
    }

    @Override
    protected void setupMenu() {
        registerOption("Download packages lists", option -> downloadList());
        registerOption("Package lists stats", option -> packageStatistics());
        registerOption("Package chunk stat", option -> chunkStatistics());
        registerOption("Verify chunk blockchain", option -> new ChainForm(ioc).runForm());
    }

    private void downloadList() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        System.out.println("\n=== Download package lists ===");

        String dirPackage = configManager.get(ConfigManager.DIR_PKG);

        if(dirPackage == null || dirPackage.isEmpty()) {
            System.out.println("Package directory is not set in configuration, failed!");
        } else {
            System.out.println("Downloading/refreshing packages list from internet.");
            DebianPackagesListCache packageCache = ioc.resolve(DebianPackagesListCache.class);
            try {
                packageCache.downloadAndCachePackagesList();
                System.out.println("Package list saved, look at stats for more insight.");
            } catch (Exception e) {
                System.out.println("Failed to download package list, probably connections issues.");
            }
        }
    }

    private void packageStatistics() {
        System.out.println("\n=== Package lists statistics ===");
        System.out.println("Parsing and counting packages, wait...\n");

        DebianPackageChunkSplitter chunkSplitter = ioc.resolve(DebianPackageChunkSplitter.class);
        Iterator<DebianComponent> comps = Arrays.stream(DebianComponent.values()).iterator();

        while (comps.hasNext()) {
            DebianComponent comp = comps.next();
            AtomicLong totalSize = new AtomicLong();
            AtomicInteger packageCount = new AtomicInteger();
            List<ChunkSplit> chunks = chunkSplitter.loadAndParseAndChunkSplitPackages(comp);
            chunks.forEach(c -> c.packages.forEach(p -> {
                totalSize.addAndGet(p.getSize());
                packageCount.getAndIncrement();
            }));

            System.out.println("Total size of repository '" + comp.getComp() + "' with all " + packageCount + " packages: " + totalSize + " bytes " + sizeToGbString(totalSize.get()));
        }

        showMessageAndWait(" ");
    }

    private void chunkStatistics() {
        System.out.println("\n=== Package chunk statistics ===");
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        int chunkNum = Integer.parseInt(configManager.get(ConfigManager.PIECE));
        System.out.println("Parsing and counting packages for chunk " + chunkNum + ", wait...\n");

        DebianPackageChunkSplitter chunkSplitter = ioc.resolve(DebianPackageChunkSplitter.class);
        Iterator<DebianComponent> comps = Arrays.stream(DebianComponent.values()).iterator();

        long allSize = 0;
        int allCount = 0;

        while (comps.hasNext()) {
            DebianComponent comp = comps.next();
            AtomicLong totalSize = new AtomicLong();
            AtomicInteger packageCount = new AtomicInteger();
            ChunkSplit chunk = chunkSplitter.loadAndParseAndChunkSplitPackages(comp).get(chunkNum-1);
            chunk.packages.forEach(p -> {
                totalSize.addAndGet(p.getSize());
                packageCount.getAndIncrement();
            });

            allSize += totalSize.get();
            allCount += packageCount.get();

            System.out.println("Total size of chunk '" + comp.getComp() + "' with all " + packageCount + " packages: " + totalSize + " bytes " + sizeToGbString(totalSize.get()));
        }
        System.out.println("Total size of chunk " + chunkNum + " with all " + allCount + " packages: " + allSize + " bytes " + sizeToGbString(allSize));

        showMessageAndWait(" ");
    }

    private void blockchainVerifyDownload() {
        System.out.println("\n=== Verify package chunk blockchain ===");
        DebianPackageBlockchain blockchain = new DebianPackageBlockchain(ioc);

        try {
            System.out.println("Loading blockchain file: " + blockchain.getBlockchainFile());
            blockchain.verifyBlockchainCSVFile();
            System.out.println("Blockchain download verified successfully.");
        } catch (IOException e) {
            System.err.println("Error verifying blockchain download: " + e.getMessage());
        }

        showMessageAndWait(" ");
    }

    private String sizeToGbString(long totalSize) {
        return String.format("(%.2f GB)", totalSize / (1024.0 * 1024.0 * 1024.0));
    }
}
