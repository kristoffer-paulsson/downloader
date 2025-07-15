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
import java.util.Scanner;

public class Main {
    private static final String DEFAULT_CONFIG = "config.properties";

    public static void main(String[] args) throws Exception {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        Scanner scanner = new Scanner(System.in);
        ConfigManager configManager = new ConfigManager(configPath);

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
                    setupConfig(configManager, scanner);
                    break;
                case "2":
                    DebianPackagesListCache.downloadAndCachePackagesList(configManager);
                    break;
                case "3":
                    System.out.println("Exiting.");
                    return;
                case "4":
                    reviewConfig(configManager);
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private static void setupConfig(ConfigManager configManager, Scanner scanner) throws IOException {
        String defaultUrl = "https://packages.debian.org/stable/allpackages?format=txt.gz";
        String defaultOutput = "allpackages.txt.gz";
        String defaultCache = "runtime-cache";

        System.out.print("Enter download URL [" + defaultUrl + "]: ");
        String url = scanner.nextLine().trim();
        configManager.set("url", url.isEmpty() ? defaultUrl : url);

        System.out.print("Enter output file name [" + defaultOutput + "]: ");
        String output = scanner.nextLine().trim();
        configManager.set("output", output.isEmpty() ? defaultOutput : output);

        System.out.print("Enter cache directory [" + defaultCache + "]: ");
        String cache = scanner.nextLine().trim();
        configManager.set("cache", cache.isEmpty() ? defaultCache : cache);

        configManager.save();
        System.out.println("Config saved to " + configManager.getProperties());
    }

    private static void reviewConfig(ConfigManager configManager) {
        System.out.println("\nCurrent config:");
        configManager.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
    }
}