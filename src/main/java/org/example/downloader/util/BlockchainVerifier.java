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
package org.example.downloader.util;

import org.example.downloader.WorkLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.example.downloader.util.BlockChainHelper.rowFromString;
import static org.example.downloader.util.Sha256Helper.computeHash;

public class BlockchainVerifier extends AbstractWorkerIterator<BlockchainVerifier.VerifyTask> {

    private BlockChainHelper.Blockchain blockchain;
    private final WorkLogger workLogger;
    private final ArtifactPath artifactPath;

    private final AtomicReference<BlockChainHelper.Row> lastRow = new AtomicReference<>();
    private final Iterator<String> stringIterator;
    private String lastHash;

    private final AtomicReference<List<BlockChainHelper.Row>> verifiedArtifacts = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<List<BlockChainHelper.Row>> brokenArtifacts = new AtomicReference<>(new ArrayList<>());
    private final AtomicBoolean brokenChain = new AtomicBoolean(false);
    private final boolean expectFinalized;
    private final AtomicBoolean isFinalized = new AtomicBoolean(false);

    public BlockchainVerifier(BlockChainHelper.Blockchain blockchain, WorkLogger workLogger, boolean expectFinalized, ArtifactPath artifactPath) {
        this.blockchain = blockchain;
        this.workLogger = workLogger;
        this.expectFinalized = expectFinalized;
        this.artifactPath = artifactPath;
        this.lastHash = computeHash(blockchain.getBlockchainFile().toFile().getName());

        try {
            stringIterator = Files.lines(blockchain.getBlockchainFile()).skip(1).iterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected VerifyTask createWorker() {
        lastRow.set(rowFromString(stringIterator.next()));
        if (!lastRow.get().verifyRowHash(lastHash)) {
            brokenChain.compareAndSet(false, true);
            throw new IllegalStateException("Invalid row hash: " + lastRow.get().hash);
        }

        lastHash = lastRow.get().hash;
        return new VerifyTask(workLogger, lastRow.get(), expectFinalized, artifactPath.artifactFile(lastRow.get()));
    }

    @Override
    public boolean hasNext() {
        return stringIterator.hasNext() && !brokenChain.get();
    }

    public boolean isBroken() {
        return brokenChain.get();
    }

    public List<BlockChainHelper.Row> getBrokenArtifacts() {
        return brokenArtifacts.get();
    }

    public List<BlockChainHelper.Row> getVerifiedArtifacts() {
        return verifiedArtifacts.get();
    }

    public interface ArtifactPath {
        Path artifactFile(BlockChainHelper.Row r);
    }

    public class VerifyTask extends AbstractWorker {

        private final BlockChainHelper.Row row;
        private final Path artifactFile;
        private final Sha256Helper.Verifier verifierTask;
        private final boolean expectFinalized;

        public VerifyTask(WorkLogger workLogger, BlockChainHelper.Row row, boolean expectFinalized, Path artifactFile) {
            super(workLogger);
            this.row = row;
            this.expectFinalized = expectFinalized;
            this.artifactFile = artifactFile;
            this.verifierTask = new Sha256Helper.Verifier(artifactFile, row.getDigest());
        }

        @Override
        public void run() {
            if(expectFinalized && row.artifact.equals("end-of-blockchain")) {
                isFinalized.compareAndSet(false, true);
                verifierTask.isComplete();
                logger.info("Verification of blockchain " + blockchain.getBlockchainFile() + " has reached EOF properly");
            } else {
                try {
                    Sha256Helper.verifySha256(verifierTask);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if(verifierTask.hasError() || !verifierTask.isComplete()) {
                    synchronized (brokenArtifacts) {
                        brokenArtifacts.get().add(row);
                    }
                    logger.severe("Verifying " + artifactFile + " for " + row.getArtifact() + " with sha256 digest of " + row.getDigest() + " failed.");
                } else if(!verifierTask.hasError() && verifierTask.isComplete()) {
                    synchronized (verifiedArtifacts) {
                        verifiedArtifacts.get().add(row);
                    }
                }
            }
        }

        @Override
        protected void stopProcessImpl() {
            verifierTask.stop();
        }

        @Override
        public float getSpeed() {
            return verifierTask.getSpeed();
        }

        @Override
        public float getTime() {
            return verifierTask.getTime();
        }

        @Override
        public long getCurrentProcessSize() {
            return verifierTask.totalBytesProcessed();
        }

        @Override
        public boolean isCompleted() {
            return verifierTask.isComplete();
        }
    }
}
