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

import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianDistribution;
import org.example.downloader.ui.MainMenu;

import java.io.*;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    private static final String DEFAULT_CONFIG = "config.properties";

    private static InversionOfControl ioc = null;

    public static void main(String[] args) throws Exception {
        initializeIoC(args);
        MainMenu menu = new MainMenu(ioc);
        menu.runMenu();
        //DebianMirrorCache mirrorCahce = ioc.resolve(DebianMirrorCache.class);
        //if(mirrorCahce.mirrorCount() == 0) mirrorCahce.loadCachedMirrors(true);
    }

    private static void initializeIoC(String[] args) {
        ioc = new InversionOfControl();

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        ioc.register(ConfigManager.class, () -> {
            try {
                return new ConfigManager(configPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ioc.register(DebianPackagesListCache.class, () -> new DebianPackagesListCache(ioc.resolve(ConfigManager.class)));

        ioc.register(Iterator.class, () -> ioc.resolve(DebianPackagesListCache.class).parseCachedPackagesList().iterator());

        ioc.register(DebianMirrorCache.class, () -> new DebianMirrorCache(ioc.resolve(ConfigManager.class)));

        ioc.register(Scanner.class, () -> new Scanner(System.in));

    }
}