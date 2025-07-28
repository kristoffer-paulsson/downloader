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

public enum JavaPlatform {
    LINUX("linux"),
    WINDOWS("windows"),
    MACOS("macosx"),
    AIX("aix"),
    SOLARIS("solaris"),
    UNKNOWN("unknown");

    private final String os;

    JavaPlatform(String os) {
        this.os = os;
    }

    public String getPlatform() {
        return os;
    }

    public static JavaPlatform fromString(String os) {
        for (JavaPlatform platform : JavaPlatform.values()) {
            if (platform.os.equalsIgnoreCase(os)) {
                return platform;
            }
        }
        return UNKNOWN;
    }
}
