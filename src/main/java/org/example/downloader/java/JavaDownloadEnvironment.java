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

import org.example.downloader.util.EnvironmentManager;
import org.example.downloader.util.Sha256Helper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaDownloadEnvironment extends EnvironmentManager {

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

    public JavaDownloadEnvironment(String environmentDirPath){
        super(Paths.get(environmentDirPath, ENVIRONMENT_FILE));
    }

    public Path getDownloadDir() {
        return getDownloadDir("java-downloads");
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

    protected <T> void streamSort(List<T> values, List<String> collection) {
        Arrays.stream(values.toArray()).sorted().iterator().forEachRemaining(value -> {
            collection.add(value.toString());
        });
    }

    public String hashOfConfiguration() {
        List<String> config = new ArrayList<>();

        streamSort(getArchitectures(), config);
        streamSort(getImages(), config);
        streamSort(getImplementations(), config);
        streamSort(getInstallers(), config);
        streamSort(getPlatforms(), config);
        streamSort(getVendors(), config);
        streamSort(getVersions(), config);

        return Sha256Helper.computeHash(String.join(",", config).toLowerCase()).substring(48).toUpperCase();
    }
}
