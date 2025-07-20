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
// File: src/main/java/org/example/downloader/DebianWorkerExecutor.java
package org.example.downloader;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class DebianWorkerExecutor {
    private final ExecutorService executorService;
    private final List<DebianWorker> activeWorkers;
    private final List<DebianWorker> pausedWorkers;
    private final Set<DebianWorker> allWorkers;
    private final DebianWorkerIterator workerIterator;
    private final Logger logger;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;
    private static final int MAX_CONCURRENT_WORKERS = 8;

    public DebianWorkerExecutor(DebianWorkerIterator workerIterator, DownloadLogger logger) {
        this.logger = logger.getLogger();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_WORKERS);
        this.activeWorkers = Collections.synchronizedList(new ArrayList<>());
        this.pausedWorkers = Collections.synchronizedList(new ArrayList<>());
        this.allWorkers = Collections.synchronizedSet(new HashSet<>());
        this.workerIterator = workerIterator;
        this.isRunning = new AtomicBoolean(false);
        this.isPaused = new AtomicBoolean(false);
    }

    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Executor already running");
            return;
        }
        if (isPaused.get()) {
            logger.info("Executor is paused; call resume() instead");
            return;
        }

        synchronized (pausedWorkers) {
            for (DebianWorker worker : pausedWorkers) {
                if (!worker.isCompleted() && !worker.isDownloading()) {
                    submitWorker(worker);
                }
            }
            pausedWorkers.clear();
        }

        submitNewWorkers();
    }

    public void resume() {
        if (!isRunning.get()) {
            isRunning.set(true);
        }
        if (!isPaused.compareAndSet(true, false)) {
            logger.info("Executor is not paused");
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

        submitNewWorkers();
        logger.info("Executor resumed");
    }

    public void pause() {
        if (!isRunning.get()) {
            logger.info("Executor is not running");
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
            logger.info("Executor paused");
        }
    }

    public void shutdown() {
        if (!isRunning.get()) {
            logger.info("Executor is already shut down");
            return;
        }
        isRunning.set(false);
        isPaused.set(false);

        synchronized (activeWorkers) {
            for (DebianWorker worker : activeWorkers) {
                if (worker.isDownloading()) {
                    worker.pauseDownload();
                    pausedWorkers.add(worker);
                }
            }
            activeWorkers.clear();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            logger.severe("Executor shutdown interrupted: " + e.getMessage());
        }
        logger.info("Executor shut down");
    }

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
        checkCompletion();
    }

    private void submitWorker(DebianWorker worker) {
        if (!worker.isCompleted()) {
            executorService.submit(() -> {
                worker.run();
                synchronized (activeWorkers) {
                    activeWorkers.remove(worker);
                    if (!worker.isPaused()) {
                        pausedWorkers.remove(worker);
                    }
                    submitNewWorkers();
                }
            });
            activeWorkers.add(worker);
        }
    }

    private void checkCompletion() {
        synchronized (activeWorkers) {
            if (activeWorkers.isEmpty() && pausedWorkers.isEmpty() && !workerIterator.hasNext() && isRunning.get()) {
                logger.info("All downloads complete, shutting down executor.");
                shutdown();
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public int getActiveWorkerCount() {
        return activeWorkers.size();
    }

    public int getPausedWorkerCount() {
        return pausedWorkers.size();
    }
}
