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
import java.util.Properties;

public class DownloadPackagesList {
    public static void runWithConfig(ConfigManager configManager) throws Exception {
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
}
