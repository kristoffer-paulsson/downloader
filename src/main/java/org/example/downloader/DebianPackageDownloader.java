package org.example.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.GZIPInputStream;

public class DebianPackageDownloader {
    private static final String DB_URL = "jdbc:sqlite:debian_packages.db";
    private static final String BASE_URL = "http://deb.debian.org/debian/dists/%s/main/binary-%s/Packages.gz";

    public static void main(String[] args) {
        // Example: Download Packages.gz for bookworm, amd64
        String distribution = "bookworm";
        String architecture = "amd64";

        try {
            // Download and process Packages.gz
            String packagesContent = downloadPackagesGz(distribution, architecture);
            printPackagesToOutput(packagesContent, distribution, architecture);
            // Insert into SQLite database
            //insertPackagesIntoDb(packagesContent, distribution, architecture);
            System.out.println("Packages successfully inserted into database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Download and decompress Packages.gz
    private static String downloadPackagesGz(String distribution, String architecture) throws Exception {
        String url = String.format(BASE_URL, distribution, architecture);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        // Download the Packages.gz file
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] compressedData = response.body();

        // Decompress GZIP
        byte[] decompressedData = decompressGzip(compressedData);
        return new String(decompressedData);
    }

    // Decompress GZIP data
    private static byte[] decompressGzip(byte[] compressed) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    // Insert package data into SQLite database
    private static void insertPackagesIntoDb(String packagesContent, String distribution, String architecture) throws SQLException {
        // Connect to SQLite database
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Create table if it doesn't exist
            createTable(conn);

            // Prepare insert statement
            String insertSql = "INSERT INTO packages (name, version, architecture, distribution) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                // Parse Packages file
                String[] packageEntries = packagesContent.split("\n\n");
                for (String entry : packageEntries) {
                    if (entry.trim().isEmpty()) continue;

                    String packageName = extractField(entry, "Package");
                    String version = extractField(entry, "Version");

                    if (packageName != null && version != null) {
                        pstmt.setString(1, packageName);
                        pstmt.setString(2, version);
                        pstmt.setString(3, architecture);
                        pstmt.setString(4, distribution);
                        pstmt.executeUpdate();
                    }
                }
            }
        }
    }

    // Create the packages table
    private static void createTable(Connection conn) throws SQLException {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS packages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    version TEXT NOT NULL,
                    architecture TEXT NOT NULL,
                    distribution TEXT NOT NULL
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    private static void printPackagesToOutput(String packagesContent, String distribution, String architecture) throws SQLException {
                // Parse Packages file
                String[] packageEntries = packagesContent.split("\n\n");
                for (String entry : packageEntries) {
                    entry += "\n";
                    if (entry.trim().isEmpty()) continue;

                    String packageName = extractField(entry, "Package");
                    String version = extractField(entry, "Version");
                    String filename = extractField(entry, "Filename");
                    String sha256digest = extractField(entry, "SHA256");
                    String md5sum = extractField(entry, "MD5sum");


                    if (packageName != null && version != null) {
                        System.out.println(packageName);
                        System.out.println(version);
                        System.out.println(architecture);
                        System.out.println(distribution);
                        System.out.println(filename);
                        System.out.println(sha256digest);
                        System.out.println("");

                    }
                }
    }

    // Extract a field from a package entry
    private static String extractField(String entry, String fieldName) {
        String regex = fieldName + ": (.*?)\n";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(entry);
        return matcher.find() ? matcher.group(1) : null;
    }
}
