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

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DebianConcurrentDownloader {
    private final ThreadPoolExecutor executor;
    private final Iterator<String> urlIterator;
    private final String outputDir;
    private final AtomicInteger activeTasks;

    public DebianConcurrentDownloader(Iterator<String> urlIterator, String outputDir, int maxThreads) {
        this.urlIterator = urlIterator;
        this.outputDir = outputDir;
        this.activeTasks = new AtomicInteger(0);
        this.executor = new ThreadPoolExecutor(
                maxThreads, maxThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void startDownloading() {
        // Ensure output directory exists
        new File(outputDir).mkdirs();

        // Start initial batch of downloads
        for (int i = 0; i < Math.min(8, executor.getMaximumPoolSize()) && urlIterator.hasNext(); i++) {
            submitDownloadTask(urlIterator.next());
        }
    }

    private void submitDownloadTask(String urlStr) {
        activeTasks.incrementAndGet();
        executor.submit(() -> {
            try {
                downloadFile(urlStr);
            } finally {
                // Submit next URL if available
                synchronized (urlIterator) {
                    if (urlIterator.hasNext()) {
                        submitDownloadTask(urlIterator.next());
                    }
                }
                // If no more tasks and this was the last active task, shutdown
                if (activeTasks.decrementAndGet() == 0) {
                    executor.shutdown();
                }
            }
        });
    }

    private void downloadFile(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String fileName = urlStr.substring(urlStr.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "downloaded_file_" + System.currentTimeMillis();
            }
            Path outputPath = Paths.get(outputDir, fileName);

            try (InputStream in = url.openStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Downloaded: " + urlStr + " to " + outputPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to download " + urlStr + ": " + e.getMessage());
        }
    }

    public void awaitCompletion() throws InterruptedException {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // Example usage
    public static void main(String[] args) throws InterruptedException {
        // Example list of URLs
        String[] urls = {
                "https://example.com/file1.pdf",
                "https://example.com/file2.jpg",
                // Add more URLs as needed
        };
        Iterator<String> urlIterator = Arrays.asList(urls).iterator();
        String outputDir = "downloads";

        DebianConcurrentDownloader downloader = new DebianConcurrentDownloader(urlIterator, outputDir, 8);
        downloader.startDownloading();
        downloader.awaitCompletion();
        System.out.println("All downloads completed.");
    }
}
