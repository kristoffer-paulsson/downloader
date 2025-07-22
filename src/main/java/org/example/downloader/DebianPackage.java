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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.example.downloader.DebianWorker.BUFFER_SIZE;

public class DebianPackage {

    public final String packageName;
    public final String version;
    public final String architecture;
    public final String filename;
    public final long size;
    public final String sha256digest;
    public final String distribution;

    DebianPackage(
            String packageName, String version, String architecture,
            String filename, long size, String sha256digest,
            String distribution
    ) {
        this.packageName = packageName;
        this.version = version;
        this.architecture = architecture;
        this.filename = filename;
        this.size = size;
        this.sha256digest = sha256digest;
        this.distribution = distribution;
    }

    public String buildDownloadUrl(String baseUrl) {
        return String.format("%s/%s", baseUrl, filename);
    }

    public String buildSavePath(ConfigManager configManager) {
        return String.format("%s/%s", configManager.get("package_dir"), filename);
    }

    public long getSize() {
        return size;
    }

    public boolean isFileComplete(String filePath) {
        try {
            return Files.size(Paths.get(filePath)) == size;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean verifySha256Digest(String filePath) throws IOException {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];

            try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    sha256.update(buffer, 0, bytesRead);
                }
            }

            byte[] computedHash = sha256.digest();
            String computedDigest = bytesToHex(computedHash);
            return computedDigest.equalsIgnoreCase(sha256digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
