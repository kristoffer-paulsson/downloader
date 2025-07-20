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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public class DebianWorkerIterator implements Iterator<DebianWorker> {
    private final Iterator<DebianPackage> packages;
    private final InversionOfControl ioc;
    private final DebianMirrorCache mirrorCache;


    DebianWorkerIterator(InversionOfControl ioc, List<DebianPackage> packages) {
        this.ioc = ioc;
        this.mirrorCache = ioc.resolve(DebianMirrorCache.class);
        this.packages = packages.iterator();
    }

    @Override
    public boolean hasNext() {
        return packages.hasNext();
    }

    @Override
    public DebianWorker next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more items in the package");
        }
        return new DebianWorker(packages.next(), ioc);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported");
    }
}