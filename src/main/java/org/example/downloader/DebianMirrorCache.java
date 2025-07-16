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

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

public class DebianMirrorCache {

    private static final String MIRROR_LIST_URL = "https://www.debian.org/mirror/list-full";
    private static final String CACHE_DIR = "runtime-cache";
    private static final String CACHE_FILE = "mirror.txt";

    public static void downloadAndCacheMirrors() {
        Set<String> mirrors = new HashSet<>();
        Pattern urlPattern = compile("(https?://[^\\s\"'>]+/debian/)");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new URL(MIRROR_LIST_URL).openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = urlPattern.matcher(line);
                while (matcher.find()) {
                    String url = matcher.group(1);
                    if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                    mirrors.add(url);
                }
            }
        } catch (IOException e) {
            System.err.println("Error downloading mirror list: " + e.getMessage());
            return;
        }

        Path cacheDir = Paths.get(CACHE_DIR);
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            System.err.println("Error creating cache directory: " + e.getMessage());
            return;
        }

        Path cacheFile = cacheDir.resolve(CACHE_FILE);
        try (BufferedWriter writer = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8)) {
            for (String mirror : mirrors) {
                writer.write(mirror);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing mirror cache: " + e.getMessage());
        }
    }

    public static List<String> loadCachedMirrors(Boolean downloadIfMissing) {
        Path cacheFile = Paths.get(CACHE_DIR, CACHE_FILE);
        List<String> mirrors = new ArrayList<>();
        if (!Files.exists(cacheFile)) {
            if(downloadIfMissing) {
                downloadAndCacheMirrors();
            } else {
                System.err.println("Mirror cache file does not exist: " + cacheFile);
                return mirrors;
            }
        }
        try (BufferedReader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    mirrors.add(line.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading mirror cache: " + e.getMessage());
        }
        return mirrors;
    }

    private static List<String> mirrors = loadCachedMirrors(false);
    private static long current;

    public static String getNextMirror() {
        current++;
        return mirrors.get((int) (current / mirrors.size()));
    }

    public static void main(String[] args) {
        mirrors = loadCachedMirrors(true);
        mirrors.forEach(System.out::println);
    }
}
