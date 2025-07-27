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

public enum JavaVersion {
    JAVA_8("8"), // Java 8 is the last version with long-term support (LTS)
    JAVA_9("9"),
    JAVA_10("10"),
    JAVA_11("11"), // Java 11 is the next LTS version after Java 8
    JAVA_12("12"),
    JAVA_13("13"),
    JAVA_14("14"),
    JAVA_15("15"),
    JAVA_16("16"),
    JAVA_17("17"), // Java 17 is the next LTS version after Java 11
    JAVA_18("18"),
    JAVA_19("19"),
    JAVA_20("20"),
    JAVA_21("21"), // Java 21 is the next LTS version after Java 17
    JAVA_22("22"),
    JAVA_23("23"),
    JAVA_24("24"),
    JAVA_25("25");

    private final String version;

    JavaVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static void main(String[] args) {
        int count = 0;
        // Example usage
        for (JavaFlavor javaEdition : JavaFlavor.values()) {
            for (JavaVersion javaVersion : JavaVersion.values()) {
                for (JavaImage javaType : JavaImage.values()) {
                    for (JavaPlatform javaEnvironment : JavaPlatform.values()) {
                        for (JavaArchitecture javaArchitecture : JavaArchitecture.values()) {
                            for (JavaInstaller javaPackage : JavaInstaller.values()) {
                                String config = String.join("-", javaEdition.getEdition(), javaVersion.getVersion(), javaType.getType(), javaEnvironment.getOs(), javaArchitecture.getArch(), javaPackage.getPackageType());
                                System.out.println(++count + ": " + config);
                            }
                        }
                    }
                }
            }
        }
    }
}
