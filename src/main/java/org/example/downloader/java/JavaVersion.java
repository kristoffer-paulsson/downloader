package org.example.downloader.java;

public enum JavaVersion {
    JAVA_8("1.8.0_382", "https://download.oracle.com/java/8/archive/"),
    JAVA_11("11.0.20", "https://download.oracle.com/java/11/archive/"),
    JAVA_17("17.0.9", "https://download.oracle.com/java/17/archive/"),
    JAVA_21("21.0.7", "https://download.oracle.com/java/21/archive/");


    private final String version;
    private final String baseUrl;

    JavaVersion(String version, String baseUrl) {
        this.version = version;
        this.baseUrl = baseUrl;
    }

    public String getVersion() {
        return version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

}
