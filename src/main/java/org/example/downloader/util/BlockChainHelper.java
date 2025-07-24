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
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

public class BlockChainHelper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Blockchain {
        private final File blockchainFile;
        private String lastHash = null;

        private BufferedWriter writer = null;

        /**
         * Constructs a Blockchain instance with the specified blockchain file.
         * The file is used to store the blockchain data.
         *
         * @param blockchainFile the file where the blockchain will be stored
         */
        public Blockchain(File blockchainFile) {
            this.blockchainFile = blockchainFile;
            this.lastHash = computeHash(blockchainFile.getName());
        }

        /**
         * Returns the file path of the blockchain file.
         *
         * @return the path to the blockchain file
         */
        public Path getBlockchainFile() {
            return blockchainFile.toPath();
        }

        /**
         * Starts the blockchain by creating a new file and writing the header.
         * This method should only be called once to initialize the blockchain.
         */
        private void start() {
            try {
                writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8);
                writer.write("artifact,digest,datetime,hash\n");
            } catch (IOException e) {
                throw new RuntimeException("Failed to start blockchain", e);
            }
        }

        /**
         * Starts or continues the blockchain based on whether the file already exists.
         * If the file exists, it verifies the existing rows using the provided predicate.
         * If it does not exist, it starts a new blockchain.
         *
         * @param verificationPredicate a predicate to verify each row in the blockchain
         */
        private void startOrContinue(Predicate<Row> verificationPredicate) {
            if(Files.exists(blockchainFile.toPath())) {
                verify(verificationPredicate);
                try {
                    writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                start();
            }
        }

        /**
         * Verifies the blockchain by reading each row from the file and checking its hash.
         * It uses the provided predicate to verify each row's content.
         *
         * @param verificationPredicate a predicate to verify each row in the blockchain
         */
        private void verify(Predicate<Row> verificationPredicate) {
            if (writer != null) {
                throw new IllegalStateException("Blockchain already started. Call start() only once.");
            }

            lastHash = computeHash(blockchainFile.getName());

            try {
                Files.lines(blockchainFile.toPath()).skip(1).forEach(line -> {
                    Row row = rowFromString(line);
                    if (!row.verifyRowHash(lastHash)) {
                        throw new IllegalStateException("Invalid row hash: " + row.hash);
                    }
                    if (!verificationPredicate.test(row)) {
                        throw new IllegalStateException("Row verification failed for: " + row.artifact);
                    }
                    lastHash = row.hash;
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to verify blockchain", e);
            }
        }

        /**
         * Adds a new row to the blockchain. The row must be built using the
         * buildNewRowAddHash method of the Row class.
         *
         * @param row the Row object to add to the blockchain
         */
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

    /**
     * Represents a single row in the blockchain.
     * Each row contains an artifact, digest, datetime, and hash.
     */
    public static class Row {
        String artifact;
        String digest;
        String datetime;
        String hash;

        /**
         * Constructs a Row instance with the specified artifact, digest and datetime.
         * The hash can be empty if not yet computed.
         *
         * @param artifact the artifact name
         * @param digest   the digest of the artifact
         * @param datetime the datetime when the row was created
         */
        private Row(String artifact, String digest, String datetime) {
            this.artifact = artifact.trim();
            this.digest = digest.trim();
            this.datetime = datetime.trim();
            this.hash = "";
        }

        /**
         * Constructs a Row instance with the specified artifact, digest, datetime, and hash.
         * This constructor is used when the hash is already computed.
         *
         * @param artifact the artifact name
         * @param digest   the digest of the artifact
         * @param datetime the datetime when the row was created
         * @param hash     the hash of the row
         */
        private Row(String artifact, String digest, String datetime, String hash) {
            this.artifact = artifact.trim();
            this.digest = digest.trim();
            this.datetime = datetime.trim();
            this.hash = hash.trim();
        }

        /**
         * Returns the artifact name.
         *
         * @return the artifact name
         */
        public String getArtifact() {
            return artifact;
        }

        /**
         * Returns the digest of the artifact.
         *
         * @return the digest
         */
        public String getDigest() {
            return digest;
        }

        /**
         * Returns the datetime when the row was created.
         *
         * @return the datetime
         */
        public String getDatetime() {
            return datetime;
        }

        /**
         * Returns the hash of the row.
         * If the hash is empty, it means it has not been computed yet.
         *
         * @return the hash
         */
        public String getHash() {
            return hash;
        }

        /**
         * Builds a new row string with the current artifact, digest, datetime, and hash.
         * This method should be called when adding a new row to the blockchain.
         *
         * @param previousHash the hash of the previous row
         * @return a formatted string representing the new row
         */
        public String buildNewRowAddHash(String previousHash) {
            if(!hash.isEmpty()) {
                throw new IllegalStateException("Illegal attempt to add hash to existing row");
            }

            this.hash = buildRowHash(previousHash);
            return String.format("%s,%s,%s,%s\n", artifact, digest, datetime, hash);
        }

        /**
         * Builds a hash for the current row based on the previous hash, artifact, digest, and datetime.
         * This method is used to compute the hash when adding a new row to the blockchain.
         *
         * @param previousHash the hash of the previous row
         * @return the computed hash as a 32-character hexadecimal string
         */
        public String buildRowHash(String previousHash) {
            if(!isValid32CharHex(previousHash)) {
                throw new IllegalArgumentException("Invalid previous hash: " + previousHash);
            }

            String newBlock = String.format("%s,%s,%s,%s", previousHash.trim(), artifact, digest, datetime);
            return computeHash(newBlock);
        }

        /**
         * Verifies if the current row's hash matches the expected hash based on the previous hash.
         * This method is used to ensure the integrity of the blockchain.
         *
         * @param previousHash the hash of the previous row
         * @return true if the current row's hash matches the expected hash, false otherwise
         */
        public boolean verifyRowHash(String previousHash) {
            String expectedHash = buildRowHash(previousHash);
            return expectedHash.equals(this.hash);
        }
    }

    /**
     * Creates a Row instance from a string representation.
     * The string should be in the format: "artifact,digest,datetime,hash".
     *
     * @param row the string representation of the row
     * @return a Row instance
     * @throws IllegalArgumentException if the string format is invalid
     */
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

    /**
     * Creates a Row instance from an artifact and its digest.
     * The datetime is set to the current time.
     *
     * @param artifact the name of the artifact
     * @param digest   the digest of the artifact
     * @return a Row instance with the current datetime
     */
    public static Row rowFromArtifact(String artifact, String digest) {
        return new Row(artifact, digest, LocalDateTime.now().format(dateTimeFormatter));
    }

    /**
     * Computes the SHA-256 hash of the given data.
     * The result is returned as a 32-character hexadecimal string.
     *
     * @param data the input data to hash
     * @return the computed hash as a lowercase hexadecimal string
     */
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

    /**
     * Validates if the input string is a valid 32-character hexadecimal string.
     *
     * @param input the string to validate
     * @return true if the string is a valid 32-character hex, false otherwise
     */
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

        Blockchain blockchain = new Blockchain(blockchainFile.toFile());
        blockchain.start();
        return blockchain;
    }

    /**
     * Continues an existing blockchain by appending new rows to the specified file.
     * The file must exist, and the verification predicate is used to validate each row.
     *
     * @param blockchainFile      the path to the existing blockchain file
     * @param verificationPredicate a predicate to verify each row in the blockchain
     * @return a Blockchain instance that continues from the existing file
     */
    public static Blockchain continueBlockchain(Path blockchainFile, Predicate<Row> verificationPredicate) {
        if (blockchainFile == null || !Files.exists(blockchainFile)) {
            throw new IllegalArgumentException("Blockchain file does not exist: " + blockchainFile);
        }

        Blockchain blockchain = new Blockchain(blockchainFile.toFile());
        blockchain.startOrContinue(verificationPredicate);
        return blockchain;
    }

    /**
     * Verifies the integrity of an existing blockchain file.
     * It reads the file and checks each row against the provided verification predicate.
     * If the file does not exist, an exception is thrown.
     *
     * @param blockchainFile      the path to the blockchain file to verify
     * @param verificationPredicate a predicate to verify each row in the blockchain
     * @return a Blockchain instance if verification is successful
     */
    public static Blockchain verifyBlockchain(Path blockchainFile, Predicate<Row> verificationPredicate) {
        if (blockchainFile == null || !Files.exists(blockchainFile)) {
            throw new IllegalArgumentException("Blockchain file does not exist: " + blockchainFile);
        }

        Blockchain blockchain = new Blockchain(blockchainFile.toFile());
        blockchain.verify(verificationPredicate);
        return blockchain;
    }
}
