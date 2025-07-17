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

public record DebianPackage(
        String packageName, String version, String architecture,
        String filename, String sha256digest, String distribution
) {

    public String buildDownloadUrl(String baseUrl) {
        return String.format("%s/%s", baseUrl, filename);
    }

    public String buildSavePath(ConfigManager configManager) {
        return String.format("%s/%s", configManager.get("package_dir"), filename);
    }
}
