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

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigManager {
    public static String DIST = "distribution";
    public static String ARCH = "architecture";
    public static String COMP = "component";
    public static String DIR_CACHE = "cache_dir";
    public static String DIR_PKG = "package_dir";

    public static String CHUNKS = "chunk_partitions";
    public static String PIECE = "chunk_piece";

    private final Path configPath;
    private final Properties properties = new Properties();

    public ConfigManager(String configFilePath) throws IOException {
        this.configPath = Paths.get(configFilePath);
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            }
        }
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public void save() throws IOException {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            properties.store(out, "Debian Downloader Configuration");
        }
    }

    public void reload() throws IOException {
        properties.clear();
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            }
        }
    }

    public Properties getProperties() {
        return properties;
    }
}