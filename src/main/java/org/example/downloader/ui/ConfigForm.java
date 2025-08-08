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
import org.example.downloader.util.Form;

import java.io.IOException;
import java.util.List;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public class ConfigForm extends Form {

    public ConfigForm(InversionOfControl ioc) {
        super(ioc, "Configuration Form");
    }

    @Override
    protected void setupForm() {
        GeneralEnvironment ge = ioc.resolve(GeneralEnvironment.class);

        registerQuestion(() -> askQuestion(
                "Enter cache directory",
                ge.get(GeneralEnvironment.DIR_CACHE, "runtime-cache"),
                this::validatePath,
                System.out::println
        ));
    }

    @Override
    protected void processForm() {
        GeneralEnvironment ge = ioc.resolve(GeneralEnvironment.class);
        List<Answer> answers = getAnswers();

        ge.set(GeneralEnvironment.DIR_CACHE, answers.get(0).getResponse());

        try {
            ge.save();
            ge.reload();
            System.out.println("Saved and reloaded new configuration.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
