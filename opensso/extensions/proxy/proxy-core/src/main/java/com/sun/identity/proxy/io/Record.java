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
 * $Id: Record.java,v 1.2 2009/10/15 07:07:58 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.IOException;

/**
 * Provides read-write storage for data. Records can grow on demand, typically
 * up to a specified limit.
 *
 * @author Paul C. Bryan
 */
public interface Record
{
    /**
     * Closes the record and releases any system resources associated with it.
     * If the record is already closed then invoking this method has no effect.
     *
     * @throws IOException if an I/O exception occurs.
     */
    public void close() throws IOException;

    /**
     * Returns the current length of the record.
     *
     * @return the length of the record
     * @throws IOException if an I/O exception occurs.
     */
    public int length() throws IOException;

    /**
     * Returns the limit of the number of bytes the record can store, or -1
     * if the limit is indefinite or otherwise unknown.
     *
     * @return the storage limit of the record, or -1 if not known.
     * @throws IOException if an I/O exception occurs.
     */
    public int limit() throws IOException;

    /**
     * Returns the current read/write position within the record.
     *
     * @return the current position within the record.
     * @throws IOException if an I/O exception occurs.
     */
    public int position() throws IOException;

    /**
     * Reads up to <tt>len</tt> bytes of data from the record at the current
     * position into an array of bytes. An attempt is made to read as many as
     * <tt>len</tt> bytes, but a smaller number may be read. The number of
     * bytes actually read is returned, or -1 if there are no more bytes to
     * read.
     *
     * @param b the byte array into which the data is read.
     * @param off the start offset in array <tt>b</tt> at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data.
     * @throws IOException if an I/O exception occurs.
     */
    public int read(byte[] b, int off, int len) throws IOException;

    /**
     * Sets the read/write position within the record. If a negative position
     * or position greater than current length is specified an
     * {@link IOException} is thrown.
     *
     * @param pos position within record to seek to.
     * @throws IOException if an I/O exception occurs.
     */
    public void seek(int pos) throws IOException;

    /**
     * Attempts to skip over a specified number of bytes of data from this
     * record, adjusting the current position accordingly. This method may end
     * up skipping over a smaller number of bytes, including 0. The actual
     * number of bytes skipped is returned.
     *
     * @param n the number of bytes requested to be skipped.
     * @return the actual number of bytes that were skipped.
     * @throws IOException if an I/O exception occurs.
     */
    public int skip(int n) throws IOException;

    /**
     * Truncates the record to the specified length. If length greater than
     * current length is specified, an {@link IOException} is thrown. If the
     * read/write position was within the truncated section, it is set to
     * the end of the record.
     *
     * @param len the new length of the record.
     * @throws IOException if an I/O exception occurs.
     */
    public void truncate(int len) throws IOException;

    /**
     * Attempts to write bytes of the requested length from the specified byte
     * array starting at the specified offset to the record. The record can
     * grow to accomodate a write, up to its established limit. If writing to
     * the record would exceed its limit, an {@link OverflowException} is
     * thrown.
     *
     * @param b the byte array containing the data to be written.
     * @param off the start offset in array <tt>b</tt> at which the data is written.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O exception occurs.
     * @throws OverflowException if the operation would cause the record to exceed its length limit.
     */
    public void write(byte[] b, int off, int len) throws IOException, OverflowException;
}

