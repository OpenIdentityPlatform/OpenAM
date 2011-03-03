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
 * $Id: TemporaryRecord.java,v 1.1 2009/10/15 07:08:28 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.File;
import java.io.IOException;

/**
 * Stores data in a byte array, automagically upgrading to a temporary file
 * when the array length limit is exceeded.
 *
 * @author Paul C. Bryan
 */
public class TemporaryRecord implements Record
{
    /** The directory to store temporary files in. */
    private File directory;

    /** The record that this record is wrapping. */
    private Record record;

    /** The temporary file that backs the file record (if any). */
    private File tmpfile = null;

    /** The length limit of the record. */
    private int limit;

    /**
     * Creates a new temporary record.
     * <p>
     * If the <tt>directory</tt> argument is <tt>null</tt>, then the
     * system-dependent default temporary directory will be used.
     *
     * @param directory the directory to store temporary files in.
     * @param recordLimit the length limit of the record, after which an {@link OverflowException} will be thrown.
     * @param arrayLimit the limit of byte array record before promoting to a file record.
     * @see java.io.File#createTempFile(String, String, File)
     */
    public TemporaryRecord(File directory, int recordLimit, int arrayLimit) {
        this.directory = directory;
        limit = recordLimit;
        this.record = new ByteArrayRecord(arrayLimit);
    }

    /**
     * Used to ensure that the record is not closed when operations are
     * performed on it.
     *
     * @throws IOExcepton if record has been closed.
       */
    private Record record() throws IOException {
        if (record == null) {
            throw new IOException("record has been closed");
        }
        return record;
    }
    /**
     * Attempts to upgrade from one record type to the next.
     *
     * @throws IOException if an I/O exception occurs.
     * @throws OverflowException if there is no further upgrade path.
     */
    private void upgrade() throws IOException, OverflowException {
        if (record instanceof ByteArrayRecord) { // can upgrade to file
            ByteArrayRecord ba = (ByteArrayRecord)record;
            tmpfile = File.createTempFile("proxy", null, directory);
            record = new FileRecord(tmpfile, limit);
            Streamer.stream(ba, record);
        }
        else { // no further upgrade path for record
            throw new OverflowException();
        }
    }

    @Override
    public void close() throws IOException {
        if (record != null) {
            record.close();
            record = null;
        }
        if (tmpfile != null) {
            tmpfile.delete();
            tmpfile = null;
        }
    }

    @Override
    public int length() throws IOException {
        return record().length();
    }

    @Override
    public int limit() throws IOException {
        return record().limit();
    }

    @Override
    public int position() throws IOException {
        return record().position();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return record().read(b, off, len);
    }

    @Override
    public void seek(int pos) throws IOException {
        record().seek(pos);
    }

    @Override    
    public int skip(int n) throws IOException {
        return record().skip(n);
    }

    @Override
    public void truncate(int len) throws IOException {
        record().truncate(len);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException, OverflowException {
        for (int n = 0; n < 2; n++) { // as many passes as there can be upgrades
            try {
                record().write(b, off, len);
                return; // successful write exits loop immediately
            }
            catch (OverflowException oe) {
                upgrade(); // this can throw OverflowException
            }
        }
        throw new IllegalStateException("exceeded the expected number of upgrade passes");
    }
}

