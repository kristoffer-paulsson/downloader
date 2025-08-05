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

import org.example.downloader.util.EnvironmentManager;

import java.nio.file.Path;


public class GeneralEnvironment extends EnvironmentManager {

    public static String DIR_CACHE = "cache_dir";
    public final static String BLOCKCHAIN_DIR = "chain";


    public GeneralEnvironment(Path configFilePath) {
        super(configFilePath);
    }

    public Path getCacheDir() {
        return getDownloadDir("runtime-cache");
    }

    public Path getCacheDir(String defaultDir) {
        return getDirectory(DIR_CACHE, defaultDir);
    }

    public void setCacheDir(Path cacheDir) {
        setDirectory(DIR_CACHE, cacheDir);
    }

    public Path getChainDir() {
        return getCacheDir().resolve(BLOCKCHAIN_DIR);
    }
}
