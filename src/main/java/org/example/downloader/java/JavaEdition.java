package org.example.downloader.java;

public enum JavaEdition {
    ORACLE("oracle"),
    CORRETO("corretto"),
    ZULU("zulu"),
    TEMURIN("temurin");

    private final String edition;

    JavaEdition(String edition) {
        this.edition = edition;
    }

    public String getEdition() {
        return edition;
    }
}
