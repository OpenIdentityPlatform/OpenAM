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
 * $Id: FileRecord.java,v 1.4 2009/10/22 01:18:23 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Implementation of a record, storing data in a file.
 *
 * @author Paul C. Bryan
 */
public class FileRecord implements Record
{
    /** Supports reading and writing data within the file. */
    private RandomAccessFile raf;

    /** The length limit of the record. */
    private int limit;

    /**
     * Creates a new file record.
     *
     * @param file the file to store data in.
     * @param limit the length limit of the record, after which an {@link OverflowException} will be thrown.
     * @throws IOException if an I/O exception occurs.
     */
    public FileRecord(File file, int limit) throws IOException {
        raf = new RandomAccessFile(file, "rw");
        this.limit = limit;
    }

    /**
     * Used to ensure that the record is not closed when operations are
     * performed on it.
     *
     * @return the allocated random access file object.
     * @throws IOExcepton if record has been closed.
     */
    private RandomAccessFile raf() throws IOException {
        if (raf == null) {
            throw new IOException("record has been closed");
        }
        return raf;
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    @Override
    public int length() throws IOException {
        return (int)(raf().length());
    }

    @Override
    public int limit() throws IOException {
        return limit;
    }

    @Override
    public int position() throws IOException {
        return (int)(raf().getFilePointer());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf().read(b, off, len);
    }

    @Override
    public void seek(int pos) throws IOException {
        raf();
        if (raf.getFilePointer() > raf.length()) {
            throw new IOException("requested position greater than record length");
        }
        raf.seek(pos);
    }

    public int skip(int n) throws IOException {
        return raf().skipBytes(n);
    }

    @Override
    public void truncate(int len) throws IOException {
        raf();
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        if (len > raf.length()) {
            throw new IOException("cannot increase length of record via truncate method");
        }
        long pos = raf.getFilePointer();
        raf.setLength(len);
        if (pos > len) {
            pos = len;
        }
        raf.seek(pos);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        raf();
        if (limit >= 0 && raf.getFilePointer() + len > limit) {
            throw new OverflowException();
        }
        raf.write(b, off, len);
    }
}

