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
package org.example.downloader.exp;

import java.io.*;

public class SignDataWithSmartCard {
    public static void main(String[] args) throws Exception {
        // Configuration
        String pin = "<YOUR_SMARTCARD_PIN>"; // Replace with your PIN
        String dataToSign = "Hello, this is data to sign!";
        byte[] dataBytes = dataToSign.getBytes("UTF-8");

        // Sign data
        SmartCardHandler handler = new SmartCardHandler(pin);
        try {
            handler.connect();
            byte[] signature = handler.signData(dataBytes);

            // Save signature
            try (FileOutputStream fos = new FileOutputStream("signature.bin")) {
                fos.write(signature);
            }

            // Extract and save public key
            SmartCardPublicKeyExtractor extractor = new SmartCardPublicKeyExtractor(pin);
            extractor.savePublicKeyToPem("public_key.pem");

            System.out.println("Data signed successfully. Signature saved to signature.bin, public key to public_key.pem");
        } finally {
            handler.disconnect();
        }
    }
}