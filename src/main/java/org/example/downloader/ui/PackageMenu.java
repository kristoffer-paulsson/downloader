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

import org.example.downloader.ConfigManager;
import org.example.downloader.InversionOfControl;
import org.example.downloader.deb.Menu;

public class PackageMenu extends Menu {
    public PackageMenu(InversionOfControl ioc) {
        super(ioc, "Debian Downloader CLI");
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup config", option -> new ConfigForm(ioc).runForm());
        registerOption("Review config", option -> reviewConfig(ioc.resolve(ConfigManager.class)));
        registerOption("Download packages list", option -> System.out.println("Doing " + option.title));
    }

    private void reviewConfig(ConfigManager configManager) {
        System.out.println("\n=== Current config ===");
        configManager.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }
}
