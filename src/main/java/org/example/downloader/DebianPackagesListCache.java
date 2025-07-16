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
import java.sql.SQLException;
import java.util.*;
import java.util.regex.*;
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
        Pattern pattern = Pattern.compile("^(\\S+)\\s+\\(([^)]+)\\)\\s+(.+)$");

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
                    printPackagesToOutput(lines.toString(), dist);
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

    private static void printPackagesToOutput(String packagesContent, String distribution) {
        // Parse Packages file
        String[] packageEntries = packagesContent.split("\n\n");
        for (String entry : packageEntries) {
            entry += "\n";
            if (entry.trim().isEmpty()) continue;

            String packageName = extractField(entry, "Package");
            String version = extractField(entry, "Version");
            String filename = extractField(entry, "Filename");
            String architecture = extractField(entry, "Architecture");
            String sha256digest = extractField(entry, "SHA256");
            String md5sum = extractField(entry, "MD5sum");


            if (packageName != null && version != null) {
                System.out.println(packageName);
                System.out.println(version);
                System.out.println(architecture);
                System.out.println(distribution);
                System.out.println(filename);
                System.out.println(sha256digest);
                System.out.println("");

            }
        }
    }

    private static String extractField(String entry, String fieldName) {
        String regex = fieldName + ": (.*?)\n";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(entry);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static Iterator<DebianPackage> parseCachedPackagesIterator(ConfigManager configManager, int startLine) {
        return parseCachedPackagesList(configManager).iterator();
    }

    public static void main(String[] args) throws Exception {
        ConfigManager configManager = new ConfigManager("config.properties");
        //downloadAndCachePackagesList(configManager);
        //List<String> mirrors = DebianMirrorCache.loadCachedMirrors();
        parseCachedPackagesList(configManager);

        /*while(result.hasNext()) {
            DebianPackage pkg = result.next();
            pkg.complement(DebianDistribution.BOOKWORM, DebianComponent.MAIN, DebianArchitecture.AMD_64);
            System.out.println(pkg.buildDownloadUrl(mirrors.get(1)).buildDownloadUrl());
            System.out.println(pkg.buildDownloadUrl(mirrors.get(1)).buildDownloadUrlAsAll());
        }*/
    }
}
