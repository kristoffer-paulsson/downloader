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
package org.example.downloader.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EnvironmentManager {

    public static String DIR_DOWNLOAD = "download_dir";

    protected final Path configPath;
    protected final Properties properties = new Properties();

    public EnvironmentManager(Path configFilePath) {
        this.configPath = configFilePath;

        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.createFile(configPath);
            }
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Java download environment properties", e);
        }
    }

    public Path getDownloadDir(String defaultDir) {
        return getDirectory(DIR_DOWNLOAD, defaultDir);
    }

    public void setDownloadDir(Path downloadDir) {
        setDirectory(DIR_DOWNLOAD, downloadDir);
    }

    public interface ToString<E> {
        String stringify(E e);
    }

    public Path getDirectory(String property, String defaultDir) {
        String someDir = get(property, defaultDir);
        if (someDir == null || someDir.isEmpty()) {
            throw new IllegalStateException("Download directory is not set in the environment");
        }
        return Paths.get(someDir);
    }

    public void setDirectory(String property, Path someDir) {
        if (someDir == null || !Files.isDirectory(someDir)) {
            throw new IllegalArgumentException("Download directory must be a valid directory");
        }
        set(property, someDir.toString());
    }

    public <E> void setMulti(String key, List<E> multi, ToString<E> name) {
        if(multi == null || multi.isEmpty()) {
            throw new IllegalArgumentException("Architectures list cannot be null or empty");
        }
        String value;
        if (multi.size() > 1) {
            ArrayList<String> array = new ArrayList<>();
            multi.forEach(arch -> {
                array.add(name.stringify(arch));
            });
            value = String.join(", ", array);
        } else {
            value = name.stringify(multi.get(0));
        }
        properties.setProperty(key, value);
    }

    public interface FromString<E> {
        E objectify(String s);
    }

    public <E> List<E> getMulti(String key, FromString<E> name) {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        String[] values = value.split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        List<E> objects = new ArrayList<>();
        for (String part : values) {
            E obj = name.objectify(part.trim());
            objects.add(obj);
        }
        return objects;
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

    protected <T> void streamSort(List<T> values, List<String> collection) {
        Arrays.stream(values.toArray()).sorted().iterator().forEachRemaining(value -> {
            collection.add(value.toString());
        });
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