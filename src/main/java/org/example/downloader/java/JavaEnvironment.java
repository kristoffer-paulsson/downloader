package org.example.downloader.java;

public enum JavaEnvironment {
    LINUX("linux"),
    WINDOWS("windows"),
    MACOS("macos"),
    SOLARIS("solaris");

    private final String os;

    JavaEnvironment(String os) {
        this.os = os;
    }

    public String getOs() {
        return os;
    }
}
