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

public class ProgressBar {
    // ANSI escape codes for colors
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void printProgress(int progress, int total, int width, String color) {
        int percent = progress * 100 / total;
        int filled = progress * width / total;

        // Select color based on input
        String colorCode = color;

        // Build the progress bar
        StringBuilder bar = new StringBuilder("\r[");
        bar.append(colorCode); // Apply color
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "=" : "-");
        }
        bar.append(ANSI_RESET); // Reset color
        bar.append(String.format("] %d%%", percent));

        System.out.print(bar);
        System.out.flush();
    }

    public static void main(String[] args) throws InterruptedException {
        int total = 100;
        int width = 50;

        // Example: Green progress bar
        for (int i = 0; i <= total / 3; i++) {
            printProgress(i, total, width, ANSI_GREEN);
            Thread.sleep(100); // Simulate work
        }

        // Example: Yellow progress bar
        for (int i = total / 3 + 1; i <= 2 * total / 3; i++) {
            printProgress(i, total, width, ANSI_YELLOW);
            Thread.sleep(100); // Simulate work
        }

        // Example: Red progress bar
        for (int i = 2 * total / 3 + 1; i <= total; i++) {
            printProgress(i, total, width, ANSI_RED);
            Thread.sleep(100); // Simulate work
        }

        System.out.println(); // Newline at the end
    }
}
