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

import org.example.downloader.ConfigManager;
import org.example.downloader.DebianMirrorCache;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.Menu;

public class MirrorMenu extends Menu {
    public MirrorMenu(InversionOfControl ioc) {
        super(ioc, "Mirror websites");
    }

    @Override
    protected void setupMenu() {
        registerOption("Download mirrors", option -> downloadMirrors());
        registerOption("Mirror stats", option -> mirrorStatistics());
    }

    private void downloadMirrors() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        System.out.println("\n=== Download mirrors ===");

        String dirCache = configManager.get(ConfigManager.DIR_CACHE);

        if(dirCache == null || dirCache.isEmpty()) {
            System.out.println("Cache directory is not set in configuration, failed!");
        } else {
            System.out.println("Downloading/refreshing mirror list from internet.");
            DebianMirrorCache mirrorCache = ioc.resolve(DebianMirrorCache.class);
            mirrorCache.downloadAndCacheMirrors();
            mirrorCache.loadCachedMirrors(false);
            System.out.println("Currently " + mirrorCache.mirrorCount() + " mirrors are available.");
        }
    }

    private void mirrorStatistics() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        System.out.println("\n=== Mirror statistics ===");
        DebianMirrorCache mirrorCache = ioc.resolve(DebianMirrorCache.class);
        mirrorCache.loadCachedMirrors(false);
        System.out.println("Currently " + mirrorCache.mirrorCount() + " mirrors are available.");
        showMessageAndWait(" ");
    }
}
