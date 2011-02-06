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
 * $Id: RecordOutputStream.java,v 1.1 2009/10/22 01:18:24 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Exposes a record as a writable output stream.
 *
 * @author Paul C. Bryan
 */
public final class RecordOutputStream extends OutputStream
{
    /** The record to wrap with the output stream. */
    private Record record;

    /**
     * Creates a new record output stream.
     *
     * @param record the record to wrap with the output stream.
     */
    public RecordOutputStream(Record record) {
        this.record = record;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] bb = new byte[1];
        bb[0] = (byte)b;
        record.write(bb, 0, 1);
    }

    @Override
    public void write(byte b[]) throws IOException {
        record.write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        record.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        record.close();
    }
}
