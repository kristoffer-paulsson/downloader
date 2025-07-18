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
import java.util.Arrays;

/**
 * A CLI TUI class for updating the terminal screen without scrolling.
 * Supports progress bars and status bars with ANSI escape codes.
 */
public class TuiScreen {
    private final int width; // Terminal width
    private final int height; // Terminal height
    private char[][] buffer; // Screen buffer
    private boolean isWindows;

    public TuiScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = new char[height][width];
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        clearBuffer();
    }

    /**
     * Clears the internal screen buffer.
     */
    private void clearBuffer() {
        for (char[] row : buffer) {
            Arrays.fill(row, ' ');
        }
    }

    /**
     * Clears the terminal screen.
     */
    public void clearScreen() throws IOException {
        if (isWindows) {
            //new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } else {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
        clearBuffer();
    }

    /**
     * Moves the cursor to the specified position.
     */
    private void moveCursor(int row, int col) {
        System.out.print(String.format("\033[%d;%dH", row + 1, col + 1));
    }

    /**
     * Updates the screen by rendering the buffer.
     */
    public void render() {
        moveCursor(0, 0);
        StringBuilder sb = new StringBuilder();
        for (char[] row : buffer) {
            sb.append(row).append('\n');
        }
        System.out.print(sb.toString());
        System.out.flush();
    }

    /**
     * Draws a progress bar at the specified row.
     * @param row The row to draw the progress bar
     * @param progress Progress percentage (0.0 to 1.0)
     * @param barWidth Width of the progress bar
     * @param label Optional label for the progress bar
     */
    public void drawProgressBar(int row, double progress, int barWidth, String label) {
        if (row < 0 || row >= height || barWidth > width) {
            return;
        }
        progress = Math.max(0.0, Math.min(1.0, progress));
        int filled = (int) (barWidth * progress);
        int startCol = (label != null) ? label.length() + 2 : 0;

        // Write label if provided
        if (label != null) {
            for (int i = 0; i < label.length() && i < width; i++) {
                buffer[row][i] = label.charAt(i);
            }
            buffer[row][label.length()] = ' ';
            buffer[row][label.length() + 1] = '|';
        }

        // Draw progress bar
        for (int i = 0; i < barWidth; i++) {
            buffer[row][startCol + i] = (i < filled) ? '█' : '─';
        }
        buffer[row][startCol + barWidth] = '|';
    }

    /**
     * Draws a status bar at the specified row.
     * @param row The row to draw the status bar
     * @param status The status message
     * @param alignLeft True to align left, false to align right
     */
    public void drawStatusBar(int row, String status, boolean alignLeft) {
        if (row < 0 || row >= height) {
            return;
        }
        int startCol = alignLeft ? 0 : width - status.length();
        if (startCol < 0) {
            status = status.substring(0, width);
            startCol = 0;
        }
        for (int i = 0; i < status.length() && (startCol + i) < width; i++) {
            buffer[row][startCol + i] = status.charAt(i);
        }
    }

    /**
     * Example usage of the TUI.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        TuiScreen tui = new TuiScreen(80, 24); // Standard terminal size
        tui.clearScreen();

        // Simulate progress and status updates
        for (int i = 0; i <= 100; i += 5) {
            tui.clearBuffer();
            double progress = i / 100.0;
            tui.drawProgressBar(5, progress, 50, "Processing");
            tui.drawStatusBar(7, "Status: Processing file " + i + "%", true);
            tui.drawStatusBar(8, "Time: " + System.currentTimeMillis(), false);
            tui.render();
            Thread.sleep(200);
        }

        // Final status
        tui.clearBuffer();
        tui.drawStatusBar(5, "Task Completed!", true);
        tui.render();
    }
}
