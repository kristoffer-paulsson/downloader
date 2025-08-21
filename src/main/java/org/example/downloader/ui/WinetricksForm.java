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

import org.example.downloader.util.Form;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.wtx.WinetricksCategory;
import org.example.downloader.wtx.WinetricksDownloadEnvironment;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * A Java-specific form implementation that extends the generic Form class.
 * It is used to collect information for the JavaDownloadEnvironment.
 */
public class WinetricksForm extends Form {

    private WinetricksDownloadEnvironment wde;

    public WinetricksForm(WinetricksDownloadEnvironment wde, InversionOfControl ioc) {
        super(ioc, "Winetricks Download Environment");
        this.wde = wde;
        try {
            wde.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setupForm() {

        registerQuestion(() -> askQuestion(
                "Enter Winetricks download environment path",
                wde.getDownloadDir("winetricks-cache").toString(),
                this::validatePath,
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which categories:",
                WinetricksCategory.toStringList(),
                wde.get(WinetricksDownloadEnvironment.EnvironmentKey.CATEGORY.getKey(), WinetricksCategory.UNKNOWN.getCategory()),
                System.out::println
        ));
    }

    @Override
    protected void processForm() {
        List<Answer> answers = getAnswers();

        wde.setDownloadDir(Paths.get(answers.get(0).getResponse()));
        wde.set(WinetricksDownloadEnvironment.EnvironmentKey.CATEGORY.getKey(), answers.get(1).getResponse());

        try {
            wde.save();
            wde.reload();
            System.out.println("Saved and reloaded the environment.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
