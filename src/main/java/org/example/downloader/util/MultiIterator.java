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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MultiIterator<E extends BasePackage> implements Iterator<E>, AutoCloseable {
    private final Iterator<Iterator<E>> iterators;
    private Iterator<E> currentIterator;

    @SafeVarargs
    public MultiIterator(Iterator<E>... iterators) {
        this.iterators = Arrays.stream(iterators).iterator();
    }

    @Override
    public boolean hasNext() {
        if (currentIterator == null) {
            if (!iterators.hasNext()) {
                return false; // No iterators available
            }
            currentIterator = iterators.next();
        }
        return currentIterator.hasNext();
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements in the multi-iterator");
        }
        return currentIterator.next();
    }

    @Override
    public void close() throws Exception {
        for (Iterator<Iterator<E>> it = iterators; it.hasNext(); ) {
            Iterator<E> iterator = it.next();
            if (iterator instanceof AutoCloseable) {
                ((AutoCloseable) iterator).close();
            }
        }
    }
}
