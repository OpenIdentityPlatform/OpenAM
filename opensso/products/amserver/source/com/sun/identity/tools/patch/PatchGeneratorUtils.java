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
* $Id: PatchGeneratorUtils.java,v 1.2 2008/09/04 22:26:12 kevinserwin Exp $
*/


package com.sun.identity.tools.patch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;


public class PatchGeneratorUtils {
    
    /**
     * Get a file object from the file system.
     *
     * @param path the path of the file
     * @return The file object the path indicate
     */
    
    public static File getFile(String path) throws FileNotFoundException{
        File file = new File(path);
        if (file.exists() && file.isFile()){
            return file;
        } else {
            throw new FileNotFoundException(path);
        }
    }
    
    /**
     * Load manifest information from a war file
     *
     * @param warFile the file object which contains the manifest information
     * @param manifestPattern the pattern of the name of the manifest file
     * @param wildCard a character which is used as a wild card in the pattern
     * @return a properties object contains the manifest information
     */
    
    public static Properties getManifest(JarFile warFile,
        String manifestPattern, char wildCard) throws Exception{
        Properties manifest = new Properties();
        for (Enumeration entries = warFile.entries();
            entries.hasMoreElements();) {
            JarEntry entry = (JarEntry)entries.nextElement();
            if ((!entry.isDirectory()) &&
                isMatch(entry.getName(), manifestPattern, wildCard)) {
                InputStream warIn = null;
                try {
                    warIn = warFile.getInputStream(entry); 
                    manifest.load(warIn);
                } finally {
                    try{
                        warIn.close();
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
        return manifest;
    }
    
    /**
     * Load manifest information from a JarInputStream
     *
     * @param in the file stream which contains the manifest information
     * @param manifestPattern the pattern of the name of the manifest file
     * @param wildCard a character which is used as a wild card in the pattern
     * @return a properties object contains the manifest information
     */
    
    public static Properties getManifest(JarInputStream in,
        String manifestPattern, char wildCard) throws Exception {
        Properties manifest = new Properties();
        JarEntry entry = null;
        while((entry = in.getNextJarEntry()) != null) {
            if ((!entry.isDirectory()) &&
                isMatch(entry.getName(), manifestPattern, wildCard)) {
                try {
                    manifest.load(in);
                } finally {
                    try{
                        in.closeEntry();
                    } catch (Exception ignored) {}
                }
                break;
            }
        }
        return manifest;
    }
    
    /**
     * Get the input stream represented by entry name from a JarInputStream 
     *
     * @param jin the JarInputStream which contains the entry
     * @param entryName the pattern of the entry name
     * @return the input stream point to the jar entry indicated by entryName
     */
    
    public static JarInputStream getCorrectInputStream(JarInputStream jin,
        String entryName) throws Exception {
        if ((entryName == null) || (entryName.length() == 0)) {
            return jin;
        }
        JarEntry entry = null;
        while ((entry = jin.getNextJarEntry()) != null) {
            if (!entry.isDirectory()) {
                String tempName = entry.getName();
                if (entryName.equals(tempName)) {
                    return new JarInputStream(jin);
                } else {
                    if (entryName.startsWith(tempName)) {
                        return getCorrectInputStream(new JarInputStream(jin),
                            entryName.substring(tempName.length() + 1,
                            entryName.length()));
                    }
                }
            }            
        }
        return null;
    }
    
    /**
     * Check if the string matches the string pattern
     *
     * @param actualString the string is going to be checked
     * @param pattern the pattern of the string we are looking for
     * @param wildCard a character which is used as a wild card in the pattern
     * @return a boolean to indicate whether the string matches the pattern
     */
    
    public static boolean isMatch(String actualString, String pattern,
        char wildCard){
        String tempPattern = pattern.trim();
        int matchOffset = 0;
        boolean matched = true;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < tempPattern.length(); i++) {
            if (tempPattern.charAt(i) != wildCard) {
                buffer.append(tempPattern.charAt(i));
            }
            if ((i == (tempPattern.length() - 1)) || (tempPattern.charAt(i)
                == wildCard)) {
                if (buffer.length() > 0) {
                    while(matchOffset < actualString.length()) {
                        int matchedIndex = actualString.indexOf(
                            buffer.toString(), matchOffset);
                        if (matchedIndex >= matchOffset) {
                            if (i != (tempPattern.length() - 1)) {
                                matchOffset = matchedIndex + buffer.length();
                            } else {
                                if (tempPattern.charAt(i) != wildCard) {
                                    if (actualString.substring(
                                        matchedIndex).length() !=
                                        buffer.length()) {
                                        matchOffset = matchedIndex + 1;
                                        continue;    
                                    }
                                }
                            }
                        } else {
                            matched = false;
                            break;
                        }
                        break;
                    }
                    buffer = new StringBuffer();
                }
            }
        }
        return matched;
    }
}
