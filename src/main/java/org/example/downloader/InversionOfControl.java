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
package org.example.downloader;

import org.example.downloader.exp.InteractiveStateUpdater;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Inversion of Control (IoC) container for managing application dependencies and central state,
 * excluding TUIRoutines for external management.
 */
public class InversionOfControl {
    // Central application state
    public static class ApplicationState {
        private boolean isRunning;
        private Map<String, Object> customState;

        public ApplicationState() {
            this.isRunning = true;
            this.customState = new HashMap<>();
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            this.isRunning = running;
        }

        public void setCustomState(String key, Object value) {
            customState.put(key, value);
        }

        public Object getCustomState(String key) {
            return customState.get(key);
        }
    }

    private final Map<Class<?>, Object> singletons;
    private final Map<Class<?>, Supplier<?>> factories;
    private final ApplicationState state;
    private final ExecutorService executor;

    public InversionOfControl() {
        this.singletons = new HashMap<>();
        this.factories = new HashMap<>();
        this.state = new ApplicationState();
        this.executor = Executors.newFixedThreadPool(3); // 2 for tasks, 1 for updates
        initializeDefaultDependencies();
    }

    /**
     * Initializes default dependencies (excludes TUIRoutines).
     */
    private void initializeDefaultDependencies() {
        registerSingleton(ApplicationState.class, state);
        registerSingleton(ExecutorService.class, executor);
        registerFactory(InteractiveStateUpdater.class, () -> {
            // TUIRoutines must be provided externally
            throw new IllegalStateException("InteractiveStateUpdater requires external TUIRoutines");
        });
    }

    /**
     * Registers a singleton instance for a given type.
     *
     * @param type    The class type.
     * @param instance The singleton instance.
     */
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }

    /**
     * Registers a factory for creating instances of a given type.
     *
     * @param type   The class type.
     * @param factory The supplier to create instances.
     */
    public <T> void registerFactory(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }

    /**
     * Resolves an instance of the specified type.
     *
     * @param type The class type.
     * @return The instance (singleton or new instance from factory).
     * @throws IllegalArgumentException If the type is not registered.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        Object instance = singletons.get(type);
        if (instance != null) {
            return (T) instance;
        }
        Supplier<?> factory = factories.get(type);
        if (factory != null) {
            return (T) factory.get();
        }
        throw new IllegalArgumentException("No binding found for type: " + type.getName());
    }

    /**
     * Gets the central application state.
     *
     * @return The application state.
     */
    public ApplicationState getState() {
        return state;
    }

    /**
     * Shuts down the IoC container and its resources.
     */
    public void shutdown() {
        state.setRunning(false);
        executor.shutdown();
        // TUIRoutines cleanup must be handled externally
    }

    /**
     * Example usage of ApplicationIoC with external TUIRoutines.
     */
    public static void main(String[] args) {
        InversionOfControl ioc = new InversionOfControl();
        // Create TUIRoutines externally with ApplicationState
        // Register InteractiveStateUpdater with external TUIRoutines
        ioc.registerFactory(InteractiveStateUpdater.class, () -> new InteractiveStateUpdater());

        InteractiveStateUpdater updater = ioc.resolve(InteractiveStateUpdater.class);
        updater.start();
        ioc.shutdown();
    }
}
