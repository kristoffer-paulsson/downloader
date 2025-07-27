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

public enum JavaImplementation {
    HOTSPOT("hotspot"),
    OPENJ9("openj9"),
    UNKNOWN("unknown");

    private final String implementation;

    JavaImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getImplementation() {
        return implementation;
    }

    public static JavaImplementation fromString(String implementation) {
        for (JavaImplementation impl : JavaImplementation.values()) {
            if (impl.implementation.equalsIgnoreCase(implementation)) {
                return impl;
            }
        }
        return UNKNOWN;
    }
}
