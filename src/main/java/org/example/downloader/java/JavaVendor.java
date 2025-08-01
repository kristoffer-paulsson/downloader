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

public enum JavaVendor {
    ORACLE("oracle", "Oracle Corporation"),
    KONA("kona", "Kona JDK"),
    MICROSOFT("microsoft", "Microsoft Corporation"),
    MANDREL("mandrel", "Red Hat"),
    SAPMACHINE("sapmachine", "SAP SE"),
    DRAGONWELL("dragonwell", "Alibaba Group"),
    JAVA_SE_RI("java-se-ri", "Java SE Reference Implementation"),
    REDHAT("redhat", "Red Hat"),
    CORRETTO("corretto", "Amazon"),
    OPENJDK("openjdk", "OpenJDK"),
    SEMERU("semeru", "Eclipse Foundation"),
    IBM("ibm", "IBM Corporation"),
    BISHENG("bisheng", "Huawei"),
    JETBRAINS("jetbrains", "JetBrains s.r.o."),
    LIBERICA("liberica", "BellSoft"),
    TRAVA("trava", "Trava JDK"),
    ADOPTOPENJDK("adoptopenjdk", "AdoptOpenJDK"),
    TEMURIN("temurin", "Eclipse Foundation (Adoptium)"),
    ZULU("zulu", "Azul Systems"),
    UNKNOWN("unknown", "Unknown Vendor");

    private final String vendorId;
    private final String vendor;

    JavaVendor(String vendorId, String vendor) {
        this.vendorId = vendorId;
        this.vendor = vendor;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getVendor() {
        return vendor;
    }

    public static JavaVendor fromString(String vendorId) {
        for (JavaVendor vendor : JavaVendor.values()) {
            if (vendor.vendorId.equalsIgnoreCase(vendorId)) {
                return vendor;
            }
        }
        return UNKNOWN;
    }

    public static List<String> toStringList() {
        List<String> vendorList = new ArrayList<>();
        for (JavaVendor vendor : JavaVendor.values()) {
            vendorList.add(vendor.getVendorId());
        }
        return vendorList;
    }
}
