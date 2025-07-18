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

import java.util.Iterator;

public class PackageMenu extends Menu {
    public PackageMenu(InversionOfControl ioc) {
        super(ioc, "Packages list");
    }

    @Override
    protected void setupMenu() {
        registerOption("Download packages list", option -> downloadList());
        registerOption("Package list stats", option -> packageStatistics());
    }

    private void downloadList() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        System.out.println("\n=== Download packages list ===");

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
        System.out.println("\n=== Package list statistics ===");
        System.out.println("Parsing and counting packages, wait...");

        DebianPackagesListCache packageCache = ioc.resolve(DebianPackagesListCache.class);
        long totalDownloadSize = 0;
        int packageCount = 0;
        Iterator<DebianPackage> packages = packageCache.parseCachedPackagesList(DebianComponent.MAIN).iterator();

        while(packages.hasNext()) {
            DebianPackage pkg = packages.next();
            packageCount++;
            totalDownloadSize += Long.parseLong(pkg.size());
        }

        System.out.println("Total packages: " + packageCount);
        System.out.println(String.format("Total download size: %s", totalDownloadSize));
        showMessageAndWait(" ");
    }
}
