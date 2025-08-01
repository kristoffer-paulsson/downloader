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
import java.util.Arrays;
import java.util.List;

public enum JavaArchitecture {
    AARCH64("aarch64"),
    S390X("s390x"),
    SPARC64("sparcv9"),
    X64("x86_64"),
    RISCV64("riscv64"),
    PPC64("ppc64"),
    PPC64LE("ppc64le"),
    UNKNOWN("unknown");

    private final String arch;

    JavaArchitecture(String arch) {
        this.arch = arch;
    }

    public String getArch() {
        return arch;
    }

    public static JavaArchitecture fromString(String arch) {
        for (JavaArchitecture architecture : JavaArchitecture.values()) {
            if (architecture.arch.equalsIgnoreCase(arch)) {
                return architecture;
            }
        }
        return UNKNOWN;
    }

    public static List<String> toStringList() {
        List<String> archList = new ArrayList<>();
        Arrays.stream(values()).iterator().forEachRemaining(genericArch -> {
            archList.add(genericArch.getArch());
        });
        return archList;
    }
}
