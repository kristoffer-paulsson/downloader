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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlockChainHelper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Blockchain {
        private final Path blockchainFile;
        private String lastHash = null;

        public Blockchain(Path blockchainFile) {
            this.blockchainFile = blockchainFile;
            this.lastHash = computeHash("artifact,digest,datetime,hash");
        }
    }

    public static class Row {
        String artifact;
        String digest;
        String datetime;
        String hash;

        private Row(String artifact, String digest, String datetime) {
            this.artifact = artifact.trim();
            this.digest = digest.trim();
            this.datetime = datetime.trim();
            this.hash = "";
        }

        private Row(String artifact, String digest, String datetime, String hash) {
            this.artifact = artifact.trim();
            this.digest = digest.trim();
            this.datetime = datetime.trim();
            this.hash = hash.trim();
        }


        public String getArtifact() {
            return artifact;
        }

        public String getDigest() {
            return digest;
        }

        public String getDatetime() {
            return datetime;
        }

        public String getHash() {
            return hash;
        }

        public String buildNewRowAddHash(String previousHash) {
            if(!hash.isEmpty()) {
                throw new IllegalStateException("Illegal attempt to add hash to existing row");
            }

            this.hash = buildRowHash(previousHash);
            return String.format("%s,%s,%s,%s\n", artifact, digest, datetime, hash);
        }

        public String buildRowHash(String previousHash) {
            if(!isValid32CharHex(previousHash)) {
                throw new IllegalArgumentException("Invalid previous hash: " + previousHash);
            }

            String newBlock = String.format("%s,%s,%s,%s", previousHash.trim(), artifact, digest, datetime);
            return computeHash(newBlock);
        }

        public boolean verifyRowHash(String previousHash) {
            String expectedHash = buildRowHash(previousHash);
            return expectedHash.equals(this.hash);
        }
    }

    public static Row rowFromString(String row) {
        String[] parts = row.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid row format: " + row);
        }
        String artifact = parts[0].trim();
        String digest = parts[1].trim();
        String datetime = parts[2].trim();
        String hash = parts[3].trim();
        return new Row(artifact, digest, datetime, hash);
    }

    public static Row rowFromArtifact(String artifact, String digest) {
        return new Row(artifact, digest, LocalDateTime.now().format(dateTimeFormatter));
    }

    private static String computeHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean isValid32CharHex(String input) {
        // Check if string is exactly 32 characters long and contains only hex characters
        return input != null && input.matches("^[0-9a-fA-F]{32}$");
    }
}
