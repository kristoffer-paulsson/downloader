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
    JAVA_8("8"),
    JAVA_11("11"),
    JAVA_17("17"),
    JAVA_21("21");


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
