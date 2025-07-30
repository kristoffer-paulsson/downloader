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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Inversion of Control (IoC) container for managing application dependencies and central state,
 * excluding TUIRoutines for external management.
 */
public class InversionOfControl {
        // Stores factory methods for types
        private final Map<Class<?>, Supplier<?>> factories = new HashMap<>();
        // Stores singleton instances
        private final Map<Class<?>, Object> singletons = new HashMap<>();
        // Synchronization object for thread safety
        private final Object lock = new Object();

        /**
         * Registers a factory method for a given type.
         *
         * @param type   The class type to register.
         * @param factory The factory method to create an instance of the type.
         * @param <T>    The type of the class.
         */
        public <T> void register(Class<T> type, Supplier<T> factory) {
            synchronized (lock) {
                factories.put(type, factory);
                // Clear any existing singleton instance when re-registering
                singletons.remove(type);
            }
        }

        /**
         * Resolves an instance of the requested type, using the registered factory method
         * to create a singleton instance if it doesn't already exist.
         *
         * @param type The class type to resolve.
         * @param <T>  The type of the class.
         * @return An instance of the requested type.
         * @throws IllegalStateException if no factory is registered for the type.
         */
        @SuppressWarnings("unchecked")
        public <T> T resolve(Class<T> type) {
            synchronized (lock) {
                // Check if singleton instance already exists
                T instance = (T) singletons.get(type);
                if (instance != null) {
                    return instance;
                }

                // Get the factory method
                Supplier<?> factory = factories.get(type);
                if (factory == null) {
                    throw new IllegalStateException("No factory registered for type: " + type.getName());
                }

                // Create instance and store as singleton
                instance = (T) factory.get();
                singletons.put(type, instance);
                return instance;
            }
        }
    }
