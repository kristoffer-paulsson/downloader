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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.io.*;

public class SmartCardPublicKeyExtractor {
    private final String pin;

    public SmartCardPublicKeyExtractor(String pin) {
        this.pin = pin;
    }

    public PublicKey getPublicKey() throws Exception {
        SmartCardHandler handler = new SmartCardHandler(pin);
        try {
            handler.connect();
            X509Certificate cert = handler.getCertificate();
            return cert.getPublicKey();
        } finally {
            handler.disconnect();
        }
    }

    public void savePublicKeyToPem(String pemFile) throws Exception {
        SmartCardHandler handler = new SmartCardHandler(pin);
        try {
            handler.connect();
            X509Certificate cert = handler.getCertificate();
            try (FileWriter fw = new FileWriter(pemFile)) {
                fw.write("-----BEGIN CERTIFICATE-----\n");
                String base64Cert = Base64.getEncoder().encodeToString(cert.getEncoded());
                for (int i = 0; i < base64Cert.length(); i += 64) {
                    fw.write(base64Cert, i, Math.min(i + 64, base64Cert.length()));
                    fw.write("\n");
                }
                fw.write("-----END CERTIFICATE-----\n");
            }
        } finally {
            handler.disconnect();
        }
    }
}