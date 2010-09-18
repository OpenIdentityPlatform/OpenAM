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
 * $Id: FileEditor.java,v 1.2 2008/06/25 05:51:29 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class that provides functionality to match the "lines" with specified 
 * pattern and remove them from the specified file.
 * 
 */
public class FileEditor {

    public FileEditor(String fileName) {
        setFileName(fileName);
    }

    /**
     * 
     * @param matchPatterns
     *            a Set of DeletePatterns
     * @return true if all the patterns were found and deleted. False if some
     *         patterns were not found. In such cases no changes are made to 
     *         the file.
     * @throws Exception
     */
    public boolean deleteLines(Set matchPatterns) throws Exception {
        boolean success = false;

        Map patternOccurrances = getPatternOccurences(matchPatterns);
        if (patternOccurrances.size() == matchPatterns.size()) {
            TreeSet removeLines = addSuccessiveDeleteLines(matchPatterns,
                    patternOccurrances);
            success = deleteLineNumbers(removeLines);
        }

        return success;
    }

    public boolean deleteLines(DeletePattern pattern) throws Exception {
        Set matchPatterns = new HashSet();
        matchPatterns.add(pattern);
        Map patternOccurrances = getPatternOccurences(matchPatterns);

        boolean success = false;
        if (patternOccurrances.size() == 1) { // Just one match should be
                                                // found
            TreeSet removeLines = addSuccessiveDeleteLines(matchPatterns,
                    patternOccurrances);
            success = deleteLineNumbers(removeLines);
        }

        return success;
    }

    private TreeSet addSuccessiveDeleteLines(Set matchPatterns,
            Map patternOccurrances) {
        TreeSet removeLines = new TreeSet(patternOccurrances.values());
        Iterator iter = matchPatterns.iterator();
        while (iter.hasNext()) {
            DeletePattern dp = (DeletePattern) iter.next();
            Integer lineNumber = (Integer) patternOccurrances.get(dp
                    .getPattern());
            int numSuccessiveLines = dp.getNumberOfSuccessiveLines();
            for (int i = 1; i <= numSuccessiveLines; i++) {
                removeLines.add(new Integer(lineNumber.intValue() + i));
            }
        }

        return removeLines;
    }

    private boolean deleteLineNumbers(TreeSet lineNumbers) throws Exception {

        // Get the line numbers and sort them using TreeSet
        Debug.log("FileEditor.deleteLineNumbers() - Lines that "
                + "will be skipped: " + lineNumbers);

        TreeSet removeLineNumbers = new TreeSet(lineNumbers);

        LineNumberReader reader = null;
        BufferedWriter writer = null;
        try {
            FileInputStream fi = new FileInputStream(getFileName());
            InputStreamReader ir = new InputStreamReader(fi);
            reader = new LineNumberReader(ir);
            reader.setLineNumber(0);

            FileOutputStream fo = new FileOutputStream(getTempFileName());
            OutputStreamWriter ow = new OutputStreamWriter(fo);
            writer = new BufferedWriter(ow);

            String lineData = null;
            Integer removeLine = (Integer) removeLineNumbers.first();
            while ((lineData = reader.readLine()) != null) {
                removeLine = verifyAndDeleteLine(reader, writer, lineData,
                        removeLineNumbers, removeLine);
            }
        } catch (Exception e) {
            Debug.log("FileEditor.removeLines()  - Exception occurred "
                    + "while removing lines from file. " + getFileName(), e);
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ie) {
                    // Ignore
                }
            }
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ie) {
                }
            }
        }

        // Delete the original file and rename temp file as original
        File originalFile = new File(getFileName());
        originalFile.delete();

        File tempFile = new File(getTempFileName());
        tempFile.renameTo(originalFile);

        return removeLineNumbers.isEmpty(); // If empty then successful
    }

    private Integer verifyAndDeleteLine(LineNumberReader reader,
            BufferedWriter writer, String lineData, TreeSet removeLineNumbers,
            Integer removeLine) throws IOException {
        Integer nextLineToRemove = removeLine;
        int lineNumber = reader.getLineNumber();
        if (!removeLineNumbers.isEmpty() && removeLine.intValue() == 
            lineNumber) 
        {
            // Determine next line to skip from being written to file
            Debug.log("FileEditor.skipLinesAndCreateTempFile() - "
                    + "Skipping line[" + lineNumber + "]=" + lineData);
            removeLineNumbers.remove(removeLine);
            if (!removeLineNumbers.isEmpty()) {
                nextLineToRemove = (Integer) removeLineNumbers.first();
            }
        } else {
            writer.write(lineData + LINE_SEP);
        }

        return nextLineToRemove;
    }

    public Map getPatternOccurences(Set matchPatterns) throws Exception {
        boolean matchFound = false;

        // A Map which stores key=value as
        // pattern-string=line-number-to-be-removed
        Map matchedLines = new HashMap();

        LineNumberReader reader = null;
        try {
            FileReader fr = new FileReader(getFileName());
            reader = new LineNumberReader(fr);
            reader.setLineNumber(0);

            String lineData = null;
            while ((lineData = reader.readLine()) != null) {
                int lineNumber = reader.getLineNumber();
                matchAndAddLineNumbers(matchPatterns, lineData, lineNumber,
                        matchedLines);
            }
        } catch (Exception e) {
            Debug.log("FileEditor.getPatternOccurences() - Exception "
                    + "occurred while searching for patterns. ", e);
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ie) {
                    // Ignore
                }
            }
        }
        return matchedLines;
    }

    private void matchAndAddLineNumbers(Set matchPatterns, String lineData,
            int lineNumber, Map matchedLines) {
        boolean isFound = false;
        Iterator iter = matchPatterns.iterator();
        while (iter.hasNext() && !isFound) {
            // Iterate through all active patterns and see if they are present
            // in line
            MatchPattern matchPattern = (MatchPattern) iter.next();
            if (matchPattern.isActive() && matchPattern.isPresent(lineData)) {
                Debug.log("FileEditor.matchAndAddLineNumbers() - Found "
                        + "pattern " + matchPattern);
                // Add the pattern & line number to the Map.
                Integer number = new Integer(lineNumber);
                matchedLines.put(matchPattern.getPattern(), number);
                if (!matchPattern.isMatchForLastOccurranceInFile()) {
                    // We don't want this match to be active anymore as the
                    // first occurance is already found.
                    matchPattern.setIsActiveFlag(false);
                }
                isFound = true;
            }
        }
    }

    private void printMap(Map map) {
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            Debug.log("FileEditor.printMap() - " + me.getKey() + "="
                    + me.getValue());
        }
    }

    private String getTempFileName() {
        return getFileName() + STR_TEMP_FILE_SUFFIX;
    }

    private String getFileName() {
        return fileName;
    }

    private void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String fileName;

    public static final String LINE_SEP = System.getProperty("line.separator");

    public static final String STR_TEMP_FILE_SUFFIX = ".tmp";

}
