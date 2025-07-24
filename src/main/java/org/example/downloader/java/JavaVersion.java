package org.example.downloader.java;

public enum JavaVersion {
    JAVA_8("1.8"),
    JAVA_11("11"),
    JAVA_17("17"),
    JAVA_21("21");


    private final String version;

    JavaVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
