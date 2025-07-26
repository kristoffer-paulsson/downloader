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

public class MultiIterator<E extends BasePackage> implements Iterator<E>, AutoCloseable {
    private final Iterator<E>[] iterators;
    private int currentIndex = 0;

    @SafeVarargs
    public MultiIterator(Iterator<E>... iterators) {
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        while (currentIndex < iterators.length) {
            if (iterators[currentIndex].hasNext()) {
                return true;
            }
            currentIndex++;
        }
        return false;
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements in the multi-iterator");
        }
        return iterators[currentIndex].next();
    }

    @Override
    public void close() throws Exception {
        for (Iterator<E> iterator : iterators) {
            if (iterator instanceof AutoCloseable) {
                ((AutoCloseable) iterator).close();
            }
        }
    }
}
