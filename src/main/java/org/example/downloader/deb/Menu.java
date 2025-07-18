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
package org.example.downloader.deb;

import org.example.downloader.InversionOfControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public abstract class Menu {
    protected final InversionOfControl ioc;

    private final String name;
    private final Scanner scanner;
    private final List<Option> options;

    public Menu(InversionOfControl ioc, String name) {
        this.ioc = ioc;
        this.scanner = ioc.resolve(Scanner.class);

        this.name = name;
        this.options = new ArrayList<>();
        setupMenu();
        registerOption("Return", (option) -> {});
    }

    protected abstract void setupMenu();

    public static class Option {
        public final String title;
        private final Consumer<Option> command;

        public Option(String title, Consumer<Option> command) {
            this.title = title;
            this.command = command;
        }

        public void executeCommand() {
            if(command == null) throw new IllegalStateException(String.format("No command for option %s", title));
            else command.accept(this);
        }
    }

    protected void registerOption(String title, Consumer<Option> execute) {
        options.add(new Option(title, execute));
    }

    public void runMenu() {
        boolean isRunning = true;

        while(isRunning) {
            displayMenu();
            int choice = readMenuChoice(options.size(), this::showMessageAndWait);
            if (choice > 0 && choice <= options.size()) options.get(choice - 1).executeCommand();
            if (choice == options.size()) isRunning = false;
        }

        close();
    }

    /**
     * Clears the console screen. Uses ANSI escape codes for Unix-like systems.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Displays a menu with a title and a list of options.
     */
    public void displayMenu() {
        clearScreen();
        System.out.println("=== " + name + " ===");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i).title);
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
    }
}
