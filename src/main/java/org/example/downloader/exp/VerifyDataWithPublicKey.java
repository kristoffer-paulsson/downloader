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

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.*;
import java.nio.file.*;
import java.util.Base64;

public class VerifyDataWithPublicKey {
    public static void main(String[] args) throws Exception {
        // Configuration
        String dataToVerify = "Hello, this is data to sign!";
        byte[] dataBytes = dataToVerify.getBytes("UTF-8");
        String publicKeyPem = "public_key.pem";
        String signatureFile = "signature.bin";

        // Load signature
        byte[] signatureBytes = Files.readAllBytes(Paths.get(signatureFile));

        // Load public key and determine algorithm
        PublicKey publicKey = loadPublicKey(publicKeyPem);
        String algorithm = publicKey.getAlgorithm().equals("EC") ? "SHA256withECDSA" : "SHA256withRSA";

        // Initialize signature verification
        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(dataBytes);

        // Verify signature
        boolean isValid = signature.verify(signatureBytes);
        System.out.println("Signature is " + (isValid ? "valid" : "invalid"));
    }

    private static PublicKey loadPublicKey(String pemFile) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(pemFile)));
        pem = pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(pem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        return cert.getPublicKey();
    }
}