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
package org.example.downloader.ui;

import java.io.IOException;
import java.util.function.Consumer;

public class NonBlockingKeyPressDetector {
    private volatile boolean running = true;
    private Thread keyListenerThread;
    private final Consumer<Character> keyHandler;

    public boolean isRunning() {
        return running;
    }

    public NonBlockingKeyPressDetector(Consumer<Character> keyHandler) {
        this.keyHandler = keyHandler;
    }

    public void startKeyListener() {
        keyListenerThread = new Thread(() -> {
            try {
                while (running) {
                    if (System.in.available() > 0) { // Non-blocking check
                        int key = System.in.read(); // Blocks until Enter
                        if (key != -1 && key != '\n' && key != '\r') { // Ignore newlines
                            keyHandler.accept((char) key);
                            if (key == 'q' || key == 27) { // 'q' or ESC to stop
                                stopKeyListener();
                                break;
                            }
                        }
                    }
                    Thread.sleep(10); // Prevent busy-waiting
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        keyListenerThread.start();
    }

    public void stopKeyListener() {
        running = false;
        if (keyListenerThread != null) {
            try {
                keyListenerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        NonBlockingKeyPressDetector detector = new NonBlockingKeyPressDetector(
                key -> System.out.println("Key pressed: " + key + " (ASCII: " + (int) key)
        );
        try {
            detector.startKeyListener();
            System.out.println("Type a key and press Enter (q or ESC to quit)...");
            // Simulate non-blocking CLI work
            for (int i = 0; i < 10; i++) {
                System.out.println("Main thread working: " + i);
                Thread.sleep(1000);
            }
        } finally {
            detector.stopKeyListener();
        }
    }
}
