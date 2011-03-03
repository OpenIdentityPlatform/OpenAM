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
 * $Id: ByteArrayRecord.java,v 1.4 2009/10/29 20:32:42 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implementation of a record, storing data in a byte array.
 *
 * @author Paul C. Bryan
 */
public class ByteArrayRecord implements Record
{
    /** The initial length of the byte array. */
    private static final int INITIAL_LEN = 8192;

    /** Current read/write position within the record. */
    private int pos = 0;

    /** Current length of the record. */
    private int len = 0;

    /** Byte array storage for the record. */
    private byte[] data;

    /** The length limit of the record. */
    private int limit;

    /**
     * Creates a new byte array record.
     *
     * @param limit the length limit of the record, after which an {@link OverflowException} will be thrown.
     */
    public ByteArrayRecord(int limit) {
        data = new byte[Math.min(INITIAL_LEN, limit)];
        this.limit = limit;
    }

    /**
     * Returns the record data as a byte array.
     *
     * @return the record data.
     */
    public byte[] bytes() {
        return Arrays.copyOf(data, len);
    }

    /**
     * Closing a byte array record has no effect. The methods in this class can
     * be called after the record has been closed without generating an
     * IOException.
     */
    @Override
    public void close() throws IOException {
    }

    @Override
    public int length() throws IOException {
        return len;
    }

    @Override
    public int limit() throws IOException {
        return limit;
    }

    @Override
    public int position() throws IOException {
        return pos;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (len > 0) {
            len = Math.min(this.len - pos, len);
            if (len == 0) {
                return -1;
            }
            System.arraycopy(data, pos, b, off, len);
            pos += len;
        }
        return len;
    }

    @Override
    public synchronized void seek(int pos) throws IOException {
        if (pos > len) {
            throw new IOException("requested position greater than record length");
        }
        this.pos = pos;
    }

    @Override
    public synchronized int skip(int n) throws IOException {
        int prev = pos;
        pos = Math.max(Math.max(pos + n, len), 0);
        return pos - prev;
    }

    @Override
    public synchronized void truncate(int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        if (len > this.len) {
            throw new IOException("cannot increase length of record via truncate method");
        }
        this.len = len;
        if (len > pos) {
            pos = len;
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException, OverflowException {
        int end = pos + len;
        if (end > limit) {
            throw new OverflowException();
        }
        if (data.length < end) { // must grow buffer
            data = Arrays.copyOf(data, Math.max(Math.min(data.length << 1, limit), end));
        }
        System.arraycopy(b, off, data, pos, len);
        pos += len;
        this.len = Math.max(this.len, end);
    }
}
