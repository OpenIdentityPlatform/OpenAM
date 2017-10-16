/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 *
 * Portions Copyrighted 2006 Sun Microsystems Inc.
 */

package org.forgerock.openam.setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.identity.shared.debug.Debug;
import com.google.common.io.ByteStreams;

/**
 * Utilities for creating zip files from directories.
 */
public final class ZipUtils {

    private final Debug logger;

    public ZipUtils(Debug logger) {
        this.logger = logger;
    }

    /**
     * Creates a zip file containing the {@literal directory} and its sub-files, including the
     * directory itself.
     *
     * @param directory The source directory which will be zipped.
     * @param destination The destination path where the zip file will be created.
     * @throws IOException If there is an error producing the zip file.
     */
    public void zipDirectory(File directory, File destination) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of: " + directory);
        }
        try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(destination))) {
            for (File file : files) {
                zipFile(outputStream, file, (directory.getParentFile().getPath() + File.separator).length());
            }
        }
    }

    private void zipFile(ZipOutputStream outputStream, File file, int stripLength) throws IOException {
        if (!file.exists()) {
            logger.warning("File, {}, not found", file);
        }
        if (file.isFile()) {
            outputStream.putNextEntry(new ZipEntry(file.getPath().replace('\\', '/').substring(stripLength)));
            try (FileInputStream inputStream = new FileInputStream(file)) {
                ByteStreams.copy(inputStream, outputStream);
                outputStream.closeEntry();
            }
        } else {
            String entryName = (file.getPath() + File.separator).replace('\\', '/').substring(stripLength);
            outputStream.putNextEntry(new ZipEntry(entryName));
            outputStream.closeEntry();
            File[] files = file.listFiles();
            if (files == null) {
                throw new IOException("Failed to list sub-directories of: " + file);
            }
            for (File subFile : files) {
                zipFile(outputStream, subFile, stripLength);
            }
        }
    }
}
