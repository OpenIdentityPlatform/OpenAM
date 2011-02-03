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
 * $Id: TemporaryStorage.java,v 1.2 2009/10/22 01:18:24 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.File;
import java.io.IOException;

/**
 * Allocates temporary records. Temporary records begin backed by
 * {@link ByteArrayRecord}, and are automagically upgraded to a
 * {@link FileRecord} when the array length limit is exceeded.
 *
 * @author Paul C. Bryan
 */
public class TemporaryStorage implements Storage
{
    /**
     * The directory where temporary files are to be created. If this variable
     * is <tt>null</tt> then the system-dependent default temporary directory
     * will be used. Default: <tt>null</tt>.
     *
     * @see java.io.File#createTempFile(String, String, File)
     */
    public File directory = null;

    /**
     * The length limit of a temporary record, after which an
     * {@link OverflowException} will be thrown on a call to the record's
     * <tt>write</tt> method. Default: 1 MiB.
     */
    public int recordLimit = 1 * 1024 * 1024;

    /**
     * The length limit of the temporary record's {@link ByteArrayRecord},
     * after which it is automatically upgraded to a {@link FileRecord}.
     * Default: 64 KiB.
     */
    public int arrayLimit = 64 * 1024;

    /**
     * Creates a new temporary storage object.
     */
    public TemporaryStorage() {
    }

    @Override
    public String create() throws IOException {
        return "NCC-1701"; // identifiers are irrelevant for temporary records
    }

    @Override
    public String create(String key) throws IOException {
        return create(); // identifiers are irrelevant for temporary records
    }

    @Override
    public Record open(String id) throws IOException {
        return new TemporaryRecord(directory, recordLimit, arrayLimit);
    }

    @Override
    public boolean exists(String id) throws IOException {
        return true;
    }

    @Override
    public void remove(String id) throws IOException {
        // temporary records are automatically removed when closed
    }
}
