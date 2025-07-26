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

import java.io.*;
import java.util.*;

public class PackageFileParser extends AbstractFileParser<BasePackageImpl> {

    public PackageFileParser(String filePath) throws IOException {
        super(filePath);
    }

    @Override
    protected BasePackageImpl parseFieldsAndCreatePackage(Map<String, StringBuilder> packageData) {
        return new BasePackageImpl(
                packageData.getOrDefault("Filename", new StringBuilder()).toString(),
                packageData.getOrDefault("Size", new StringBuilder("0")).toString(),
                packageData.getOrDefault("SHA256", new StringBuilder()).toString()
        );
    }
}
