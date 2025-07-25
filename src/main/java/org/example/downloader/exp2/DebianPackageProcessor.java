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
package org.example.downloader.exp2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DebianPackageProcessor {
    public static void main(String[] args) {
        String inputPath = "Packages.gz";
        String outputPath = "FilteredPackages.gz";
        List<DebianPackage> packages = new ArrayList<>();

        // Parse packages
        try (DebianPackageParser parser = new DebianPackageParser(inputPath)) {
            parser.forEachRemaining(packages::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Optionally filter or modify packages
        List<DebianPackage> filteredPackages = packages.stream()
                .filter(pkg -> pkg.section().equals("games")) // Example: keep only games
                .collect(Collectors.toList());

        // Write to new Packages.gz
        try {
            DebianPackageBuilder.buildPackagesFile(filteredPackages, outputPath);
            System.out.println("Filtered Packages.gz file created at: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
