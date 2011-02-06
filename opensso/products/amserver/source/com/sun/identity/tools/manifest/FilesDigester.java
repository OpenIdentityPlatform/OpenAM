/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: FilesDigester.java,v 1.2 2008/09/04 22:26:12 kevinserwin Exp $
*/


package com.sun.identity.tools.manifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.Properties;

public class FilesDigester implements ManifestConstants {
    
    protected LinkedList includePattern;
    protected LinkedList excludePattern;
    protected char wildCard;
    protected boolean recursive;
    
    /**
     * FileDigester constructor
     *
     * @param includePattern A list of patterns of file name should be included.
     * @param excludePattern A list of patterns of file name should be excluded.
     * @param wildCard The wildcard character which is used in the pattern.
     */
    
    public FilesDigester(LinkedList includePattern, LinkedList excludePattern,
        char wildCard, boolean recursive) {
        this.includePattern = includePattern;
        this.excludePattern = excludePattern;
        this.wildCard = wildCard;
        this.recursive = recursive;
    }
    
    /**
     * This function calculate the hash value of a war file.
     *
     * This function will calculate the manifest according to the decompressed
     * files contained in the war file.
     *
     * @param hashAlg The algorithm to be used for calculating the hash.
     * @param digestResult The Properties to store the results.
     * @param wfile The war file to be processed.
     * @param intoJar The flat to indicate whether to handle jar file by using
     *        its decompressed contents.
     */
    
    protected void digestWarFile(String hashAlg, Properties digestResult,
        JarFile wfile, boolean intoJar){
        Enumeration wEnum = wfile.entries();
        byte[] digestCode = null;
        String wename = null;
        InputStream in = null;
        try {            
            while (wEnum.hasMoreElements()) {
                JarEntry we = (JarEntry) wEnum.nextElement();
                if (!we.isDirectory()) {
                    wename=we.getName();
                    if (wename.endsWith(JAR_FILE_EXT) && (intoJar)) {
                        in=wfile.getInputStream(we);
                        digestCode = digestJarFile(hashAlg, in);
                        in.close();
                    } else{
                        in = wfile.getInputStream(we);
                        digestCode = Utils.getHash(hashAlg, in);
                        in.close();
                    }
                    appendResult(digestResult, wename, digestCode);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
                in = null;
            }
        }
    }
    
    /**
     * This function calculate the hash value of a jar file.
     *
     * This function handles the jar file as a concatenation of the decompressed
     * files it contains.
     *
     * @param hashAlg The algorithm to be used for calculating the hash.
     * @param in The InputStream of the jar file to be processed.
     */
    
    protected byte[] digestJarFile(String hashAlg, InputStream in){
        JarInputStream jin = null;
        try {
            jin=new JarInputStream(in);
            JarEntry je = null;
            MessageDigest md = MessageDigest.getInstance(hashAlg);
            while ((je=jin.getNextJarEntry()) != null) {
                if (!je.isDirectory()) {
                    md = Utils.hashing(md, jin);
                }
                jin.closeEntry();
            }
            jin.close();
            return md.digest();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } finally {
            if (jin != null) {
                try {
                    jin.close();
                } catch (IOException ignored) {
                }
                jin = null;
            }
        }
        return null;
    }
    
    /**
     * This function calculate the hash value of a file.
     *
     * @param hashAlg The algorithm to be used for calculating the hash.
     * @param file The file to be processed.
     * @param digestResult The Properties to store the results.
     * @param ignoredPath The parent's path to ignore when printing the 
     *        manifest entries.
     * @param intoJar The flag to indicate whether to specially handle 
     *        jar file.
     * @param intoWar The flag to indicate whether to specially handle 
     *        war file.
     */
    
    public void digest(String hashAlg, File file, Properties digestResult,
        String ignoredPath, boolean intoJar, boolean intoWar){
        if (file.exists()) {
            if (file.isDirectory()) {
                if (recursive) {
                    File[] tempFiles = null;
                    if (includePattern != null) {
                        tempFiles = file.listFiles(new GeneralFileFilter(
                            includePattern));
                    } else{
                        tempFiles = file.listFiles();
                    }
                    for (int i = 0; i < tempFiles.length; i++) {
                        if (tempFiles[i].isDirectory()) {
                            digest(hashAlg, tempFiles[i], digestResult,
                                ignoredPath, intoJar, intoWar);
                        } else{
                            if (excludePattern != null) {
                                if (!Utils.isMatch(tempFiles[i].getName(),
                                    excludePattern, wildCard)) {
                                    digest(hashAlg, tempFiles[i], digestResult,
                                        ignoredPath, intoJar, intoWar);
                                }
                            } else{
                                digest(hashAlg, tempFiles[i], digestResult, 
                                    ignoredPath, intoJar, intoWar);
                            }
                        }
                    }
                }
            } else{
                if (file.getName().endsWith(WAR_FILE_EXT) && (intoWar)) {
                    try {
                        digestWarFile(hashAlg, digestResult, new JarFile(file),
                            intoJar);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else{
                    byte[] digestedbyte = null;
                    if ((file.getName().endsWith(JAR_FILE_EXT)) && (intoJar)) {
                        FileInputStream fin = null;
                        try {
                            fin = new FileInputStream(file);
                            digestedbyte = digestJarFile(hashAlg, fin);
                            fin.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } finally {
                            if (fin != null) {
                                try {
                                    fin.close();
                                } catch (IOException ignored) {
                                }
                                fin = null;
                            }
                        }
                    } else{
                        FileInputStream fin = null;
                        try {
                            fin = new FileInputStream(file);
                            digestedbyte = Utils.getHash(hashAlg, fin);                        
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } finally {
                            if (fin != null) {
                                try {
                                    fin.close();
                                } catch (IOException ignored) {
                                }
                                fin = null;
                            }
                        }
                    }
                    String tempPath=file.getPath();
                    tempPath=tempPath.substring(tempPath.indexOf(ignoredPath) +
                        ignoredPath.length()).replaceAll("\\\\",
                        FILE_SEPARATOR);
                    if (tempPath.startsWith(FILE_SEPARATOR)) {
                        tempPath=tempPath.substring(1);
                    }
                    appendResult(digestResult, tempPath, digestedbyte);
                }    
            }
        }
    }
    
    /**
     * This function append the result to the StringBuffer.
     *
     * @param result The properties to store the results.
     * @param path The path of the entry.
     * @param digestedbyte The byte array which contains the digested result.
     */
    
    protected void appendResult(Properties result, String path,
        byte[] digestedbyte){
        result.setProperty(path, Utils.translateHashToString(digestedbyte));
    }
    
}
