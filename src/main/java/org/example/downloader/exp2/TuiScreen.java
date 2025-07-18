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
 * Supports progress bars, status bars, ANSI color codes, and a threaded run loop with Escape or 'q'/'Q' key detection using System.in.
 */
public class TuiScreen {
    private final int width; // Terminal width
    private final int height; // Terminal height
    private char[][] buffer; // Screen buffer for characters
    private String[][] colorBuffer; // Buffer for ANSI color codes
    private boolean isWindows;
    private volatile boolean running; // Flag to control the TUI loop

    // ANSI color codes
    public enum Color {
        RESET("\033[0m"),
        RED("\033[31m"),
        GREEN("\033[32m"),
        YELLOW("\033[33m"),
        BLUE("\033[34m"),
        MAGENTA("\033[35m"),
        CYAN("\033[36m"),
        WHITE("\033[37m"),
        BOLD_RED("\033[1;31m"),
        BOLD_GREEN("\033[1;32m"),
        BOLD_YELLOW("\033[1;33m"),
        BOLD_BLUE("\033[1;34m"),
        BOLD_MAGENTA("\033[1;35m"),
        BOLD_CYAN("\033[1;36m"),
        BOLD_WHITE("\033[1;37m");

        private final String code;

        Color(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public TuiScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.buffer = new char[height][width];
        this.colorBuffer = new String[height][width];
        this.isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        this.running = true;
        clearBuffer();
    }

    /**
     * Clears the internal screen and color buffers.
     */
    private void clearBuffer() {
        for (char[] row : buffer) {
            Arrays.fill(row, ' ');
        }
        for (String[] row : colorBuffer) {
            Arrays.fill(row, Color.RESET.getCode());
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
     * Updates the screen by rendering the buffer with colors.
     */
    public void render() {
        moveCursor(0, 0);
        StringBuilder sb = new StringBuilder();
        String lastColor = Color.RESET.getCode();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                String currentColor = colorBuffer[row][col];
                if (!currentColor.equals(lastColor)) {
                    sb.append(currentColor);
                    lastColor = currentColor;
                }
                sb.append(buffer[row][col]);
            }
            sb.append(Color.RESET.getCode()).append('\n');
            lastColor = Color.RESET.getCode();
        }
        System.out.print(sb.toString());
        System.out.flush();
    }

    /**
     * Draws a progress bar at the specified row with color.
     * @param row The row to draw the progress bar
     * @param progress Progress percentage (0.0 to 1.0)
     * @param barWidth Width of the progress bar
     * @param label Optional label for the progress bar
     * @param filledColor Color for filled portion
     * @param emptyColor Color for empty portion
     * @param labelColor Color for the label
     */
    public void drawProgressBar(int row, double progress, int barWidth, String label,
                                Color filledColor, Color emptyColor, Color labelColor) {
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
                colorBuffer[row][i] = labelColor.getCode();
            }
            buffer[row][label.length()] = ' ';
            colorBuffer[row][label.length()] = Color.RESET.getCode();
            buffer[row][label.length() + 1] = '|';
            colorBuffer[row][label.length() + 1] = Color.RESET.getCode();
        }

        // Draw progress bar
        for (int i = 0; i < barWidth; i++) {
            buffer[row][startCol + i] = (i < filled) ? '█' : '─';
            colorBuffer[row][startCol + i] = (i < filled) ? filledColor.getCode() : emptyColor.getCode();
        }
        buffer[row][startCol + barWidth] = '|';
        colorBuffer[row][startCol + barWidth] = Color.RESET.getCode();
    }

    /**
     * Draws a status bar at the specified row with color.
     * @param row The row to draw the status bar
     * @param status The status message
     * @param alignLeft True to align left, false to align right
     * @param textColor Color for the status text
     */
    public void drawStatusBar(int row, String status, boolean alignLeft, Color textColor) {
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
            colorBuffer[row][startCol + i] = textColor.getCode();
        }
    }

    /**
     * Stops the TUI loop.
     */
    public void stop() {
        this.running = false;
    }

    /**
     * Checks for Escape key or 'q'/'Q' press using System.in in a non-blocking manner.
     * @return true if Escape or 'q'/'Q' is pressed, false otherwise
     */
    private boolean isEscapePressed() throws IOException {
        if (System.in.available() > 0) {
            int ch = System.in.read();
            return ch == 27 || ch == 'q' || ch == 'Q'; // Escape or q/Q
        }
        return false;
    }

    /**
     * Runs the TUI in a loop with the provided update logic, checking for Escape or 'q'/'Q'.
     * @param updateLogic A Runnable defining the screen update logic
     * @param updateIntervalMs The interval between updates in milliseconds
     * @return The Thread running the TUI loop
     */
    public Thread runScreen(Runnable updateLogic, long updateIntervalMs) {
        Thread tuiThread = new Thread(() -> {
            try {
                clearScreen();
                while (running) {
                    if (isEscapePressed()) {
                        stop();
                        continue; // Skip rendering to show exit message
                    }
                    clearBuffer();
                    updateLogic.run();
                    render();
                    Thread.sleep(updateIntervalMs);
                }
                // Final cleanup
                clearScreen();
                drawStatusBar(5, "TUI Exited", true, Color.BOLD_GREEN);
                render();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        tuiThread.start();
        return tuiThread;
    }

    /**
     * Example usage of the TUI with threaded run loop, color support, and Escape/q/Q detection using System.in.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        TuiScreen tui = new TuiScreen(80, 24); // Standard terminal size

        // Define update logic for the TUI
        final int[] progressCounter = {0}; // Mutable counter for lambda
        Runnable updateLogic = () -> {
            double progress = (progressCounter[0] % 101) / 100.0;
            tui.drawProgressBar(5, progress, 50, "Processing",
                    Color.GREEN, Color.RED, Color.BOLD_CYAN);
            tui.drawStatusBar(7, "Status: Processing file " + (progressCounter[0] % 101) + "%",
                    true, Color.BOLD_YELLOW);
            tui.drawStatusBar(8, "Time: " + System.currentTimeMillis(),
                    false, Color.BOLD_MAGENTA);
            tui.drawStatusBar(10, "Press ESC or q to exit", true, Color.BOLD_WHITE);
            progressCounter[0] += 5;
        };

        // Start the TUI loop
        Thread tuiThread = tui.runScreen(updateLogic, 200);

        // Wait for the TUI thread to finish
        tuiThread.join();
    }
}
