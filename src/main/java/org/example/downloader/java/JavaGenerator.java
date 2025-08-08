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
package org.example.downloader.java;

import org.example.downloader.util.DownloadHelper;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class JavaGenerator {

    public static void main(String[] args) {

        try {
            URL packetData = new URI("https://joschi.github.io/java-metadata/metadata/all.json").toURL();
            Path cachePath = Path.of("./all.json");
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }
}