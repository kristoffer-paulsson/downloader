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
package org.example.downloader.deb;

import java.util.List;
import java.util.stream.Stream;

public enum DebianDistribution {
    JESSIE("jessie"),
    STRETCH("stretch"),
    BUSTER("buster"),
    BULLSEYE("bullseye"),
    BOOKWORM("bookworm");

    final String dist;

    DebianDistribution(String distribution) {
        this.dist = distribution;
    }

    public String getDist() {
        return this.dist;
    }

    public static List<String> toStringList() {
        return Stream.of(values()).map(DebianDistribution::getDist).toList();
    }
}
