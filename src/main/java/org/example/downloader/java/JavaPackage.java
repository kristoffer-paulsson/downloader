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

import org.example.downloader.util.BasePackage;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * architecture: ppc64
 * features: [],
 * file_type: tar.gz
 * filename: OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz
 * image_type: jdk
 * java_version: 10.0.2+13
 * jvm_impl: openj9
 * md5: e4522fb0776bdc9bcd358c8684f4d907
 * md5_file: OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz.md5
 * os: aix
 * release_type: ga
 * sha1: 4bbf0790db14743f070c48354e9cc376db73ae97
 * sha1_file: OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz.sha1
 * sha256: c0597a4a58533123c145139432ead20e4e3256b0546dfe5f9d4f81d0f192fe15
 * sha256_file: OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz.sha256
 * sha512: ba111055b9cbe14c21b3e4efcf3035a4560fb81a4a567e55e141d860d7e012e3042474ef88024dd34a9ff3b0be92f6d6aaac255dac4712d9fe9ece6abe4b0af3
 * sha512_file: OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz.sha512
 * size: 223727277,
 * url: https://github.com/AdoptOpenJDK/openjdk10-openj9-releases/releases/download/jdk-10.0.2%2B13_openj9-0.9.0/OpenJDK10-OPENJ9_ppc64_AIX_jdk-10.0.2.13_openj9-0.9.0.tar.gz
 * vendor: adoptopenjdk
 * version: 10.0.2+13.openj9-0.9.0
 */
public class JavaPackage implements BasePackage {

    private final JavaArchitecture arch;
    private final JavaInstaller file;

    private final String filename;

    private final JavaImage image;
    private final String javaVersion;
    private final JavaVersion version;
    private final JavaImplementation implementation;
    private final JavaPlatform platform;

    private final String sha256Digest;
    private final String size;
    private final String url;

    private final JavaVendor vendor;
    private final String vendorVersion;


    JavaPackage(
            String architecture,
            String fileType,
            String filename,
            String image,
            String javaVersion,
            String implementation,
            String platform,
            String sha256Digest,
            String size,
            String url,
            String vendor,
            String vendorVersion
    ) {
        this.arch = JavaArchitecture.fromString(architecture);
        this.file = JavaInstaller.fromString(fileType);
        this.filename = filename;
        this.image = JavaImage.fromString(image);
        this.javaVersion = javaVersion.trim();
        this.version = JavaVersion.fromString(javaVersion);
        this.implementation = JavaImplementation.fromString(implementation);
        this.platform = JavaPlatform.fromString(platform);
        this.sha256Digest = sha256Digest;
        this.size = size;
        this.url = url;
        this.vendor = JavaVendor.fromString(vendor);
        this.vendorVersion = vendorVersion.trim();
    }

    public JavaArchitecture getArch() {
        return arch;
    }

    public JavaInstaller getInstaller() {
        return file;
    }

    public JavaImage getImage() {
        return image;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public JavaVersion getVersion() {
        return version;
    }

    public JavaImplementation getImplementation() {
        return implementation;
    }

    public JavaPlatform getPlatform() {
        return platform;
    }

    public String getUrl() {
        return url;
    }

    public JavaVendor getVendor() {
        return vendor;
    }

    public String getVendorVersion() {
        return vendorVersion;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getSize() {
        return size;
    }

    public long getByteSize() {
        return Long.parseLong(size);
    }

    @Override
    public String getSha256Digest() {
        return sha256Digest;
    }

    public URL getRealUrl() throws MalformedURLException, URISyntaxException {
        return new URI(url).toURL();
    }

    public String uniqueKey() {
        return String.format("%s-%s-%s-%s-%s-%s-%s", getVendor(), getVersion(), getImplementation(), getImage(), getPlatform(), getArch(), getInstaller());
    }

    @Override
    public String toString() {
        return "JavaPackage{" +
                "arch=" + arch +
                ", file=" + file +
                ", filename='" + filename + '\'' +
                ", image=" + image +
                ", javaVersion='" + javaVersion + '\'' +
                ", version=" + version +
                ", implementation=" + implementation +
                ", platform=" + platform +
                ", sha256Digest='" + sha256Digest + '\'' +
                ", size='" + size + '\'' +
                ", url='" + url + '\'' +
                ", vendor=" + vendor +
                ", vendorVersion='" + vendorVersion + '\'' +
                '}';
    }
}
