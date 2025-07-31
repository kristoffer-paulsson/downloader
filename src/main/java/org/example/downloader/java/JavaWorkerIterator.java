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
package org.example.downloader.java;

import org.example.downloader.DownloadLogger;
import org.example.downloader.util.AbstractWorkerIterator;
import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.Worker;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Iterator;

public class JavaWorkerIterator extends AbstractWorkerIterator<JavaPackage> {

    private final JavaDownloadEnvironment jde;
    private final Iterator<JavaPackage> packageIterator;
    private final DownloadLogger logger;

    JavaWorkerIterator(JavaDownloadEnvironment jde, DownloadLogger logger) {
        this.jde = jde;
        this.packageIterator = JavaParser.filterPackages(jde).iterator();
        this.logger = logger;
    }

    @Override
    protected Worker<JavaPackage> createWorker() {
        JavaPackage pkg = packageIterator.next();
        try {
            Path downloadPath = Path.of(String.format("%s/%s", jde.getDownloadDir(), pkg.getFilename()));
            return new Worker<>(pkg, new DownloadHelper.Download(pkg.getRealUrl(), downloadPath), logger);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return packageIterator.hasNext();
    }
}
