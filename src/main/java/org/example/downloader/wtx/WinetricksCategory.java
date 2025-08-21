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
package org.example.downloader.wtx;

import java.util.ArrayList;
import java.util.List;

public enum WinetricksCategory {
    DLLS("dlls"),
    FONTS("fonts"),
    BENCHMARKS("benchmarks"),
    MISC("misc"),
    APPS("apps"),
    UNKNOWN("unknown");

    private final String category;

    WinetricksCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public static WinetricksCategory fromString(String category) {
        for (WinetricksCategory category1 : WinetricksCategory.values()) {
            if (category1.category.equalsIgnoreCase(category)) {
                return category1;
            }
        }
        return UNKNOWN;
    }

    public static List<String> toStringList() {
        List<String> categoryList = new ArrayList<>();
        for (WinetricksCategory category : WinetricksCategory.values()) {
            categoryList.add(category.getCategory());
        }
        return categoryList;
    }
}
