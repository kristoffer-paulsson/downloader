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
package org.example.downloader.util;

/**
 * Represents a base package with essential metadata.
 * This class is used to encapsulate the common attributes of a package.
 */
public interface BasePackage {

    public String getFilename();

    public String getSize();

    public String getSha256Digest();

    public String uniqueKey();

    public default long getByteSize() {
        return Long.parseLong(getSize());
    }
}
