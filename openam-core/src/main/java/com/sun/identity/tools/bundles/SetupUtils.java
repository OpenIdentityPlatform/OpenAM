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
 * $Id: SetupUtils.java,v 1.7 2009/10/30 21:10:21 weisun2 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.tools.bundles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;

public class SetupUtils implements SetupConstants{

    private SetupUtils() {
    }
    
    /**
     * Loads properties from file.
     *
     * @param fileName The path to the properties file.
     * @return The Properties object of the specified propertoes file.
     */
    
    public static Properties loadProperties(String fileName)
        throws IOException {
        Properties prop = new Properties();
        InputStream propIn = new FileInputStream(fileName);
        try {
            prop.load(propIn);
        } finally {
            propIn.close();
        }
        return prop;
    }

    /**
     * Prompts user for input from system in.
     *
     * @param message The text message to prompt the user.
     * @return The user input.
     */
    
    public static String getUserInput(String message)
        throws IOException {
        String userInput = null;
        BufferedReader in = new BufferedReader(
            new InputStreamReader(System.in));
        System.out.print(message);
        userInput = in.readLine();
        return userInput;
    }

    public static String getUserInput(String message, String def)
        throws IOException {
        String ret = getUserInput(MessageFormat.format(message, def));
        return (ret.trim().length() > 0 ? ret : def);
    }

    /**
     * Gets the map for text replacement.
     *
     * @param bundle The ResourceBundle which contains the tokens' name.
     * @param confProp The properties which may contain the tokens value.
     * @return The properties object as a map for text replacement.
     */
    
    public static Properties getTokens(ResourceBundle bundle,
        Properties confProp) {
        String currentOS = determineOS();
        String tokenString = bundle.getString(currentOS + TOKEN);
        int tokensOffset = 0;
        int commaIndex = tokenString.indexOf(",", tokensOffset);
        String tempVarName = null;
        String tempVarValue = null;
        Properties tokens = new Properties();
        do {
            if (commaIndex > tokensOffset) {
                tempVarName = tokenString.substring(tokensOffset, commaIndex).
                    trim();
                try{
                    tempVarValue = bundle.getString(currentOS + "." +
                        tempVarName);
                } catch (MissingResourceException ex) {
                    tempVarValue = bundle.getString(tempVarName);
                }
                tempVarValue = evaluatePropertiesValue(tempVarValue, confProp);
                if (tempVarValue != null) {
                    tokens.setProperty(tempVarName, tempVarValue);
                }
            } else {
                if (tokensOffset < tokenString.length()) {
                    tempVarName = tokenString.substring(tokensOffset,
                        tokenString.length()).trim();
                    if (tempVarName.length() > 0) {
                        try{
                            tempVarValue = bundle.getString(currentOS + "." +
                                tempVarName);
                        } catch (MissingResourceException ex) {
                            tempVarValue = bundle.getString(tempVarName);    
                        }
                    } else {
                        break;
                    }
                    tempVarValue = evaluatePropertiesValue(tempVarValue,
                        confProp);
                    if (tempVarValue != null) {
                        tokens.setProperty(tempVarName, tempVarValue);
                    }
                }
                break;
            }
            tokensOffset = commaIndex + 1;
            commaIndex = tokenString.indexOf(",", tokensOffset);
        } while (true);
        return tokens;
    }

    /**
     * Lookups and replaces the variables in the string
     *
     * @param value The string may contain variables which need to be replaced.
     * @param lookupProp The properties may contain the real values.
     * @return The String with variables replaced by correct value.
     */
    
    public static String evaluatePropertiesValue(String value,
        Properties lookupProp) {
        if (value == null) {
          return null;
        }
        String returnValue = value;
        if (lookupProp != null) {
            int offset = 0;
            int refPrefix = 0;
            int refSuffix = 0;
            String key = null;
            String realValue = null;
            while (((refPrefix = returnValue.indexOf(VAR_PREFIX)) >= 0)
                && ((refSuffix = returnValue.indexOf(VAR_SUFFIX)) >
                refPrefix)) {
                key = returnValue.substring(refPrefix + VAR_PREFIX.length(),
                    refSuffix);
                if ((lookupProp != null) && (lookupProp.containsKey(key))) {
                    realValue = lookupProp.getProperty(key);
                } else {
                    if (key.equals(BASE_DIR)) {
                        try {
                            realValue = new File(".").getCanonicalPath();
                        } catch (IOException ignored) {
                        }
                    } else {
                        realValue = System.getProperty(key);
                    }
                }
                if (realValue != null) {
                    if (realValue.indexOf("\\") >= 0) {
                        realValue = realValue.replaceAll("\\\\", "/");
                    }
                    returnValue = returnValue.replaceAll(REX_VAR_PREFIX + key +
                        REX_VAR_SUFFIX, realValue);
                }
                //offset = refSuffix + 1;
            }
        }
        return returnValue;
    }

    /**
     * Get the from files list and the to file list.
     *
     * @param fromDir The directory of the source files located
     * @param toDir The directory of the destinated files located
     * @param fromFilePattern The pattern of the name of source files.
     * @param toFilePattern The pattern of the name of destinated files.
     * @param fromFilesList (as return) List of files from the source directory.
     * @param toFilesList (as return) List of files of the destinated directory.
     */
    
    public static void getFiles(File fromDir, File toDir,
        String fromFilePattern, String toFilePattern,
        LinkedList fromFilesList, LinkedList toFilesList) {
        File[] fromFiles = fromDir.listFiles(
            new GeneralFileFilter(fromFilePattern));
        for (int i = 0 ; i < fromFiles.length ; i++) {
            fromFilesList.addLast(fromFiles[ i ]);
            if (fromFiles[ i ].isDirectory()) {
                toFilesList.addLast(new File(toDir, fromFiles[ i ].getName()));
                getFiles(fromFiles[ i ], new File(toDir, fromFiles[ i ]
                    .getName()), fromFilePattern, toFilePattern, fromFilesList,
                    toFilesList);
            } else {
                toFilesList.addLast(new File(toDir,
                    SetupUtils.transformFileName(fromFilePattern,
                    toFilePattern, fromFiles[ i ])));
            }
        }
    }

    /**
     * Determines the current operating system.
     *
     * @return The string to represent the current operating system.
     */
    
    public static String determineOS() {
        if ((OS_ARCH.toLowerCase().indexOf(X86) >= 0) || 
            (OS_ARCH.toLowerCase().indexOf(X64) >= 0)){
            if (OS_NAME.toLowerCase().indexOf(WINDOWS) >= 0) {
                return WINDOWS;
            } else {
                if (OS_NAME.toLowerCase().indexOf(SUNOS) >= 0) {
                    return X86SOLARIS;
                } else {
                    return LINUX;
                }
            }
        } else { 
            if (OS_NAME.toLowerCase().indexOf(AIX) >= 0) {
                return AIX; 
            } else {
                return SOLARIS;
            }
        }
    }

    /**
     * Transform the file name by using string patterns.
     *
     * @param from The pattern of the source file.
     * @param to The pattern of the destinated file.
     * @param file The file to be transform.
     * @return The transformed name of the file.
     */
    
    public static String transformFileName(String from, String to, File file) {
        return transformFileName(from, to, file, DEFAULT_WILD_CARD);
    }

    /**
     * Transform the file name by using string patterns.
     *
     * @param from The pattern of the source file.
     * @param to The pattern of the destinated file.
     * @param file The file to be transform.
     * @param wildCard The wildcard character is used in the string pattern.
     * @return The transformed name of the file.
     */
    
    public static String transformFileName(String from, String to, File file,
        char wildCard) {
        if (file.isDirectory()) {
            return file.getName();
        } else {
            String fileName = file.getName();
            LinkedList tokensToKeep = new LinkedList();
            StringBuilder nameToReturn = new StringBuilder();
            String tempFrom = from.trim();
            String tempTo = to.trim();
            int fileNameOffset = 0;
            boolean matched = true;
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < tempFrom.length(); i++) {
                if (tempFrom.charAt(i) != wildCard) {
                    buffer.append(tempFrom.charAt(i));
                }
                if ((i == (tempFrom.length() - 1)) ||
                    (tempFrom.charAt(i) == wildCard)) {
                    if (buffer.length() > 0) {
                        int matchedIndex = fileName.indexOf(buffer.toString(),
                            fileNameOffset);
                        if (matchedIndex >= fileNameOffset) {
                            if (matchedIndex != fileNameOffset) {
                                tokensToKeep.addLast(fileName.substring(
                                    fileNameOffset, matchedIndex));
                            } else {
                                if ((tempFrom.charAt(i) == wildCard) &&
                                    (i == tempFrom.length() - 1)) {
                                    if (matchedIndex +
                                        buffer.toString().length() <
                                        fileName.length()) {
                                            tokensToKeep.addLast(
                                                fileName.substring(
                                                    matchedIndex +
                                                    buffer.toString().length(),
                                                    fileName.length()));
                                    }
                                }
                            }
                            if (i != (tempFrom.length() - 1)) {
                                fileNameOffset = matchedIndex + buffer.length();
                            } else {
                                if (tempFrom.charAt(i) != wildCard) {
                                    if (fileName.substring(matchedIndex)
                                        .length() != buffer.length()) {
                                        matched = false;
                                        break;
                                    }
                                }
                            }
                        } else {
                            matched = false;
                            break;
                        }
                        buffer = new StringBuffer();
                    }
                }
            }
            if (matched) {
                for (int i = 0; i < tempTo.length(); i++) {
                    if (tempTo.charAt(i) != wildCard) {
                        nameToReturn.append(tempTo.charAt(i));
                    } else {
                        if (tokensToKeep.size() > 0) {
                            nameToReturn.append(tokensToKeep.removeFirst());
                        }
                    }
                }
                return nameToReturn.toString();
            }
        }
        return null;
    }
    
    /**
     * Ungzip the gzip archive from source file to destinated directory.
     *
     * @param srcFile The path to the source gzip archive.
     * @param destDir The destinated directory for the decompression.
     */
    
    public static void ungzip(String srcFile, String destDir) throws
        IOException {
        String tempDestFileName = srcFile.substring(0,
            srcFile.lastIndexOf("."));
        tempDestFileName = tempDestFileName.substring(srcFile.lastIndexOf(
            FILE_SEPARATOR) + 1);
        File bdbDir = new File(destDir);
        if (!bdbDir.exists()) {
            bdbDir.mkdir();
            byte[] buffer = new byte[BUFFER_SIZE];
            GZIPInputStream gzin = new GZIPInputStream(new
                FileInputStream(srcFile));
            FileOutputStream fout = new FileOutputStream(new File(destDir,
                tempDestFileName));
            int byteRead = 0;
            while ((byteRead = gzin.read(buffer)) != -1) {
                fout.write(buffer, 0, byteRead);
            }
            gzin.close();
            fout.close();
        }
    }
    
    /**
     * Prints the usage through system out.
     * @param bundle The ResourceBundle which contains the message.
     */
    
    public static void printUsage(ResourceBundle bundle){
        System.out.println(bundle.getString("message.info.usage"));
    }
    
    /**
     * Lookups and set the resource bundle variables to the Properties
     *
     * @param bundle The ResourceBundle is going to be evaluated.
     * @param lookupProp The properties may contain the real values and the
     *        storage of the results.
     */
    
    public static void evaluateBundleValues(ResourceBundle bundle,
        Properties lookupProp){
        Enumeration propNames = bundle.getKeys();
        while (propNames.hasMoreElements()) {
            String name = (String) propNames.nextElement();
            String value = (String) SetupUtils.evaluatePropertiesValue(bundle
                .getString(name), lookupProp);
            if (value != null) {
                lookupProp.setProperty(name, value);
            }
        }
    }
    
    /**
     * Copy and replace the variables in the scripts.
     *
     * @param bundle The ResourceBundle which contains the prompt messages.
     * @param lookupProp The properties which contains the variables map, file
     *        patterns, source directory, and destinated directory.
     */
    
    public static void copyAndFilterScripts(ResourceBundle bundle,
        Properties lookupProp) throws IOException{
        String currentOS = determineOS();
        
        String fromFilePattern = lookupProp.getProperty(currentOS + FROM_FILE);
        String toFilePattern = lookupProp.getProperty(currentOS + TO_FILE);
        String tempFromDir = lookupProp.getProperty(currentOS + FROM_DIR);
        String tempToDir = lookupProp.getProperty(currentOS + TO_DIR);
        File fromDir = new File(tempFromDir);
        File toDir = new File(tempToDir);
        if (toDir.isAbsolute()) {
            toDir = new File(toDir.getName());
        }
        Properties tokens = SetupUtils.getTokens(bundle, lookupProp);
        LinkedList fromFilesList = new LinkedList();
        LinkedList toFilesList = new LinkedList();
        SetupUtils.getFiles(fromDir, toDir, fromFilePattern, toFilePattern,
            fromFilesList, toFilesList);
        ListIterator srcIter = fromFilesList.listIterator();
        ListIterator destIter = toFilesList.listIterator();
        while ((srcIter.hasNext()) && (destIter.hasNext())) {
            File srcFile = (File) srcIter.next();
            File destFile = (File) destIter.next();
            CopyUtils.copyFile(srcFile, destFile, tokens, true, false);
        }
        if (! currentOS.equals(WINDOWS)) {
            Process proc = Runtime.getRuntime().exec("/bin/chmod -R +x " +
                toDir.getName());
            try {
                if (proc.waitFor() != 0) {
                    System.out.println(bundle.getString("message.info." +
                        "permission.scripts"));
                }
            } catch (InterruptedException ex) {
                System.out.println(bundle.getString("message.info." +
                        "permission.scripts"));
                //ex.printStackTrace();
            }
        }
        System.out.println(bundle.getString("message.info.success") + " " +
            (new File(".")).getCanonicalPath() + FILE_SEPARATOR +
            toDir.getName());
    }
}
