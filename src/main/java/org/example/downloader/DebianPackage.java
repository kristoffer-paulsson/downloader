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



public class DebianPackage {
    public final String packageName;
    public final String version;
    public final String architecture;
    public final String filename;
    public final String sha256digest;
    public final String distribution;

    public DebianPackage(
            String packageName,
            String version,
            String architecture,
            String filename,
            String sha256digest,
            String distribution
    ) {
        this.packageName = packageName;
        this.version = version;
        this.architecture = architecture;
        this.filename = filename;
        this.sha256digest = sha256digest;
        this.distribution = distribution;
    }

    public String buildDownloadUrl(String baseUrl) {
        return String.format("%s/%s", baseUrl, filename);
    }

    public String buildSavePath(ConfigManager configManager) {
        return String.format("%s/%s", configManager.get("package_dir"), filename);
    }
}
