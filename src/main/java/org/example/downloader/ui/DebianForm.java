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

import org.example.downloader.deb.DebianArchitecture;
import org.example.downloader.deb.DebianDistribution;
import org.example.downloader.deb.DebianDownloadEnvironment;
import org.example.downloader.java.*;
import org.example.downloader.util.Form;
import org.example.downloader.util.InversionOfControl;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A Debian-specific form implementation that extends the generic Form class.
 * It is used to collect information for the DebianDownloadEnvironment.
 */
public class DebianForm extends Form {

    private final DebianDownloadEnvironment dde;

    public DebianForm(DebianDownloadEnvironment dde, InversionOfControl ioc) {
        super(ioc, "Debian Download Environment");
        this.dde = dde;
        try {
            dde.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setupForm() {

        registerQuestion(() -> askQuestion(
                "Enter Debian download environment path",
                dde.getDownloadDir("package-cache").toString(),
                this::validatePath,
                System.out::println
        ));

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Enter distribution",
                DebianDistribution.toStringList(),
                dde.getDistribution().getDist(),
                System.out::println
        ));

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Enter architecture",
                DebianArchitecture.toStringList(),
                dde.getArchitecture().getArch(),
                System.out::println
        ));

        registerQuestion(() -> askQuestion(
                "Number of chunk partitions",
                Integer.toString(dde.getChunks()),
                this::validateInt,
                System.out::println
        ));

        registerQuestion(() -> askMultipleChoiceQuestion(
                "Download which partition",
                createRange(Integer.parseInt(String.valueOf(getAnswers().get(3).getResponse()))),
                Integer.toString(dde.getPiece()),
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
        List<Answer> answers = getAnswers();

        dde.setDownloadDir(Paths.get(answers.get(0).getResponse()));
        dde.setDistribution(DebianDistribution.fromString(answers.get(1).getResponse()));
        dde.setArchitecture(DebianArchitecture.fromString(answers.get(2).getResponse()));
        dde.setChunks(Integer.parseInt(answers.get(3).getResponse()));
        dde.setPiece(Integer.parseInt(answers.get(4).getResponse()));

        try {
            dde.save();
            dde.reload();
            System.out.println("Saved and reloaded the environment.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
