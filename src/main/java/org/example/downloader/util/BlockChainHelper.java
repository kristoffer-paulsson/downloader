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
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.example.downloader.util.Sha256Helper.computeHash;

public class BlockChainHelper {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class Blockchain {
        private final File blockchainFile;
        private boolean isFinalized = false;
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
            try {
                String lastLine = readLastLine(blockchainFile.toPath());
                if(lastLine == null) {
                    this.lastHash = computeHash(blockchainFile.getName());
                }
                else {
                    Row lastRow = rowFromString(lastLine);
                    this.lastHash = lastRow.hash;
                    this.isFinalized = lastRow.artifact.equals("end-of-blockchain");
                }
            } catch (IOException e) {
                this.lastHash = computeHash(blockchainFile.getName());
            }
        }

        public boolean isFinalized() {
            return isFinalized;
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
        public void start() {
            if(isFinalized)
                throw new IllegalStateException("Blockchain already finalized");
            try {
                writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8);
                writer.write("artifact,metadata,digest,datetime,hash\n");
                writer.flush();
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
                boolean isFinalized = verify(verificationPredicate);
                if (isFinalized) {
                    throw new IllegalStateException("Blockchain is finalized. Cannot continue.");
                }
                try {
                    writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                start();
            }
        }

        public void resume() {
            if(isFinalized)
                throw new IllegalStateException("Blockchain already finalized");
            try {
                writer = Files.newBufferedWriter(blockchainFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Verifies the integrity of the blockchain by reading the file and checking each row.
         * It ensures that the hashes are valid and that the rows match the expected format.
         * If the blockchain is finalized, it checks for the end-of-blockchain marker.
         *
         * @param verificationPredicate a predicate to verify each row in the blockchain
         * @return true if the blockchain is finalized, false otherwise
         */
        private boolean verify(Predicate<Row> verificationPredicate) {
            if(!isFinalized) {
                throw new IllegalStateException("Blockchain must be finalized to properly verify.");
            }
            if (writer != null) {
                throw new IllegalStateException("Blockchain already started. Call start() only once.");
            }

            lastHash = computeHash(blockchainFile.getName());
            AtomicReference<Row> lastRow = new AtomicReference<>();

            try {
                Files.lines(blockchainFile.toPath()).skip(1).forEach(line -> {
                    lastRow.set(rowFromString(line));
                    if (!lastRow.get().verifyRowHash(lastHash)) {
                        throw new IllegalStateException("Invalid row hash: " + lastRow.get().hash);
                    }
                    if (!lastRow.get().artifact.equals("end-of-blockchain")) {
                        if (!verificationPredicate.test(lastRow.get())) {
                            throw new IllegalStateException("Row verification failed for: " + lastRow.get().artifact);
                        }
                    }
                    lastHash = lastRow.get().hash;
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to verify blockchain", e);
            }

            // Check if the last row is the end-of-blockchain marker
            // Blockchain is not finalized
            return lastRow.get().artifact.equals("end-of-blockchain"); // Blockchain is finalized
        }

        /**
         * Adds a new row to the blockchain. The row must be built using the
         * buildNewRowAddHash method of the Row class.
         *
         * @param row the Row object to add to the blockchain
         */
        public void addRow(Row row) {
            if(isFinalized) {
                throw new IllegalStateException("Blockchain already finalized");
            }
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

        /**
         * Adds a new row to the blockchain with the specified artifact and digest.
         * The datetime is set to the current time, and the hash is computed automatically.
         *
         * @param artifact the name of the artifact
         * @param digest   the digest of the artifact
         */
        public void addRow(String artifact, String digest) {
            addRow(artifact, "n/a", digest);
        }

        public void addRow(String artifact, String metadata, String digest) {
            Row row = rowFromArtifact(artifact, metadata, digest);
            addRow(row);
        }

        /**
         * First finalizes teh blockchain by adding a final row with the last hash.
         * Finalizes the blockchain by closing the writer and ensuring all data is flushed.
         * This method should be called when done with the blockchain to ensure data integrity.
         */
        public void finalizeBlockchain() {
            if (writer == null) {
                throw new IllegalStateException("Blockchain not started. Call start() before finalizing.");
            }

            // Add a final row with the last hash
            Row finalRow = new Row(
                    "end-of-blockchain",
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    LocalDateTime.now().format(dateTimeFormatter),
                    lastHash
            );
            addRow(finalRow);

            // Close the writer to ensure all data is flushed
            close();
        }


        /**
         * Closes the blockchain writer. This should be called when done with the blockchain
         * to ensure all data is flushed and the file is properly closed.
         */
        public void close() {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close blockchain writer", e);
                }
                writer = null;
            }
        }
    }

    /**
     * Represents a single row in the blockchain.
     * Each row contains an artifact, digest, datetime, and hash.
     */
    public static class Row {
        String artifact;
        String metadata;
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
        private Row(String artifact, String metadata, String digest, String datetime) {
            this.artifact = artifact.trim();
            this.metadata = metadata.trim();
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
        private Row(String artifact, String metadata, String digest, String datetime, String hash) {
            this.artifact = artifact.trim();
            this.metadata = metadata.trim();
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

        public String getMetadata() {
            return metadata;
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
            return String.format("%s,%s,%s,%s,%s\n", artifact, metadata, digest, datetime, hash);
        }

        /**
         * Builds a hash for the current row based on the previous hash, artifact, digest, and datetime.
         * This method is used to compute the hash when adding a new row to the blockchain.
         *
         * @param previousHash the hash of the previous row
         * @return the computed hash as a 32-character hexadecimal string
         */
        public String buildRowHash(String previousHash) {
            if(!Sha256Helper.isValid64CharHex(previousHash)) {
                throw new IllegalArgumentException("Invalid previous hash: " + previousHash);
            }
            String newBlock = String.format("%s,%s,%s,%s,%s", previousHash.trim(), artifact, metadata, digest, datetime);
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
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid row format: " + row);
        }
        String artifact = parts[0].trim();
        String metadata = parts[1].trim();
        String digest = parts[2].trim();
        String datetime = parts[3].trim();
        String hash = parts[4].trim();
        return new Row(artifact, metadata, digest, datetime, hash);
    }

    /**
     * Creates a Row instance from an artifact and its digest.
     * The datetime is set to the current time.
     *
     * @param artifact the name of the artifact
     * @param digest   the digest of the artifact
     * @return a Row instance with the current datetime
     */
    public static Row rowFromArtifact(String artifact, String metadata, String digest) {
        return new Row(artifact, metadata, digest, LocalDateTime.now().format(dateTimeFormatter));
    }

    public static Path createNewFilename(Path blockchainDir, String name) {
        if (name == null || name.length() < 4) {
            throw new IllegalArgumentException("Blockchain name must be at least 4 characters long");
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        return blockchainDir.resolve(name + "-" + timestamp + ".csv");
    }

    public static Optional<Path>globLatestFile(Path blockchainDir, String name) throws IOException {
        // Glob pattern: matches <name>-<any digits>.csv
        String globPattern = name + "-[0-9]{14}.csv";
        return Files.list(blockchainDir)
                .filter(path -> path.getFileName().toString().matches(globPattern))
                .max(Comparator.comparing(path -> {
                    // Extract timestamp from file name (e.g., blockchain-20250804144233.csv -> 20250804144233)
                    String fileName = path.getFileName().toString();
                    return fileName.substring(name.length() + 1, fileName.length() - 4);
                }));
    }

    public static String readLastLine(Path filePath) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
            long fileLength = file.length() - 1;
            if (fileLength < 0) return null;

            StringBuilder sb = new StringBuilder();
            for (long pointer = fileLength; pointer >= 0; pointer--) {
                file.seek(pointer);
                char c = (char) file.read();
                if (c == '\n' || c == '\r') {
                    if (pointer != fileLength) {
                        break;
                    }
                }
                sb.append(c);
            }
            return sb.reverse().toString().trim();
        }
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
        Path blockchainFile = createNewFilename(blockchainDir, name);

        try {
            Files.createDirectories(blockchainFile.getParent());
            Files.createFile(blockchainFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Blockchain(blockchainFile.toFile());
    }

    public static Optional<Blockchain> resumeBlockchain(Path blockchainDir, String name) {
        Optional<Path> blockchainFile;
        try {
            blockchainFile = globLatestFile(blockchainDir, name);
        } catch (IOException | NoSuchElementException e) {
            throw new IllegalStateException("Blockchain file not found");
        }

        return blockchainFile.map(path -> new Blockchain(path.toFile()));
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

