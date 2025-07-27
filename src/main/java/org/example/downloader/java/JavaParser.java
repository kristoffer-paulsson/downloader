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

import org.example.downloader.util.AbstractFileParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public class JavaParser extends AbstractFileParser<JavaPackage> {

    public Map<String, Set<String>> statistics = Map.of(
            "architecture", new HashSet<>(),
            "file_type", new HashSet<>(),
            "image_type", new HashSet<>(),
            "os", new HashSet<>(),
            "vendor", new HashSet<>(),
            "java_version", new HashSet<>(),
            "jvm_impl", new HashSet<>()
    );

    public JavaParser(String filePath) throws IOException {
        super(filePath);
    }

    @Override
    protected JavaPackage parseFieldsAndCreatePackage(Map<String, StringBuilder> packageData) {
        statistics.get("architecture").add(packageData.getOrDefault("architecture", new StringBuilder()).toString());
        statistics.get("file_type").add(packageData.getOrDefault("file_type", new StringBuilder()).toString());
        statistics.get("image_type").add(packageData.getOrDefault("image_type", new StringBuilder()).toString());
        statistics.get("os").add(packageData.getOrDefault("os", new StringBuilder()).toString());
        statistics.get("vendor").add(packageData.getOrDefault("vendor", new StringBuilder()).toString());
        statistics.get("java_version").add(packageData.getOrDefault("java_version", new StringBuilder()).toString());
        statistics.get("jvm_impl").add(packageData.getOrDefault("jvm_impl", new StringBuilder()).toString());

        /**
         *             String architecture,
         *             String file_type,
         *             String filename,
         *             String image_type,
         *             String java_version,
         *             String jvm_impl,
         *             String os,
         *             String sha256,
         *             String size,
         *             String url,
         *             String vendor,
         *             String version
         * */

        return new JavaPackage(
                packageData.getOrDefault("architecture", new StringBuilder()).toString(),
                packageData.getOrDefault("file_type", new StringBuilder()).toString(),
                packageData.getOrDefault("filename", new StringBuilder()).toString(),
                packageData.getOrDefault("image_type", new StringBuilder()).toString(),
                packageData.getOrDefault("java_version", new StringBuilder()).toString(),
                packageData.getOrDefault("jvm_impl", new StringBuilder()).toString(),
                packageData.getOrDefault("os", new StringBuilder()).toString(),
                packageData.getOrDefault("sha256", new StringBuilder()).toString(),
                packageData.getOrDefault("size", new StringBuilder()).toString(),
                packageData.getOrDefault("url", new StringBuilder()).toString(),
                packageData.getOrDefault("vendor", new StringBuilder("0")).toString(),
                packageData.getOrDefault("version", new StringBuilder()).toString()
        );
    }

    public static void main(String[] args) {
        try {
            JavaParser parser = new JavaParser("Java.gz");
            while (parser.hasNext()) {
                JavaPackage pkg = parser.next();
                System.out.println("Parsed package: " + pkg.toString());
            }
            parser.close();

            parser.statistics.forEach((key, value) -> {
                System.out.println(key + ": " + value);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
