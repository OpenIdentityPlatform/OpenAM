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
 * $Id: FileHandler.java,v 1.1 2008/07/02 18:45:44 kanduls Exp $
 */

package com.sun.identity.tune.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class has utility functions for token replacement,delete line and 
 * inserting lines into file.
 */

public class FileHandler {

    private String fileName = null;
    private ArrayList lines = null;

    /**
     * Constructs FileHandler object
     * 
     * @param fileName Absolute path of the file.
     * 
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public FileHandler(String fileName)
    throws FileNotFoundException, IOException {
        this.fileName = fileName;
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String str = null;
        lines = new ArrayList();
        while ((str = in.readLine()) != null) {
            lines.add(str);
        }
        in.close();
    }
    
    /**
     * This method returns the line matching the Regular expression 
     * from the file.
     * 
     * @param regExp Regular expression to be matched.
     * @return Line matching regular expression.
     */
    public String getLine(String regExp) {
        int iLineNum = getLineNum(regExp);
        return iLineNum != -1 ? (String) lines.get(iLineNum) : "";
    }
    /**
     * Returns the line no matching the regular expression.
     * 
     * @param regExp Regular expression to be matched.
     * @return Line number matching regular expression.
     */
    public int getLineNum(String regExp) {
        Pattern p = Pattern.compile(regExp);
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            String line = (String) lines.get(i);
            Matcher m = p.matcher(line);
            if (m.find()) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Returns pattern matching lines if <code>invert</code> is set to true else
     * it will return lines that doesn't match pattern.
     * 
     * @param pattern pattern to be matched.
     * @param invert if set to false lines that doesn't match the pattern are 
     * returned.
     * @return list of lines.
     */
    public String[] getMattchingLines(String pattern, boolean invert) {
        List matList = new ArrayList();
        Pattern p = Pattern.compile(pattern);
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            String line = (String) lines.get(i);
            Matcher m = p.matcher(line);
            boolean found = m.find();
            if (!invert && found) {
                matList.add(getLine(i));
            } else if (invert && !found) {
                matList.add(getLine(i));
            }
        }
        String[] arr = new String[matList.size()];
        for (i = 0; i < matList.size(); i++) {
            arr[i] = new String(matList.get(i).toString());
        }
        return arr;
    }
    /**
     * Returns the line number if the text is in the line.
     * 
     * @param text Text to be matched in the file.
     * @return line number in the file.
     */
    public int lineContains(String text) {
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            String str = (String) lines.get(i);
            if (str.indexOf(text) != -1) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Returns the line number containing the exact text.
     * 
     * @param text Text to be matched in the file.
     * @return line number in the file.
     */
    public int lineEquals(String text) {
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            String str = (String) lines.get(i);
            if (str.equals(text)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Returns the line given the line number in the file.
     * 
     * @param lineNum Required line number.
     * @return Line from the file.
     */
    public String getLine(int lineNum) {
        return (String) lines.get(lineNum);
    }
    
    /**
     * Returns the line no matching the patterns in set of lines.  This will
     * first finds the starting line number based on the "patternBegin",
     * ending line number based on "patternEnd" and searches for the line which 
     * contains "patternFind" between staring line number and 
     * ending line number.
     * 
     * @param patternBegin Starting line pattern.
     * @param patternEnd Ending line pattern.
     * @param patternFind Pattern to be find between starting and ending lines.
     * @return line number.
     */
    public int getPatternMatchLine(String patternBegin, String patternEnd,
            String patternFind) {
        int size = lines.size();
        int i = 0;
        int start = 0, end = 0;
        for (i = 0; i < size; i++) {
            if (((String) lines.get(i)).trim().startsWith(patternBegin)) {
                break;
            }
        }
        if (i == size) {
            return size - 1;
        }
        start = i;
        for (; i < size; i++) {
            if (((String) lines.get(i)).trim().startsWith(patternEnd)) {
                break;
            }
        }
        if (i == size) {
            end = i - 1;
        } else {
            end = i;
        }

        for (i = end; i >= start; i--) {
            if (((String) lines.get(i)).trim().startsWith(patternFind)) {
                break;
            }
        }
        if (i == start - 1) {
            return end - 1;
        }
        return i;
    }
    
    /**
     * Returns the line number starting with the given string
     * @param str Starting string
     * @return line number.
     */
    public int startsWith(String str) {
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            if (((String) lines.get(i)).startsWith(str) == true) {
                break;
            }
        }
        if (i == size) {
            i = -1;
        }
        return i;
    }
    
    /**
     * Returns the line number starting with "startStr" and ends with "endStr".
     * 
     * @param startStr Starting string
     * @param endStr Ending string.
     * @return line number.
     */
    public int startsWithAndEndsWith(String startStr, String endStr) {
        int size = lines.size();
        int i = 0;
        for (i = 0; i < size; i++) {
            String line = ((String) lines.get(i)).trim();
            if (line.startsWith(startStr) == true &&
                    line.endsWith(endStr) == true) {
                break;
            }
        }
        if (i == size) {
            i = -1;
        }
        return i;
    }
    
    /**
     * Removes the line from the file.
     * 
     * @param lineNum Line number to be deleted.
     */
    public void removeLine(int lineNum) {
        lines.remove(lineNum);
        return;
    }
    
    /**
     * Removes the matching lines
     * 
     * @param matchStrings Array of lines to be deleted.
     */
    public void removeMatchingLines(String matchStrings[]) {
        int lineNum = -1;
        for (int i = 0; i < matchStrings.length; i++) {
            lineNum = lineContains(matchStrings[i]);
            if (lineNum >= 0) {
                removeLine(lineNum);
            }
        }
    }
    
    /**
     * Inserts given line into the file 
     * 
     * @param lineNum Line number where the line should inserted.
     * @param text Line to be inserted.
     */
    public void insertLine(int lineNum, String text) {
        lines.add(lineNum, text);
        return;
    }
    
    /**
     * Replaces the line with the given test.
     * 
     * @param lineNum Line number to be replaced.
     * @param text Text to be replaced.
     * @return
     */
    public String replaceLine(int lineNum, String text) {
        String retVal = (String) lines.remove(lineNum);
        lines.add(lineNum, text);
        return retVal;
    }
    
    /**
     * Appends the line to the end of the file
     * @param line Line to be appended
     */
    public void appendLine(String line) {
        if (lines == null) {
            throw new RuntimeException("File is already closed.");
        }
        lines.add(line);
        return;
    }
    
    /**
     * Writes back all the lines to the file.  This commits all the changes
     * made to the file to the physical file. 
     * 
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        int size = lines.size();
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        for (int i = 0; i < size; i++) {
            out.write((String) lines.get(i));
            if (i != size) {
                out.write('\n');
            }
        }
        out.close();
    }
}
