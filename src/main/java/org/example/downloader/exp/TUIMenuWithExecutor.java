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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TUIMenuWithExecutor {
    // Enum to represent different menu states
    private enum MenuState {
        MAIN_MENU,
        SETTINGS_MENU,
        TASK_MENU,
        EXIT
    }

    private static final Scanner scanner = new Scanner(System.in);
    private static MenuState currentState = MenuState.MAIN_MENU;
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {
        // Start background task
        startBackgroundTasks();

        // Main TUI loop
        while (isRunning.get()) {
            clearScreen();
            switch (currentState) {
                case MAIN_MENU:
                    displayMainMenu();
                    handleMainMenuInput();
                    break;
                case SETTINGS_MENU:
                    displaySettingsMenu();
                    handleSettingsMenuInput();
                    break;
                case TASK_MENU:
                    displayTaskMenu();
                    handleTaskMenuInput();
                    break;
                case EXIT:
                    shutdown();
                    break;
            }
        }

        // Clean up
        scanner.close();
        executor.shutdown();
    }

    // Clear console screen (works on Unix-like systems; for Windows, use "cls")
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // Display Main Menu
    private static void displayMainMenu() {
        System.out.println("=== Main Menu ===");
        System.out.println("1. Go to Settings");
        System.out.println("2. Go to Task Menu");
        System.out.println("3. Exit");
        System.out.print("Enter choice (1-3): ");
    }

    // Handle Main Menu input
    private static void handleMainMenuInput() {
        String input = scanner.nextLine().trim();
        switch (input) {
            case "1":
                currentState = MenuState.SETTINGS_MENU;
                break;
            case "2":
                currentState = MenuState.TASK_MENU;
                break;
            case "3":
                currentState = MenuState.EXIT;
                break;
            default:
                System.out.println("Invalid input! Press Enter to continue...");
                scanner.nextLine();
        }
    }

    // Display Settings Menu
    private static void displaySettingsMenu() {
        System.out.println("=== Settings Menu ===");
        System.out.println("1. Change Something (Placeholder)");
        System.out.println("2. Back to Main Menu");
        System.out.print("Enter choice (1-2): ");
    }

    // Handle Settings Menu input
    private static void handleSettingsMenuInput() {
        String input = scanner.nextLine().trim();
        switch (input) {
            case "1":
                System.out.println("Settings changed! Press Enter to continue...");
                scanner.nextLine();
                break;
            case "2":
                currentState = MenuState.MAIN_MENU;
                break;
            default:
                System.out.println("Invalid input! Press Enter to continue...");
                scanner.nextLine();
        }
    }

    // Display Task Menu
    private static void displayTaskMenu() {
        System.out.println("=== Task Menu ===");
        System.out.println("1. Submit New Task");
        System.out.println("2. Back to Main Menu");
        System.out.print("Enter choice (1-2): ");
    }

    // Handle Task Menu input
    private static void handleTaskMenuInput() {
        String input = scanner.nextLine().trim();
        switch (input) {
            case "1":
                submitNewTask();
                System.out.println("Task submitted! Press Enter to continue...");
                scanner.nextLine();
                break;
            case "2":
                currentState = MenuState.MAIN_MENU;
                break;
            default:
                System.out.println("Invalid input! Press Enter to continue...");
                scanner.nextLine();
        }
    }

    // Start background tasks using ExecutorService
    private static void startBackgroundTasks() {
        executor.submit(() -> {
            while (isRunning.get()) {
                try {
                    System.out.println("Background task running...");
                    Thread.sleep(5000); // Simulate work every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // Submit a new task to ExecutorService
    private static void submitNewTask() {
        executor.submit(() -> {
            try {
                System.out.println("Executing new task...");
                Thread.sleep(2000); // Simulate task execution
                System.out.println("Task completed!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // Shutdown the application
    private static void shutdown() {
        System.out.println("Shutting down...");
        isRunning.set(false);
    }
}
