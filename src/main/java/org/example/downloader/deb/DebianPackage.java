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

import org.example.downloader.util.BasePackage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DebianPackage implements BasePackage {

    public final String packageName;
    public final String version;
    public final String architecture;
    public final String filename;
    public final long size;
    public final String sha256digest;
    public final String distribution;

    DebianPackage(
            String packageName, String version, String architecture,
            String filename, long size, String sha256digest,
            String distribution
    ) {
        this.packageName = packageName;
        this.version = version;
        this.architecture = architecture;
        this.filename = filename;
        this.size = size;
        this.sha256digest = sha256digest;
        this.distribution = distribution;
    }

    public String buildDownloadUrl(String baseUrl) {
        return String.format("%s/%s", baseUrl, filename);
    }

    public Path buildSavePath(DebianDownloadEnvironment dde) {
        return Paths.get(String.format("%s/%s", dde.getDownloadDir(), filename));
    }

    @Override
    public String getFilename() {
        return filename;
    }

    public String getSize() {
        return Long.toString(size);
    }

    @Override
    public String getSha256Digest() {
        return sha256digest;
    }

    @Override
    public String uniqueKey() {
        return String.format("%s_%s_%s_%s", packageName, version, distribution, architecture);
    }
}
