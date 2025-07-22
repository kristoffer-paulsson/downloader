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

import javax.smartcardio.*;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.PublicKey;

public class SmartCardHandler {
    private final CardTerminal terminal;
    private final String pin;
    private CardChannel channel;
    private String keyAlgorithm; // RSA or EC

    public SmartCardHandler(String pin) throws CardException {
        this.pin = pin;
        TerminalFactory factory = TerminalFactory.getDefault();
        terminal = factory.terminals().list().stream()
                .findFirst()
                .orElseThrow(() -> new CardException("No smart card reader found"));
    }

    public void connect() throws CardException {
        Card card = terminal.connect("T=0");
        channel = card.getBasicChannel();
        authenticate();
        // Determine key algorithm
        X509Certificate cert = null;
        try {
            cert = getCertificate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PublicKey publicKey = cert.getPublicKey();
        keyAlgorithm = publicKey.getAlgorithm(); // RSA or EC
    }

    private void authenticate() throws CardException {
        byte[] pinBytes = pin.getBytes();
        byte[] apdu = new byte[5 + pinBytes.length];
        apdu[0] = (byte) 0x00; // CLA
        apdu[1] = (byte) 0x20; // INS: VERIFY
        apdu[2] = (byte) 0x00; // P1
        apdu[3] = (byte) 0x80; // P2: PIN
        apdu[4] = (byte) pinBytes.length; // Lc
        System.arraycopy(pinBytes, 0, apdu, 5, pinBytes.length);

        ResponseAPDU response = channel.transmit(new CommandAPDU(apdu));
        if (response.getSW() != 0x9000) {
            throw new CardException("PIN verification failed: SW=" + Integer.toHexString(response.getSW()));
        }
    }

    public X509Certificate getCertificate() throws Exception {
        byte[] certTag = new byte[]{(byte) 0x5C, 0x03, 0x5F, (byte) 0xC1, 0x05}; // Tag: 5FC105
        byte[] apdu = new byte[]{
                (byte) 0x00, // CLA
                (byte) 0xCB, // INS: GET DATA
                (byte) 0x3F, // P1
                (byte) 0xFF, // P2
                (byte) 0x05, // Lc
                (byte) 0x5C, 0x03, 0x5F, (byte) 0xC1, 0x05 // Data: Tag
        };

        ResponseAPDU response = channel.transmit(new CommandAPDU(apdu));
        if (response.getSW() != 0x9000) {
            throw new CardException("Failed to get certificate: SW=" + Integer.toHexString(response.getSW()));
        }

        byte[] data = response.getData();
        int offset = 2; // Skip TLV header (0x53, length)
        byte[] certBytes = new byte[data.length - offset];
        System.arraycopy(data, offset, certBytes, 0, certBytes.length);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public byte[] signData(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);

        byte[] apdu;
        if (keyAlgorithm.equals("RSA")) {
            // RSA signing
            apdu = new byte[5 + hash.length + 7];
            apdu[0] = (byte) 0x00; // CLA
            apdu[1] = (byte) 0x87; // INS: AUTHENTICATE
            apdu[2] = (byte) 0x01; // P1: RSA
            apdu[3] = (byte) 0x9A; // P2: Slot 9a
            apdu[4] = (byte) (hash.length + 7); // Lc
            apdu[5] = (byte) 0x7C; // Dynamic Auth Template
            apdu[6] = (byte) (hash.length + 5); // Length
            apdu[7] = (byte) 0x82; // Response Data
            apdu[8] = (byte) 0x00; // No response data
            apdu[9] = (byte) 0x81; // Input Data
            apdu[10] = (byte) (hash.length + 1); // Length
            apdu[11] = (byte) 0x01; // RSA PKCS#1 padding
            System.arraycopy(hash, 0, apdu, 12, hash.length);
        } else if (keyAlgorithm.equals("EC")) {
            // ECDSA signing
            apdu = new byte[5 + hash.length + 5];
            apdu[0] = (byte) 0x00; // CLA
            apdu[1] = (byte) 0x87; // INS: AUTHENTICATE
            apdu[2] = (byte) 0x11; // P1: ECDSA
            apdu[3] = (byte) 0x9A; // P2: Slot 9a
            apdu[4] = (byte) (hash.length + 5); // Lc
            apdu[5] = (byte) 0x7C; // Dynamic Auth Template
            apdu[6] = (byte) (hash.length + 3); // Length
            apdu[7] = (byte) 0x82; // Response Data
            apdu[8] = (byte) 0x00; // No response data
            apdu[9] = (byte) 0x81; // Input Data
            apdu[10] = (byte) hash.length; // Length
            System.arraycopy(hash, 0, apdu, 11, hash.length);
        } else {
            throw new IllegalStateException("Unsupported key algorithm: " + keyAlgorithm);
        }

        ResponseAPDU response = channel.transmit(new CommandAPDU(apdu));
        if (response.getSW() != 0x9000) {
            throw new CardException("Signing failed: SW=" + Integer.toHexString(response.getSW()));
        }

        byte[] responseData = response.getData();
        if (responseData[0] != 0x7C || responseData[2] != (byte) 0x82) {
            throw new CardException("Invalid signature response format");
        }
        int sigLength = (responseData[3] & 0xFF) << 8 | (responseData[4] & 0xFF);
        byte[] signature = new byte[sigLength];
        System.arraycopy(responseData, 5, signature, 0, sigLength);
        return signature;
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void disconnect() throws CardException {
        if (channel != null) {
            channel.getCard().disconnect(true);
        }
    }
}