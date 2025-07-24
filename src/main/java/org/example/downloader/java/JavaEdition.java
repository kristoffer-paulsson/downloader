package org.example.downloader.java;

public enum JavaEdition {
    ORACLE("oracle"),
    // https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk17-0-13-later-archive-downloads.html
    // https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

    CORRETO("corretto"),
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

    JavaEdition(String edition) {
        this.edition = edition;
    }

    public String getEdition() {
        return edition;
    }
}
