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
import java.util.zip.GZIPInputStream;

public class DebianPackagesListCache {
    private static final String BASE_URL = "http://deb.debian.org/debian/dists/%s/main/binary-%s/Packages.gz";

    public static void downloadAndCachePackagesList(ConfigManager configManager) throws Exception {
        String outputPath = configManager.get("output");
        String cacheDir = configManager.get("cache");
        String dist = configManager.get("distribution");
        String arch = configManager.get("architecture");

        if (dist == null || arch == null || outputPath == null || cacheDir == null) {
            System.err.println("Config file must contain 'distribution', 'architecture', 'output', and 'cache' properties.");
            return;
        }

        String url = String.format(BASE_URL, dist, arch);

        Path cachePath = Path.of(cacheDir);
        Files.createDirectories(cachePath);

        Path outputFile = cachePath.resolve(String.format("%s-%s.txt.gz", dist, arch));

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

    public static List<DebianPackage> parseCachedPackagesList(ConfigManager configManager) {
        String cache = configManager.get("cache", "runtime-cache");
        String dist = configManager.get("distribution");
        String arch = configManager.get("architecture");

        String outputFile = String.format("%s-%s.txt.gz", dist, arch);
        File file = new File(cache, outputFile);

        List<DebianPackage> packages = new ArrayList<>();

        try (
                InputStream fileStream = new FileInputStream(file);
                InputStream gzipStream = file.getName().endsWith(".gz") ?
                        new GZIPInputStream(fileStream) : fileStream;
                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, java.nio.charset.StandardCharsets.UTF_8))
        ) {
            String line;
            StringBuilder lines = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    DebianPackage dpkg = buildDebianPackage(lines.toString(), dist);
                    if(dpkg != null)
                        packages.add(dpkg);
                    lines = new StringBuilder();
                }
                lines.append(line);
                lines.append("\n");
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return packages;
    }

    private static DebianPackage buildDebianPackage(String packagesContent, String distribution) {
        String[] packageEntries = packagesContent.split("\n\n");

        String packageName = null;
        String version = null;
        String filename = null;
        String architecture = null;
        String sha256digest = null;

        for (String entry : packageEntries) {
            entry += "\n";
            if (entry.trim().isEmpty()) continue;

            packageName = (packageName == null) ? extractField(entry, "Package") : packageName;
            version = (version == null) ? extractField(entry, "Version") : version;
            filename = (filename == null) ? extractField(entry, "Filename") : filename;
            architecture = (architecture == null) ? extractField(entry, "Architecture") : architecture;
            sha256digest = (sha256digest == null) ? extractField(entry, "SHA256") : sha256digest;

        }

        if (packageName != null && version != null) {
            return new DebianPackage(packageName, version, architecture, filename, sha256digest, distribution);
        }
        return null;
    }

    private static String extractField(String entry, String fieldName) {
        String regex = fieldName + ": (.*?)\n";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(entry);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static Iterator<DebianPackage> parseCachedPackagesIterator(ConfigManager configManager) {
        return parseCachedPackagesList(configManager).iterator();
    }

    public static void main(String[] args) throws Exception {
        ConfigManager configManager = new ConfigManager("config.properties");
        List<String> mirrors = DebianMirrorCache.loadCachedMirrors(false);
        Iterator<DebianPackage> result = parseCachedPackagesList(configManager).iterator();

        while(result.hasNext()) {
            DebianPackage pkg = result.next();
            String url = pkg.buildDownloadUrl(mirrors.get(1));
            System.out.println(url);
        }
    }
}
