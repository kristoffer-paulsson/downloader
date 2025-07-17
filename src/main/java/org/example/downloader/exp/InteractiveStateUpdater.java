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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that manages interactive console updates for task states and metadata,
 * integrated with a TUI for user interaction.
 */
public class InteractiveStateUpdater {
    private final TUIRoutines tui;
    private final ExecutorService executor;
    private final MetaData metaData;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isUpdating;

    // Enum for menu states
    private enum MenuState {
        MAIN_MENU,
        TASK_MENU,
        EXIT
    }

    public InteractiveStateUpdater() {
        this.tui = new TUIRoutines();
        this.executor = Executors.newFixedThreadPool(3); // 2 for tasks, 1 for updates
        this.metaData = new MetaData();
        this.isRunning = new AtomicBoolean(true);
        this.isUpdating = new AtomicBoolean(false);
    }

    /**
     * Starts the interactive state updater, including the TUI loop and background updates.
     */
    public void start() {
        // Start background state update thread
        startStateUpdateThread();

        // Main TUI loop
        MenuState currentState = MenuState.MAIN_MENU;
        while (isRunning.get()) {
            tui.clearScreen();
            switch (currentState) {
                case MAIN_MENU:
                    currentState = handleMainMenu();
                    break;
                case TASK_MENU:
                    currentState = handleTaskMenu();
                    break;
                case EXIT:
                    shutdown();
                    break;
            }
        }

        // Clean up
        tui.close();
        executor.shutdown();
    }

    /**
     * Starts a background thread to periodically update the console with task states.
     */
    private void startStateUpdateThread() {
        executor.submit(() -> {
            while (isRunning.get()) {
                if (isUpdating.get()) {
                    // Skip update if user is interacting with the menu
                    try {
                        Thread.sleep(100);
                        continue;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                tui.clearScreen();
                System.out.println("=== Current Task States ===");
                metaData.displayTaskStates();
                System.out.println("\nPress Enter to return to menu...");

                // Wait briefly to allow user to see updates
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * Handles the main menu.
     *
     * @return The next menu state.
     */
    private MenuState handleMainMenu() {
        List<String> options = List.of("View Tasks", "Exit");
        isUpdating.set(false); // Allow user interaction
        tui.displayMenu("Main Menu", options);
        int choice = tui.readMenuChoice(options.size(), message -> tui.showMessageAndWait(message));

        switch (choice) {
            case 1:
                return MenuState.TASK_MENU;
            case 2:
                return MenuState.EXIT;
            default:
                return MenuState.MAIN_MENU;
        }
    }

    /**
     * Handles the task menu.
     *
     * @return The next menu state.
     */
    private MenuState handleTaskMenu() {
        List<String> options = List.of("Submit New Task", "Back to Main Menu");
        isUpdating.set(false); // Allow user interaction
        tui.displayMenu("Task Menu", options);
        int choice = tui.readMenuChoice(options.size(), message -> tui.showMessageAndWait(message));

        switch (choice) {
            case 1:
                submitNewTask();
                tui.showMessageAndWait("Task submitted!");
                return MenuState.TASK_MENU;
            case 2:
                return MenuState.MAIN_MENU;
            default:
                return MenuState.TASK_MENU;
        }
    }

    /**
     * Submits a new task to the ExecutorService and updates metadata.
     */
    private void submitNewTask() {
        String taskId = "Task-" + System.currentTimeMillis();
        metaData.addTask(taskId, "Running");
        executor.submit(() -> {
            try {
                metaData.updateTaskState(taskId, "Running");
                Thread.sleep(5000); // Simulate task execution
                metaData.updateTaskState(taskId, "Completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                metaData.updateTaskState(taskId, "Failed: Interrupted");
            }
        });
    }

    /**
     * Shuts down the application.
     */
    private void shutdown() {
        isRunning.set(false);
        tui.showMessageAndWait("Shutting down...");
    }

    /**
     * Metadata class to track task states.
     */
    private static class MetaData {
        private final Map<String, String> taskStates;

        public MetaData() {
            this.taskStates = new ConcurrentHashMap<>();
        }

        /**
         * Adds a new task with an initial state.
         *
         * @param taskId The task ID.
         * @param state  The initial state.
         */
        public void addTask(String taskId, String state) {
            taskStates.put(taskId, state);
        }

        /**
         * Updates the state of a task.
         *
         * @param taskId The task ID.
         * @param state  The new state.
         */
        public void updateTaskState(String taskId, String state) {
            taskStates.put(taskId, state);
        }

        /**
         * Displays the current state of all tasks.
         */
        public void displayTaskStates() {
            if (taskStates.isEmpty()) {
                System.out.println("No tasks running.");
            } else {
                taskStates.forEach((taskId, state) ->
                        System.out.println(taskId + ": " + state));
            }
        }
    }

    /**
     * Main method to demonstrate the InteractiveStateUpdater.
     */
    public static void main(String[] args) {
        InteractiveStateUpdater updater = new InteractiveStateUpdater();
        updater.start();
    }
}
