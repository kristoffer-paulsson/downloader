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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlockChainHelper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Blockchain {
        private final File blockchainFile;
        private String lastHash = null;

        private BufferedWriter writer = null;

        public Blockchain(File blockchainFile) {
            this.blockchainFile = blockchainFile;
            this.lastHash = computeHash(blockchainFile.getName());
        }

        public Path getBlockchainFile() {
            return blockchainFile.toPath();
        }

        private void start() {
            try {
                writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8);
                writer.write("artifact,digest,datetime,hash\n");
            } catch (IOException e) {
                throw new RuntimeException("Failed to start blockchain", e);
            }
        }

        public void addRow(Row row) {
            if (writer == null) {
                throw new IllegalStateException("Blockchain not started. Call start() before adding rows.");
            }

            try {
                writer.write(row.buildNewRowAddHash(lastHash));
                lastHash = row.hash;
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to blockchain file", e);
            }
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

    /**
     * Starts a new blockchain with the given name in the specified directory.
     * The name must be at least 4 characters long, a timestamp will be appended
     * to ensure uniqueness. The unique filename is used to create the initial hash.
     *
     * @param blockchainDir the directory where the blockchain file will be created
     * @param name          the unique name of the blockchain
     * @return a new Blockchain instance
     */
    public static Blockchain startBlockchain(Path blockchainDir, String name) {
        if (name == null || name.length() < 4) {
            throw new IllegalArgumentException("Blockchain name must be at least 4 characters long");
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        Path blockchainFile = blockchainDir.resolve(name + "-" + timestamp + ".csv");

        try {
            Files.createDirectories(blockchainFile.getParent());
            Files.createFile(blockchainFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Blockchain(blockchainFile.toFile());
    }

    public static Blockchain continueBlockchain(Path blockchainFile, String lastHash) {
        /*if (blockchainFile == null || !blockchainFile.toString().endsWith(".csv")) {
            throw new IllegalArgumentException("Invalid blockchain file path: " + blockchainFile);
        }
        if (!isValid32CharHex(lastHash)) {
            throw new IllegalArgumentException("Invalid last hash: " + lastHash);
        }
        Blockchain blockchain = new Blockchain(blockchainFile);
        blockchain.lastHash = lastHash;
        return blockchain;*/
        return null;
    }

    public static Blockchain verifyBlockchain(Path blockchainFile) {
        return null; // Placeholder for future implementation
    }
}
