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

import org.example.downloader.GeneralEnvironment;
import org.example.downloader.WorkLogger;
import org.example.downloader.deb.DebianDownloadEnvironment;
import org.example.downloader.deb.DebianMetadataDownloader;
import org.example.downloader.deb.DebianMirrorCache;
import org.example.downloader.util.Action;
import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.PrintHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class DebianMetadataAction extends Action {

    protected GeneralEnvironment ge;
    protected DebianDownloadEnvironment dde;
    protected MyObject executorHolder;
    WorkLogger logger;

    BlockChainHelper.Blockchain chain;

    public DebianMetadataAction(InversionOfControl ioc, String name) {
        super(ioc, name);
    }

    @Override
    protected void setupAction() {
        ge = ioc.resolve(GeneralEnvironment.class);
        dde = getEnvironmentManager();
        executorHolder = new MyObject();
        logger = ioc.resolve(WorkLogger.class);
    }

    protected DebianDownloadEnvironment getEnvironmentManager() {
        return ioc.resolve(DebianDownloadEnvironment.class);
    }

    @Override
    public void runAction() {
        setupAction();

        checkBadMirrors();
        checkMirrors();

        DebianMetadataDownloader metadataDownloader = new DebianMetadataDownloader(ge, dde, logger);

        progressWorker(executorHolder, metadataDownloader, logger, (eh) -> {
            ProgressBar.printProgressMsg(
                    executorHolder.executor.getCurrentTotalBytes(),
                    100 * 1024 * 1024,
                    50,
                    ProgressBar.ANSI_GREEN,
                    "Downloading packages lists " + PrintHelper.formatByteSize(executorHolder.executor.getCurrentTotalBytes())
            );
        });

    }

    private void checkBadMirrors() {
        Path badMirrorsFile = DebianMirrorCache.badMirrorFile(ge);
        if(Files.exists(badMirrorsFile)) {
            if(promptYesNo("The Debian bad mirrors file exists", "Delete", 'd', "Keep", 'k')) {
                try {
                    Files.deleteIfExists(badMirrorsFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void checkMirrors() {
        Path mirrorsFile = DebianMirrorCache.mirrorFile(ge);
        if(Files.exists(mirrorsFile)) {
            if(promptYesNo("Do you wish to renew the mirrors file", "Download", 'd', "Leave", 'l')) {
                System.out.println("Downloading...");
                new DebianMirrorCache(ge).downloadAndCacheMirrors();
                System.out.println("Debian mirrors file " + PrintHelper.coloredMessage("renewed", PrintHelper.ANSI_BLUE) + "!");
            } else {
                System.out.println("Skipping renewal of mirrors file!");
            }
        } else {
            System.out.println("Debian mirrors file is missing, downloading...");
            new DebianMirrorCache(ge).downloadAndCacheMirrors();
        }
        if(!Files.exists(DebianMirrorCache.mirrorFile(ge))) {
            System.out.println(PrintHelper.coloredMessage("Debian mirrors file missing", PrintHelper.ANSI_RED) + ", must be investigated manually");
        } else {
            DebianMirrorCache dmc = new DebianMirrorCache(ge);
            dmc.loadCachedMirrors(false);
            System.out.println("There are " + PrintHelper.coloredMessage(String.valueOf(dmc.mirrorCount()), PrintHelper.ANSI_BLUE) + " Debian mirrors available.");
        }
    }
}
