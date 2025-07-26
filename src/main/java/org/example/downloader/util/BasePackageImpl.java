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
public class BasePackageImpl implements BasePackage{
    private final String filename;
    private final String size;
    private final String sha256digest;

    public BasePackageImpl(
            String filename, String size, String sha256digest
    ) {
        this.filename = filename;
        this.size = size;
        this.sha256digest = sha256digest;
    }

    public String getFilename() {
        return filename;
    }

    public String getSize() {
        return size;
    }

    public String getSha256Digest() {
        return sha256digest;
    }
}
