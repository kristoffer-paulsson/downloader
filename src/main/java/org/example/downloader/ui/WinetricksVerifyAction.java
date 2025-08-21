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

import org.example.downloader.util.BlockChainHelper;
import org.example.downloader.util.BlockchainVerifier;
import org.example.downloader.util.InversionOfControl;
import org.example.downloader.util.PrintHelper;
import org.example.downloader.wtx.WinetricksDownloadEnvironment;
import org.example.downloader.wtx.WinetricksPackage;
import org.example.downloader.wtx.WinetricksParser;


public class WinetricksVerifyAction extends AbstractVerifyAction<WinetricksDownloadEnvironment, WinetricksPackage> {

    public static String FILENAME = "winetricks_download-%s";

    public WinetricksVerifyAction(InversionOfControl ioc, String name) {
        super(ioc, name);
    }

    @Override
    protected WinetricksDownloadEnvironment getEnvironmentManager() {
        return ioc.resolve(WinetricksDownloadEnvironment.class);
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
        WinetricksParser.filterPackages(em).forEach((p) -> {
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
