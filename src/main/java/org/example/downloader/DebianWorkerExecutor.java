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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebianWorkerExecutor {
    private final ExecutorService executorService;
    private final List<DebianWorker> activeWorkers;
    private final List<DebianWorker> pausedWorkers;
    private final Set<DebianWorker> allWorkers;
    private final DebianWorkerIterator workerIterator;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    private static final int MAX_CONCURRENT_WORKERS = 8;

    /**
     * Constructor that initializes the executor with an iterator of DebianWorker instances.
     * @param workerIterator Iterator providing DebianWorker instances
     */
    public DebianWorkerExecutor(DebianWorkerIterator workerIterator) {
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_WORKERS);
        this.activeWorkers = Collections.synchronizedList(new ArrayList<>());
        this.pausedWorkers = Collections.synchronizedList(new ArrayList<>());
        this.allWorkers = Collections.synchronizedSet(new HashSet<>());
        this.workerIterator = workerIterator;
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
    }

    /**
     * Starts the executor, processing workers from the iterator up to the concurrency limit.
     */
    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            System.out.println("Executor already running");
            return;
        }
        if (isPaused.get()) {
            System.out.println("Executor is paused; call resume() instead");
            return;
        }

        // Submit any previously paused workers
        synchronized (pausedWorkers) {
            for (DebianWorker worker : pausedWorkers) {
                if (!worker.isCompleted() && !worker.isDownloading()) {
                    submitWorker(worker);
                }
            }
            pausedWorkers.clear();
        }

        // Submit new workers from the iterator
        submitNewWorkers();
    }

    /**
     * Resumes paused downloads and continues processing new workers.
     */
    public void resume() {
        if (!isRunning.get()) {
            isRunning.set(true);
        }
        if (!isPaused.compareAndSet(true, false)) {
            System.out.println("Executor is not paused");
            return;
        }

        synchronized (pausedWorkers) {
            for (DebianWorker worker : pausedWorkers) {
                if (!worker.isCompleted() && !worker.isDownloading()) {
                    worker.resumeDownload();
                    activeWorkers.add(worker);
                }
            }
            pausedWorkers.clear();
        }

        // Submit new workers from the iterator
        submitNewWorkers();
        System.out.println("Executor resumed");
    }

    /**
     * Pauses all active downloads.
     */
    public void pause() {
        if (!isRunning.get()) {
            System.out.println("Executor is not running");
            return;
        }
        if (isPaused.compareAndSet(false, true)) {
            synchronized (activeWorkers) {
                for (DebianWorker worker : activeWorkers) {
                    if (worker.isDownloading()) {
                        worker.pauseDownload();
                        pausedWorkers.add(worker);
                    }
                }
                activeWorkers.clear();
            }
            System.out.println("Executor paused");
        }
    }

    /**
     * Shuts down the executor gracefully, allowing running downloads to complete.
     */
    public void shutdown() {
        if (!isRunning.get()) {
            System.out.println("Executor is already shut down");
            return;
        }
        isRunning.set(false);
        isPaused.set(false);

        // Pause all active downloads to preserve partial files
        synchronized (activeWorkers) {
            for (DebianWorker worker : activeWorkers) {
                if (worker.isDownloading()) {
                    worker.pauseDownload();
                    pausedWorkers.add(worker);
                }
            }
            activeWorkers.clear();
        }

        // Shut down the executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Executor shut down");
    }

    /**
     * Submits new workers from the iterator up to the concurrency limit.
     */
    private void submitNewWorkers() {
        synchronized (activeWorkers) {
            while (activeWorkers.size() < MAX_CONCURRENT_WORKERS && workerIterator.hasNext() && isRunning.get() && !isPaused.get()) {
                DebianWorker worker = workerIterator.next();
                if (!allWorkers.contains(worker)) {
                    submitWorker(worker);
                    allWorkers.add(worker);
                }
            }
        }

        // Check if all downloads are complete
        checkCompletion();
    }

    /**
     * Submits a single worker to the executor service.
     * @param worker The DebianWorker to submit
     */
    private void submitWorker(DebianWorker worker) {
        if (!worker.isCompleted()) {
            executorService.submit(() -> {
                worker.run();
                synchronized (activeWorkers) {
                    activeWorkers.remove(worker);
                    if (!worker.isPaused()) {
                        pausedWorkers.remove(worker); // Remove from paused if completed
                    }
                    // Submit new workers if available
                    submitNewWorkers();
                }
            });
            activeWorkers.add(worker);
        }
    }

    /**
     * Checks if all downloads are complete and shuts down if no more work remains.
     */
    private void checkCompletion() {
        synchronized (activeWorkers) {
            if (activeWorkers.isEmpty() && pausedWorkers.isEmpty() && !workerIterator.hasNext() && isRunning.get()) {
                shutdown();
            }
        }
    }

    /**
     * Checks if the executor is running.
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Checks if the executor is paused.
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * Gets the number of active workers.
     * @return Number of currently running workers
     */
    public int getActiveWorkerCount() {
        return activeWorkers.size();
    }

    /**
     * Gets the number of paused workers.
     * @return Number of paused workers
     */
    public int getPausedWorkerCount() {
        return pausedWorkers.size();
    }
}
