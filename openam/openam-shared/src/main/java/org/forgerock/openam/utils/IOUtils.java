/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class for handling I/O streams
 *
 * @author Peter Major
 */
public final class IOUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private IOUtils() {
    }

    /**
     * Reads the file content at the given path, and returns its UTF-8 content
     * in String format.
     *
     * @param path The path to the file
     * @return content of the file at the given path
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the file does not exists at the given
     * path
     */
    public static String getFileContent(String path) throws IOException {
        return getFileContent(path, DEFAULT_ENCODING);
    }

    /**
     * Reads the file content at the given path, and returns its content in
     * String format using the given encoding.
     *
     * @param path The path to the file
     * @param encoding encoding of the file
     * @return content of the file at the given path
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the file does not exists at the given
     * path
     */
    public static String getFileContent(String path, String encoding) throws IOException {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("The supplied file: " + path
                    + " either does not exists, or is not readable");
        }

        return readStream(new FileInputStream(file), encoding);
    }

    /**
     * Reads a files content from the classpath using UTF-8 encoding.
     *
     * @param path the path to the file on the classpath
     * @return the content of the file
     * @throws IOException if an error occured while reading
     * @throws IllegalArgumentException if the file does not exists
     */
    public static String getFileContentFromClassPath(String path) throws IOException {
        return getFileContentFromClassPath(path, DEFAULT_ENCODING);
    }

    /**
     * Reads a files content from the classpath using the given encoding.
     *
     * @param path the path to the file on the classpath
     * @param encoding encoding of the file
     * @return the content of the file
     * @throws IOException if an error occured while reading
     * @throws IllegalArgumentException if the file does not exists
     */
    public static String getFileContentFromClassPath(String path, String encoding) throws IOException {
        InputStream is = IOUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("The supplied file: " + path
                    + " is missing from the classpath");
        }

        return readStream(is, encoding);
    }

    /**
     * Reads the InputStream and tries to interpret its content as String using
     * UTF-8 encoding
     * @param is the inputstream to read
     * @return The string representation of the inputstreams content
     * @throws IOException if there was an error while reading the stream
     */
    public static String readStream(InputStream is) throws IOException {
        return readStream(is, DEFAULT_ENCODING);
    }

    /**
     * Reads the InputStream and tries to interpret its content as String using
     * the given encoding
     * @param is the inputstream to read
     * @param encoding the encoding to be used
     * @return The string representation of the inputstreams content
     * @throws IOException if there was an error while reading the stream
     */
    public static String readStream(InputStream is, String encoding) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, encoding));
            while ((line = br.readLine()) != null) {
                content.append(line).append('\n');
            }
        } finally {
            closeIfNotNull(br);
        }
        return content.toString();
    }

    /**
     * Closes the passed in resource if not null.
     *
     * @param closeable The resource that needs to be closed.
     */
    public static void closeIfNotNull(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
}
