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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.maven.plugins.inject.content;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Class which provides methods that perform IO operations, such as copying contents into a file, copying and deleting
 * a temporary file.
 *
 * @since 12.0.0
 */
class IOFactory {

    /**
     * Creates a new BufferedReader instance.
     *
     * @param file The file.
     * @return A BufferedReader.
     * @throws FileNotFoundException If the file does not exist.
     */
    BufferedReader newReader(File file) throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
    }

    /**
     * Creates a new BufferedWriter instance.
     *
     * @param file The file.
     * @return A BufferedWriter.
     * @throws FileNotFoundException If the file does not exist.
     */
    BufferedWriter newWriter(File file) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
    }

    /**
     * Creates a temporary file with the same name as the given file but with ".tmp" prepended to it.
     *
     * @param file The file to create a temporary file from.
     * @return The temporary file.
     * @throws IOException If there is a problem creating the temporary file.
     */
    File createTemporaryFile(File file) throws IOException {
        return File.createTempFile(file.getName(), ".tmp", file.getParentFile());
    }

    /**
     * Writes the given content file using the given writer, converting each line using the given
     * {@code ContentConverter}.
     *
     * @param writer The writer.
     * @param content The content.
     * @param contentConverter The content converter.
     * @throws IOException If there is a problem reading the content file or writing the content.
     */
    void writeContent(BufferedWriter writer, File content, ContentConverter contentConverter) throws IOException {
        BufferedReader contentReader = null;
        try {
            contentReader = newReader(content);
            String line;
            while ((line = contentReader.readLine()) != null) {
                writeLine(writer, contentConverter.convert(line));
            }
        } finally {
            if (contentReader != null) {
                contentReader.close();
            }
        }
    }

    /**
     * Writes the given line with the given writer, followed by a new line.
     *
     * @param writer The writer.
     * @param line The line.
     * @throws IOException If there is a problem writing the line.
     */
    void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
    }

    /**
     * <p>Moves the file with the given file name, {@code fromFile}, to the file, {@code toFile}.</p>
     *
     * @param fromFile The file to move from.
     * @param toFile The file to move to.
     * @throws IOException If there is a problem reading the temporary file, writing to the destination file or the
     * temporary file could not be deleted.
     */
    void moveTo(File fromFile, File toFile) throws IOException {
        if (toFile.exists()) {
            if (!toFile.delete()) {
                throw new IOException("Failed to move file from " + fromFile + " to " + toFile.getAbsolutePath() +
                        ": destination file exists and cannot be removed.");
            }
        }
        if (!fromFile.renameTo(toFile)) {
            throw new IOException("Failed to move file from, " + fromFile + ", to " + toFile.getAbsolutePath());
        }
    }
}
