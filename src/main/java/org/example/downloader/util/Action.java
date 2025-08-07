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

import org.example.downloader.WorkLogger;

import java.util.Scanner;
import java.util.function.Consumer;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public abstract class Action {
    protected final InversionOfControl ioc;

    private final String name;
    private final Scanner scanner;

    public Action(InversionOfControl ioc, String name) {
        this.ioc = ioc;
        this.scanner = ioc.resolve(Scanner.class);

        this.name = name;
    }

    protected abstract void setupAction();


    public abstract void runAction();

    public <E extends AbstractWorker> void progressWorker(
            MyObject executorHolder,
            AbstractWorkerIterator<E> workerIterator,
            WorkLogger logger,
            Consumer<MyObject> updater
    ) {
        executorHolder.executor = new WorkerExecutor(workerIterator, logger);
        executorHolder.indicator = new Thread(() -> {

            executorHolder.executor.start();
            while (executorHolder.executor.isRunning()) {
                try {
                    Thread.sleep(10);
                    updater.accept(executorHolder);
                    /*ProgressBar.printProgressMsg(
                            executorHolder.executor.getCurrentTotalBytes(),
                            totalSize.get() - downloadedSize.get(),
                            50,
                            ProgressBar.ANSI_GREEN,
                            "Downloading " + PrintHelper.formatByteSize(executorHolder.executor.getCurrentTotalBytes())
                    );*/
                } catch (InterruptedException e) {
                    //
                }
            }
            executorHolder.executor.shutdown();
        });

        try {
            executorHolder.indicator.start();
            executorHolder.indicator.join();
            System.out.println();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class MyObject {
        WorkerExecutor executor;
        Thread indicator;
    }

    /**
     * Clears the console screen. Uses ANSI escape codes for Unix-like systems.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
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
        if(message != null && !message.isEmpty()) System.out.println(message);
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Closes the scanner to free resources.
     */
    public void close() {
    }
}
