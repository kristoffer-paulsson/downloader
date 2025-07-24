package org.example.downloader.java;

public enum JavaType {
    JDK("jdk"),
    JRE("jre");

    private final String type;

    JavaType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
