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
package org.example.downloader.exp2;

import java.util.List;
import java.util.Objects;

public final class DebianPackage {
    private final String packageName;
    private final String source;
    private final String version;
    private final int installedSize;
    private final String maintainer;
    private final String architecture;
    private final List<String> depends;
    private final List<String> preDepends;
    private final List<String> suggests;
    private final List<String> recommends;
    private final List<String> replaces;
    private final List<String> breaks;
    private final String description;
    private final String homepage;
    private final String descriptionMd5;
    private final List<String> tag;
    private final String section;
    private final String priority;
    private final String filename;
    private final long size;
    private final String md5sum;
    private final String sha256;

    public DebianPackage(
            String packageName,        // Package
            String source,            // Source (optional)
            String version,           // Version
            int installedSize,        // Installed-Size
            String maintainer,        // Maintainer
            String architecture,      // Architecture
            List<String> depends,     // Depends (optional, multi-value)
            List<String> preDepends,  // Pre-Depends (optional, multi-value)
            List<String> suggests,    // Suggests (optional, multi-value)
            List<String> recommends,  // Recommends (optional, multi-value)
            List<String> replaces,    // Replaces (optional, multi-value)
            List<String> breaks,      // Breaks (optional, multi-value)
            String description,       // Description (multi-row)
            String homepage,          // Homepage (optional)
            String descriptionMd5,    // Description-md5
            List<String> tag,         // Tag (optional, multi-value)
            String section,           // Section
            String priority,          // Priority
            String filename,          // Filename
            long size,                // Size
            String md5sum,            // MD5sum
            String sha256             // SHA256
    ) {
        this.packageName = packageName;
        this.source = source;
        this.version = version;
        this.installedSize = installedSize;
        this.maintainer = maintainer;
        this.architecture = architecture;
        this.depends = depends;
        this.preDepends = preDepends;
        this.suggests = suggests;
        this.recommends = recommends;
        this.replaces = replaces;
        this.breaks = breaks;
        this.description = description;
        this.homepage = homepage;
        this.descriptionMd5 = descriptionMd5;
        this.tag = tag;
        this.section = section;
        this.priority = priority;
        this.filename = filename;
        this.size = size;
        this.md5sum = md5sum;
        this.sha256 = sha256;
    }

    public String packageName() {
        return packageName;
    }

    public String source() {
        return source;
    }

    public String version() {
        return version;
    }

    public int installedSize() {
        return installedSize;
    }

    public String maintainer() {
        return maintainer;
    }

    public String architecture() {
        return architecture;
    }

    public List<String> depends() {
        return depends;
    }

    public List<String> preDepends() {
        return preDepends;
    }

    public List<String> suggests() {
        return suggests;
    }

    public List<String> recommends() {
        return recommends;
    }

    public List<String> replaces() {
        return replaces;
    }

    public List<String> breaks() {
        return breaks;
    }

    public String description() {
        return description;
    }

    public String homepage() {
        return homepage;
    }

    public String descriptionMd5() {
        return descriptionMd5;
    }

    public List<String> tag() {
        return tag;
    }

    public String section() {
        return section;
    }

    public String priority() {
        return priority;
    }

    public String filename() {
        return filename;
    }

    public long size() {
        return size;
    }

    public String md5sum() {
        return md5sum;
    }

    public String sha256() {
        return sha256;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DebianPackage) obj;
        return Objects.equals(this.packageName, that.packageName) &&
                Objects.equals(this.source, that.source) &&
                Objects.equals(this.version, that.version) &&
                this.installedSize == that.installedSize &&
                Objects.equals(this.maintainer, that.maintainer) &&
                Objects.equals(this.architecture, that.architecture) &&
                Objects.equals(this.depends, that.depends) &&
                Objects.equals(this.preDepends, that.preDepends) &&
                Objects.equals(this.suggests, that.suggests) &&
                Objects.equals(this.recommends, that.recommends) &&
                Objects.equals(this.replaces, that.replaces) &&
                Objects.equals(this.breaks, that.breaks) &&
                Objects.equals(this.description, that.description) &&
                Objects.equals(this.homepage, that.homepage) &&
                Objects.equals(this.descriptionMd5, that.descriptionMd5) &&
                Objects.equals(this.tag, that.tag) &&
                Objects.equals(this.section, that.section) &&
                Objects.equals(this.priority, that.priority) &&
                Objects.equals(this.filename, that.filename) &&
                this.size == that.size &&
                Objects.equals(this.md5sum, that.md5sum) &&
                Objects.equals(this.sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, source, version, installedSize, maintainer, architecture, depends, preDepends, suggests, recommends, replaces, breaks, description, homepage, descriptionMd5, tag, section, priority, filename, size, md5sum, sha256);
    }

    @Override
    public String toString() {
        return "DebianPackage[" +
                "packageName=" + packageName + ", " +
                "source=" + source + ", " +
                "version=" + version + ", " +
                "installedSize=" + installedSize + ", " +
                "maintainer=" + maintainer + ", " +
                "architecture=" + architecture + ", " +
                "depends=" + depends + ", " +
                "preDepends=" + preDepends + ", " +
                "suggests=" + suggests + ", " +
                "recommends=" + recommends + ", " +
                "replaces=" + replaces + ", " +
                "breaks=" + breaks + ", " +
                "description=" + description + ", " +
                "homepage=" + homepage + ", " +
                "descriptionMd5=" + descriptionMd5 + ", " +
                "tag=" + tag + ", " +
                "section=" + section + ", " +
                "priority=" + priority + ", " +
                "filename=" + filename + ", " +
                "size=" + size + ", " +
                "md5sum=" + md5sum + ", " +
                "sha256=" + sha256 + ']';
    }

}
