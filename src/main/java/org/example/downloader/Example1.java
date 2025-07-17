package org.example.downloader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Example1 {
    public static void main(String[] args) {
        // Create a list of DebianWorker instances (or use a custom iterator)
        List<DebianWorker> workers = new ArrayList<>();
        ConfigManager configManager = null;
        workers.add(new DebianWorker(
                new DebianPackage("pkg1", "1.0", "amd64", "pkg1.deb", "sha256hash1", "stable"),
                configManager, "https://deb.debian.org/debian"
        ));
        workers.add(new DebianWorker(
                new DebianPackage("pkg2", "1.0", "amd64", "pkg2.deb", "sha256hash2", "stable"),
                configManager, "https://deb.debian.org/debian"
        ));
        Iterator<DebianWorker> workerIterator = workers.iterator();

        // Create and start the executor
        DebianWorkerExecutor executor = new DebianWorkerExecutor(workerIterator);
        executor.start();

// Pause after some time
        try {
            Thread.sleep(5000); // Wait 5 seconds
            executor.pause();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

// Check status
        System.out.println("Is paused: " + executor.isPaused());
        System.out.println("Active workers: " + executor.getActiveWorkerCount());
        System.out.println("Paused workers: " + executor.getPausedWorkerCount());

// Resume downloads
        executor.resume();

// Shut down gracefully
        executor.shutdown();

        // Simulate restart after crash or shutdown
// Re-create the same workers and iterator
        Iterator<DebianWorker> newIterator = workers.iterator();
        DebianWorkerExecutor newExecutor = new DebianWorkerExecutor(newIterator);
        newExecutor.start(); // Resumes partial downloads
    }
}
