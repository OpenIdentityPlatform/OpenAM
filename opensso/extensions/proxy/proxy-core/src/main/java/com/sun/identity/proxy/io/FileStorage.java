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
 * $Id: FileStorage.java,v 1.1 2009/10/22 01:18:23 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.io;

import java.io.File;
import java.io.IOException;

/**
 * Stores records as files in a filesystem.
 * <p>
 * This class does not check to see if files opened or removed were actually
 * created by this class. Therefore, any arbitrary file in the filesystem can
 * be opened using the {@link #open} method.
 *
 * @author Paul C. Bryan
 */
public class FileStorage implements Storage
{
    /** The directory where new files are created. */
    public File directory = null;

    /** Default file extension for newly created files. */
    public String suffix = ".record";

    /** Limit the length limit of records, after which an {@link OverflowException} will be thrown. */
    public int limit = -1;

    /**
     * Creates a new file storage object.
     * <p>
     * If the specified directory is <tt>null</tt>, then the system-dependent
     * default temporary-file directory will be used.
     *
     * @param directory the directory where new files are created.
     * @throws IOException if an I/O exception occurs.
     */
    public FileStorage(File directory) throws IOException {
        if (directory == null) {
            directory = new File(System.getProperty("java.io.tmpdir"));
        }
        if (!directory.isDirectory()) {
            throw new IOException("directory is not a directory");
        }
        this.directory = directory;
    }

    /**
     * Creates a new file storage object.
     * <p>
     * If the specified directory is <tt>null</tt>, then the system-dependent
     * default temporary-file directory will be used.
     *
     * @param directory the directory where new files are created.
     * @throws IOException if an I/O exception occurs.
     */
    public FileStorage(String directory) throws IOException {
        this(directory != null ? new File(directory) : null);
    }

    /**
     * Creates a new file with a storage-selected default filename.
     */
    @Override
    public String create() throws IOException {
        return File.createTempFile("file", suffix, directory).toString();
    }

    /**
     * Creates a new file with with suggested key as filename.
     */
    @Override
    public String create(String key) throws IOException {
        if (key != null) {
            File f = new File(directory, key + (suffix != null ? suffix : ".tmp"));
            try {
                if (f.createNewFile()) {
                    return f.toString();
                }
            }
            catch (IOException ioe) {
            }
        }
        return create(); // if all else fails, use default
    }

    @Override
    public Record open(String id) throws IOException {
        return new FileRecord(new File(id), limit);
    }

    @Override
    public boolean exists(String id) throws IOException {
        return new File(id).exists();
    }

    @Override
    public void remove(String id) throws IOException {
        new File(id).delete();
    }
}
