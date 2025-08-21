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
import org.example.downloader.util.AbstractFileParser;
import org.example.downloader.util.PrintHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class WinetricksParser extends AbstractFileParser<WinetricksPackage> {

    public Map<String, Set<String>> statistics = Map.of(
            "verbs", new HashSet<>(),
            "categories", new HashSet<>()
    );

    public WinetricksParser(String filePath) throws IOException {
        super(filePath);
    }

    public WinetricksParser(InputStream fileStream) throws IOException {
        super(fileStream);
    }

    @Override
    protected WinetricksPackage parseFieldsAndCreatePackage(Map<String, StringBuilder> packageData) {
        // Collect statistics
        if (packageData.containsKey("Verb")) {
            statistics.get("verbs").add(packageData.get("Verb").toString());
        }
        if (packageData.containsKey("Category")) {
            statistics.get("categories").add(packageData.get("Category").toString());
        }

        return new WinetricksPackage(
                packageData.getOrDefault("Filename", new StringBuilder()).toString(),
                packageData.getOrDefault("Url", new StringBuilder()).toString(),
                packageData.getOrDefault("Sha256", new StringBuilder()).toString(),
                packageData.getOrDefault("Size", new StringBuilder("0")).toString(),
                packageData.getOrDefault("Verb", new StringBuilder()).toString(),
                packageData.getOrDefault("Category", new StringBuilder()).toString()
        );
    }

    public static class Filter {
        WinetricksDownloadEnvironment wde;

        List<WinetricksCategory> categoryFilter;

        Filter(WinetricksDownloadEnvironment wde) {
            this.wde = wde;

            categoryFilter = wde.getCategories();
            if(categoryFilter.get(0) == WinetricksCategory.UNKNOWN)
                categoryFilter = List.of(WinetricksCategory.values());

        }

        public boolean filterPackage(WinetricksPackage pkg) {
            boolean add = true;

            add = !pkg.getSize().equals("-1") && add;
            add = categoryFilter.contains(pkg.getCategory()) && add;

            return add;
        }
    }

    public static WinetricksParser.Filter createFilter(WinetricksDownloadEnvironment wde) {
        return new Filter(wde);
    }

    public static List<WinetricksPackage> filterPackages(WinetricksDownloadEnvironment wde) {
        HashMap<String, WinetricksPackage> filteredPackages = new HashMap<>();
        WinetricksParser.Filter filter = createFilter(wde);

        try {
            WinetricksParser parser = new WinetricksParser(ClassLoader.getSystemResourceAsStream("Winetricks.gz"));

            while (parser.hasNext()) {
                WinetricksPackage pkg = parser.next();
                if(filter.filterPackage(pkg)){
                    filteredPackages.put(pkg.uniqueKey(), pkg);
                }
            }

            parser.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return List.copyOf(filteredPackages.values());
    }

    public static void main(String[] args) {
        try {
            long totalSize = 0;
            WinetricksParser parser = new WinetricksParser("Winetricks.gz");
            while (parser.hasNext()) {
                WinetricksPackage pkg = parser.next();
                totalSize += pkg.getByteSize();
                System.out.println("Parsed package: " + pkg.toString());
            }
            parser.close();

            parser.statistics.forEach((key, value) -> {
                System.out.println(key + ": " + value);
            });
            System.out.println("Total size of all packages: " + totalSize + " bytes");
            System.out.println("Total size of all packages: " + PrintHelper.formatByteSize(totalSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
