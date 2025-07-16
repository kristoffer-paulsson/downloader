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

import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianComponent;
import org.example.downloader.deb.DebianDistribution;

public class DebianPackage {
    public final String packageName;
    public final String version;
    public final String description;

    public DebianDistribution distribution;
    public DebianComponent component;
    public DebianArchitecture architecture;

    public DebianPackage(String packageName, String version, String description) {
        this.packageName = packageName;
        this.version = version;
        this.description = description;
    }

    public void complement(DebianDistribution distribution, DebianComponent component, DebianArchitecture architecture) {
        this.distribution = distribution;
        this.component = component;
        this.architecture = architecture;
    }
    @Override
    public String toString() {
        return "DebianPackage{name='" + packageName + "', version='" + version + "', description='" + description + "'}";
    }

    public DebianDownloadUrl buildDownloadUrl(String baseUrl) {
        return new DebianDownloadUrl(this, baseUrl);
    }
}
