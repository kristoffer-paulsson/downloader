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
package org.example.downloader.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class JavaDownloadEnvironment {

    public static final String ENVIRONMENT_FILE = "java-download.properties";

    public enum EnvironmentKey {
        ARCH("java_arch"),
        IMAGE("java_image"),
        IMPLEMENTATION("java_impl"),
        INSTALLER("java_installer"),
        PLATFORM("java_platform"),
        VENDOR("java_vendor"),
        VERSION("java_version");

        private final String key;

        EnvironmentKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private final Properties properties = new Properties();
    private final Path configPath;

    public JavaDownloadEnvironment(String environmentDirPath) throws IOException {
        this.configPath = Paths.get(environmentDirPath, ENVIRONMENT_FILE);
        if (!Files.exists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        }
    }

    public void setArchitectures(List<JavaArchitecture> architectures) {
        setMulti(EnvironmentKey.ARCH.getKey(), architectures, JavaArchitecture::getArch);
    }

    public List<JavaArchitecture> getArchitectures() {
        return getMulti(EnvironmentKey.ARCH.getKey(), JavaArchitecture::fromString);
    }

    public void setImages(List<JavaImage> images) {
        setMulti(EnvironmentKey.IMAGE.getKey(), images, JavaImage::getImage);
    }

    public List<JavaImage> getImages() {
        return getMulti(EnvironmentKey.IMAGE.getKey(), JavaImage::fromString);
    }

    public void setImplementations(List<JavaImplementation> implementations) {
        setMulti(EnvironmentKey.IMPLEMENTATION.getKey(), implementations, JavaImplementation::getImplementation);
    }

    public List<JavaImplementation> getImplementations() {
        return getMulti(EnvironmentKey.IMPLEMENTATION.getKey(), JavaImplementation::fromString);
    }

    public void setInstallers(List<JavaInstaller> installers) {
        setMulti(EnvironmentKey.INSTALLER.getKey(), installers, JavaInstaller::getInstaller);
    }

    public List<JavaInstaller> getInstallers() {
        return getMulti(EnvironmentKey.INSTALLER.getKey(), JavaInstaller::fromString);
    }

    public void setPlatforms(List<JavaPlatform> platforms) {
        setMulti(EnvironmentKey.PLATFORM.getKey(), platforms, JavaPlatform::getPlatform);
    }

    public List<JavaPlatform> getPlatforms() {
        return getMulti(EnvironmentKey.PLATFORM.getKey(), JavaPlatform::fromString);
    }

    public void setVendors(List<JavaVendor> vendors) {
        setMulti(EnvironmentKey.VENDOR.getKey(), vendors, JavaVendor::getVendor);
    }

    public List<JavaVendor> getVendors() {
        return getMulti(EnvironmentKey.VENDOR.getKey(), JavaVendor::fromString);
    }

    public void setVersions(List<JavaVersion> versions) {
        setMulti(EnvironmentKey.VERSION.getKey(), versions, JavaVersion::getVersion);
    }

    public List<JavaVersion> getVersions() {
        return getMulti(EnvironmentKey.VERSION.getKey(), JavaVersion::fromString);
    }

    public interface ToString<E> {
        String stringify(E e);
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
            value = String.join(",", array);
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
