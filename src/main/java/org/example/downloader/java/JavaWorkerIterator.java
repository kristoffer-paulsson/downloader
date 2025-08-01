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

import org.example.downloader.WorkLogger;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.DownloadHelper;
import org.example.downloader.util.WorkerIterator;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;

public class JavaWorkerIterator extends WorkerIterator<JavaPackage> {

    private final JavaDownloadEnvironment jde;
    private final Iterator<JavaPackage> packageIterator;
    private final WorkLogger logger;
    private final BlockChainHelper.Blockchain chain;

    public JavaWorkerIterator(JavaDownloadEnvironment jde, WorkLogger logger) {
        this.jde = jde;
        this.packageIterator = JavaParser.filterPackages(jde).iterator();
        this.logger = logger;
        this.chain = null;
    }

    public JavaWorkerIterator(
            JavaDownloadEnvironment jde,
            HashMap<String, JavaPackage> packages,
            BlockChainHelper.Blockchain chain,
            WorkLogger logger
    ) {
        this.jde = jde;
        this.packageIterator = packages.values().iterator();
        this.chain = chain;
        this.logger = logger;
    }

    @Override
    protected JavaWorker createWorker() {
        JavaPackage pkg = packageIterator.next();
        try {
            Path downloadPath = Path.of(String.format("%s/%s", jde.getDownloadDir(), pkg.getFilename()));
            return new JavaWorker(pkg, new DownloadHelper.Download(pkg.getRealUrl(), downloadPath), chain, logger);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return packageIterator.hasNext();
    }
}
