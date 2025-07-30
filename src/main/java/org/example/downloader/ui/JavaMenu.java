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

import org.example.downloader.InversionOfControl;
import org.example.downloader.deb.Menu;
import org.example.downloader.java.JavaDownloadEnvironment;

public class JavaMenu extends Menu {
    public JavaMenu(InversionOfControl ioc) {
        super(ioc, "Java Downloader CLI");
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup environment", option -> new JavaForm(new JavaDownloadEnvironment("./"), ioc).runForm());
        registerOption("View environment", option ->reviewConfig(new JavaDownloadEnvironment("./")));
        registerOption("Download Worker", option -> reviewConfig(ioc.resolve(JavaDownloadEnvironment.class)));
    }

    private void reviewConfig(JavaDownloadEnvironment jds) {
        System.out.println("\n=== Current config ===");
        jds.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }
}
