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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileDigestParser {
    // Class to hold digest and filename
    public static class FileEntry {
        private final String digest;
        private final String filename;

        public FileEntry(String digest, String filename) {
            this.digest = digest;
            this.filename = filename;
        }

        public String getDigest() {
            return digest;
        }

        public String getFilename() {
            return filename;
        }

        @Override
        public String toString() {
            return "Digest: " + digest + ", Filename: " + filename;
        }
    }

    public static List<FileEntry> parseFile(String filePath) throws IOException {
        List<FileEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Split on two spaces
                String[] parts = line.split("  ");
                if (parts.length != 2) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }

                String digest = parts[0].trim();
                String filename = parts[1].trim();

                // Validate digest (assuming SHA-256, 64 hex characters)
                if (digest.matches("[a-fA-F0-9]{64}")) {
                    entries.add(new FileEntry(digest, filename));
                } else {
                    System.err.println("Invalid digest format: " + digest);
                }
            }
        }

        return entries;
    }

    public static void main(String[] args) {
        String filePath = "path/to/your/file.txt"; // Replace with your file path
        try {
            List<FileEntry> entries = parseFile(filePath);
            for (FileEntry entry : entries) {
                System.out.println(entry);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
