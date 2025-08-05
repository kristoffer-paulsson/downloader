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
import org.example.downloader.DebianPackageBlockchain;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.Form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class containing reusable TUI routines for displaying menus,
 * handling user input, asking questions, collecting answers, and processing them.
 */
public class ChainForm extends Form {

    public ChainForm(InversionOfControl ioc) {
        super(ioc, "Blockchain chunk verification");
    }

    @Override
    protected void setupForm() {
        ConfigManager configManager = ioc.resolve(ConfigManager.class);

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Verify which chunk",
                createRange(Integer.parseInt(configManager.get(ConfigManager.CHUNKS))),
                configManager.get(ConfigManager.PIECE),
                System.out::println
        ));
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

        int chunkNumber = Integer.parseInt(answers.get(0).getResponse());

        System.out.println("Chunk number: " + answers.get(0).getResponse());

        DebianPackageBlockchain blockchain = new DebianPackageBlockchain(ioc, chunkNumber);

        try {
            System.out.println("Loading blockchain file: " + blockchain.getBlockchainFile());
            blockchain.verifyBlockchainCSVFile();
        } catch (IOException e) {
            System.err.println("Error verifying blockchain download: " + e.getMessage());
        }

        showMessageAndWait(" ");
    }
}
