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

import org.example.downloader.java.JavaPackage;
import org.example.downloader.java.JavaParser;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.deb.Menu;
import org.example.downloader.java.JavaDownloadEnvironment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.lang.module.ModuleDescriptor.Version;

public class JavaMenu extends Menu {
    public JavaMenu(InversionOfControl ioc) {
        super(ioc, "Java Downloader CLI");
        ioc.register(JavaDownloadEnvironment.class, () -> new JavaDownloadEnvironment("./"));
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup environment", option -> new JavaForm(ioc.resolve(JavaDownloadEnvironment.class), ioc).runForm());
        registerOption("View environment", option -> reviewConfig(ioc.resolve(JavaDownloadEnvironment.class)));
        registerOption("Download Worker", option -> runDownloadWorker());
    }

    private void reviewConfig(JavaDownloadEnvironment jds) {
        System.out.println("\n=== Current config ===");
        jds.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }

    private void runDownloadWorker() {

        JavaParser.filterPackages(ioc.resolve(JavaDownloadEnvironment.class)).forEach((p) -> {
            System.out.println(p.uniqueKey() + ", " + p.getJavaVersion() + ", " + p.getVendorVersion());
        });

        new Thread(() -> {

        }).start();
    }
}
