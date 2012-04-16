/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: CopyUtils.java,v 1.3 2008/09/22 20:49:28 kevinserwin Exp $
 *
 */

package com.sun.identity.tools.bundles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class CopyUtils {

    protected static int BUFFER_SIZE = 8192;
    
    private CopyUtils() {
    }
    
    /**
     * Copies file from source to destination.
     *
     * The destination directories will be created if it is not in the system
     *
     * @param sourceFile The source file object.
     * @param destFile The destination file object.
     * @param overwrite The flat to indicate whether to overwrite.
     * @param preserveLastModified The flat to preserve modified date
     */
    public static void copyFile(File sourceFile, File destFile,
        boolean overwrite, boolean preserveLastModified) throws IOException {
        copyFile(sourceFile, destFile, null, overwrite, preserveLastModified);
    }

    /**
     * Copies file from source to destination.
     *
     * The destination directories will be created if it is not in the system
     *
     * @param sourceFile The source file object.
     * @param destFile The destination file object.
     * @param tokens The map to replace the file contents.
     * @param overwrite The flat to indicate whether to overwrite.
     * @param preserveLastModified The flat to preserve modified date
     */
    
    public static void copyFile(File sourceFile, File destFile,
        Properties tokens, boolean overwrite, boolean preserveLastModified)
        throws IOException {
        if (overwrite || (!destFile.exists()) ||
            (destFile.lastModified() < sourceFile.lastModified())) {
            if (destFile.exists() && destFile.isFile()) {
                destFile.delete();
            }
            File parent = destFile.getParentFile();
            if ((parent != null) && (!parent.exists())) {
                parent.mkdirs();
            }
            if (sourceFile.isDirectory()) {
                destFile.mkdirs();
                return;
            }
            String line = null;
            if ((tokens != null) && (!tokens.isEmpty())) {
                BufferedReader in = new BufferedReader(new
                    FileReader(sourceFile));
                BufferedWriter out = new BufferedWriter(new
                    FileWriter(destFile));
                try {
                    while ((line = in.readLine()) != null) {
                        if (line.length() > 0) {
                            Enumeration tokensName = tokens.propertyNames();
                            while (tokensName.hasMoreElements()) {
                                String name = (String) tokensName.nextElement();
                                if (line.indexOf(name) >= 0) {
                                    line = line.replaceAll(name,
                                        tokens.getProperty(name));
                                }
                            }
                            out.write(line);
                            out.newLine();
                        } else {
                            out.newLine();
                        }
                    }
                } finally {
                    out.close();
                    in.close();
                }
            } else {
                FileInputStream in = null;
                FileOutputStream out = null;
                try {
                    in = new FileInputStream(sourceFile);
                    out = new FileOutputStream(destFile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count = 0;
                    while ((count = in.read(buffer, 0, buffer.length)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } finally {
                    out.close();
                    in.close();
                }
            }
            if (preserveLastModified) {
                destFile.setLastModified(sourceFile.lastModified());
            }
        }
    }
    
    /**
     * Copies file from source to destination.
     *
     * The destination directories will be created if it is not in the system
     *
     * @param in The input stream object..
     * @param destFile The destination file object.
     * @param overwrite The flat to indicate whether to overwrite.
     */
    
    public static void copyFileFromJar(InputStream in, File destFile,
        boolean overwrite)
        throws IOException {
        
        if (overwrite || (!destFile.exists())) {
            if (destFile.exists() && destFile.isFile()) {
                destFile.delete();
            }
            File parent = destFile.getParentFile();
            if ((parent != null) && (!parent.exists())) {
                parent.mkdirs();
            }

            String line = null;

                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(destFile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count = 0;
                    while ((count = in.read(buffer, 0, buffer.length)) != -1) {
                        out.write(buffer, 0, count);
                    }
                } finally {
                    out.close();
                    in.close();
                }
            
        }
        
    }   
    
}
