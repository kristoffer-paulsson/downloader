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
package org.example.downloader;

import java.util.List;

public class ChunkSplit {
    public final long startIndex;
    public final long endIndex;
    public final int chunkNum;
    public final List<DebianPackage> packages;

    ChunkSplit(List<DebianPackage> packages, int chunkNum, long startIndex, long endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.chunkNum = chunkNum;
        this.packages = packages;
    }
}
