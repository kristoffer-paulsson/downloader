package org.example.downloader.java;

import org.example.downloader.util.BasePackage;

public class JavaPackage implements BasePackage {

    private final String filename;
    private final String size;
    private final String sha256Digest;
    //private final String url;


    JavaPackage(String filename, String size, String sha256Digest) {
        this.filename = filename;
        this.size = size;
        this.sha256Digest = sha256Digest;
        //this.url = url;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public String getSha256Digest() {
        return sha256Digest;
    }

    /*public String getUrl() {
        return url;
    }*/
}
