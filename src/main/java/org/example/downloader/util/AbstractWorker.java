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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public abstract class AbstractWorker implements Runnable {

    protected final DownloadLogger workLogger;
    protected final Logger logger;
    protected final AtomicBoolean isRunning;

    public AbstractWorker(DownloadLogger workLogger) {
        this.workLogger = workLogger;
        this.logger = workLogger.getLogger();
        this.isRunning = new AtomicBoolean(false);
    }

    @Override
    public abstract void run(); /*{
        if (!isRunning.compareAndSet(false, true)) {
            logger.warning("Download already in progress for " + downloadTask);
            return;
        }

        try {

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            isRunning.set(false);
        }
    }*/

    protected abstract void stopProcessImpl();

    final public void stopProcessing() {
        stopProcessImpl();
        isRunning.set(false);
    }

    public abstract float getSpeed();
    public abstract float getTime();
    public abstract long getCurrentProcessSize();

    public boolean isRunning() { return isRunning.get() && !isCompleted(); }
    public abstract boolean isCompleted();
}