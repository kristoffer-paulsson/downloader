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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public enum DebianComponent {
    MAIN("main"),
    CONTRIB("contrib"),
    NON_FREE("non-free"),
    NON_FREE_FIRMWARE("non-free-firmware");

    final String comp;

    DebianComponent(String component) {
        this.comp = component;
    }

    public String getComp() {
        return this.comp;
    }

    public static List<String> toStringList() {
        List<String> compList = new ArrayList<>();
        Arrays.stream(values()).iterator().forEachRemaining(genericArch -> {
            compList.add(genericArch.getComp());
        });
        return compList;
    }
}
