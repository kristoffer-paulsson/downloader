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
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;

public class DebianPackagesListCache {

    public static void downloadAndCachePackagesList(ConfigManager configManager) throws Exception {
        String url = configManager.get("url");
        String outputPath = configManager.get("output");
        String cacheDir = configManager.get("cache");

        if (url == null || outputPath == null || cacheDir == null) {
            System.err.println("Config file must contain 'url', 'output', and 'cache' properties.");
            return;
        }

        Path cachePath = Path.of(cacheDir);
        Files.createDirectories(cachePath);

        Path outputFile = cachePath.resolve(outputPath);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream in = response.body();
             FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            in.transferTo(out);
        }

        System.out.println("Downloaded to " + outputFile);
    }

    public static List<DebianPackage> parseCachedPackagesList(ConfigManager configManager, int startLine) {
        String output = configManager.get("output", "allpackages.txt");
        String cache = configManager.get("cache", "runtime-cache");
        File file = new File(cache, output);

        List<DebianPackage> packages = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(\\S+)\\s+\\(([^)]+)\\)\\s+(.+)$");

        try (
            InputStream fileStream = new FileInputStream(file);
            InputStream gzipStream = file.getName().endsWith(".gz") ?
                new GZIPInputStream(fileStream) : fileStream;
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, java.nio.charset.StandardCharsets.UTF_8))
        ) {
            String line;
            int currentLine = 0;

            while (currentLine < startLine - 1 && (line = reader.readLine()) != null) {
                currentLine++;
            }

            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (line.trim().isEmpty()) continue;

                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String name = matcher.group(1);
                    String version = matcher.group(2);
                    String description = matcher.group(3);
                    packages.add(new DebianPackage(name, version, description));
                } else {
                    System.err.println("Line " + currentLine + " does not match expected format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return packages;
    }

    public static Iterator<DebianPackage> parseCachedPackagesIterator(ConfigManager configManager, int startLine) {
        return parseCachedPackagesList(configManager,startLine).iterator();
    }

    public static void main(String[] args) throws Exception {
        ConfigManager configManager = new ConfigManager("config.properties");
        downloadAndCachePackagesList(configManager);
        List<String> mirrors = DebianMirrorCache.loadCachedMirrors();
        Iterator<DebianPackage> result = parseCachedPackagesIterator(configManager, 7);

        while(result.hasNext()) {
            DebianPackage pkg = result.next();
            pkg.complement(DebianDistribution.BOOKWORM, DebianComponent.MAIN, DebianArchitecture.AMD_64);
            System.out.println(pkg.buildDownloadUrl(mirrors.get(1)).buildDownloadUrl());
            System.out.println(pkg.buildDownloadUrl(mirrors.get(1)).buildDownloadUrlAsAll());
        }
    }
}
