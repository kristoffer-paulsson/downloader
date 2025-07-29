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

import java.util.ArrayList;
import java.util.List;

public enum JavaImage {
    JDK("jdk"),
    JRE("jre"),
    UNKNOWN("unknown");

    private final String type;

    JavaImage(String type) {
        this.type = type;
    }

    public String getImage() {
        return type;
    }

    public static JavaImage fromString(String type) {
        for (JavaImage image : JavaImage.values()) {
            if (image.type.equalsIgnoreCase(type)) {
                return image;
            }
        }
        return UNKNOWN;
    }

    public static List<String> toStringList() {
        List<String> imageList = new ArrayList<>();
        for (JavaImage image : JavaImage.values()) {
            imageList.add(image.getImage());
        }
        return imageList;
    }
}
