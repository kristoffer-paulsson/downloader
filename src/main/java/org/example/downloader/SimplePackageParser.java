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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class SimplePackageParser {

    static class Package {
        String name;
        String version;
        String description;

        public Package(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }

        @Override
        public String toString() {
            return "Package{name='" + name + "', version='" + version + "', description='" + description + "'}";
        }
    }

    public static List<Package> parsePackages(File filePath, int startLine, int numRows) {
        List<Package> packages = new ArrayList<>();
        // Regex pattern to match package entries: name (version) description
        Pattern pattern = Pattern.compile("^(\\S+)\\s+\\(([^)]+)\\)\\s+(.+)$");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int currentLine = 0;

            // Skip lines until the startLine (1-based index)
            while (currentLine < startLine - 1 && (line = reader.readLine()) != null) {
                currentLine++;
            }

            // Read and parse the specified number of rows
            int rowsParsed = 0;
            while (rowsParsed < numRows && (line = reader.readLine()) != null) {
                currentLine++;
                // Skip empty lines or lines that don't match the expected format
                if (line.trim().isEmpty()) {
                    continue;
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String name = matcher.group(1);
                    String version = matcher.group(2);
                    String description = matcher.group(3);
                    packages.add(new Package(name, version, description));
                    rowsParsed++;
                } else {
                    System.err.println("Line " + currentLine + " does not match expected format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return packages;
    }

    public static List<Package> parseGzipPackages(File filePath, int startLine) {
        List<Package> packages = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(\\S+)\\s+\\(([^)]+)\\)\\s+(.+)$");

        try (
                InputStream fileStream = new FileInputStream(filePath);
                InputStream gzipStream = filePath.getName().endsWith(".gz") ?
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
                    packages.add(new Package(name, version, description));
                } else {
                    System.err.println("Line " + currentLine + " does not match expected format: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return packages;
    }

    public static void main(String[] args) throws IOException {
        ConfigManager configManager = new ConfigManager("config.properties");
        String output = configManager.get("output", "allpackages.txt");
        String cache = configManager.get("cache", "runtime-cache");
        File file = new File(cache, output);

        List<Package> result = parseGzipPackages(file, 7);
        result.forEach(System.out::println);
    }
}