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

    public static List<Pair<URL, URL>> generateOracleDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        String baseUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s";
        String baseDigestUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s.sha256";
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
                            String sha256Url = String.format(
                                    baseDigestUrl,
                                    version,
                                    type,
                                    version,
                                    platform,
                                    architecture,
                                    packageType
                            );
                            try {
                                urls.add(new Pair<>(
                                        new URI(url).toURL(),
                                        new URI(sha256Url).toURL()
                                ));
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

    public static List<Pair<URL, URL>> generateCorrettoDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        String baseUrl = "https://corretto.aws/downlaods/latest/amazon-corretto-%s-%s-%s-%s.%s";
        String baseDigestUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s.sha256";
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
                            String sha256Url = String.format(
                                    baseDigestUrl,
                                    version,
                                    architecture,
                                    platform,
                                    type,
                                    packageType
                            );
                            try {
                                urls.add(new Pair<>(
                                        new URI(url).toURL(),
                                        new URI(sha256Url).toURL()
                                ));
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

    public static HashMap<JavaVersion, Pair<String, String>> temurinMinorPatch = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, new Pair<>("u462-b08", "u462b08"),
            JavaVersion.JAVA_11, new Pair<>(".0.28+6", ".0.28_6"),
            JavaVersion.JAVA_17, new Pair<>(".0.16+8", ".0.16_8"),
            JavaVersion.JAVA_21, new Pair<>(".0.8+9", ".0.8_9")
    ));

    public static HashMap<JavaVersion, String> temurinJDKTemplate = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_11, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_17, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_21, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s"
    ));

    public static HashMap<JavaVersion, String> temurinJRETemplate = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_11, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_17, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s",
            JavaVersion.JAVA_21, "https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s%s/OpenJDK%sU-%s_%s_%s_hotspot_%s%s.%s"
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
                            new Pair<>(JavaArchitecture.AARCH64, JavaPackage.MSI)
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

    public static List<Pair<URL, URL>> generateTemurinDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        // https://corretto.aws/[latest/latest_checksum]/amazon-corretto-[corretto_version]-[cpu_arch]-[os]-[package_type].[file_extension]
        for (JavaFlavor javaEdition : List.of(JavaFlavor.TEMURIN)) {
            for (JavaVersion javaVersion : temurinVersions) {
                for (JavaType javaType : temurinTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaPackage>>> entry : temurinPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaPackage> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaPackage javaPackage = pair.getSecond();

                            String baseUrl = "";
                            if (javaType == JavaType.JDK) {
                                baseUrl = temurinJDKTemplate.get(javaVersion);
                            } else if (javaType == JavaType.JRE) {
                                baseUrl = temurinJRETemplate.get(javaVersion);
                            }

                            if(javaVersion != JavaVersion.JAVA_21 && javaPlatform == JavaPlatform.WINDOWS && javaArchitecture == JavaArchitecture.AARCH64) {
                                continue; // No AARCH64 support for Windows in Temurin 8, 11, and 17
                            } else if (javaPlatform == JavaPlatform.WINDOWS && javaArchitecture == JavaArchitecture.SPARC64) {
                                continue; // No SPARC64 support for Windows in Temurin
                            } else if((javaVersion == JavaVersion.JAVA_8 || javaVersion == JavaVersion.JAVA_11) && javaPlatform == JavaPlatform.LINUX && javaArchitecture == JavaArchitecture.RISCV64) {
                                continue; // No RISC-V support for Linux in Temurin 8 and 11
                            } else if(javaVersion == JavaVersion.JAVA_8 && javaPlatform == JavaPlatform.MACOS && javaArchitecture == JavaArchitecture.AARCH64) {
                                continue; // No AARCH64 support for macOS in Temurin 8
                            } else if (javaVersion == JavaVersion.JAVA_8 && javaPlatform == JavaPlatform.LINUX && (javaArchitecture == JavaArchitecture.S390X)) {
                                continue;
                            }

                            String version = javaVersion.getVersion();
                            String type = javaType.getType();
                            String platform = javaPlatform.getOs();
                            String architecture = javaArchitecture.getArch();
                            String packageType = javaPackage.getPackageType();

                            if(javaPlatform == JavaPlatform.MACOS) {
                                platform = "mac"; // For Temurin, macOS platform is prefixed with mac
                            }

                            String url = String.format(
                                    baseUrl,
                                    version,
                                    version,
                                    temurinMinorPatch.get(javaVersion).getFirst(),
                                    version,
                                    type,
                                    architecture,
                                    platform,
                                    version,
                                    temurinMinorPatch.get(javaVersion).getSecond(),
                                    packageType
                            );
                            try {
                                urls.add(new Pair<>(
                                        new URI(url).toURL(),
                                        new URI(url+".sha256.txt").toURL()
                                ));
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

    // -----------------------------------------------------------------------------------------

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
        /*List<List<URL>> allUrls = new ArrayList<>();
        allUrls.add(generateOracleDownloadUrls());

        for (URL url : allUrls.get(0)) {
            try {
                String size = DownloadHelper.downloadSmallData(url);
                System.out.println(size + ": " + url);
            } catch (RuntimeException e) {
                System.out.println(e);
            }
        }*/

        //List<Pair<URL, URL>> urls = generateOracleDownloadUrls();
        //List<Pair<URL, URL>> urls = generateCorrettoDownloadUrls();
        List<Pair<URL, URL>> urls = generateTemurinDownloadUrls();

        for (Pair<URL, URL> url : urls) {
            try {
                long size = DownloadHelper.queryUrlFileDownloadSize(url.getFirst());
                String sha256 = DownloadHelper.downloadSmallData(url.getSecond());
                System.out.println(size + ": " + url.getFirst() + " | SHA256: " + sha256);
            } catch (RuntimeException e) {
                System.out.println(e);
            }
        }
    }
}