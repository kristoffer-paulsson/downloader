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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebianPackageBlockchain {
    public final static String BLOCKCHAIN_DIR = "chain";

    private final InversionOfControl ioc;
    private final ConfigManager configManager;
    private final Path chainDir;
    private final Path blockchainFile;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileWriter writer = null;

    private DebianPackage lastPackage = null;
    private String lastHash = null;

    DebianPackageBlockchain(InversionOfControl ioc) {
        this.ioc = ioc;
        this.configManager = ioc.resolve(ConfigManager.class);
        this.chainDir = Path.of(configManager.get("cache_dir"), BLOCKCHAIN_DIR);
        this.blockchainFile = chainDir.resolve("blockchain.csv");
        this.lastHash =  computeHash(
            String.format(
                    "package,version,sha256digest,%s,hash\n",
                    dateTimeFormatter.format(LocalDateTime.now()
                    )
            )
        );
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
                escape(lastHash),
                escape(pkg.packageName),
                escape(pkg.version),
                escape(pkg.sha256digest),
                escape(datetime)
        );
        this.lastHash = computeHash(row);
        String fullRow = row + "," + lastHash + "\n";
        writer.write(fullRow);
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
