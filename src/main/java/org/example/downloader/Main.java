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
import java.nio.file.*;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static final String DEFAULT_CONFIG = "config.properties";

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nDebian Downloader CLI");
            System.out.println("1. Setup config");
            System.out.println("2. Download packages list");
            System.out.println("3. Exit");
            System.out.println("4. Review config");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    setupConfig(configPath, scanner);
                    break;
                case "2":
                    DownloadPackagesList.runWithConfig(configPath);
                    break;
                case "3":
                    System.out.println("Exiting.");
                    return;
                case "4":
                    reviewConfig(configPath);
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void setupConfig(String configPath, Scanner scanner) throws IOException {
        Properties config = new Properties();

        String defaultUrl = "https://packages.debian.org/stable/allpackages?format=txt.gz";
        String defaultOutput = "allpackages.txt.gz";
        String defaultCache = "runtime-cache";

        System.out.print("Enter download URL [" + defaultUrl + "]: ");
        String url = scanner.nextLine().trim();
        config.setProperty("url", url.isEmpty() ? defaultUrl : url);

        System.out.print("Enter output file name [" + defaultOutput + "]: ");
        String output = scanner.nextLine().trim();
        config.setProperty("output", output.isEmpty() ? defaultOutput : output);

        System.out.print("Enter cache directory [" + defaultCache + "]: ");
        String cache = scanner.nextLine().trim();
        config.setProperty("cache", cache.isEmpty() ? defaultCache : cache);

        try (OutputStream out = Files.newOutputStream(Paths.get(configPath))) {
            config.store(out, "Debian Downloader Configuration");
        }
        System.out.println("Config saved to " + configPath);
    }

    private static void reviewConfig(String configPath) {
        Properties config = new Properties();
        try (InputStream in = Files.newInputStream(Paths.get(configPath))) {
            config.load(in);
            System.out.println("\nCurrent config:");
            config.forEach((k, v) -> System.out.println(k + " = " + v));
        } catch (IOException e) {
            System.out.println("Could not read config file: " + e.getMessage());
        }
    }
}