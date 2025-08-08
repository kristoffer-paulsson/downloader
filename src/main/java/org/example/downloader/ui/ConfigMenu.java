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
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.Menu;

public class ConfigMenu extends Menu {
    public ConfigMenu(InversionOfControl ioc) {
        super(ioc, "Configuration menu");
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup config", option -> new ConfigForm(ioc).runForm());
        registerOption("Review config", option -> reviewConfig(ioc.resolve(GeneralEnvironment.class)));
    }

    private void reviewConfig(GeneralEnvironment ge) {
        System.out.println("\n=== General environment ===");
        ge.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }
}
