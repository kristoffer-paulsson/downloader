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

import org.example.downloader.deb.WorkerTask;
import org.example.downloader.ui.MainMenu;
import java.io.*;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String DEFAULT_CONFIG = "config.properties";

    private static InversionOfControl ioc = null;

    public static void main(String[] args) throws Exception {
        initializeIoC(args);

        MainMenu menu = new MainMenu(ioc);
        menu.runMenu();

        cleanup();
    }

    private WorkerTask task = WorkerTask.IDLE;

    private DebianWorkerExecutor executor;

    Main() {
        setIdleExecutor();
    }

    public WorkerTask getTask() {
        return task;
    }

    public DebianWorkerExecutor getExecutor() {
        return executor;
    }

    private void setIdleExecutor() {
        this.executor = new DebianWorkerExecutor(new DebianWorkerIterator(ioc, List.of()));
    }

    public void setExecutor(DebianWorkerIterator iterator, WorkerTask task) {
        if(task != WorkerTask.IDLE) {
            executor.shutdown();
        }
        this.task = WorkerTask.DOWNLOAD;

        this.executor = new DebianWorkerExecutor(iterator);
        this.executor.start();
    }

    private static void cleanup() {
        if (ioc != null) {
            ioc.resolve(DebianWorkerExecutor.class).shutdown();
            ioc.resolve(DownloadLogger.class).rotateLogFile(true);
        }
    }

    private static void initializeIoC(String[] args) {
        ioc = new InversionOfControl();

        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;
        ioc.register(ConfigManager.class, () -> {
            try {
                return new ConfigManager(configPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ioc.register(DownloadLogger.class, () -> {
            try {
                return new DownloadLogger(ioc.resolve(ConfigManager.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ioc.register(Main.class, Main::new);

        ioc.register(DebianPackagesListCache.class, () -> new DebianPackagesListCache(ioc.resolve(ConfigManager.class)));

        ioc.register(DebianPackageChunkSplitter.class, () -> new DebianPackageChunkSplitter(ioc));

        ioc.register(DebianMirrorCache.class, () -> new DebianMirrorCache(ioc.resolve(ConfigManager.class)));

        ioc.register(Scanner.class, () -> new Scanner(System.in));

    }
}