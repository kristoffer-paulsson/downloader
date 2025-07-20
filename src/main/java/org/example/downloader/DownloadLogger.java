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

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class DownloadLogger {
    private final Logger logger;
    private final Path logFile;
    private final long maxFileSize = 16 * 1024 * 1024; // 5MB

    DownloadLogger(ConfigManager configManager) throws IOException {
        Path logDir = Paths.get(configManager.get(ConfigManager.DIR_CACHE), "logs");
        Files.createDirectories(logDir);
        this.logFile = logDir.resolve("downloader.log");

        this.logger = Logger.getLogger("DebianDownloaderLogger");
        this.logger.setUseParentHandlers(false);

        rotateLogFile(true);
        addFileHandler();
    }

    private void addFileHandler() throws IOException {
        // Remove existing handlers
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        FileHandler fileHandler = new FileHandler(logFile.toString(), true);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
    }

    public void rotateLogFile(boolean force) throws IOException {
        if (Files.exists(logFile) && (Files.size(logFile) > maxFileSize || force)) {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            Path rotatedFile = logFile.getParent().resolve("downloader-" + timestamp + ".log");
            Files.move(logFile, rotatedFile, StandardCopyOption.REPLACE_EXISTING);
            addFileHandler();
        }
    }

    public synchronized void log(String message) {
        logger.log(new LogRecord(Level.WARNING, message));
    }

    public Logger getLogger() {
        return logger;
    }
}
