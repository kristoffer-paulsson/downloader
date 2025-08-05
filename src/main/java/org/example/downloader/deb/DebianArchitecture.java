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

import org.example.downloader.java.JavaArchitecture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public enum DebianArchitecture {
    AMD_64("amd64"),
    ARM_64("arm64"),
    ARMEL("armel"),
    ARMHF("armhf"),
    I_386("i386"),
    MIPS_64EL("mips64el"),
    MIPS_EL("mipsel"),
    PPC_64EL("ppc64el"),
    RISC_V64("riscv64"),
    S_390X("s390x"),
    ALL("all");

    final String arch;

    DebianArchitecture(String arch) {
        this.arch = arch;
    }

    public String getArch() {
        return this.arch;
    }

    public static List<String> toStringList() {
        List<String> archList = new ArrayList<>();
        Arrays.stream(values()).iterator().forEachRemaining(genericArch -> {
            archList.add(genericArch.getArch());
        });
        return archList;
    }

    public static DebianArchitecture fromString(String arch) {
        for (DebianArchitecture architecture : DebianArchitecture.values()) {
            if (architecture.arch.equalsIgnoreCase(arch)) {
                return architecture;
            }
        }
        return ALL;
    }
}
