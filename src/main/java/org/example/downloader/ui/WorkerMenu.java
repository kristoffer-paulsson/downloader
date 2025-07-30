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
package org.example.downloader.ui;

import org.example.downloader.*;
import org.example.downloader.deb.Menu;
import org.example.downloader.deb.WorkerTask;
import org.example.downloader.util.InversionOfControl;

import java.io.IOException;

public class WorkerMenu extends Menu {
    public WorkerMenu(InversionOfControl ioc) {
        super(ioc, "Downloader worker");
    }

    @Override
    protected void setupMenu() {
        registerOption("Start download worker", option -> startDownloading());
        registerOption("Stop download worker", option -> stopDownloading());
        registerOption("Pause download worker", option -> pauseDownloading());
        registerOption("Resume download worker", option -> resumeDownloading());
    }

    private void startDownloading() {
        System.out.println("\n=== Starting downloading ===");
        Main main = ioc.resolve(Main.class);

        if(main.getTask() != WorkerTask.DOWNLOAD) {
            // DebianPackageChunkSplitter splitter = ioc.resolve(DebianPackageChunkSplitter.class);
            // main.setExecutor(splitter.jointWorkerIterator(), WorkerTask.DOWNLOAD);

            ioc.register(DebianPackageBlockchain.class, () -> new DebianPackageBlockchain(ioc));
            try {
                System.out.println("Starting download blockchain worker...");
                main.setExecutor(ioc.resolve(DebianPackageBlockchain.class).startBlockchainCSVFile(), WorkerTask.DOWNLOAD);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Download worker started.");
        } else if (main.getExecutor().isRunning()) {
            System.out.println("Download worker is already running.");
        } else {
            System.out.println("Download worker is not running.");
        }
        showMessageAndWait(" ");
    }

    private void stopDownloading() {
        System.out.println("\n=== Stopping downloading ===");
        Main main = ioc.resolve(Main.class);

        if (main.getTask() == WorkerTask.DOWNLOAD) {
            main.getExecutor().shutdown();
            System.out.println("Download worker stopped.");
        } else {
            System.out.println("No download worker is running.");
        }
        showMessageAndWait(" ");
    }

    private void pauseDownloading() {
        System.out.println("\n=== Pausing downloading ===");
        Main main = ioc.resolve(Main.class);

        if (main.getTask() != WorkerTask.DOWNLOAD) {
            System.out.println("No download worker is running.");
        } else if(!main.getExecutor().isPaused()) {
            main.getExecutor().pause();
            System.out.println("Download worker paused.");
        } else {
            System.out.println("Download worker is already paused.");
        }
        showMessageAndWait(" ");
    }

    private void resumeDownloading() {
        System.out.println("\n=== Resuming downloading ===");
        Main main = ioc.resolve(Main.class);

        if (main.getTask() != WorkerTask.DOWNLOAD) {
            System.out.println("No download worker is running.");
        } else if(main.getExecutor().isPaused()) {
            main.getExecutor().resume();
            System.out.println("Download worker resumed.");
        } else {
            System.out.println("Download worker is not paused.");
        }
        showMessageAndWait(" ");
    }
}
