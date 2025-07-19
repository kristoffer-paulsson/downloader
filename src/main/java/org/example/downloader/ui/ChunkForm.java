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
import org.example.downloader.deb.DebianComponent;
import org.example.downloader.deb.DebianDistribution;
import org.example.downloader.deb.Form;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public class ChunkForm extends Form {

    public ChunkForm(InversionOfControl ioc) {
        super(ioc, "Chunk Configuration Form");
    }

    @Override
    protected void setupForm() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);

        registerQuestion(() -> askQuestion(
                "Number of chunk partitions",
                configManager.get(ConfigManager.CHUNKS),
                this::validateInt,
                System.out::println
        ));

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Download which partition",
                createRange(Integer.parseInt(String.valueOf(getAnswers().get(0).getResponse()))),
                configManager.get(ConfigManager.PIECE),
                System.out::println
        ));
    }

    private boolean validateInt(String p) {
        try {
            int num = Integer.parseInt(p);
            return num > 0 && num <= 16;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> createRange(int stopAt) {
        List<String> range = new ArrayList<>();
        int index = 1;
        while(index <= stopAt) {
            range.add(String.format("%s", index++));
        }
        return range;
    }

    @Override
    protected void processForm() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);
        List<Answer> answers = getAnswers();

        configManager.set(ConfigManager.CHUNKS, answers.get(0).getResponse());
        System.out.println(answers.get(1).getResponse());
        configManager.set(ConfigManager.PIECE, answers.get(1).getResponse());

        try {
            configManager.save();
            configManager.reload();
            System.out.println("Saved and reloaded new configuration.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
