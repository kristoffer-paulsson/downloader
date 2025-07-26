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

import org.example.downloader.util.Pair;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaGenerator {

    public static JavaType[] oracleTypes = {
            JavaType.JDK
    };

    public static JavaVersion[] oracleVersions = {
            JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> oraclePlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.DEB),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.RPM),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.RPM)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.EXE),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.DMG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.DMG)
                    )
            )
    );

    public static List<URL> generateOracleDownladUrls() {
        List<URL> urls = new ArrayList<>();
        String baseUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s";
        for (JavaEdition javaEdition : List.of(JavaEdition.ORACLE)) {
            for (JavaVersion javaVersion : oracleVersions) {
                for (JavaType javaType : oracleTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> entry : oraclePlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaPackage> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaPackage javaPackage = pair.getSecond();
                            String url = String.format(
                                    baseUrl,
                                    javaVersion.getVersion(),
                                    javaType.getType(),
                                    javaVersion.getVersion(),
                                    javaPlatform.getOs(),
                                    javaArchitecture.getArch(),
                                    javaPackage.getPackageType()
                            );
                            try {
                                urls.add(new URI(url).toURL());
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        return urls;
    }

    public static void main(String[] args) {
    List<URL> urls = generateOracleDownladUrls();
        for (URL url : urls) {
            System.out.println(url);
        }
    }
}
