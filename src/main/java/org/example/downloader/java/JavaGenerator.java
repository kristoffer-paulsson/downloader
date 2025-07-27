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

import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
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

    public static List<URL> generateOracleDownloadUrls() {
        List<URL> urls = new ArrayList<>();
        String baseUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.ORACLE)) {
            for (JavaVersion javaVersion : oracleVersions) {
                for (JavaType javaType : oracleTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> entry : oraclePlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaPackage> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaPackage javaPackage = pair.getSecond();

                            String version = javaVersion.getVersion();
                            String type = javaType.getType();
                            String platform = javaPlatform.getOs();
                            String architecture = javaArchitecture.getArch();
                            String packageType = javaPackage.getPackageType();

                            if(javaVersion == JavaVersion.JAVA_17) {
                                continue; // Oracle does not provide free access JDK 17 downloads anymore, only JDK 21
                            }

                            String url = String.format(
                                    baseUrl,
                                    version,
                                    type,
                                    version,
                                    platform,
                                    architecture,
                                    packageType
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

    public static JavaType[] correttoTypes = {
            JavaType.JDK
    };

    public static JavaVersion[] correttoVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> correttoPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.DEB),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.RPM),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.DEB),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.RPM)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.PKG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.PKG)
                    )
            )
    );

    public static List<URL> generateCorrettoDownloadUrls() {
        List<URL> urls = new ArrayList<>();
        // https://corretto.aws/[latest/latest_checksum]/amazon-corretto-[corretto_version]-[cpu_arch]-[os]-[package_type].[file_extension]
        String baseUrl = "https://corretto.aws/downlaods/latest/amazon-corretto-%s-%s-%s-%s.%s";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.CORRETTO)) {
            for (JavaVersion javaVersion : correttoVersions) {
                for (JavaType javaType : correttoTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> entry : correttoPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaPackage> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaPackage javaPackage = pair.getSecond();

                            String version = javaVersion.getVersion();
                            String type = javaType.getType();
                            String platform = javaPlatform.getOs();
                            String architecture = javaArchitecture.getArch();
                            String packageType = javaPackage.getPackageType();

                            String url = String.format(
                                    baseUrl,
                                    version,
                                    architecture,
                                    platform,
                                    type,
                                    packageType
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

    // https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jre_aarch64_linux_hotspot_8u462b08.tar.gz
    // https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_aarch64_linux_hotspot_8u462b08.tar.gz

    // https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28+6/OpenJDK11U-jre_aarch64_mac_hotspot_11.0.28_6.pkg.sig
    // https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28+6/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.28_6.tar.gz

    // https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9+9.1/OpenJDK17U-jre_x64_windows_hotspot_17.0.9_9.msi
    // https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9+9.1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip

    // https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8+9/OpenJDK21U-jdk_x64_mac_hotspot_21.0.8_9.tar.gz
    // https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8+9/OpenJDK21U-jdk_aarch64_windows_hotspot_21.0.8_9.msi

    public static HashMap<JavaVersion, Pair<String, String>> temurinMinorPatch = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, new Pair<>("u462-b08", "u462b08"),
            JavaVersion.JAVA_11, new Pair<>(".0.28+6", ".0.28_6"),
            JavaVersion.JAVA_17, new Pair<>(".0.9+9.1", ".0.9_9"),
            JavaVersion.JAVA_21, new Pair<>(".0.8+9", ".0.8_9")
    ));

    public static HashMap<JavaVersion, String> temurinTemplate = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, "",
            JavaVersion.JAVA_11, "",
            JavaVersion.JAVA_17, "",
            JavaVersion.JAVA_21, ""
    ));

    public static HashMap<JavaPlatform, String> temurinPlatformsTranslation = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, "linux",
                    JavaPlatform.WINDOWS, "windows",
                    JavaPlatform.MACOS, "mac",
                    JavaPlatform.AIX, "aix"
            )
    );

    public static JavaType[] temurinTypes = {
            JavaType.JDK, JavaType.JRE
    };

    public static JavaVersion[] temurinVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> temurinPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.RISCV64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.S390X, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.PPC64LE, JavaPackage.TAR_GZ)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.MSI),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.SPARC64, JavaPackage.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.PKG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.PKG)
                    ),
                    JavaPlatform.AIX, List.of(
                            new Pair<>(JavaArchitecture.PPC64, JavaPackage.TAR_GZ)
                    )
            )
    );

    // -----------------------------------------------------------------------------------------

    // https://cdn.azul.com/zulu/bin/zulu21.44.17-ca-jdk21.0.8-linux.x86_64.rpm
    // https://cdn.azul.com/zulu/bin/zulu17.60.17-ca-jdk17.0.16-win_x64.msi
    // https://cdn.azul.com/zulu/bin/zulu11.82.19-ca-jdk11.0.28-macosx_aarch64.dmg
    // https://cdn.azul.com/zulu/bin/zulu8.88.0.19-ca-jdk8.0.462-linux_x64.zip

    public static HashMap<JavaVersion, Pair<String, String>> zuluMinorPatch = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, new Pair<>(".88.0.19", ".0.462"),
            JavaVersion.JAVA_11, new Pair<>(".82.19", ".0.28"),
            JavaVersion.JAVA_17, new Pair<>(".60.17", ".0.16"),
            JavaVersion.JAVA_21, new Pair<>(".44.17", ".0.8")
    ));

    public static JavaType[] zuluTypes = {
            JavaType.JDK
    };

    public static JavaVersion[] zuluVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> zuluPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.DEB),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.RPM),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.RPM),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.DEB)
                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.MSI),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.ZIP)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaPackage.DMG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.ZIP),
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.DMG)
                    )
            )
    );

    public static List<URL> generateZuluDownloadUrls() {
        List<URL> urls = new ArrayList<>();
        // https://cdn.azul.com/zulu/bin/zulu8.88.0.19-ca-jdk8.0.462-linux_x64.zip
        String baseUrl = "https://cdn.azul.com/zulu/bin/zulu%s%s-ca-%s%s%s-%s_%s.%s";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.CORRETTO)) {
            for (JavaVersion javaVersion : zuluVersions) {
                for (JavaType javaType : zuluTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> entry : zuluPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaPackage> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaPackage javaPackage = pair.getSecond();

                            if(javaVersion == JavaVersion.JAVA_8 || javaVersion == JavaVersion.JAVA_11) {
                                if(javaPlatform == JavaPlatform.WINDOWS && javaArchitecture == JavaArchitecture.AARCH64) {
                                    continue; // No AARCH64 support for Windows in Zulu 8 and 11
                                }
                            }

                            String version = javaVersion.getVersion();
                            String type = javaType.getType();
                            String platform = javaPlatform.getOs();
                            String architecture = javaArchitecture.getArch();
                            String packageType = javaPackage.getPackageType();

                            if(javaPackage == JavaPackage.DEB && javaArchitecture == JavaArchitecture.AARCH64) {
                                architecture = "arm64"; // For Zulu, AARCH64 is translated to arm64 for DEB packages
                            } else if (javaPackage == JavaPackage.DEB && javaArchitecture == JavaArchitecture.X64) {
                                architecture = "amd64"; // For Zulu, X64 is translated to amd64 for DEB packages
                            } else if (javaPackage == JavaPackage.RPM && javaArchitecture == JavaArchitecture.X64) {
                                architecture = "x86_64"; // For Zulu, X64 is translated to x86_64 for RPM packages
                            }

                            if (javaPlatform == JavaPlatform.WINDOWS) {
                                platform = "win"; // For Zulu, Windows platform is prefixed with win
                            } else if(javaPlatform == JavaPlatform.MACOS) {
                                platform = "macosx"; // For Zulu, macOS platform is prefixed with macosx
                            }

                            String url = String.format(
                                    baseUrl,
                                    version,
                                    zuluMinorPatch.get(javaVersion).getFirst(),
                                    type,
                                    version,
                                    zuluMinorPatch.get(javaVersion).getSecond(),
                                    platform, //zuluPlatformsTranslation.get(javaPlatform),
                                    architecture,
                                    packageType
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
        List<List<URL>> allUrls = new ArrayList<>();
        allUrls.add(generateOracleDownloadUrls());
        allUrls.add(generateCorrettoDownloadUrls());
        allUrls.add(generateZuluDownloadUrls());

        for (URL url : allUrls.get(1)) {
            try {
                long size = DownloadHelper.queryUrlFileDownloadSizeWithRedirect(url);
                System.out.println(size + ": " + url);
            } catch (RuntimeException e) {
                System.out.println(e);
            }
        }
    }
}