/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ReopeningFileOutputStream.java,v 1.2 2010/01/27 01:19:24 pbryan Exp $
 *
 * Copyright 2009–2010 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream for writing data to a file, reopening the file as necessary.
 * <p>
 * This class provides the same basic functionality as a
 * {@link FileOutputStream}, with the exception that it can detect the removal
 * of the underlying file, and reopen for writing as necessary. This is useful
 * for logging for example, where a log file may be removed in automatic log
 * rotation (as opposed to being truncated).
 * <p>
 * Note: checking for the presence of the file for each write operation adds
 * overhead and depends on the efficiency of the operating system platform it
 * is deployed on. Writing arrays of bytes is far more efficient than writing
 * individual bytes.
 *
 * @author Paul C. Bryan
 */
public class ReopeningFileOutputStream extends OutputStream
{
    /** Underlying file output stream to write to. */
    private OutputStream out;

    /** Specifies whether existing file contents be appened to. */
    private boolean append = false;

    /** The file to be opened for writing. */
    private File file;

    /**
     * Creates an output file stream to write to the file with the specified
     * name.
     *
     * @param name the system-dependent filename.
     * @throws IOException if the file could not be opened for writing due to an I/O exception.
     * @throws SecurityException if the file could not be opened for writing due to a security exception.
     */
    public ReopeningFileOutputStream(String name) throws IOException {
        this(name != null ? new File(name) : null);
    }

    /**
     * Creates an output file stream to write to the file with the specified
     * name, specifying if existing file contents should be appended to or not.
     *
     * @param name the system-dependent filename.
     * @param append specifies whether existing file contents be appened to.
     * @throws IOException if the file could not be opened for writing due to an I/O exception.
     * @throws SecurityException if the file could not be opened for writing due to a security exception.
     */
    public ReopeningFileOutputStream(String name, boolean append) throws IOException {
        this(name != null ? new File(name) : null, append);
    }

    /**
     * Creates a reopening file output stream to write to the file represented
     * by the specified File object.
     *
     * @param file the file to be opened for writing.
     * @throws IOException if the file could not be opened for writing due to an I/O exception.
     * @throws SecurityException if the file could not be opened for writing due to a security exception.
     */
    public ReopeningFileOutputStream(File file) throws IOException {
        this(file, false);
    }

    /**
     * Creates a reopening file output stream to write to the file represented
     * by the specified File object, specifying if existing file contents should
     * be appended to or not.
     *
     * @param file the file to be opened for writing.
     * @param append specifies whether existing file contents be appened to.
     * @throws IOException if the file could not be opened for writing due to an I/O exception.
     * @throws SecurityException if the file could not be opened for writing due to a security exception.
     */
    public ReopeningFileOutputStream(File file, boolean append) throws IOException {
        this.file = file;
        this.append = append;
        open();
    }

    /**
     * Checks if the file exists in the filesystem. If not, the currently open
     * file is closed, and a new one is reopened for writing.
     *
     * @throws IOException if an I/O exception occurs.
     */
    private void exists() throws IOException {
        if (!file.exists()) {
            open();
        }
    }

    /**
     * Opens the file for writing. If a file was previously opened, it is closed
     * first.
     *
     * @throws IOException if an I/O exception occurs.
     */
    private synchronized void open() throws IOException {
        close();
        out = new FileOutputStream(file, append);
    }

    @Override
    public void write(int b) throws IOException {
        exists();
        out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        exists();
        out.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        exists();
        out.write(b, off, len);
    }

    @Override
    public synchronized void close() throws IOException {
        if (out != null) {
            out.close();
        }
        out = null;
    }
}
