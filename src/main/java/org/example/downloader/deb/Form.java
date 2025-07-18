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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public abstract class Form {
    protected final InversionOfControl ioc;

    private final String name;
    private final Scanner scanner;

    private final List<BooleanSupplier> questions;
    private final List<Answer> answers; // Stores collected answers

    public Form(InversionOfControl ioc, String name) {
        this.ioc = ioc;
        this.scanner = ioc.resolve(Scanner.class);
        this.questions = new ArrayList<>();
        this.answers = new ArrayList<>();

        this.name = name;
        setupForm();
    }

    protected abstract void setupForm();


    protected void registerQuestion(BooleanSupplier question) {
        questions.add(question);
    }

    public void runForm() {
        questions.forEach(q -> {
            while(!q.getAsBoolean());
        });

        processForm();
        close();
    }

    protected abstract void processForm();

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


    public void displayMenu2(String title, List<String> options) {
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
    public int readMenuChoice(int maxOptions, int standard, Consumer<String> onInvalid) {
        String input = scanner.nextLine().trim();
        int choice = -1;

        if(input.trim().isEmpty() && standard >= 1 && standard <= maxOptions) {
            return standard;
        } else {
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                onInvalid.accept("Invalid input! Please enter a number.");
                return -1;
            }
            if (choice >= 1 && choice <= maxOptions) {
                return choice;
            } else {
                onInvalid.accept("Choice must be between 1 and " + maxOptions + "!");
                return -1;
            }
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
    public boolean askQuestion(String question, String standard, Predicate<String> validator, Consumer<String> onInvalid) {
        clearScreen();

        boolean standardOption = standard != null && !standard.isEmpty();

        System.out.println("=== Question ===");
        if(standardOption)
            System.out.print(question + " [" + standard + "]: ");
        else
                System.out.print(question + ": ");
        String answer = scanner.nextLine().trim();

        if(answer.isEmpty() && standardOption) {
            answer = standard;
            System.out.println("Default option [" + standard + "] chosen.");
        }

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
    public boolean askMultipleChoiceQuestion(String question, List<String> options, String standard, Consumer<String> onInvalid) {
        clearScreen();
        int standardOption = -1;
        System.out.println("=== " + question + " ===");
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            if(option.equals(standard)) {
                standardOption = i+1;
                System.out.println((i + 1) + ". " + option + " [default]");
            } else
                System.out.println((i + 1) + ". " + option);
        }
        System.out.print("Select an option (1-" + options.size() + "): ");

        int choice = readMenuChoice(options.size(), standardOption, onInvalid);

        if(standardOption != -1) {
            choice = standardOption;
            System.out.println("Default option [" + standard + "] chosen.");
        }

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

    }

    /**
     * Example usage of TUIRoutines with question-asking and answer-processing.
     */
    public void runMenu2() {
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
            displayMenu2("Main Menu", mainMenuOptions);
            int choice = readMenuChoice(mainMenuOptions.size(), -1,  message -> showMessageAndWait(message));

            switch (choice) {
                case 1:
                    // Ask a free-text question with validation (e.g., non-empty)
                    askQuestion("What is your name?", null,
                            s -> !s.isEmpty(),
                            message -> showMessageAndWait(message));

                    // Ask a numeric question with validation (e.g., positive integer)
                    askQuestion("How old are you?", null,
                            s -> {
                                try {
                                    return Integer.parseInt(s) > 0;
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            },
                            message -> showMessageAndWait(message));

                    // Ask a multiple-choice question
                    askMultipleChoiceQuestion("What is your favorite color?",
                            colorOptions,
                            null,
                            message -> showMessageAndWait(message));
                    break;

                case 2:
                    // Process collected answers
                    processAnswers(answer -> {
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
        close();
    }
}
