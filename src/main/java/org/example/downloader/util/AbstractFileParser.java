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
package org.example.downloader.util;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public abstract class AbstractFileParser<E extends BasePackage> implements Iterator<E>, AutoCloseable {
    private BufferedReader reader;
    private Map<String, StringBuilder> currentPackage;
    private String nextLine;
    private String currentField;

    public AbstractFileParser(String filePath) throws IOException {
        InputStream fileStream = new FileInputStream(filePath);
        initialize(fileStream);
    }

    public AbstractFileParser(InputStream fileStream) throws IOException {
        initialize(fileStream);
    }

    protected void initialize(InputStream fileStream) throws IOException {
        GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
        reader = new BufferedReader(new InputStreamReader(gzipStream));
        currentPackage = new HashMap<>();
        currentField = null;
        nextLine = reader.readLine(); // Read the first line
    }

    @Override
    public boolean hasNext() {
        try {
            while (nextLine != null) {
                if (nextLine.trim().isEmpty() && !currentPackage.isEmpty()) {
                    return true; // Ready to yield a package
                }
                processLine();
            }
            // Check for a final package
            if (!currentPackage.isEmpty()) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public E next(){
        if (!hasNext()) {
            this.close();
            throw new NoSuchElementException("No more packages to parse");
        }
        Map<String, StringBuilder> data = currentPackage;
        currentPackage = new HashMap<>();
        currentField = null;
        try {
            nextLine = reader.readLine(); // Move to the next line
        } catch (IOException e) {
            e.printStackTrace();
            nextLine = null;
        }
        return parseFieldsAndCreatePackage(data);
    }

    protected abstract E parseFieldsAndCreatePackage(Map<String, StringBuilder> packageData);

    private void processLine() throws IOException {
        while (nextLine != null && !nextLine.trim().isEmpty()) {
            if (nextLine.startsWith(" ")) {
                if (currentField != null) {
                    currentPackage.computeIfAbsent(currentField, k -> new StringBuilder())
                            .append("\n")
                            .append(nextLine.trim());
                }
            } else {
                int colonIndex = nextLine.indexOf(":");
                if (colonIndex != -1) {
                    currentField = nextLine.substring(0, colonIndex).trim();
                    String value = nextLine.substring(colonIndex + 1).trim();
                    currentPackage.computeIfAbsent(currentField, k -> new StringBuilder())
                            .append(value);
                }
            }
            nextLine = reader.readLine();
        }
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
