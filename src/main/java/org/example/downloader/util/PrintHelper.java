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
package org.example.downloader.util;

public class PrintHelper {

    // ANSI Foreground Colors (Standard)
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_MAGENTA = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // ANSI Foreground Colors (Light/Bright)
    public static final String ANSI_LIGHT_BLACK = "\u001B[90m";
    public static final String ANSI_LIGHT_RED = "\u001B[91m";
    public static final String ANSI_LIGHT_GREEN = "\u001B[92m";
    public static final String ANSI_LIGHT_YELLOW = "\u001B[93m";
    public static final String ANSI_LIGHT_BLUE = "\u001B[94m";
    public static final String ANSI_LIGHT_MAGENTA = "\u001B[95m";
    public static final String ANSI_LIGHT_CYAN = "\u001B[96m";
    public static final String ANSI_LIGHT_WHITE = "\u001B[97m";

    // ANSI Background Colors (Standard)
    public static final String ANSI_BG_BLACK = "\u001B[40m";
    public static final String ANSI_BG_RED = "\u001B[41m";
    public static final String ANSI_BG_GREEN = "\u001B[42m";
    public static final String ANSI_BG_YELLOW = "\u001B[43m";
    public static final String ANSI_BG_BLUE = "\u001B[44m";
    public static final String ANSI_BG_MAGENTA = "\u001B[45m";
    public static final String ANSI_BG_CYAN = "\u001B[46m";
    public static final String ANSI_BG_WHITE = "\u001B[47m";

    // ANSI Background Colors (Light/Bright)
    public static final String ANSI_BG_LIGHT_BLACK = "\u001B[100m";
    public static final String ANSI_BG_LIGHT_RED = "\u001B[101m";
    public static final String ANSI_BG_LIGHT_GREEN = "\u001B[102m";
    public static final String ANSI_BG_LIGHT_YELLOW = "\u001B[103m";
    public static final String ANSI_BG_LIGHT_BLUE = "\u001B[104m";
    public static final String ANSI_BG_LIGHT_MAGENTA = "\u001B[105m";
    public static final String ANSI_BG_LIGHT_CYAN = "\u001B[106m";
    public static final String ANSI_BG_LIGHT_WHITE = "\u001B[107m";

    public static final String ANSI_RESET = "\u001B[0m";

    public static String coloredMessage(String message, String ansiColor) {
        return ansiColor + message + ANSI_RESET;
    }

    public static String formatSpeed(float bytesPerSecond) {
        if (bytesPerSecond < 0) {
            return "0 B/s";
        }

        final String[] units = {"B/s", "KB/s", "MB/s", "GB/s"};
        int unitIndex = 0;
        float speed = bytesPerSecond;

        while (speed >= 1024 && unitIndex < units.length - 1) {
            speed /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", speed, units[unitIndex]);
    }

    public static String formatByteSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }

        final String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    public static String formatTime(float seconds) {
        if (seconds < 0) {
            return "0s";
        }

        final String[] units = {"s", "m", "h", "d"};
        final long[] thresholds = {60, 3600, 86400}; // seconds in a minute, hour, day
        int unitIndex = 0;
        float time = seconds;

        while (time >= thresholds[unitIndex] && unitIndex < thresholds.length - 1) {
            time /= thresholds[unitIndex];
            unitIndex++;
        }

        return String.format("%.2f %s", time, units[unitIndex]);
    }
}
