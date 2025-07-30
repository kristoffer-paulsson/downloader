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
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianComponent;
import org.example.downloader.deb.DebianDistribution;
import org.example.downloader.deb.Form;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Enter component",
                DebianComponent.toStringList(),
                configManager.get(ConfigManager.COMP),
                System.out::println
        ));

        registerQuestion(() -> askQuestion(
                "Enter cache directory",
                configManager.get(ConfigManager.DIR_CACHE, "runtime-cache"),
                this::validatePath,
                System.out::println
        ));

        registerQuestion(() -> askQuestion(
                "Enter package directory",
                configManager.get(ConfigManager.DIR_PKG, "package-cache"),
                this::validatePath,
                System.out::println
        ));
    }

    @Override
    protected void processForm() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        List<Answer> answers = getAnswers();

        configManager.set(ConfigManager.DIST, answers.get(0).getResponse());
        configManager.set(ConfigManager.ARCH, answers.get(1).getResponse());
        configManager.set(ConfigManager.COMP, answers.get(2).getResponse());
        configManager.set(ConfigManager.DIR_CACHE, answers.get(3).getResponse());
        configManager.set(ConfigManager.DIR_PKG, answers.get(4).getResponse());

        configManager.set(ConfigManager.CHUNKS, configManager.get(ConfigManager.CHUNKS, "1"));
        configManager.set(ConfigManager.PIECE, configManager.get(ConfigManager.PIECE, "1"));

        try {
            configManager.save();
            configManager.reload();
            System.out.println("Saved and reloaded new configuration.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
