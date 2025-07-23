package org.example.downloader.exp2;

import org.example.downloader.exp.*;
import java.io.*;

public class SmartCard {
    public static void main(String[] args) throws Exception {
        String pin = "123456";
        String dataToSign = "Hello, this is data to sign!";
        byte[] dataBytes = dataToSign.getBytes("UTF-8");

        SmartCardHandler handler = new SmartCardHandler(pin);
        try {
            handler.connect();
            byte[] signature = handler.signData(dataBytes);
            try (FileOutputStream fos = new FileOutputStream("signature.bin")) {
                fos.write(signature);
            }
            SmartCardPublicKeyExtractor extractor = new SmartCardPublicKeyExtractor(pin);
            extractor.savePublicKeyToPem("public_key.pem");
            System.out.println("Signature generated and public key saved.");
        } finally {
            handler.disconnect();
        }
    }
}