/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FileUtils.java,v 1.3 2010/02/09 21:34:01 hari44 Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.text.SimpleDateFormat;

public class FileUtils {

    /**
     * Copies a file
     * 
     * @param source
     *            file
     * @param destination
     *            file
     * @return true if copy operation succeed
     */

    public static boolean copyFile(String source, String destination)
            throws Exception {

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        boolean result = false;

        try {
            bis = new BufferedInputStream(new FileInputStream(source));
            bos = new BufferedOutputStream(new FileOutputStream(destination));

            byte[] buf = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, bytesRead);
            }
            result = true;
        } catch (Exception e) { // Log & throw exception
            Debug.log("FileUtils.copyFile(): Error occurred while copying "
                    + "file: " + source + " to: " + destination, e);
            throw e;
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    Debug.log("FileUtils.copyFile(): Error occurred while "
                            + "closing input stream for: " + source);
                }
            }
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    Debug.log("FileUtils.copyFile(): Error occurred while "
                            + "closing output stream for: " + destination);
                }
            }
        }

        return result;
    }

    /**
     * Method copyJarFile to destination directory
     * 
     * @param src
     * @param dest
     * 
     */
    public static void copyJarFile(String srcDir, String destDir,
            String filename) throws Exception {
        String src = srcDir + FILE_SEP + filename;
        String dest = destDir + FILE_SEP + filename;
        JarInputStream jarIn = null;
        JarOutputStream jarOut = null;

        try {
            jarIn = new JarInputStream(new FileInputStream(src));
            jarOut = new JarOutputStream(new FileOutputStream(dest));

            // Allocate a buffer for reading entry data.
            byte[] buffer = new byte[4096];
            JarEntry entry;

            while ((entry = jarIn.getNextJarEntry()) != null) {

                // Write the entry to the output JAR
                jarOut.putNextEntry(entry);

                int bytesRead;
                while ((bytesRead = jarIn.read(buffer)) != -1) {
                    jarOut.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) { // Log & throw exception
            Debug.log("FileUtils.copyJarFile(): Error occurred while copying "
                    + "jar file: " + src + " to: " + dest, e);
            throw e;
        } finally {
            // Flush and close all the streams
            if (jarIn != null) {
                try {
                    jarIn.close();
                } catch (IOException e) {
                    Debug.log("FileUtils.copyJarFile(): Error occurred while "
                            + "closing input stream for: " + src);
                }
            }
            if (jarOut != null) {
                try {
                    jarOut.flush();
                    jarOut.close();
                } catch (IOException e) {
                    Debug.log("FileUtils.copyJarFile(): Error occurred while "
                            + "closing output stream for: " + dest);
                }
            }
        }
    }

    /**
     * Method copyDirContents
     * 
     * @param srcDir
     * @param destDir
     * 
     */
    public static void copyDirContents(File srcDir, File destDir)
            throws Exception {

        String[] items = srcDir.list();
        if (items != null) {
            int count = items.length;
            for (int i = 0; i < count; i++) {
                String entryName = items[i];
                File entry = new File(srcDir, entryName);
                if (entry.isDirectory()) {
                    File targetDir = new File(destDir, entryName);
                    targetDir.mkdirs();
                    copyDirContents(entry, targetDir);
                } else if (entry.isFile()) {
                    String srcPath = entry.getAbsolutePath();
                    String destPath = destDir.getAbsolutePath() + FILE_SEP
                            + entryName;
                    copyFile(srcPath, destPath);
                }
            }
        }
    }

    /**
     * Method removeDir. All the files in the directory will be removed
     * 
     * @param directory
     * to be removed. @ return true if the directory is deleted. False other
     *            wise
     */
    public static boolean removeDir(File dir) {

        String[] list = dir.list();
        // Delete the contents of the directory!
        if (list != null) {
            int count = list.length;
            for (int i = 0; i < count; i++) {
                String fileName = list[i];
                File file = new File(dir, fileName);
                if (file.isDirectory()) {
                    removeDir(file);
                } else if (!file.delete()) {
                    Debug.log("FileUtils.removeDir() Unable to delete file: "
                            + file.getAbsolutePath());
                }
            }
        }
        boolean status = dir.delete();
        // Now delete the directory!
        if (!status) {
            Debug.log("FileUtils.removeDir() Unable to delete directory: "
                    + dir.getAbsolutePath());
        }

        return status;
    }


    /**
     * Method removeJarFiles. jar files will be removed from source directory
     *
     * @param srcDir - Source directory
     * @param filename - file to be deleted
     *
     */
    public static void removeJarFiles(String srcDir, String fileName) { 
     
      String src = srcDir + FILE_SEP + fileName; 
      File file = new File(src);
      if (file.exists()) {
          file.delete();
      }
      else
          Debug.log("FileUtils.removeJarFiles() Unable to remove file");
    }
 

    /**
     * Method removeFiles. All the files existing in source directory 
     * will be removed from Destination Directory
     *
     * @param srcDir  - Source directory to compare files
     * @param desDir  - Destination directory to remove files
     *
     */
    public static void removeFiles(String srcDir, String desDir) {
      File srcFile = new File(srcDir); 
      String[] list = srcFile.list();
      if (list != null) {
         int count = list.length;
         for (int i = 0; i < count; i++) {
             String fileName = list[i];
             File file = new File(desDir, fileName);
             if (file.isFile()) {
                 file.delete();
             }else {
               Debug.log("FileUtils.removeFiles() Unable to delete file");
             }
         }
      }
    }   

   
    public static void removeLines(String fileName, String value) throws Exception {
      ArrayList list = new ArrayList();
      BufferedReader in   = new BufferedReader(new FileReader(fileName));
      String lines;
      while((lines = in.readLine()) != null) {
            list.add(lines);
      }
      in.close();
      FileWriter fw = new FileWriter(fileName);
      for(int i=0; i<list.size(); i++) {
          String linedata = (String)list.get(i);
          if(!(linedata.contains(value))) {
             fw.write(linedata + "\n");
          }
      }
      fw.close();
    }

   

    public static void appendDataToFile(String fileName, String data)
            throws Exception {
        BufferedWriter bw = null;
        try {
            FileOutputStream fo = new FileOutputStream(fileName, true);
            OutputStreamWriter ow = new OutputStreamWriter(fo);
            bw = new BufferedWriter(ow);

            bw.write(LINE_SEP + data);
        } catch (Exception e) {
            Debug.log("FileUtils.appendDataToFile() - Exception occurred "
                    + "while appending data to file: '" + fileName + "'. ", e);
            throw e;
        } finally {
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException io) {
                    // Ignore
                }
            }
        }
    }

    public static void backupFile(String fileName, String suffix)
            throws Exception {
        // Obtain the current time
        Date currentDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String tstamp = formatter.format(currentDate);

        // Create a backup file
        String bkupFileName = fileName + suffix;
        bkupFileName = bkupFileName + "-" + tstamp;
        copyFile(fileName, bkupFileName);
    }

    /*
     * Is file valid @param filename
     * 
     * @return boolean
     */
    public static boolean isFileValid(String filename) {

        boolean result = false;
        if ((filename != null) && (filename.length() > 0)) {
            File file = new File(filename);
            if (file.exists() && file.isFile() && file.canRead()) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Method replaceBackWithForward
     * 
     * 
     * @param str
     * 
     * @return
     * 
     */
    public static String replaceBackWithForward(String str) {
        char backSlash = '\\';
        char forwardSlash = '/';
        String returnStr = str;

        if (str != null) {
            returnStr = str.replace(backSlash, forwardSlash);
        }

        return returnStr;
    }

    /*
     * Is directory valid @param directory
     * 
     * @return boolean
     */
    public static boolean isDirValid(String directory) {

        boolean result = false;
        if ((directory != null) && (directory.length() > 0)) {
            File dir = new File(directory);

            if (dir.exists() && dir.isDirectory()) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Method getLineByNumber
     * 
     * 
     * @param filePath
     * @param lineNum
     * 
     * @return
     * 
     */
    public static String getLineByNumber(String filePath, int lineNum) {

        String retval = null;
        int curLine = -1;

        try {
            LineNumberReader reader = getLineNumReader(filePath);

            if ((reader != null) && (lineNum >= 1)) {
                retval = reader.readLine();
                curLine = reader.getLineNumber();

                while ((retval != null) && (curLine < lineNum)) {
                    retval = reader.readLine();
                    curLine = reader.getLineNumber();
                }

                reader.close();
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.getLineByNumber() threw exception : ", ex);
            retval = null;
        }

        return retval;
    }

    /**
     * Method matchPattern
     * 
     * 
     * @param line
     * @param pattern
     * @param matchBegin
     * @param matchEnd
     * @param ignoreCase
     * 
     * @return
     * 
     */
    public static boolean matchPattern(String line, String pattern,
            boolean matchBegin, boolean matchEnd, boolean ignoreCase) {

        boolean matchB = false;
        boolean matchE = false;
        boolean matchP = false;
        boolean match = false;
        int patLen = pattern.length();

        line = line.trim();

        int lineLen = line.length();

        if (lineLen >= patLen) {
            if (matchBegin) {
                matchB = line.regionMatches(ignoreCase, 0, pattern, 0, patLen);
            } else if (matchEnd) {
                int frmOff = lineLen - patLen;
                matchE = line.regionMatches(ignoreCase, frmOff, pattern, 0,
                        patLen);
            } else {
                if (ignoreCase) {
                    String tempLine = new String(line);
                    String tempPattern = new String(pattern);
                    tempLine.toLowerCase();
                    tempPattern.toLowerCase();
                    if (tempLine.indexOf(tempPattern) >= 0) {
                        matchP = true;
                    }
                } else {
                    if (line.indexOf(pattern) >= 0) {
                        matchP = true;
                    }
                }
            }
        }

        if (matchB || matchE || matchP) {
            match = true;
        }

        return match;
    }

    /**
     * Method getLineWithPattern
     * 
     * 
     * @param filePath
     * @param pattern
     * @param matchBegin
     * @param matchEnd
     * @param ignoreCase
     * @param beginAtLine
     * 
     * @return
     * 
     */
    public static String getLineWithPattern(String filePath, String pattern,
            boolean matchBegin, boolean matchEnd, boolean ignoreCase,
            int beginAtLine) {

        String line = null;
        String prevLine = null;
        boolean match = false;
        String retLine = null;

        try {
            if (pattern != null) {

                LineNumberReader reader = getLineNumReader(filePath);

                if (reader != null) {
                    line = reader.readLine();
                    if (beginAtLine > 0) {
                        int lineCount = 0;
                        while ((line != null) && (lineCount < beginAtLine)) {
                            line = reader.readLine();
                            lineCount++;
                        }
                    }

                    while ((line != null) && !match) {
                        match = matchPattern(line, pattern, matchBegin,
                                matchEnd, ignoreCase);
                        prevLine = new String(line);
                        line = reader.readLine();
                    }

                    if (match) {
                        Debug.log("FileUtils.getLineWithPattern : Match [ "
                                + pattern + " ] => " + prevLine);
                        retLine = prevLine;
                    }

                    reader.close();
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.getLineWithPattern() threw exception : ", ex);
        }

        return retLine;
    }

    /**
     * Method getFirstOccurence
     * 
     * 
     * @param filePath
     * @param pattern
     * @param matchBegin
     * @param matchEnd
     * @param ignoreCase
     * @param beginAtLine
     * 
     * @return
     * 
     */
    public static int getFirstOccurence(String filePath, String pattern,
            boolean matchBegin, boolean matchEnd, boolean ignoreCase,
            int beginAtLine) {

        int retVal = -1;
        String line = null;
        String prevLine = null;
        boolean match = false;

        try {
            if (pattern != null) {
                LineNumberReader reader = getLineNumReader(filePath);
                if (reader != null) {
                    line = reader.readLine();
                    if (beginAtLine > 0) {
                        int lineCount = 0;
                        while ((line != null) && (lineCount < beginAtLine)) {
                            line = reader.readLine();
                            lineCount++;
                        }
                    }
                    while ((line != null) && !match) {
                        match = matchPattern(line, pattern, matchBegin,
                                matchEnd, ignoreCase);
                        prevLine = new String(line);
                        retVal = reader.getLineNumber();
                        line = reader.readLine();
                    }

                    if (match) {
                        Debug.log("FileUtils.getFirstOccurence : Match [ "
                                + pattern + " ] => " + prevLine);
                    } else {
                        retVal = -1;
                    }
                    reader.close();
                }
            }
        } catch (Exception ex) {
            retVal = -1;
            Debug.log("FileUtils.getFirstOccurence() threw exception : ", ex);
        }

        return retVal;
    }

    /**
     * Method getFirstOccurence
     * 
     * 
     * @param filePath
     * @param pattern
     * @param matchBegin
     * @param matchEnd
     * @param ignoreCase
     * 
     * @return
     * 
     */
    public static int getFirstOccurence(String filePath, String pattern,
            boolean matchBegin, boolean matchEnd, boolean ignoreCase) {
        return getFirstOccurence(filePath, pattern, matchBegin, matchEnd,
                ignoreCase, 0);
    }

    /**
     * Method getLastOccurence
     * 
     * 
     * @param filePath
     * @param pattern
     * @param matchBegin
     * @param matchEnd
     * @param ignoreCase
     * 
     * @return
     * 
     */
    public static int getLastOccurence(String filePath, String pattern,
            boolean matchBegin, boolean matchEnd, boolean ignoreCase) {

        int retVal = -1;
        String line = null;
        boolean match = false;
        boolean notFound = true;

        try {
            LineNumberReader reader = getLineNumReader(filePath);
            if (reader != null) {
                line = reader.readLine();

                while (line != null) {
                    match = matchPattern(line, pattern, matchBegin, matchEnd,
                            ignoreCase);
                    if (match) {
                        notFound = false;
                        retVal = reader.getLineNumber();
                        Debug.log("FileUtils.getLastOccurence : Match [ "
                                + pattern + " ] => " + line);
                    }
                    line = reader.readLine();
                }
                reader.close();
            }
            if (notFound) {
                retVal = -1;
            }
        } catch (Exception ex) {
            retVal = -1;
            Debug.log("FileUtils.getLastOccurence() threw exception : ", ex);
        }

        return retVal;
    }

    /**
     * Method addMapProperty
     * 
     * Adds a map property like <b>key[name]=value</b> in PRODUCT
     * Config.properties
     * 
     * @param key
     *            key name of the property
     * @param name
     *            name of the property
     * @param value
     *            value of the property
     * 
     */
    public static boolean addMapProperty(String configPath, String key,
            String name, String value) {
        boolean result = true;
        boolean isExisting = false;
        int index = 0;

        try {
            // find the index of property insertion
            index = findPropertyIndex(configPath, key);

            String newLine = null;
            String line = null;
            String val = null;

            if (index > 0) {
                newLine = key + "[" + name + "]" + "=" + value;
                // remove the empty map and add the entry
                line = getLineByNumber(configPath, index);
                val = isPropertyValid(line);

                if (val == null || val.length() == 0) {
                    result = result
                            && removeLinesByNum(configPath, index + 1, 1);
                }

                result = result
                        && insertLineByNumber(configPath, index + 1, newLine);
            } else {
                String[] lines = { newLine };
                result = result && appendLinesToFile(configPath, lines);
            }

            if (result) {
                Properties prop = getProperties(configPath);
                if (prop != null) {
                    Enumeration propNames = prop.propertyNames();
                    while (propNames.hasMoreElements()) {
                        if (((String) propNames.nextElement()).startsWith(key))
                        {
                            isExisting = true;
                            break;
                        }
                    }
                    result = isExisting;
                } else {
                    result = false;
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.addMapProperty() threw exception : ", ex);
            result = false;
        }

        Debug.log("FileUtils.addMapProperty() result : " + result);
        return result;
    }

    /**
     * Adds an entry to a list property in Product Config properties
     * 
     * 
     * @param fileName
     *            name of the config file
     * @param list
     *            property name of the property for the list
     * @param value
     *            name of the list property to be added
     * 
     * Ex: Adds a property like <b>property[index] = value</b> in Product
     * Config properties
     * 
     * @return boolean true or false
     * 
     */
    public static boolean addListProperty(String fileName, String property,
            String value) {
        boolean status = false;

        try {
            if (((fileName != null) && (fileName.trim().length() > 0))
                    && ((property != null) && (property.trim().length() > 0))
                    && ((value != null) && (value.trim().length() > 0))) {

                // Find location to add the list property
                int locationToAdd = findPropertyIndex(fileName, property);
                if (locationToAdd <= 0) {
                    locationToAdd = getTotalLineNums(fileName);
                }

                // Create the new map if the value is non empty string
                ArrayList list = createListValue(fileName, property, value);

                // Add the list property
                if ((list != null) && (!list.isEmpty())) {
                    Iterator iter = list.iterator();
                    int index = 0;
                    while (iter.hasNext()) {
                        StringBuffer strBuf = new StringBuffer();
                        strBuf.append(property).append(SQRBRACKET_OPEN).append(
                                index).append(SQRBRACKET_CLOSE).append(SPACE)
                                .append(EQUAL_TO).append((String) iter.next());
                        index++;
                        if (strBuf != null) {
                            status = FileUtils.insertLineByNumber(fileName,
                                    locationToAdd + 1, strBuf.toString());
                            locationToAdd++;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.addListProperty() threw exception :", ex);
        }

        return status;
    }

    /**
     * Method declaration
     * 
     * 
     * @param fileName
     * @param property
     * @param value
     * 
     * @return
     * 
     * @see
     */
    private static ArrayList createListValue(String fileName, String property,
            String value) {

        ArrayList returnList = new ArrayList();

        try {
            ArrayList lineNums = new ArrayList();
            File file = new File(fileName);
            if (file.exists() && file.canRead()) {
                LineNumberReader lineRead = new LineNumberReader(
                        new FileReader(file));
                String presentLine = lineRead.readLine();

                while (presentLine != null) {
                    if (presentLine.startsWith(property)
                            && presentLine.length() > property.length()) {
                        char ch = presentLine.charAt(property.length());
                        if (ch == '[' || ch == ' ' || ch == '=') {
                            int presentLineNo = lineRead.getLineNumber();
                            if (presentLineNo > 0) {
                                lineNums.add(new Integer(presentLineNo));
                            }
                        }
                    }
                    presentLine = lineRead.readLine();
                }
                if (lineRead != null) {
                    lineRead.close();
                }
            }

            // Now remove these lines and store them
            if ((lineNums != null) && (lineNums.size() > 0)) {
                Iterator iter = lineNums.iterator();
                int count = 0;
                while (iter.hasNext()) {
                    int lineNum = ((Integer) iter.next()).intValue();
                    if (lineNum > 0) {
                        if (count > 0) { // decrement one line since file is
                                            // shorter
                            lineNum = lineNum - count;
                        }
                        String line = FileUtils.removeLinesByNumber(fileName,
                                lineNum, 1);
                        if (line != null) {
                            count++;
                            String val = isPropertyValid(line); // add only non
                                                                // empty vals
                            if ((val != null) && (val.length() > 0)) {
                                returnList.add(val);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.createListValue() threw exception :", ex);
        }

        // Finally add the new list element value
        returnList.add(SPACE + value);

        return returnList;

    }

    /**
     * Method declaration
     * 
     * 
     * @param line
     * 
     * @return
     * 
     * @see
     */
    private static String isPropertyValid(String line) {
        String str = null;
        String retVal = null;

        try {
            int index = line.indexOf(EQUAL_TO);
            str = line.substring(index + 1, line.length());
            if ((str != null) && (str.trim().length() > 0)) {
                retVal = str;
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.isListEntryValid() threw exception :", ex);
        }

        return retVal;
    }

    /**
     * Method declaration
     * 
     * 
     * @param fileName
     * 
     * @return
     * 
     * @see
     */
    public static int getTotalLineNums(String fileName) {
        int lineNum = 0;
        try {
            File file = new File(fileName);
            if (file.exists() && file.canRead()) {
                LineNumberReader lineRead = new LineNumberReader(
                        new FileReader(file));
                String presentLine = lineRead.readLine();
                while (presentLine != null) {
                    presentLine = lineRead.readLine();
                    lineNum = lineRead.getLineNumber();
                }
                if (lineRead != null) {
                    lineRead.close();
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.getTotalLineNums() threw exception :", ex);
        }
        return lineNum;
    }

    /**
     * Method insertLineByNumber
     * 
     * 
     * @param filePath
     * @param lineNum
     * @param line
     * 
     * @return
     * 
     */
    public static boolean insertLineByNumber(String filePath, int lineNum,
            String line) {

        boolean success = false;

        try {
            if (line != null) {
                LineNumberReader reader = getLineNumReader(filePath);
                StringWriter writer = new StringWriter();

                if (lineNum > 1) {
                    reader = (LineNumberReader) copyTillLine(reader, writer,
                            lineNum);
                }
                writeLine(writer, line);

                if (reader != null) {
                    success = copyTillEnd(reader, writer);
                    if (!success) {
                        throw new Exception("ERROR: Failed to copy lines");
                    }
                    String tempFilePath = filePath + ".tmp";
                    success = writeToFile(tempFilePath, writer);
                    if (!success) {
                        throw new Exception("ERROR: Writing to File");
                    }
                    File tempFile = new File(tempFilePath);
                    if (tempFile.exists() && tempFile.isFile()) {
                        copyFile(tempFilePath, filePath);
                    }

                    tempFile.delete();

                    success = true;
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.insertLineByNumber() threw exception : ", ex);
        }

        return success;
    }

    /**
     * Method removeLinesByNum
     * 
     * 
     * @param filePath
     * @param lineNum
     *            begining line number
     * @param numLines
     *            total number of lines to remove
     * 
     * @return true if removal is successful
     * 
     */
    public static boolean removeLinesByNum(String filePath, int lineNum,
            int numLines) {
        boolean result = true;
        try {
            String line = removeLinesByNumber(filePath, lineNum, numLines);
            Debug.log("FileUtils.removeLinesByNum : removing line = " + line);
            result = ((line == null) ? false : true);

        } catch (Exception e) {
            Debug.log("FileUtils.removeLinesByNum() threw exception : ", e);
            result = false;
        }
        return result;
    }

    /**
     * Method removeLinesByNumber
     * 
     * 
     * @param filePath
     * @param lineNum
     * @param numLines
     * 
     * @return
     * 
     */
    public static String removeLinesByNumber(String filePath, int lineNum,
            int numLines) {

        String line = null;
        boolean success = false;

        try {
            LineNumberReader reader = getLineNumReader(filePath);
            StringWriter writer = new StringWriter();

            if (lineNum > 1) {
                reader = (LineNumberReader) copyTillLine(reader, writer,
                        lineNum);
            }
            if (reader != null) {
                if (numLines <= 0) {
                    numLines = 1;
                }
                for (int i = 0; i < numLines; i++) {
                    line = reader.readLine();
                }
                success = copyTillEnd(reader, writer);
                if (!success) {
                    throw new Exception("ERROR: Failed to copy lines");
                }
                String tempFilePath = filePath + ".tmp";
                success = writeToFile(tempFilePath, writer);
                if (!success) {
                    throw new Exception("ERROR: Writing to File");
                }
                File tempFile = new File(tempFilePath);
                if (tempFile.exists() && tempFile.isFile()) {
                    copyFile(tempFilePath, filePath);
                }

                tempFile.delete();
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.removeLinesByNumber() threw exception : ", 
                    ex);
        }

        return line;
    }

    /**
     * Appends the given set of lines to the specified file.
     *
     * @param filePath
     * @param linesToAppend
     * @return true for success, false otherwise
     */
    public static boolean appendLinesToFile(String filePath,
            String[] linesToAppend) {
        return appendLinesToFile(filePath, linesToAppend, false);
    }

    /**
     * Appends the given set of lines to the specified file.
     * 
     * @param filePath
     * @param linesToAppend
     * @param create should the file be created if it does not exist
     * @return true for success, false otherwise
     */
    public static boolean appendLinesToFile(String filePath,
            String[] linesToAppend, boolean create) {
        boolean result = false;
        try {
            if (linesToAppend != null && linesToAppend.length > 0) {
                LineNumberReader reader = getLineNumReader(filePath, create);
                StringWriter writer = new StringWriter();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    writeLine(writer, line);
                }

                for (int i = 0; i < linesToAppend.length; i++) {
                    writeLine(writer, linesToAppend[i]);
                }
                String tempFilePath = filePath + ".tmp";
                result = writeToFile(tempFilePath, writer);

                if (result) {
                    File tempFile = new File(tempFilePath);
                    if (tempFile.exists() && tempFile.isFile()) {
                        result = copyFile(tempFilePath, filePath);

                        if (result) {
                            result = tempFile.delete();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            result = false;
            Debug.log("FileUtils.appendLinesToFile() threw exception : ", ex);
        }

        return result;
    }

    /**
     * Method getProperties
     * 
     * Loads a properties file into memory and creates a
     * <code>java.util.Properties</code> object
     * 
     * @param configPath
     *            path to the properties file
     * @return prop a <code>java.util.Properties</code> object from the
     *         specified file or <code>null</code> in case of an error in
     *         processing
     */

    public static Properties getProperties(String configPath) {
        Properties prop = new Properties();
        synchronized (prop) {
            prop.clear();
            try {
                BufferedInputStream bufInStream = new BufferedInputStream(
                        new FileInputStream(configPath));
                prop.load(bufInStream);
            } catch (Exception e) {
                e.printStackTrace();
                prop = null;
            }
        }// end sync
        return prop;
    }

    /**
     * Method writeToFile
     * 
     * 
     * @param filePath
     * @param writer
     * 
     * @return
     * 
     */
    private static boolean writeToFile(String filePath, Writer writer) {

        boolean success = false;

        try {
            if (writer != null) {
                File destFile = new File(filePath);

                if (destFile.exists()) {
                    // System.out.println("WARNING :File " + destFile +
                    // "Overwritten");
                    destFile.delete();
                }
                byte[] data = writer.toString().getBytes();
                writer.close();
                FileOutputStream out = new FileOutputStream(destFile);
                out.write(data);
                out.flush();
                out.close();

                success = true;
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.writeToFile() threw exception : ", ex);
        }

        return success;
    }

    /**
     * Method writeLine
     * 
     * 
     * @param writer
     * @param line
     * 
     */
    private static void writeLine(Writer writer, String line) {

        if ((writer != null) && (line != null)) {
            try {
                writer.write(line);
                writer.write(LINE_SEP);
            } catch (Exception ex) {
                Debug.log("FileUtils.writeLine() threw exception : ", ex);
            }
        }
    }

    /**
     * Method copyTillLine
     * 
     * 
     * @param reader
     * @param writer
     * @param lineNum
     * 
     * @return
     * 
     */
    private static Reader copyTillLine(LineNumberReader reader, Writer writer,
            int lineNum) {

        String line = null;
        int curLine = -1;

        try {
            if ((reader != null) && (writer != null) && (lineNum > 1)) {
                do {
                    line = reader.readLine();
                    if (line != null) {
                        writeLine(writer, line);
                        curLine = reader.getLineNumber();
                    }
                } while ((line != null) && (curLine < lineNum - 1));
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.copyTillLine() threw exception : ", ex);
            reader = null;
        }

        return reader;
    }

    /**
     * Method copyTillEnd
     * 
     * 
     * @param reader
     * @param writer
     * 
     * @return
     * 
     */
    private static boolean copyTillEnd(BufferedReader reader, Writer writer) {

        boolean success = false;
        String line = null;

        try {
            if ((reader != null) && (writer != null)) {
                line = reader.readLine();
                while (line != null) {
                    writeLine(writer, line);
                    line = reader.readLine();
                }
            }

            reader.close();
            success = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            Debug.log("FileUtils.copyTillEnd() threw exception : ", ex);
        }

        return success;
    }

    /**
     * Method declaration
     * 
     * 
     * @param fileName
     * @param property
     * 
     * @return
     * 
     * @see
     */
    private static int findPropertyIndex(String fileName, String property) {
        int matchIndex = -1;
        try {
            File file = new File(fileName);
            if (file.exists() && file.canRead()) {
                LineNumberReader lineRead = new LineNumberReader(
                        new FileReader(file));
                String prevLine = null;
                String presentLine = lineRead.readLine();
                int prevLineNum = -1;
                int presentLineNo = 0;
                String pattern = property;

                while (presentLine != null) {
                    prevLine = presentLine;
                    prevLineNum = presentLineNo;
                    presentLineNo = lineRead.getLineNumber();
                    presentLine = lineRead.readLine();

                    if ((presentLine != null)
                            && (presentLine.indexOf(pattern) >= 0)
                            && (!presentLine.startsWith(HASH))) {
                        matchIndex = presentLineNo;
                        break;

                    }
                }

                if (lineRead != null) {
                    lineRead.close();
                }
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.findPropertyIndex() threw exception : ", ex);
        }

        return matchIndex;
    }

    /**
     * Method getLineNumReader
     *
     *
     * @param filePath
     *
     * @return
     *
     */
    private static LineNumberReader getLineNumReader(String filePath) {
        return getLineNumReader(filePath, false);
    }

    /**
     * Method getLineNumReader
     * 
     * 
     * @param filePath the path to the file
     * @param create should we create the file if it does not exist
     *
     * @return
     * 
     */
    private static LineNumberReader getLineNumReader(String filePath, boolean create) {

        LineNumberReader reader = null;

        try {
            File srcFile = new File(filePath);

            if (srcFile.exists() && srcFile.isFile()) {
                reader = new LineNumberReader(new FileReader(srcFile));
                reader.setLineNumber(0);
            } else if (!srcFile.exists() && create) {
                FileUtils.writeLine(new FileWriter(srcFile), "");
                reader = new LineNumberReader(new FileReader(srcFile));
                reader.setLineNumber(0);
            }
        } catch (Exception ex) {
            Debug.log("FileUtils.getLineNumReader() threw exception : ", ex);
        }

        return reader;
    }

    /**
     * Returns the parent folder upper level count to the current path
     * 
     * @param path path to a file/folder
     * @param level how much should we go upper in the path
     * @return path to the parent upper the given level, for example: 
     * (/a/b/c/d, 3) will result in /a
     */
    public static String getParentDirPath(String path, int level) {
        String ret = path;
        for (int i = 0 ; i < level; i++) {
            if (path != null) {
                ret = getParentDirPath(path);
            }
        }

        return ret;
    }

    /**
     * Returns the parent folder of the given file/folder
     *
     * @param path path to a file/folder
     * @return path to the parent, it may be null, if there is no parent.
     */
    public static String getParentDirPath(String path) {
        File dir = new File(path);
        return dir.getParent();
    }

    private static final String FILE_SEP = 
        System.getProperty("file.separator");

    private static final String LINE_SEP = 
        System.getProperty("line.separator");

    /** Field SQRBRACKET_OPEN * */
    public static final String SQRBRACKET_OPEN = "[";

    /** Field SQRBRACKET_CLOSE * */
    public static final String SQRBRACKET_CLOSE = "]";

    /** Field EXAMPLE_FIELD * */
    public static final String EXAMPLE_FIELD = "Example:";

    /** Field HASH * */
    public static final String HASH = "#";

    /** Field HASH * */
    public static final String EQUAL_TO = "=";

    /** Field HASH * */
    public static final String SPACE = " ";

}
