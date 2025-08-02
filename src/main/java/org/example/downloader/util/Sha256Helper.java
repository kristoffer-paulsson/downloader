package org.example.downloader.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for SHA-256 hash operations.
 * Provides methods to verify SHA-256 digests of files and validate hexadecimal strings.
 */
public class Sha256Helper {

    public static int BUFFER_SIZE = 8192;

    public static class Verifier {
        private final Path filePath;
        String sha256digest;

        private float startTime;
        private boolean hasExited = false;
        private boolean isComplete = false;
        private boolean hasError = false;

        private float speed = 0.0f;

        private int currentProcessSize = 0;

        public Verifier(Path filePath, String sha256digest) {
            this.filePath = filePath;
            this.sha256digest = sha256digest;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getSha256digest() { return  sha256digest; }

        public void stop() {
            if(startTime == 0) {
                throw new IllegalStateException("Download has not started yet.");
            }
            hasExited = true;
        }

        public float getTime() {
            return (System.currentTimeMillis() - startTime) / 1000.0f;
        }

        public float getSpeed() {
            return speed;
        }

        public void forceComplete() {
            isComplete = true;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public boolean hasError() { return hasError; }

        private long totalSize = 0;

        public long totalByteSize() { return totalSize; }

        private long bytesProcessed = 0;
        private long currentByte = 0;

        public long totalBytesProcessed() { return bytesProcessed + currentByte; }
    }

    public static boolean verifySha256(Verifier task) throws IOException {
        try {
            if (!isValid64CharHex(task.sha256digest)) {
                throw new IllegalArgumentException("Invalid SHA-256 digest format. Must be a 32-character hexadecimal string.");
            }
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];

            task.totalSize = Files.size(task.filePath);
            try (InputStream inputStream = Files.newInputStream(task.filePath)) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1 && !task.hasExited) {
                    sha256.update(buffer, 0, bytesRead);
                    task.bytesProcessed += bytesRead;
                }
            }

            byte[] computedHash = sha256.digest();
            if(task.totalByteSize() == task.totalBytesProcessed())
                task.isComplete = true;
            String computedDigest = bytesToHex(computedHash);
            task.hasError = !computedDigest.equalsIgnoreCase(task.sha256digest);
            return !task.hasError();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies the SHA-256 digest of a file against a provided digest string.
     *
     * @param filePath the path to the file to verify
     * @param sha256digest the expected SHA-256 digest as a 32-character hexadecimal string
     * @return true if the computed digest matches the provided digest, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean verifySha256Digest(Path filePath, String sha256digest) throws IOException {
        return verifySha256(new Verifier(filePath, sha256digest));
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the hexadecimal representation of the byte array
     */
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

    /**
     * Computes the SHA-256 hash of the given data.
     * The result is returned as a 32-character hexadecimal string.
     *
     * @param data the input data to hash
     * @return the computed hash as a lowercase hexadecimal string
     */
    public static String computeHash(String data) {
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
    public static boolean isValid64CharHex(String input) {
        return input != null && input.matches("^[0-9a-fA-F]{64}$");
    }
}
