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
 * $Id: CachedInputStream.java,v 1.1 2009/10/22 01:18:23 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * Wraps an input stream, caching read data and allows rewinding to the
 * beginning of the stream.
 * <p>
 * This class is useful in cases where request entities need to be re-read
 * (e.g. retrying requests after authentication) and where responses need to
 * be partially read before relaying to the remote client (e.g.
 * parsing to detect presence or absence of session state).
 * <p>
 * When no additional data needs to be cached, the {@link #stop} method should
 * be called. After this, all subsequent requests for data are passed-through
 * to the underlying stream without storing data.
 *
 * @author Paul C. Bryan
 */
public class CachedInputStream extends InputStream
{
    /** Indicates whether this object is (still) caching content. */
    private boolean caching = true;

    /** The record to store cached data in. */
    private Record record;

    /** The input stream to wrap and cache. */
    private InputStream in;

    /** The position in the record of the start of cached input. */
    private int start;

    /**
     * Creates a new cached stream, wrapping the specified input stream and
     * storing cached data in the specified record.
     *
     * @param in the input stream to wrap and cache.
     * @param record the record to store cached data in.
     * @throws IOException if an I/O exception occurs.
     */
    public CachedInputStream(InputStream in, Record record) throws IOException {
        this.in = in;
        this.record = record;
        this.start = record.position();
    }

    /**
     * Returns <tt>true</tt> if the current position is within the cached
     * data.
     *
     * @return <tt>true</tt> if position is within cache.
     * @throws IOException if an I/O exception occurs.
     */
    private boolean isPositionInCache() throws IOException {
        return (record.position() < record.length());
    }

    @Override
    public int available() throws IOException {
        int n;
        n = record.length() - record.position(); // check cache record first
        if (n == 0) { // nothing in cache record; use underlying stream
            n = in.available();
        }
        return n;
    }

    @Override
    public void close() throws IOException
    {
        if (record != null) {
            record.close();
            record = null;
        }

        if (in != null) {
            in.close();
            in = null;
        }
    }

    @Override
    public void finalize() {
        try {
            close();
        }
        catch (IOException ioe) {
        }
    }

    /**
     * Has no effect, as mark/reset are not supported by this stream.
     */
    @Override
    public void mark(int readlimit) {
    }

    /**
     * @return <code>false</code> unconditionally.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        return (read(b, 0, 1) > 0 ? b[0] : -1);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * @throws OverflowException if reading data results in overflowing the cache.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || (b != null && len > b.length - off)) {
            throw new IndexOutOfBoundsException();
        }
        if (isPositionInCache()) {
            return record.read(b, off, len); // read from cache
        }
        int n = in.read(b, off, len);
        if (n > 0 && caching) {
            record.write(b, off, n);
        }
        return n;
    }

    /**
     * @throws IOException unconditionally.
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    /**
     * Sets the read position of the stream to the beginning.
     *
     * @return a reference to this object.
     * @throws IOException if an I/O exception occurs.
     */
    public CachedInputStream rewind() throws IOException {
        if (!caching) {
            throw new IOException("caching has been stopped");
        }
        record.seek(start);
        return this;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) { // per interface contract
            return 0;
        }
        int i = (int)Math.min(n, Integer.MAX_VALUE);
        if (isPositionInCache()) {
            return record.skip(i);
        }
        else if (!caching) {
            return in.skip(n); // can safely use the long value
        }
        else {
            return Streamer.stream(in, record, i); // just cache it
        }
    }

    /**
     * Signals that further reads of the underlying stream should not be
     * cached.
     */
    public void stop() {
        caching = false;
    }
}
