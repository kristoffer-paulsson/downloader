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
package org.example.downloader.exp;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class DebianPackageSizeSum {
    public static void main(String[] args) {
        String filePath = "package-cache/dists/bookworm/main/binary-amd64/Packages.gz"; // Replace with your file path
        long totalSize = 0;
        int packageCount = 0;
        String component = "main";

        try (FileInputStream fis = new FileInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Size: ")) {
                    try {
                        // Extract the size value after "Size: "
                        String sizeStr = line.substring(6).trim();
                        long size = Long.parseLong(sizeStr);
                        totalSize += size;
                        packageCount++;
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid size format in line: " + line);
                    }
                }
            }

            System.out.println("Total size of repository '" + component + "' with all " + packageCount + " packages: " + totalSize + " bytes");

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
