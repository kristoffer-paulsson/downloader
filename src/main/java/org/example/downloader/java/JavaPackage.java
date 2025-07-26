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

public enum JavaPackage {
    DEB("deb"),
    RPM("rpm"),
    ZIP("zip"),
    MSI("msi"),
    EXE("exe"),
    TAR_GZ("tar.gz"),
    DMG("dmg"),
    PKG("pkg");

    private final String packageType;

    JavaPackage(String packageType) {
        this.packageType = packageType;
    }

    public String getPackageType() {
        return packageType;
    }
}
