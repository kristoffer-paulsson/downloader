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
public class Menu {
    private final InversionOfControl ioc;

    private final Scanner scanner;
    private final List<Answer> answers; // Stores collected answers

    public Menu(InversionOfControl ioc) {
        this.ioc = ioc;
        this.scanner = new Scanner(System.in);
        this.answers = new ArrayList<>();
    }

    /**
     * Represents a collected answer with a question and response.
     */
    public static class Answer {
        private final String question;
        private final String response;

        public Answer(String question, String response) {
            this.question = question;
            this.response = response;
        }

        public String getQuestion() {
            return question;
        }

        public String getResponse() {
            return response;
        }

        @Override
        public String toString() {
            return "Q: " + question + " | A: " + response;
        }
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
     * Asks a question and collects a free-text answer, with optional validation.
     *
     * @param question   The question to display.
     * @param validator  A predicate to validate the answer (null if no validation).
     * @param onInvalid  A callback to handle invalid input.
     * @return True if the answer was valid and collected, false otherwise.
     */
    public boolean askQuestion(String question, Predicate<String> validator, Consumer<String> onInvalid) {
        clearScreen();
        System.out.println("=== Question ===");
        System.out.print(question + ": ");
        String answer = scanner.nextLine().trim();

        if (validator == null || validator.test(answer)) {
            answers.add(new Answer(question, answer));
            return true;
        } else {
            onInvalid.accept("Invalid answer! Please try again.");
            return false;
        }
    }

    /**
     * Asks a multiple-choice question and collects the selected option.
     *
     * @param question   The question to display.
     * @param options    The list of possible options.
     * @param onInvalid  A callback to handle invalid input.
     * @return True if a valid selection was made and collected, false otherwise.
     */
    public boolean askMultipleChoiceQuestion(String question, List<String> options, Consumer<String> onInvalid) {
        clearScreen();
        System.out.println("=== " + question + " ===");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
        System.out.print("Select an option (1-" + options.size() + "): ");

        int choice = readMenuChoice(options.size(), onInvalid);
        if (choice != -1) {
            answers.add(new Answer(question, options.get(choice - 1)));
            return true;
        }
        return false;
    }

    /**
     * Processes collected answers using a provided consumer.
     *
     * @param processor A consumer to process each answer.
     */
    public void processAnswers(Consumer<Answer> processor) {
        clearScreen();
        System.out.println("=== Processing Answers ===");
        if (answers.isEmpty()) {
            System.out.println("No answers collected.");
        } else {
            answers.forEach(answer -> {
                processor.accept(answer);
                System.out.println(answer);
            });
        }
        showMessageAndWait("Processing complete.");
    }

    /**
     * Returns the list of collected answers.
     *
     * @return The list of answers.
     */
    public List<Answer> getAnswers() {
        return new ArrayList<>(answers); // Return a copy to prevent external modification
    }

    /**
     * Clears all collected answers.
     */
    public void clearAnswers() {
        answers.clear();
    }

    /**
     * Closes the scanner to free resources.
     */
    public void close() {
        scanner.close();
    }

    /**
     * Example usage of TUIRoutines with question-asking and answer-processing.
     */
    public void runMenu() {
        Menu tui = new Menu(new InversionOfControl());
        boolean running = true;

        // Define menu options
        List<String> mainMenuOptions = List.of(
                "Answer Questions",
                "Process Answers",
                "Exit"
        );

        // Define multiple-choice question options
        List<String> colorOptions = List.of("Red", "Blue", "Green");

        while (running) {
            tui.displayMenu("Main Menu", mainMenuOptions);
            int choice = tui.readMenuChoice(mainMenuOptions.size(), message -> tui.showMessageAndWait(message));

            switch (choice) {
                case 1:
                    // Ask a free-text question with validation (e.g., non-empty)
                    tui.askQuestion("What is your name?",
                            s -> !s.isEmpty(),
                            message -> tui.showMessageAndWait(message));

                    // Ask a numeric question with validation (e.g., positive integer)
                    tui.askQuestion("How old are you?",
                            s -> {
                                try {
                                    return Integer.parseInt(s) > 0;
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            },
                            message -> tui.showMessageAndWait(message));

                    // Ask a multiple-choice question
                    tui.askMultipleChoiceQuestion("What is your favorite color?",
                            colorOptions,
                            message -> tui.showMessageAndWait(message));
                    break;

                case 2:
                    // Process collected answers
                    tui.processAnswers(answer -> {
                        // Example processing: could save to file, validate, etc.
                        System.out.println("Processing: " + answer.getQuestion() + " -> " + answer.getResponse());
                    });
                    break;

                case 3:
                    System.out.println("Exiting...");
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
