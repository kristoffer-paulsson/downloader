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

import org.example.downloader.deb.DebianDownloadEnvironment;
import org.example.downloader.deb.DebianPackage;
import org.example.downloader.deb.DebianParser;
import org.example.downloader.util.*;

import java.nio.file.Path;


public class DebianVerifyAction extends AbstractVerifyAction<DebianDownloadEnvironment, DebianPackage> {

    public static String FILENAME = "debian_%s_%s_%s_%s";

    public DebianVerifyAction(InversionOfControl ioc, String name) {
        super(ioc, name);
    }

    @Override
    protected DebianDownloadEnvironment getEnvironmentManager() {
        return ioc.resolve(DebianDownloadEnvironment.class);
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
        DebianParser.chunkPackages(em).get(em.getPiece()-1).packages.forEach((p) -> {
            allPackages.put(p.getSha256Digest(), p);
            totalSize.getAndAdd(p.getByteSize());
            count.getAndIncrement();
        });
    }

    protected String generateBlockchainFilename() {
        return String.format(FILENAME, em.getDistribution().getDist(), em.getArchitecture().getArch(), em.getChunks(), em.getPiece());
    }

    protected String generateArtifactPath(BlockChainHelper.Row r) {
        return allPackages.get(r.getDigest()).buildSavePath(em).toString();
    }

    protected BlockchainVerifier createBlockchainVerifier() {
        return new BlockchainVerifier(chain, logger, (r) -> Path.of(generateArtifactPath(r)));
    }
}
