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
* $Id: Manifest.java,v 1.3 2009/02/17 18:41:41 kevinserwin Exp $
*/

package com.sun.identity.tools.manifest;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Manifest implements ManifestConstants{
    String manifestName;    
    public File srcFile;
    public File destFile;
    static boolean verbose;
    FilesDigester digester;
    Properties digestResult;
    static char wildCard;
    static LinkedList includePattern = null;
    static LinkedList excludePattern = null;    
    static String headerFilePath;
    static String srcFilePath;
    static String destFilePath;
    static boolean recursive;
    static String includePatternString;
    static String excludePatternString;
    static String wildCardString;
    static String digestAlg;
    boolean intoJar;
    boolean intoWar;
    static boolean overwrite;
    
    public void setDefaultProperties() {
        verbose = Boolean.valueOf(DEFAULT_VERBOSE).booleanValue();
        recursive = Boolean.valueOf(DEFAULT_RECURSIVE).booleanValue();
        digestAlg = SHA1;
        intoJar = Boolean.valueOf(DEFAULT_DIGEST_HANDLEJAR).booleanValue();
        intoWar = Boolean.valueOf(DEFAULT_DIGEST_HANDLEWAR).booleanValue();
        manifestName = DEFAULT_MANIFEST_FILE_NAME;
        overwrite = Boolean.valueOf(DEFAULT_OVERWRITE).booleanValue();
        wildCard = DEFAULT_WILD_CARD;       
    }
    
  
    private void getProperties() {

         headerFilePath = System.getProperty(HEADER_FILE_PATH);
         srcFilePath = System.getProperty(SRC_FILE_PATH);
         destFilePath = System.getProperty(DEST_FILE_PATH);
         verbose = Boolean.valueOf(System.getProperty(VERBOSE_OPTION,
            DEFAULT_VERBOSE)).booleanValue();
         recursive=Boolean.valueOf(System.getProperty(RECURSIVE,
            DEFAULT_RECURSIVE)).booleanValue();
         includePatternString = System.getProperty(INCLUDE_PATTERN);
         excludePatternString = System.getProperty(EXCLUDE_PATTERN);
         manifestName = System.getProperty(MANIFEST_NAME,
            DEFAULT_MANIFEST_FILE_NAME);
         wildCardString = System.getProperty(WILDCARD_CHAR);
         digestAlg=System.getProperty(DIGEST_ALG, SHA1);
         intoJar = Boolean.valueOf(System.getProperty(DIGEST_HANDLEJAR,
            DEFAULT_DIGEST_HANDLEJAR)).booleanValue();
         intoWar = Boolean.valueOf(System.getProperty(DIGEST_HANDLEWAR,
            DEFAULT_DIGEST_HANDLEWAR)).booleanValue();
         overwrite = Boolean.valueOf(System.getProperty(OVERWRITE,
            DEFAULT_OVERWRITE)).booleanValue();
         
        if ((includePatternString != null) &&
            (includePatternString.length() > 0)) {
            includePattern = new LinkedList();
            int offset = 0;
            int index = includePatternString.indexOf(PATTERN_SEPARATOR, offset);
            do{
                if (index > offset) {
                    includePattern.add(includePatternString.substring(offset,
                        index).trim());
                } else{
                    if (offset < includePatternString.length()) {
                        String tempPattern = includePatternString.substring(
                            offset, includePatternString.length()).trim();
                        if (tempPattern.length() > 0) {
                            includePattern.add(tempPattern);
                        }
                    }
                    break;
                }
                offset = index + 1;
                index = includePatternString.indexOf(PATTERN_SEPARATOR, offset);
            } while (true);
        }
        if ((excludePatternString!=null) &&
            (excludePatternString.length() > 0)) {
            excludePattern = new LinkedList();
            int offset = 0;
            int index = excludePatternString.indexOf(PATTERN_SEPARATOR, offset);
            do{
                if (index > offset) {
                    excludePattern.add(excludePatternString.substring(offset,
                        index).trim());
                } else{
                    if (offset < excludePatternString.length()) {
                        String tempPattern = excludePatternString.substring(
                            offset, excludePatternString.length()).trim();
                        if (tempPattern.length() > 0) {
                            excludePattern.add(tempPattern);
                        }
                    }
                    break;
                }
                offset = index + 1;
                index = excludePatternString.indexOf(PATTERN_SEPARATOR, offset);
            } while (true);
        }
        
        
        wildCard = DEFAULT_WILD_CARD;
        if (wildCardString != null) {
           wildCard = wildCardString.trim().charAt(0);
        }
    }
    
    private boolean setSourceFile(String srcFilePath) {
        if (srcFilePath == null) {
            System.out.println("Source file not specified!");
            return false;
        }        
        this.srcFilePath = srcFilePath;      
        srcFile = new File(srcFilePath);
        if (! srcFile.isFile()) {
            System.out.println("Source file not found!");
            return false;
        }    
        return true;
    }
   
    public void setProperties(Properties prop) {
               digestResult = new Properties(prop);
    }
    
    public String getProperty(String propertyName) {
        return digestResult.getProperty(propertyName);
    }
    
    public Enumeration getPropertyNames() {
        return digestResult.propertyNames();
    }
    
    public void removeProperty(String propertyName) {
        digestResult.remove(propertyName);
    }
    
    private  void writeDestFile() {
            destFile = new File(destFilePath);
            BufferedOutputStream fout = null;
            try {
                if (destFile.isDirectory()) {
                    fout = new BufferedOutputStream(new FileOutputStream(
                        new File(destFile, manifestName)));
                } else{
                    if (destFile.isFile() && (!overwrite)) {
                        fout = new BufferedOutputStream(new FileOutputStream(
                            destFile, true));
                    } else{
                        File parentFile = destFile.getParentFile();
                        if ((parentFile != null) && (!parentFile.exists())) {
                            parentFile.mkdirs();
                        }
                        fout = new BufferedOutputStream(new FileOutputStream(
                            destFile));
                    }
                }
                if (headerFilePath != null) {
                    File headerFile = new File(headerFilePath);
                    if ((headerFile.exists()) && (headerFile.isFile())) {
                        BufferedReader fr = new BufferedReader(new
                            FileReader(headerFile));
                        String line;
                        while ((line = fr.readLine()) != null) {
                            fout.write(line.getBytes());
                            fout.write("\n".getBytes());
                        }
                        fr.close();
                    } 
                } else {
                    long currentTimeInMillis = System.currentTimeMillis();
            
                    // Create Date object.
                    Date date = new Date(currentTimeInMillis);
                    //Specify the desired date format
                    String DATE_FORMAT = "yyyyMMddhhmm";
                    //Create object of SimpleDateFormat and pass the desired date format.
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);  
                    
                    fout.write("identifier=(".getBytes());
                    fout.write(sdf.format(date).getBytes());
                    fout.write(")\n".getBytes());
                }
                digestResult.store(fout, "");
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException ignored) {
                    }
                    fout = null;
                }
            }
    }
    
    private void createManifest() {
        digester = new FilesDigester(includePattern,
            excludePattern, wildCard, recursive);
        digestResult = new Properties();
        digester.digest(digestAlg, srcFile, digestResult, srcFilePath, intoJar,
            intoWar);
    }

    public boolean createManifest(String srcFilePath, String destFilePath,
            String headerFilePath, boolean intoJar, boolean intoWar) {
        this.intoJar = intoJar;
        this.intoWar = intoWar;
                
        if (setSourceFile(srcFilePath) == false) {
            return false;
        }

        if (verbose) {
            if (destFilePath != null) {
                System.out.print("Creating Manifest file "+ destFilePath);
            } else {
                System.out.print("Displaying Manifest file ");
            }
            System.out.println(" for "+srcFilePath+"\n");
        }    
        createManifest();

        if (destFilePath != null) {
            this.destFilePath = destFilePath;
            writeDestFile();
        } else {
            long currentTimeInMillis = System.currentTimeMillis();
            
            // Create Date object.
            Date date = new Date(currentTimeInMillis);
            //Specify the desired date format
            String DATE_FORMAT = "yyyyMMddhhmm";
            //Create object of SimpleDateFormat and pass the desired date format.
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            
            digestResult.setProperty("identifier","generated-"+sdf.format(date));
        }
        return true;
    }
    
    public static void main(String[] args) {     
        Manifest m = new Manifest();
        
        m.getProperties();
        m.createManifest(srcFilePath, destFilePath, headerFilePath, 
                true, true);

    }
    
    /**
     * Prints  the usage for using the patch generation utility
     *
     */
    public static void printUsage(PrintStream out){
        System.out.println("Usage: java ARGUMENTS -jar Manifest.jar");
        System.out.println("\nARGUMENTS");
        System.out.println("\t-D\"" + MANIFEST_CREATE_FILE + "=<RTM zip file>\"");
        System.out.println("\tPath of RTM zip file [Required].");
        System.out.println("\n\t-D\"" + LATEST_WAR_FILE +
            "=<latest zip file>\"");
        System.out.println("\tPath of the resulting patch file [Optional].");
        System.out.println("\n\t-D\"" + IDENTIFIER_ENTRY +
            "=<name of the entry which indicate version>\"");
        System.out.println("\tName of the entry in manifest indicate identity " +
            "[Default: " + DEFAULT_IDENTIFIER_ENTRY + "].");
        System.out.println("\n\t-D\"" + MANIFEST_PATTERN +
            "=<pattern of manifest file>\"");
        System.out.println("\tPattern of the manifest file in the war file " +
            "[Default: " + DEFAULT_MANIFEST_PATTERN + "].");
        System.out.println("\n\t-D\"" + MANIFEST_FILE_NAME +
            "=<name of the manifest file in the resulting file>\"");
        System.out.println("\tName of the manifest file " +
            "[Required if " + VERSION_FILE + " is not defined].");
        System.out.println("\n\t-D\"" + WILDCARD_CHAR + 
            "=<char to be used as wildcard>\"");
        System.out.println("\tWild card character [Default: " +
            DEFAULT_WILDCARD_CHAR + "].");
        System.out.println("\n\t-D\"" + VERSION_FILE +
            "=<properties file indicate version>\"");
        System.out.println("\tProperties file indicate the version of patch " +
            "[Optional].");
        System.out.println("\n\t-D\"" + PROPERTIES_FILE +
            "=<propeties file has the above directive defined>\"");
        System.out.println("\tProperties file have above directives defined " +
            "[Optional].");
    }    
    
}
