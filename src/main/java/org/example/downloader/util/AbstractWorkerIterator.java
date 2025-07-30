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

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractWorkerIterator implements Iterator<Worker> {

    protected abstract Worker createWorker();

    @Override
    public abstract boolean hasNext();

    @Override
    public Worker next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more items in the package");
        }
        return createWorker();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported");
    }
}