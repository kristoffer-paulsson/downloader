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
import org.example.downloader.java.JavaDownloadEnvironment;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.Menu;
import org.example.downloader.wtx.WinetricksDownloadEnvironment;


public class WinetricksMenu extends Menu {

    public WinetricksMenu(InversionOfControl ioc) {
        super(ioc, "Winetricks Downloader CLI");
        ioc.register(WinetricksDownloadEnvironment.class, () -> new WinetricksDownloadEnvironment(ioc.resolve(GeneralEnvironment.class).getCacheDir()));
    }

    @Override
    protected void setupMenu() {
        registerOption("Setup environment", option -> new WinetricksForm(ioc.resolve(WinetricksDownloadEnvironment.class), ioc).runForm());
        registerOption("View environment", option -> reviewConfig(ioc.resolve(WinetricksDownloadEnvironment.class)));
        registerOption("Downloader", option -> new JavaDownloadAction(ioc, "Downloader").runAction());
        registerOption("Blockchain Verifier", option -> new JavaVerifyAction(ioc, "Blockchain Verifier").runAction());
    }

    private void reviewConfig(WinetricksDownloadEnvironment wds) {
        System.out.println("\n=== Current environment ===");
        wds.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        showMessageAndWait(" ");
    }
}
