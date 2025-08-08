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

import org.example.downloader.java.JavaDownloadEnvironment;
import org.example.downloader.java.JavaPackage;
import org.example.downloader.java.JavaParser;
import org.example.downloader.util.*;


public class JavaVerifyAction extends AbstractVerifyAction<JavaDownloadEnvironment, JavaPackage> {

    public static String FILENAME = "java_download-%s";

    public JavaVerifyAction(InversionOfControl ioc, String name) {
        super(ioc, name);
    }

    @Override
    protected JavaDownloadEnvironment getEnvironmentManager() {
        return ioc.resolve(JavaDownloadEnvironment.class);
    }

    @Override
    public void runAction() {
        setupAction();

        artifactInventory();

        try {
            if(!prepareResumeBlockchain()){
                return;
            }

            BlockchainVerifier verifier = createBlockchainVerifier();

            verifierThread(executorHolder, verifier, logger, totalSize.get());

            postVerificationAnalyze(executorHolder, verifier);

        } catch (IllegalStateException e) {
            System.out.println("Failed to verify blockchain because of " + e.getMessage());
        }

        System.out.println("Totally " + allPackages.size() + " artifacts needs yet to be downloaded for completion.");
    }

    @Override
    protected void loadArtifactInventory() {
        JavaParser.filterPackages(em).forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
    }

    @Override
    protected String generateBlockchainFilename() {
        return String.format(FILENAME, em.hashOfConfiguration());
    }

    protected String generateArtifactPath(BlockChainHelper.Row r) {
        return String.format(
                "%s/%s",
                em.getDownloadDir().toString(),
                allPackages.get(r.getDigest()).getFilename()
        );
    }
}
