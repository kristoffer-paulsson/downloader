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
import java.util.Arrays;
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

    public Path getDownloadDir() {
        return getDownloadDir("package-cache");
    }

    public void setArchitectures(List<DebianArchitecture> architectures) {
        setMulti(EnvironmentKey.ARCH.getKey(), architectures, DebianArchitecture::getArch);
    }

    public List<DebianArchitecture> getArchitectures() {
        return getMulti(EnvironmentKey.ARCH.getKey(), DebianArchitecture::fromString);
    }

    public void setDistribution(List<DebianDistribution> architectures) {
        setMulti(EnvironmentKey.DISTRO.getKey(), architectures, DebianDistribution::getDist);
    }

    public List<DebianDistribution> getDistribution() {
        return getMulti(EnvironmentKey.DISTRO.getKey(), DebianDistribution::fromString);
    }

    public String hashOfConfiguration() {
        List<String> config = new ArrayList<>();

        streamSort(getArchitectures(), config);
        streamSort(getDistribution(), config);

        return Sha256Helper.computeHash(String.join(",", config).toLowerCase()).substring(56).toUpperCase();
    }
}
