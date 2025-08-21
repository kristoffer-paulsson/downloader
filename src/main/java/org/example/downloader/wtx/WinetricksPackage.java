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
package org.example.downloader.wtx;

import org.example.downloader.util.BasePackage;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class WinetricksPackage implements BasePackage {

    private final String filename;

    private final String sha256Digest;
    private final String size;
    private final String url;

    private final String verb;
    private final WinetricksCategory category;

    /**
     * "Filename: %s\nUrl: %s\nSha256: %s\nSize: %s\nVerb: %s\nCategory: %s\n\n"
     * */
    WinetricksPackage(
            String filename,
            String url,
            String sha256Digest,
            String size,
            String verb,
            String category
    ) {
        this.filename = filename;
        this.url = url;
        this.sha256Digest = sha256Digest;
        this.size = size;
        this.verb = verb;
        this.category = WinetricksCategory.fromString(category);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public String getSha256Digest() {
        return sha256Digest;
    }

    public String getVerb() {
        return verb;
    }

    public WinetricksCategory getCategory() {
        return category;
    }

    public URL getRealUrl() throws MalformedURLException, URISyntaxException {
        return new URI(url).toURL();
    }

    public String uniqueKey() {
        return String.format("%s-%s", filename, sha256Digest);
    }

    @Override
    public String toString() {
        return String.format(
                "Filename: %s, Url: %s, Sha256: %s, Size: %s, Verb: %s, Category: %s",
                filename, url, sha256Digest, size, verb, category
        );
    }
}
