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

public enum JavaInstaller {
    DEB("deb"),
    RPM("rpm"),
    ZIP("zip"),
    MSI("msi"),
    EXE("exe"),
    TAR_GZ("tar.gz"),
    TAR_XZ("tar.xz"),
    TGZ("tgz"),
    APK("apk"),
    DMG("dmg"),
    PKG("pkg"),
    UNKNOWN("unknown");

    private final String packageType;

    JavaInstaller(String packageType) {
        this.packageType = packageType;
    }

    public String getInstaller() {
        return packageType;
    }

    public static JavaInstaller fromString(String packageType) {
        for (JavaInstaller installer : JavaInstaller.values()) {
            if (installer.packageType.equalsIgnoreCase(packageType)) {
                return installer;
            }
        }
        return UNKNOWN;
    }
}
