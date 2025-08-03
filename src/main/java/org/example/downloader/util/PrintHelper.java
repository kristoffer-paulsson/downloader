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
