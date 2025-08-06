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

import org.example.downloader.java.*;
import org.example.downloader.util.EnvironmentManager;
import org.example.downloader.util.Sha256Helper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DebianDownloadEnvironment extends EnvironmentManager {

    public static final String ENVIRONMENT_FILE = "debian-download.properties";

    public enum EnvironmentKey {
        ARCH("debian_arch"),
        DISTRO("debian_distro"),
        CHUNKS("debian_chunks"),
        PIECE("debian_piece");

        private final String key;

        EnvironmentKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public DebianDownloadEnvironment(String environmentDirPath){
        super(Paths.get(environmentDirPath, ENVIRONMENT_FILE));
    }

    @Override
    public Path getDownloadDir() {
        return getDownloadDir("package-cache");
    }

    public void setArchitecture(DebianArchitecture architecture) {
        set(EnvironmentKey.ARCH.getKey(), architecture.getArch());
    }

    public DebianArchitecture getArchitecture() {
        return DebianArchitecture.fromString(get(EnvironmentKey.ARCH.getKey()));
    }

    public void setDistribution(DebianDistribution distribution) {
        set(EnvironmentKey.DISTRO.getKey(), distribution.getDist());
    }

    public DebianDistribution getDistribution() {
        return DebianDistribution.fromString(get(EnvironmentKey.DISTRO.getKey()));
    }

    public int getChunks() {
        return Integer.parseInt(get(EnvironmentKey.CHUNKS.getKey()));
    }

    public void setChunks(int piece) {
        set(EnvironmentKey.CHUNKS.getKey(), Integer.toString(piece));
    }

    public int getPiece() {
        return Integer.parseInt(get(EnvironmentKey.PIECE.getKey()));
    }

    public void setPiece(int piece) {
        set(EnvironmentKey.PIECE.getKey(), Integer.toString(piece));
    }

    public String hashOfConfiguration() {
        List<String> config = new ArrayList<>();

        config.add(getArchitecture().getArch());
        config.add(getDistribution().getDist());

        return Sha256Helper.computeHash(String.join(",", config).toLowerCase()).substring(56).toUpperCase();
    }
}
