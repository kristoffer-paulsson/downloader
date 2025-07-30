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

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ParallelProgressBars {
    // ANSI escape codes for colors
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CLEAR_LINE = "\u001B[2K"; // Clear line
    public static final String ANSI_CURSOR_UP = "\u001B[1A"; // Move cursor up one line

    // Shared state for progress
    private static final int[] progresses = new int[3]; // Progress for each bar
    private static final int total = 100; // Total progress for each task
    private static final int width = 40; // Bar width
    private static final Object lock = new Object(); // For thread-safe printing

    public static void printProgressBars(String[] taskNames, String[] colors) {
        synchronized (lock) {
            // Move cursor up to start of previous bars (if any)
            for (int i = 0; i < progresses.length; i++) {
                if (i > 0) System.out.print(ANSI_CURSOR_UP);
            }
            // Print each progress bar
            for (int i = 0; i < progresses.length; i++) {
                int progress = progresses[i];
                int percent = progress * 100 / total;
                int filled = progress * width / total;

                // Select color
                String colorCode;
                switch (colors[i].toLowerCase()) {
                    case "yellow":
                        colorCode = ANSI_YELLOW;
                        break;
                    case "red":
                        colorCode = ANSI_RED;
                        break;
                    case "green":
                    default:
                        colorCode = ANSI_GREEN;
                }

                // Build the progress bar
                StringBuilder bar = new StringBuilder(ANSI_CLEAR_LINE + "\r> Task :" + taskNames[i] + " [");
                bar.append(colorCode);
                for (int j = 0; j < width; j++) {
                    bar.append(j < filled ? "=" : "-");
                }
                bar.append(ANSI_RESET);
                bar.append(String.format("] %d%%", percent));

                System.out.println(bar);
            }
            System.out.flush();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String[] taskNames = {"compileJava", "testClasses", "buildDocs"};
        String[] colors = {"green", "yellow", "red"};
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Start three tasks in parallel
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                for (int j = 0; j <= total; j++) {
                    synchronized (lock) {
                        progresses[taskId] = j; // Update progress for this task
                        printProgressBars(taskNames, colors);
                    }
                    try {
                        // Simulate work with different speeds
                        Thread.sleep(taskId == 0 ? 100 : taskId == 1 ? 150 : 200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Shutdown executor and wait for tasks to complete
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("\nAll tasks completed!");
    }
}
