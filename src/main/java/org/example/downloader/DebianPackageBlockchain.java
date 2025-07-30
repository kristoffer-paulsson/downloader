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

import org.example.downloader.util.InversionOfControl;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

public class DebianPackageBlockchain {
    public final static String BLOCKCHAIN_DIR = "chain";

    private final InversionOfControl ioc;
    private final ConfigManager configManager;
    private final Path chainDir;
    private final Path blockchainFile;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileWriter writer = null;

    private String lastHash = null;

    public DebianPackageBlockchain(InversionOfControl ioc) {
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.chainDir = Path.of(configManager.get("cache_dir"), BLOCKCHAIN_DIR);
        this.blockchainFile = buildBlockchainFilePath(Integer.parseInt(configManager.get(ConfigManager.PIECE)));
        this.lastHash =  computeHash("package,version,sha256digest,datetime,hash\n");
    }

    public DebianPackageBlockchain(InversionOfControl ioc, int chunkNum) {
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.chainDir = Path.of(configManager.get("cache_dir"), BLOCKCHAIN_DIR);
        this.blockchainFile = buildBlockchainFilePath(chunkNum);
        this.lastHash =  computeHash("package,version,sha256digest,datetime,hash\n");
    }

    public String getBlockchainFile() {
        return blockchainFile.toString();
    }

    private Path buildBlockchainFilePath(int piece) {
        String fileName = String.format("chunk_blockchain_%s_%s.csv", piece, configManager.get(ConfigManager.CHUNKS));
        return chainDir.resolve(fileName);
    }

    public Map<String, DebianPackage> getPackages() {
        Map<String, DebianPackage> packages = new java.util.HashMap<>();
        ioc.resolve(DebianPackageChunkSplitter.class).getJointChunkPackages().forEach((pkg) -> {
            packages.put(pkg.sha256digest, pkg);
        });
        return packages;
    }

    public DebianWorkerIterator startBlockchainCSVFile() throws IOException {
        if (!Files.exists(blockchainFile)) {
            initiateBlockchainCSVFile();
            return new DebianWorkerIterator(ioc, new ArrayList<>(getPackages().values()));
        } else {
            return continueBlockchainCSVFile();
        }
    }

    public DebianWorkerIterator continueBlockchainCSVFile() throws IOException {
        if (!Files.exists(blockchainFile)) {
            throw new IOException("Blockchain file " + blockchainFile + " does not exist, cannot continue.");
        }

        Map<String, DebianPackage> chunk = getPackages(); // Continue from existing packages

        String previousHash = this.lastHash;
        try (var reader = Files.newBufferedReader(blockchainFile, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    throw new IOException("Malformed blockchain row: " + line);
                }
                String rowWithoutHash = String.join(",", parts[0], parts[1], parts[2], parts[3]);
                String expectedHash = computeHash(previousHash + "," + rowWithoutHash);
                String actualHash = parts[4];

                DebianPackage pkg = chunk.get(parts[2]);
                if (!pkg.verifySha256Digest(pkg.buildSavePath(configManager))) {
                    throw new IOException("Package verification failed for: " + pkg.packageName + " at line: " + line);
                } else {
                    chunk.remove(parts[2]);
                }

                if (!expectedHash.equals(actualHash)) {
                    throw new IOException("Blockchain hash mismatch at line: " + line);
                }
                previousHash = actualHash;
            }
        }
        this.lastHash = previousHash;
        this.writer = new FileWriter(blockchainFile.toFile(), true);

        if(chunk.isEmpty()) {
            logPackage(endPackage());
            writer.close();
            this.writer = null; // Reset writer to indicate no logging mode
        }

        return new DebianWorkerIterator(ioc, new ArrayList<>(chunk.values()));
    }

    private DebianPackage endPackage() {
        // This method is used to finalize the package logging
        // It can be used to log the last package or finalize the blockchain
        return new DebianPackage(
                "end-of-blockchain",
                "0.0.0",
                "all",
                "end.txt",
                0,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                configManager.get(ConfigManager.DIST)
        );
    }

    public boolean verifyBlockchainCSVFile() throws IOException {
        if (!Files.exists(blockchainFile)) {
            throw new IOException("Blockchain file does not exist, cannot verify.");
        }

        DebianPackage endPackage = endPackage();
        Map<String, DebianPackage> chunk = getPackages();
        int chunkSize = chunk.size();
        chunk.put(endPackage.sha256digest, endPackage);

        int lineCount = 0;

        String previousHash = this.lastHash;
        try (var reader = Files.newBufferedReader(blockchainFile, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // Skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    throw new IOException("Malformed blockchain row: " + line);
                }
                String rowWithoutHash = String.join(",", parts[0], parts[1], parts[2], parts[3]);
                String expectedHash = computeHash(previousHash + "," + rowWithoutHash);
                String actualHash = parts[4];

                DebianPackage pkg = chunk.get(parts[2]);
                boolean isEndMarker = "end-of-blockchain".equals(parts[0]);
                if (!isEndMarker && (!pkg.verifySha256Digest(pkg.buildSavePath(configManager)))) {
                    throw new IOException("Package sha256 digest failed for: " + parts[2] + " at line: " + line);
                }
                chunk.remove(parts[2]);

                if (!expectedHash.equals(actualHash)) {
                    throw new IOException("Blockchain hash mismatch at line: " + line);
                }
                lineCount++;
                System.out.println("Package verified: " + pkg.packageName + "-" + pkg.version + " with hash: " + actualHash);

                previousHash = actualHash;
            }

            if (!chunk.isEmpty()) {
                for (DebianPackage pkg : chunk.values()) {
                    System.out.println("Remaining package: " + pkg.packageName + "-" + pkg.version);
                }
                System.out.println("Total lines in blockchain: " + lineCount);
                System.out.println("Number of packages in chunk: " + chunkSize);
                System.out.println("Number of packages left: " + chunk.size());
                System.out.println("Warning: Not all packages were verified, remaining packages: " + chunk.size());
            } else {
                System.out.println("Total lines in blockchain: " + lineCount);
                System.out.println("Number of packages in chunk: " + chunkSize);
                System.out.println("Number of packages left: " + chunk.size());
                System.out.println("All packages verified successfully.");
            }
        }
        this.lastHash = previousHash;
        return true;
    }

    public void initiateBlockchainCSVFile() throws IOException {
        if (!Files.exists(chainDir)) {
            Files.createDirectories(chainDir);
        }
        if (!Files.exists(blockchainFile)) {
            try (FileWriter writer = new FileWriter(blockchainFile.toFile(), true)) {
                writer.write("package,version,sha256digest,datetime,hash\n");
                this.writer = writer;
            }
        }
    }

    public synchronized void logPackage(DebianPackage pkg) throws IOException {
        if (writer == null) {
           throw new IOException("Blockchain not in logging mode");
        }
        String datetime = LocalDateTime.now().format(dateTimeFormatter);
        String row = String.join(",",
                escape(pkg.packageName),
                escape(pkg.version),
                escape(pkg.sha256digest),
                escape(datetime)
        );
        this.lastHash = computeHash(this.lastHash + "," + row);
        String fullRow = row + "," + lastHash + "\n";
        writer.write(fullRow);
        writer.flush();
    }

    private String computeHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
