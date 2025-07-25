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
package org.example.downloader.java;

public enum JavaFlavor {
    ORACLE("oracle"),
    // https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk17-0-13-later-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

    CORRETTO("corretto"),
    // https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html
    // https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html
    // https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html
    // https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html

    ZULU("zulu"),
    // https://cdn.azul.com/zulu/bin/

    TEMURIN("temurin");
    // https://github.com/adoptium/temurin8-binaries/releases
    // https://github.com/adoptium/temurin11-binaries/releases
    // https://github.com/adoptium/temurin17-binaries/releases
    // https://github.com/adoptium/temurin21-binaries/releases

    private final String edition;

    JavaFlavor(String edition) {
        this.edition = edition;
    }

    public String getEdition() {
        return edition;
    }
}
