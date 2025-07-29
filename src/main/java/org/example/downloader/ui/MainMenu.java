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

public class MainMenu extends Menu {
    public MainMenu(InversionOfControl ioc) {
        super(ioc, "Debian Downloader CLI");
    }

    @Override
    protected void setupMenu() {
        registerOption("Configuration", option -> new ConfigMenu(ioc).runMenu());
        registerOption("Mirror websites", option -> new MirrorMenu(ioc).runMenu());
        registerOption("Package lists", option -> new PackageMenu(ioc).runMenu());
        registerOption("Workers", option -> new WorkerMenu(ioc).runMenu());
        registerOption("Environment", option -> new JavaMenu(ioc).runMenu());
    }
}
