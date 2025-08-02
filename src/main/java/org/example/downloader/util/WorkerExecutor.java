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

import org.example.downloader.DownloadLogger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class WorkerExecutor {
    private final ExecutorService executorService;
    private final List<AbstractWorker> activeWorkers;
    private final AbstractWorkerIterator<?> workerIterator;
    private final Logger logger;
    private final AtomicBoolean isRunning;
    private final AtomicLong totalBytesCompleted = new AtomicLong();
    private static final int MAX_CONCURRENT_WORKERS = 8;

    public WorkerExecutor(AbstractWorkerIterator<?> workerIterator, DownloadLogger logger) {
        this.logger = logger.getLogger();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_WORKERS);
        this.activeWorkers = Collections.synchronizedList(new ArrayList<>());
        this.workerIterator = workerIterator;
        this.isRunning = new AtomicBoolean(false);
    }

    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Executor already running");
            return;
        }
        submitNewWorkers();
    }

    public void shutdown() {
        if (!isRunning.get()) {
            logger.info("Executor is already shut down");
            return;
        }
        isRunning.set(false);

        synchronized (activeWorkers) {
            for (AbstractWorker worker : activeWorkers) {
                if (worker.isRunning()) {
                    worker.stopProcessing();
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
            while (activeWorkers.size() < MAX_CONCURRENT_WORKERS && workerIterator.hasNext() && isRunning.get()) {
                submitWorker(workerIterator.next());
            }
        }
        checkCompletion();
    }

    private void submitWorker(AbstractWorker worker) {
        if (!worker.isCompleted()) {
            executorService.submit(() -> {
                worker.run();
                synchronized (activeWorkers) {
                    activeWorkers.remove(worker);
                    totalBytesCompleted.getAndAdd(worker.getCurrentProcessSize());

                    submitNewWorkers();
                }
            });
            activeWorkers.add(worker);
        }
    }

    private void checkCompletion() {
        synchronized (activeWorkers) {
            if (activeWorkers.isEmpty() && !workerIterator.hasNext() && isRunning.get()) {
                logger.info("All downloads complete, shutting down executor.");
                shutdown();
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public int getActiveWorkerCount() {
        return activeWorkers.size();
    }

    public float getSpeed() {
        AtomicReference<Float> speed = new AtomicReference<>((float) 0);
        synchronized (activeWorkers) {
            activeWorkers.forEach((w) -> speed.updateAndGet(v -> (v + w.getSpeed())));
        }
        return speed.get();
    }

    public long getCurrentTotalBytes() {
        AtomicLong processedBytes = new AtomicLong();
        processedBytes.getAndAdd(totalBytesCompleted.get());

        synchronized (activeWorkers) {
            activeWorkers.forEach((w) -> processedBytes.addAndGet(w.getCurrentProcessSize()));
        }

        return processedBytes.get();
    }
}
