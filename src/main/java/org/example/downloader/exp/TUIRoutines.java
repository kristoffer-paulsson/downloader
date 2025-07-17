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

import java.util.Scanner;
import java.util.List;
import java.util.function.Consumer;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, and managing console output.
 */
public class TUIRoutines {
    private final Scanner scanner;

    public TUIRoutines() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Clears the console screen. Uses ANSI escape codes for Unix-like systems.
     * For Windows, you may need to adapt this method.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Displays a menu with a title and a list of options.
     *
     * @param title   The title of the menu.
     * @param options A list of menu options to display.
     */
    public void displayMenu(String title, List<String> options) {
        clearScreen();
        System.out.println("=== " + title + " ===");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        System.out.print("Enter choice (1-" + options.size() + "): ");
    }

    /**
     * Reads and validates user input for a menu with the given number of options.
     *
     * @param maxOptions The number of valid menu options.
     * @param onInvalid  A callback to handle invalid input.
     * @return The user's choice (1-based index) or -1 if invalid.
     */
    public int readMenuChoice(int maxOptions, Consumer<String> onInvalid) {
        String input = scanner.nextLine().trim();
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= maxOptions) {
                return choice;
            } else {
                onInvalid.accept("Choice must be between 1 and " + maxOptions + "!");
                return -1;
            }
        } catch (NumberFormatException e) {
            onInvalid.accept("Invalid input! Please enter a number.");
            return -1;
        }
    }

    /**
     * Displays a message and waits for the user to press Enter.
     *
     * @param message The message to display.
     */
    public void showMessageAndWait(String message) {
        System.out.println(message);
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Closes the scanner to free resources.
     */
    public void close() {
        scanner.close();
    }

    /**
     * Example usage of TUIRoutines in a standalone program.
     */
    public static void main(String[] args) {
        TUIRoutines tui = new TUIRoutines();
        boolean running = true;

        // Define menu options
        List<String> mainMenuOptions = List.of(
                "View Profile",
                "Edit Settings",
                "Exit"
        );

        while (running) {
            // Display menu
            tui.displayMenu("Main Menu", mainMenuOptions);

            // Handle input
            int choice = tui.readMenuChoice(mainMenuOptions.size(), message -> {
                tui.showMessageAndWait(message);
            });

            // Process choice
            switch (choice) {
                case 1:
                    tui.showMessageAndWait("Displaying profile...");
                    break;
                case 2:
                    tui.showMessageAndWait("Editing settings...");
                    break;
                case 3:
                    tui.showMessageAndWait("Exiting...");
                    running = false;
                    break;
                default:
                    // Invalid input handled by readMenuChoice
                    break;
            }
        }

        // Clean up
        tui.close();
    }
}
