package org.example.downloader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MavenDependencyDownloader {

    public static void main(String[] args) throws Exception {
        String inputFile = "pom_list.txt"; // Text file with POM file paths or URLs
        String outputDir = "downloaded-jars"; // Directory to save downloaded JARs
        String cacheDir = outputDir + "/custom-cache"; // Custom cache directory

        // Create output and cache directories if they don't exist
        Files.createDirectories(Paths.get(outputDir));
        Files.createDirectories(Paths.get(cacheDir));

        // Initialize Maven Resolver
        RepositorySystem repoSystem = newRepositorySystem();
        RepositorySystemSession session = newRepositorySystemSession(cacheDir); // Use custom cache
        List<RemoteRepository> repositories = new ArrayList<>();
        repositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());

        // Read POM file paths/URLs from input file
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                processPomFile(line.trim(), repoSystem, session, repositories, outputDir);
            }
        }
    }

    private static void processPomFile(String pomPath, RepositorySystem repoSystem, RepositorySystemSession session,
                                       List<RemoteRepository> repositories, String outputDir) {
        try {
            // Read POM file (from local file or URL)
            InputStream pomInputStream;
            if (pomPath.startsWith("http://") || pomPath.startsWith("https://")) {
                pomInputStream = new URL(pomPath).openStream();
            } else {
                pomInputStream = new FileInputStream(pomPath);
            }

            // Parse POM file
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            Model model;
            try (pomInputStream) {
                model = pomReader.read(pomInputStream);
            }

            // Process dependencies
            model.getDependencies().forEach(dep -> {
                String groupId = dep.getGroupId();
                String artifactId = dep.getArtifactId();
                String version = dep.getVersion();
                String scope = dep.getScope();

                // Skip test or provided scope dependencies if desired
                if (scope != null && (scope.equals("test") || scope.equals("provided"))) {
                    return;
                }

                // Resolve and download artifact
                try {
                    DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
                    ArtifactRequest request = new ArtifactRequest();
                    request.setArtifact(artifact);
                    request.setRepositories(repositories);

                    ArtifactResult result = repoSystem.resolveArtifact(session, request);
                    File artifactFile = result.getArtifact().getFile();
                    System.out.println("Downloaded: " + artifactFile.getAbsolutePath());

                    // Copy to output directory
                    Files.copy(artifactFile.toPath(), Paths.get(outputDir, artifactFile.getName()));
                } catch (ArtifactResolutionException e) {
                    System.err.println("Failed to resolve " + groupId + ":" + artifactId + ":" + version + ": " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("Failed to copy artifact: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to process POM file: " + pomPath + " - " + e.getMessage());
        }
    }

    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(org.eclipse.aether.spi.connector.RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newRepositorySystemSession(String cacheDir) {
        org.eclipse.aether.repository.LocalRepositoryManager repoManager = newRepositorySystem()
                .newLocalRepositoryManager(
                        new org.eclipse.aether.impl.DefaultLocalRepositoryProvider().newLocalRepositoryManager(
                                new LocalRepository(cacheDir) // Use custom cache directory
                        )
                );
        org.eclipse.aether.impl.DefaultRepositorySystemSession session =
                new org.eclipse.aether.impl.DefaultRepositorySystemSession();
        session.setLocalRepositoryManager(repoManager);

        // Optional: Configure proxy if needed
        DefaultProxySelector proxySelector = new DefaultProxySelector();
        // Example: proxySelector.add(new SimpleProxy("http", "proxy.host", 8080, "username", "password"), null);
        session.setProxySelector(proxySelector);

        return session;
    }
}
