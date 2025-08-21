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
package org.example.downloader.wtx;

import org.example.downloader.java.*;
import org.example.downloader.util.EnvironmentManager;
import org.example.downloader.util.Sha256Helper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WinetricksDownloadEnvironment extends EnvironmentManager {

    public static final String ENVIRONMENT_FILE = "winetricks-download.properties";

    public enum EnvironmentKey {
        VERB("wtx_verb"),
        CATEGORY("wtx_category");

        private final String key;

        EnvironmentKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public WinetricksDownloadEnvironment(Path environmentDirPath){
        super(environmentDirPath.resolve(ENVIRONMENT_FILE));
    }

    @Override
    public Path getDownloadDir() {
        return getDownloadDir("winetricks-cache");
    }

    public String hashOfConfiguration() {
        List<String> config = new ArrayList<>();

        return Sha256Helper.computeHash(String.join(",", config).toLowerCase()).substring(56).toUpperCase();
    }
}
