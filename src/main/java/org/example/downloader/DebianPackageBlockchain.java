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

import java.nio.file.Path;

public class DebianPackageBlockchain {
    public final static String BLOCKCHAIN_DIR = "chain";
    private final Path packageDir;
    private final Path chainDir;

    DebianPackageBlockchain(ConfigManager configManager) {
        this.packageDir = Path.of(configManager.get("package_dir"));
        this.chainDir = Path.of(configManager.get("cache_dir"), BLOCKCHAIN_DIR);
    }
}
