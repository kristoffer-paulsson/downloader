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
import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianDistribution;
import org.example.downloader.deb.Form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
        ConfigManager configManager = ioc.resolve(ConfigManager.class);

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Enter distribution",
                DebianDistribution.toStringList(),
                configManager.get(ConfigManager.DIST),
                System.out::println
        ));

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Enter architecture",
                DebianArchitecture.toStringList(),
                configManager.get(ConfigManager.ARCH),
                System.out::println
        ));
    }

    @Override
    protected void processForm() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        List<Answer> answers = getAnswers();

        configManager.set(ConfigManager.DIST, answers.get(0).getResponse());
        configManager.set(ConfigManager.ARCH, answers.get(1).getResponse());

        try {
            configManager.save();
            configManager.reload();
            System.out.println("Saved and reloaded new configuration.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
