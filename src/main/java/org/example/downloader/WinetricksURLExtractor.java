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
package org.example.downloader;

import org.example.downloader.util.Sha256Helper;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.*;

/**
 * # Usage: w_download_to (packagename|path to download file) url [shasum [filename [cookie jar]]]
 * w_download_to()
 * # Usage: w_download url [shasum [filename [cookie jar]]]
 * w_download()
 * */
public class WinetricksURLExtractor {
    private static final String WINETRICKS_URL = "https://raw.githubusercontent.com/Winetricks/winetricks/master/src/winetricks";
    private static final String CACHE_DIR = "cache-winetricks";

    private static final String WINRAR_URL = "https://www.win-rar.com/fileadmin/winrar-versions";
    private static final String WINRAR_EXE = "winrar-%s-%s%s.exe"; // ARCH, VERSION, LANG
    private static final String WINRAR_VERSION = "713";
    private static final String[] WINRAR_ARCH = { "x32", "x64" };
    private static final String[] WINRAR_LANG = { "", "dk", "d", "pl", "pt", "ru", "uk", "sc", "tc", "" };

    private static final List<String> WINRAR_NAMES = new ArrayList<>();
    private static Iterator<String> WINRAR_ITER;

    public static void buldWinrarNames() {
        for (String lang: WINRAR_LANG) {
            for (String arch: WINRAR_ARCH) {
                WINRAR_NAMES.add(String.format(WINRAR_EXE, arch, WINRAR_VERSION, lang));
            }
        }
        WINRAR_ITER = WINRAR_NAMES.iterator();
    }

    private static final String DROID_URL = "https://github.com/android/platform_frameworks_base/blob/feef9887e8f8eb6f64fc1b4552c02efb5755cdc1/data/fonts/";


    public static void main(String[] args) throws IOException, URISyntaxException {
        String winetricksFile = downloadWinetricksScript();

        buldWinrarNames();

        extractFromWinetricks(winetricksFile);
    }

    private static void extractFromWinetricks(String winetricksFile) throws IOException, URISyntaxException {
        try (BufferedReader reader = new BufferedReader(new FileReader(winetricksFile))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                if(line.contains("do_droid ")) {
                    line = line.trim();
                    lineCount++;

                    List<String> fields = List.of(line.split(" "));
                    doDroid(fields);
                } else if(line.contains("w_download")) {
                    line = line.trim();
                    lineCount++;

                    if(line.contains("_W_winrar_url")){
                        line = line.replace("${_W_winrar_url}", WINRAR_URL);
                        line = line.replace("${_W_winrar_exe}", WINRAR_ITER.next());
                    }

                    List<String> fields = List.of(line.split(" "));

                    if(line.startsWith("w_download_manual")) {
                        if(line.contains("W_PACKAGE")) {
                            System.out.println(line);
                        } else {
                            wDownloadManualHandler(fields);
                        }
                    } else if(line.startsWith("w_download_to")) {
                        if(line.contains("W_PACKAGE") || line.contains("_W_tmpdir")) {
                            System.out.println(line);
                        } else {
                            wDownloadToHandler(fields);
                        }
                    } else if(line.startsWith("w_download")) {
                        if(line.contains("_W_droid_url")) {
                            // Implemented
                            //System.out.println(line);
                        } else {
                            wDownloadHandler(fields);
                        }
                    } else {
                        lineCount--;
                        //System.out.println(line);
                    }
                }
            }
            System.out.println("Total number of lines: " + lineCount);
        }
    }

    private static URL extractURL(String value) {
        if(value.startsWith("\"")) {
            value = value.substring(1, value.length()-1);
        }
        try {
            return new URI(value).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String extractSha256(String value) {
        if(!Sha256Helper.isValid64CharHex(value)) {
            //return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            throw new IllegalArgumentException("Value not a 64 letter hex sha256");
        }
        return value;
    }

    private static String extractFilename(String value, URL url) {
        if(value.isEmpty()) {
            return Path.of(url.getFile()).getFileName().toString();
        } else {
            return value;
        }
    }

    private static String extractValue(List<String>fields , int index) {
        if(fields.size() > index) {
            return fields.get(index);
        } else {
            return "";
        }
    }

    /**
     * # Usage: w_download url [shasum [filename [cookie jar]]]
     * */
    private static void wDownloadHandler(List<String> fields) {
        if(fields.size() < 2) {
            System.out.println(String.join(" ", fields));
            return;
        }
        URL url = extractURL(fields.get(1));
        String sha256Digest = extractSha256(extractValue(fields,2));
        String filename = extractFilename(extractValue(fields,3), url);
        System.out.println(String.format("%s, %s, %s", filename, sha256Digest, url));
    }

    /**
     * # Usage: w_download_to (packagename|path to download file) url [shasum [filename [cookie jar]]]
     * */
    private static void wDownloadToHandler(List<String> fields) {
        if(fields.size() < 2) {
            System.out.println(String.join(" ", fields));
            return;
        }
        int fieldIdx = 1;
        URL url;
        String pkgName = "";
        try {
            url = extractURL(fields.get(fieldIdx));
            fieldIdx++;
        } catch (IllegalArgumentException e) {
            pkgName = fields.get(fieldIdx);
            fieldIdx++;
            url = extractURL(fields.get(fieldIdx));
            fieldIdx++;
        }
        String sha256Digest = extractSha256(extractValue(fields,fieldIdx));
        fieldIdx++;
        String filename = extractFilename(extractValue(fields, fieldIdx), url);
        System.out.println(String.format("%s, %s, %s, %s", filename, sha256Digest, url, pkgName));
    }

    private static void wDownloadManualHandler(List<String> fields) {
        if(fields.size() < 2) {
            System.out.println(String.join(" ", fields));
            return;
        }
        URL url = extractURL(fields.get(1));
        String sha256Digest;
        String filename = "";
        try {
            sha256Digest = extractSha256(extractValue(fields,2));
        } catch (IllegalArgumentException e) {
            sha256Digest = extractSha256(extractValue(fields,3));
            filename = extractFilename(fields.get(2), url);
        }
        System.out.println(String.format("%s, %s, %s, %s", filename, sha256Digest, url));
    }

    private static void doDroid(List<String> fields) {
        //System.out.println(String.join(",", fields));
        String value = String.format("w_download %s%s?raw=true %s %s", DROID_URL, fields.get(1), fields.get(fields.size()-1), fields.get(1));
        //System.out.println(value);
        List<String> fields2 = List.of(value.split(" "));
        wDownloadHandler(fields2);
    }


    private static String downloadWinetricksScript() throws IOException {
        String tempFile = "winetricks_temp";
        URL url = new URL(WINETRICKS_URL);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return tempFile;
    }
}