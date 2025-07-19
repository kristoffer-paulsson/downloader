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
import org.example.downloader.deb.Menu;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class PackageMenu extends Menu {
    public PackageMenu(InversionOfControl ioc) {
        super(ioc, "Package lists");
    }

    @Override
    protected void setupMenu() {
        registerOption("Download packages lists", option -> downloadList());
        registerOption("Package lists stats", option -> packageStatistics());
        registerOption("Package chunk stat", option -> chunkStatistics());
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

        Iterator<DebianComponent> comps = Arrays.stream(DebianComponent.values()).iterator();

        while (comps.hasNext()) {
            statistics(comps.next());
        }

        showMessageAndWait(" ");
    }

    private void chunkStatistics() {
        System.out.println("\n=== Package chunk statistics ===");
        System.out.println("Parsing and counting package chunk, wait...\n");

        Iterator<DebianComponent> comps = Arrays.stream(DebianComponent.values()).iterator();

        while (comps.hasNext()) {
            statistics(comps.next());
        }

        showMessageAndWait(" ");
    }

    private void statistics(DebianComponent component) {
        DebianPackagesListCache packageCache = ioc.resolve(DebianPackagesListCache.class);
        Path filePath = packageCache.repositoryPath(component);

        long totalSize = 0;
        int packageCount = 0;

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Size: ")) {
                    try {
                        // Extract the size value after "Size: "
                        String sizeStr = line.substring(6).trim();
                        long size = Long.parseLong(sizeStr);
                        totalSize += size;
                        packageCount++;
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid size format in line: " + line);
                    }
                }
            }

            System.out.println("Total size of repository '" + component.getComp() + "' with all " + packageCount + " packages: " + totalSize + " bytes " + sizeToGbString(totalSize));

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private String sizeToGbString(long totalSize) {
        return String.format("(%.2f GB)", totalSize / (1024.0 * 1024.0 * 1024.0));
    }
}
