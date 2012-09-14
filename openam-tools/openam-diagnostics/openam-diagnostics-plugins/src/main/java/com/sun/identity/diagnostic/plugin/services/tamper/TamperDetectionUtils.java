/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TamperDetectionUtils.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.tamper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public class TamperDetectionUtils {
    
    public static final String ENTRY_SEPARATOR = "";
    public static final int BUFFER_SIZE = 8192;
    public static final String SHA1 = "SHA1";
    private static byte[] buf = new byte[BUFFER_SIZE];
    
    public static Properties getChecksum(
        String alg, 
        File configDir, 
        Set<String> dirFilter, 
        Set<String> fileFilter
    ) throws Exception {
        ArrayList<File> filesList = new ArrayList<File>();
        TamperDetectionUtils.getRequiredFiles(configDir, dirFilter, fileFilter, 
            filesList);
        String[] fileNames = new String[filesList.size()];
        String absConfigDir = configDir.getAbsolutePath();
        for (int i = 0; i < filesList.size(); i++) {
            String absPath = filesList.get(i).getAbsolutePath();
            fileNames[i] = absPath.substring(absConfigDir.length() + 1, 
                absPath.length()).replaceAll("\\\\", "/");
        }
        Properties checksumList = new Properties();
        for (String filePath : fileNames) {
            File fileEntry = new File(configDir, filePath);
            if (fileEntry.exists() && fileEntry.isFile()) {
                InputStream in = null;
                byte[] hash = null;
                try {
                    in = new FileInputStream(fileEntry);
                    hash = TamperDetectionUtils.getHash(alg, in);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception ignored) {}
                    }
                }
                checksumList.setProperty(filePath, translateHashToString(hash));
            }
        }
        return checksumList;
    }
    
    private static byte[] getHash(String algorithm, InputStream in)
    throws Exception {
        MessageDigest md=MessageDigest.getInstance(algorithm);
        return hashing(md, in).digest();
    }
    
    private static MessageDigest hashing(MessageDigest md, InputStream in)
    throws IOException {
        DigestInputStream din = new DigestInputStream(in, md);
        synchronized(buf){
            while (din.read(buf) != -1);
        }
        return md;
    }
    
    private static String translateHashToString(byte[] hash) {
        StringBuilder hashBuffer = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            hashBuffer.append(Character.forDigit((hash[i] >> 4) & 0x0F, 16));
            hashBuffer.append(Character.forDigit(hash[i] & 0x0F, 16));
        }
        return hashBuffer.toString();
    }
    
    private static void getRequiredFiles(
        File currentRoot, 
        Set<String> dirFilter, 
        Set<String> fileFilter, 
        ArrayList<File> resultFiles
    ) {
        File[] files = currentRoot.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if ((fileFilter == null) || 
                    (!fileFilter.contains(f.getName()))) {
                    resultFiles.add(f);
                }
            } else {
                if (f.isDirectory()) {
                    if ((dirFilter == null) || 
                        (!dirFilter.contains(f.getName()))) {
                        getRequiredFiles(f, dirFilter, fileFilter, resultFiles);
                    }
                }
            }
        }
    }
}
