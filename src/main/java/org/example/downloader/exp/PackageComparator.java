package org.example.downloader.exp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageComparator {
    public static void main(String[] args) {
        String bookwormUrl = "http://deb.debian.org/debian/dists/bookworm/main/binary-amd64/Packages.gz";
        String trixieUrl = "http://deb.debian.org/debian/dists/trixie/main/binary-amd64/Packages.gz";

        try {
            Set<String> bookwormPackages = getJavaPackages(bookwormUrl);
            Set<String> trixiePackages = getJavaPackages(trixieUrl);

            System.out.println("Packages unique to bookworm: " + diffSets(bookwormPackages, trixiePackages));
            System.out.println("Packages unique to trixie: " + diffSets(trixiePackages, bookwormPackages));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<String> getJavaPackages(String repoUrl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(repoUrl))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] decompressed = decompressGzip(response.body()); // Implement decompression
        String content = new String(decompressed);

        Set<String> javaPackages = new HashSet<>();
        Pattern packagePattern = Pattern.compile("Package: (.*?java.*?)\n", Pattern.CASE_INSENSITIVE);
        Matcher matcher = packagePattern.matcher(content);

        while (matcher.find()) {
            javaPackages.add(matcher.group(1));
        }

        return javaPackages;
    }

    public static Set<String> diffSets(Set<String> set1, Set<String> set2) {
        Set<String> diff = new HashSet<>(set1);
        diff.removeAll(set2);
        return diff;
    }

    // Placeholder for GZIP decompression
    private static byte[] decompressGzip(byte[] compressed) throws Exception {
        // Implement GZIP decompression here
        return compressed;
    }
}
