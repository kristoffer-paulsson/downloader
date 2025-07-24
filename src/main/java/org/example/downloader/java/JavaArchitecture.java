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

public enum JavaArchitecture {
    PPC64("ppc64"),
    PPC64LE("ppc64le"),
    RISCV64("riscv64"),
    S390X("s390x"),
    SPARC64("sparc64"),
    X64("x64"),
    AARCH64("aarch64");

    private final String arch;

    JavaArchitecture(String arch) {
        this.arch = arch;
    }

    public String getArch() {
        return arch;
    }
}
