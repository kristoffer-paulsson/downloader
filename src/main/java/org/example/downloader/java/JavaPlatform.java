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
    SOLARIS("solaris");

    private final String os;

    JavaPlatform(String os) {
        this.os = os;
    }

    public String getOs() {
        return os;
    }
}
