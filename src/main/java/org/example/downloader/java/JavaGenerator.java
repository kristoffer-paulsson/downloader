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

import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.JsonParser;
import org.example.downloader.util.Pair;

import java.io.*;
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

    public static JavaImage[] oracleTypes = {
            JavaImage.JDK
    };

    public static JavaVersion[] oracleVersions = {
            JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> oraclePlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.DEB),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.RPM),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.RPM)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.EXE),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.DMG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.DMG)
                    )
            )
    );

    public static List<Pair<URL, URL>> generateOracleDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        String baseUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s";
        String baseDigestUrl = "https://download.oracle.com/java/%s/latest/%s-%s_%s-%s_bin.%s.sha256";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.ORACLE)) {
            for (JavaVersion javaVersion : oracleVersions) {
                for (JavaImage javaType : oracleTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> entry : oraclePlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaInstaller> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaInstaller javaPackage = pair.getSecond();

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

    public static String oracleSha256Parser(String digest) {
        String sha256 = digest.trim();
        BlockChainHelper.isValid32CharHex(sha256);
        return sha256.trim();
    }

    public static JavaImage[] correttoTypes = {
            JavaImage.JDK
    };

    public static JavaVersion[] correttoVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> correttoPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.DEB),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.RPM),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.DEB),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.RPM)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.PKG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.PKG)
                    )
            )
    );

    public static List<Pair<URL, URL>> generateCorrettoDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        String baseUrl = "https://corretto.aws/downlaods/latest/amazon-corretto-%s-%s-%s-%s.%s";
        String baseDigestUrl = "https://corretto.aws/downlaods/latest_sha256/amazon-corretto-%s-%s-%s-%s.%s";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.CORRETTO)) {
            for (JavaVersion javaVersion : correttoVersions) {
                for (JavaImage javaType : correttoTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> entry : correttoPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaInstaller> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaInstaller javaPackage = pair.getSecond();

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

    public static String correttoSha256Parser(String digest) {
        String sha256 = digest.trim();
        BlockChainHelper.isValid32CharHex(sha256);
        return sha256.trim();
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

    public static JavaImage[] temurinTypes = {
            JavaImage.JDK, JavaImage.JRE
    };

    public static JavaVersion[] temurinVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> temurinPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.RISCV64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.S390X, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.PPC64LE, JavaInstaller.TAR_GZ)

                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.MSI),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.MSI)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.PKG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.PKG)
                    ),
                    JavaPlatform.AIX, List.of(
                            new Pair<>(JavaArchitecture.PPC64, JavaInstaller.TAR_GZ)
                    )
            )
    );

    public static List<Pair<URL, URL>> generateTemurinDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        for (JavaFlavor javaEdition : List.of(JavaFlavor.TEMURIN)) {
            for (JavaVersion javaVersion : temurinVersions) {
                for (JavaImage javaType : temurinTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> entry : temurinPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaInstaller> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaInstaller javaPackage = pair.getSecond();

                            String baseUrl = "";
                            if (javaType == JavaImage.JDK) {
                                baseUrl = temurinJDKTemplate.get(javaVersion);
                            } else if (javaType == JavaImage.JRE) {
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

    public static String temurinSha256Parser(String digest) {
        String sha256 = digest.split(" ")[0].trim();
        BlockChainHelper.isValid32CharHex(sha256);
        return sha256.trim();
    }

    // -----------------------------------------------------------------------------------------

    public static HashMap<JavaVersion, Pair<String, String>> zuluMinorPatch = new HashMap<>(Map.of(
            JavaVersion.JAVA_8, new Pair<>(".88.0.19", ".0.462"),
            JavaVersion.JAVA_11, new Pair<>(".82.19", ".0.28"),
            JavaVersion.JAVA_17, new Pair<>(".60.17", ".0.16"),
            JavaVersion.JAVA_21, new Pair<>(".44.17", ".0.8")
    ));

    public static JavaImage[] zuluTypes = {
            JavaImage.JDK
    };

    public static JavaVersion[] zuluVersions = {
            JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17, JavaVersion.JAVA_21
    };

    public static HashMap<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> zuluPlatforms = new HashMap<>(
            Map.of(
                    JavaPlatform.LINUX, List.of(
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.DEB),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.RPM),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.RPM),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.DEB)
                    ),
                    JavaPlatform.WINDOWS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.MSI),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.ZIP)
                    ),
                    JavaPlatform.MACOS, List.of(
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.X64, JavaInstaller.DMG),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.TAR_GZ),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.ZIP),
                            new Pair<>(JavaArchitecture.AARCH64, JavaInstaller.DMG)
                    )
            )
    );

    public static List<Pair<URL, URL>> generateZuluDownloadUrls() {
        List<Pair<URL, URL>> urls = new ArrayList<>();
        String baseUrl = "https://cdn.azul.com/zulu/bin/zulu%s%s-ca-%s%s%s-%s_%s.%s";
        String baseDigestUrl = "https://github.com/joschi/zulu-metadata/raw/master/checksums/zulu%s%s-ca-%s%s%s-%s_%s.%s.sha256";
        for (JavaFlavor javaEdition : List.of(JavaFlavor.CORRETTO)) {
            for (JavaVersion javaVersion : zuluVersions) {
                for (JavaImage javaType : zuluTypes) {
                    for (Map.Entry<JavaPlatform, List<Pair<JavaArchitecture, JavaInstaller>>> entry : zuluPlatforms.entrySet()) {
                        JavaPlatform javaPlatform = entry.getKey();
                        for (Pair<JavaArchitecture, JavaInstaller> pair : entry.getValue()) {
                            JavaArchitecture javaArchitecture = pair.getFirst();
                            JavaInstaller javaPackage = pair.getSecond();

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

                            if(javaPackage == JavaInstaller.DEB && javaArchitecture == JavaArchitecture.AARCH64) {
                                architecture = "arm64"; // For Zulu, AARCH64 is translated to arm64 for DEB packages
                            } else if (javaPackage == JavaInstaller.DEB && javaArchitecture == JavaArchitecture.X64) {
                                architecture = "amd64"; // For Zulu, X64 is translated to amd64 for DEB packages
                            } else if (javaPackage == JavaInstaller.RPM && javaArchitecture == JavaArchitecture.X64) {
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
                            String sha256Url = String.format(
                                    baseDigestUrl,
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

    public static String zuluSha256Parser(String digest) {
        String sha256 = digest.trim();
        BlockChainHelper.isValid32CharHex(sha256);
        return sha256.trim();
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
        //List<Pair<URL, URL>> urls = generateTemurinDownloadUrls();
        /*List<Pair<URL, URL>> urls = generateZuluDownloadUrls();

        for (Pair<URL, URL> url : urls) {
            try {
                long size = DownloadHelper.queryUrlFileDownloadSize(url.getFirst());
                String sha256 = DownloadHelper.downloadSmallData(url.getSecond());
                //String sha256 = "N/A";
                System.out.println(size + ": " + url.getFirst() + " | SHA256: " + sha256.trim());
            } catch (RuntimeException e) {
                System.out.println(e);
            }
        }*/
        try {
            URL packetData = new URI("https://joschi.github.io/java-metadata/metadata/all.json").toURL();
            Path cachePath = Path.of("./all.json");
            long allDownloadedSize = DownloadHelper.continueDownload(new DownloadHelper.Download(packetData, cachePath));

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cachePath.toFile())));
            JsonParser jsonParser = new JsonParser();
            Object jsonData = jsonParser.parse(reader);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}