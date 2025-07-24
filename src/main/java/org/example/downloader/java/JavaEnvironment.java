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

public enum JavaEnvironment {
    LINUX("linux"),
    WINDOWS("windows"),
    MACOS("macos"),
    SOLARIS("solaris");

    private final String os;

    JavaEnvironment(String os) {
        this.os = os;
    }

    public String getOs() {
        return os;
    }
}
