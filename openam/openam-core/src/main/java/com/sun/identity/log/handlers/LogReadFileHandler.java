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
 * $Id: LogReadFileHandler.java,v 1.8 2009/04/07 23:21:01 hvijay Exp $
 *
 */
/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogQuery;
import com.sun.identity.log.LogReader;
import com.sun.identity.log.QueryElement;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.util.LogRecordSorter;

/**
 * LogReadFileHandler class implements LogReadHandler interface.
 * This class name will be stored as a configuration parameter.
 * LogReader will instantiate it at run time (when messages are logged
 * into file). This class reads the disk file, applies query if any,
 * sorts records on field name when required, collects most recent records
 * (default option) or all records. It returns result 2D String to the
 * caller (LogReader), it also exports a method that returns archived filename
 * present in current set.
 **/
public class LogReadFileHandler implements LogReadHandler {
    // private attributes

    private String logFileName;
    private com.sun.identity.log.LogQuery queryChriteria = null;
    private int maxNoOfRecs = LogQuery.MOST_RECENT_MAX_RECORDS;
    private String version = "#Version:";
    private String fieldName = "#Fields: ";
    private LogRecordSorter sorter = null;
    private String[][] queryResult;    // will hold the return value
    // internal storage for records
    private ArrayList listOfValidRecords = new ArrayList();
    private ArrayList columnIndices = null;

    /** constructor does nothing
     **/
    public LogReadFileHandler() {
    }

    /**
     * LogReader calls this method method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param fileName is complete filename with path
     * @param logQuery is user specified qury chriteria with sorting requirement
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws Exception if it fails any of operation.
     */
    public String[][] logRecRead(String fileName, LogQuery logQuery,
            boolean sourceData)
            throws IOException, NoSuchFieldException,
            IllegalArgumentException, RuntimeException,
            Exception {
        // if the object is persistence use it otherwise don't
        this.cleaner();
        this.logFileName = fileName;
        boolean hdrExist = false;
        if (sourceData == true) {
            queryChriteria = logQuery;
        }
        // to collect field names
        try {
            hdrExist = this.getFieldNames(true, logQuery);
        } catch (IOException e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        } catch (RuntimeException e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        } catch (Exception e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        }
        if (hdrExist == false) {
            return queryResult;
        }
        if (logQuery != null) {
            // caller has to handle what is MOST_RECENT_MAX_RECORDS
            // this class has no idea about the value
            if ((logQuery.getNumRecordsWanted() ==
                    LogQuery.MOST_RECENT_MAX_RECORDS) ||
                    (logQuery.getNumRecordsWanted() < LogQuery.ALL_RECORDS)) {
                this.maxNoOfRecs = 1;
            } else {
                this.maxNoOfRecs = logQuery.getNumRecordsWanted();
            }
        } else {
            this.maxNoOfRecs = 1;// can't be 0, headers will be returned
        }
        try {
            if (sourceData == true) {
                this.getRecords(true);
            } else {
                this.getRecords(false);
            }
        } catch (IOException e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        } catch (IllegalArgumentException e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        } catch (RuntimeException e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        } catch (Exception e) {
            throw e; // catch & rethrow it else it cud be caught in other place
        }

        int recSize = listOfValidRecords.size();
        // checks whether it has got any record or not
        if (recSize <= 0) {
            // if no record found return null
            return null;
        }

        // if Sorting is specified
        if (queryChriteria != null) {
            String sortByField = queryChriteria.getSortingField();
            if (sortByField != null) {
                try {
                    this.sorter = new LogRecordSorter(sortByField,
                            listOfValidRecords);
                    queryResult = this.sorter.getSortedRecords();
                    return (queryResult);
                } catch (NoSuchFieldException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (IllegalArgumentException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (RuntimeException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (Exception e) {
                    throw e;// catch & rethrow else cud be caught in other place
                // don't do any processing
                }
            }
        }
        queryResult = new String[recSize][];
        for (int i = 0; i < recSize; i++) {
            queryResult[i] = (String[]) listOfValidRecords.get(i);
        }
        return queryResult;
    }

    /**
     * LogReader calls this method method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param fileNames is a Set of filenames complete with path
     * @param logQuery is user specified qury chriteria with sorting requirement
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws Exception if it fails any of operation.
     */
    public String[][] logRecRead(
            Set fileNames,
            LogQuery logQuery,
            boolean sourceData) throws IOException, NoSuchFieldException, IllegalArgumentException,
            RuntimeException, Exception {
        // if the object is persistence use it otherwise don't
        this.cleaner();

        Set fNames = new HashSet();
        boolean isFirstFile = true;
        for (Iterator it = fileNames.iterator(); it.hasNext();) {
            String ss = (String) it.next();
            fNames.add(ss);
            this.logFileName = ss;

            if (Debug.messageEnabled()) {
                Debug.message("File:logRecRead/2: processing file " + ss +
                        ", sourceData = " + sourceData);
            }

            boolean hdrExist = false;
            if (sourceData == true) {
                queryChriteria = logQuery;
            }
            // to collect field names
            try {
                hdrExist = this.getFieldNames(isFirstFile, logQuery);
                isFirstFile = false;
            } catch (IOException e) {
                throw e; // catch & rethrow it
            } catch (RuntimeException e) {
                throw e; // catch & rethrow it
            } catch (Exception e) {
                throw e; // catch & rethrow it
            }
            if (hdrExist == false) {
                return queryResult;
            }

            //
            //  maxNoOfRecs starts out as MOST_RECENT_MAX_RECORDS (-1).
            //
            //  when MOST_RECENT_MAX_RECORDS (-1) specified in the LogQuery,
            //  LogReader will change it to the value read from the
            //  attribute LogConstants.MAX_RECORDS.
            //
            //  getNumRecordsWanted will be the max (default 500),
            //  ALL_RECORDS (-2), or some positive non-zero number
            //

            if (logQuery != null) {
                // caller has to handle what is MOST_RECENT_MAX_RECORDS
                // this class has no idea about the value
                if ((logQuery.getNumRecordsWanted() ==
                        LogQuery.MOST_RECENT_MAX_RECORDS) ||
                        (logQuery.getNumRecordsWanted() < LogQuery.ALL_RECORDS)) {
                    this.maxNoOfRecs = 1;
                } else {
                    this.maxNoOfRecs = logQuery.getNumRecordsWanted();
                }
            } else {
                this.maxNoOfRecs = 1;// can't be 0, headers will be returned
            }

            //
            //  getRecordsMulti() uses maxNoOfRecs
            //

            try {
                if (sourceData == true) {
                    this.getRecordsMulti(true);
                } else {
                    this.getRecordsMulti(false);
                }
            } catch (IOException e) {
                throw e; // catch & rethrow
            } catch (IllegalArgumentException e) {
                throw e; // catch & rethrow
            } catch (RuntimeException e) {
                throw e; // catch & rethrow
            } catch (Exception e) {
                throw e; // catch & rethrow
            }
        } // end of for loop for all files

        int recSize = listOfValidRecords.size();

        // checks whether it has got any record or not
        if (recSize <= 0) {
            // if no record found return null
            return null;
        }

        // if Sorting is specified
        if (queryChriteria != null) {
            String sortByField = queryChriteria.getSortingField();
            if (sortByField != null) {
                try {
                    this.sorter = new LogRecordSorter(sortByField,
                            listOfValidRecords);
                    queryResult = this.sorter.getSortedRecords();
                    return (queryResult);
                } catch (NoSuchFieldException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (IllegalArgumentException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (RuntimeException e) {
                    throw e;// catch & rethrow else cud be caught in other place
                } catch (Exception e) {
                    throw e;// catch & rethrow else cud be caught in other place
                // don't do any processing
                }
            }
        }
        queryResult = new String[recSize][];
        for (int i = 0; i < recSize; i++) {
            queryResult[i] = (String[]) listOfValidRecords.get(i);
        }
        return queryResult;
    }

    // This method collects all the ELF header fields.
    private boolean getFieldNames(boolean addFields, LogQuery logQry)
            throws IOException,
            RuntimeException, Exception {
        String fieldBuffer;
        boolean foundHeader = false;
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(this.logFileName));

            while ((fieldBuffer = fileReader.readLine()) != null) {
                if (fieldBuffer.trim().length() <= 0) {
                    continue;  // invalid, so ignore
                }
                if (fieldBuffer.startsWith(version) == true) {
                    continue;  // line contain version information
                }
                if (fieldBuffer.startsWith(fieldName) == true) {
                    String hdrStr = fieldBuffer.substring(fieldName.length());
                    if (foundHeader == true) {
                        break; // this is error.
                    }
                    ArrayList tmpList = getFields(hdrStr, true);
                    String[] spltHdrStr = null;

                    ArrayList al = null;
                    if (logQry != null) {
                        al = logQry.getColumns();
                        if (al != null) {
                            columnIndices = new ArrayList();
                            spltHdrStr = new String[al.size()];
                        } else {
                            spltHdrStr = new String[tmpList.size()];
                        }
                    } else {
                        spltHdrStr = new String[tmpList.size()];
                    }

                    int j = 0;
                    int tmpListSz = tmpList.size();

                    for (int i = 0; i < tmpListSz; i++) {
                        String tmps = (String) tmpList.get(i);
                        if (al != null) {
                            if (al.contains(tmps)) {
                                columnIndices.add(j, Integer.toString(i));
                                spltHdrStr[j++] = tmps;
                            }
                        } else {
                            spltHdrStr[j++] = tmps;
                        }
                    }

                    //
                    //  don't add Field names "record" if not first file
                    //
                    //  also find out if specific columns (fields) where
                    //  requested.  if so, then only include those, and
                    //  make a list of indices that should be stored from
                    //  the subsequent records read.
                    //
                    if (addFields) {
                        listOfValidRecords.add(spltHdrStr);
                    }
                    foundHeader = true;
                    break;
                }
            }
            fileReader.close();
        } catch (IOException e) {
            String msg = "Problem in reading " + logFileName;
            throw new IOException(msg);
        } catch (RuntimeException e) {
            String msg = "Problem in reading " + logFileName;
            throw new RuntimeException(msg);
        } catch (Exception e) {
            throw e;
        }
        return (foundHeader);
    }

    // reads file, splits into fields, validates field values and collects it.
    private boolean getRecords(boolean isSourceData)
            throws IOException, RuntimeException {
        String bufferedStr;
        String dummyStr = null;
        StringBuilder dummySbuf = new StringBuilder(" ");
        int dcnt = 0;

        if (columnIndices != null) {
            dcnt = columnIndices.size();
        } else {
            dcnt = LogConstants.MAX_FIELDS + 2; // max cols for secure record
        }
        for (int i = 1; i < dcnt; i++) {
            dummySbuf.append("\t ");
        }
        dummyStr = dummySbuf.toString();

        BufferedReader flRead = null;
        try {
            flRead = new BufferedReader(new FileReader(logFileName));
            while ((bufferedStr = flRead.readLine()) != null) {
                if (bufferedStr.trim().length() <= 0) {
                    if (LogReader.isLogSecure()) {
                        Debug.error("LogReadFileHandler.getRecords: " +
                                "Blank line in secure log");
                        bufferedStr = dummyStr;
                    } else {
                        continue; // no field value, so ignore
                    }
                }
                if (bufferedStr.startsWith(version) == true) {
                    continue; // line contains version of the elf file
                }
                if (bufferedStr.startsWith(fieldName) == true) {
                    continue; // header already collected, ignore it
                }
                // temporary field holder
                ArrayList listOfFields = new ArrayList();
                listOfFields = this.getFields(bufferedStr, isSourceData);

                String[] spltStrArr = null;
                if (columnIndices != null) {
                    spltStrArr = new String[columnIndices.size()];
                } else {
                    spltStrArr = new String[listOfFields.size()];
                }

                int j = 0;
                for (int i = 0; i < listOfFields.size(); i++) {
                    if (columnIndices != null) {
                        if (columnIndices.contains(Integer.toString(i))) {
                            spltStrArr[j++] = (String) listOfFields.get(i);
                        }
                    } else {
                        spltStrArr[i] = (String) listOfFields.get(i);
                    }
                }
                if (queryChriteria == null) {
                    this.collect(spltStrArr);
                } else if (this.applyQuery(spltStrArr) == true) {
                    this.collect(spltStrArr);
                }
                continue;
            }
        } catch (RuntimeException e) {
            String msg = "Problem in reading " + logFileName;
            throw new IOException(msg);
        } catch (Exception e) {
            String msg = "Problem in reading " + logFileName;
            throw new RuntimeException(msg);
        } finally {
            flRead.close();
        }

        return true;
    }

    //
    // reads file, splits into fields, validates field values and collects it.
    // this version is for the multi-file read.
    //
    // collect the records locally, then add them to the global
    // listOfValueRecords
    //
    private boolean getRecordsMulti(boolean isSourceData)
            throws IOException, RuntimeException {
        String bufferedStr;
        int localNumRecs = 0;
        ArrayList localListOfValidRecords = new ArrayList();
        try {
            BufferedReader flRead = new BufferedReader(new FileReader(logFileName));
            while ((bufferedStr = flRead.readLine()) != null) {
                if (bufferedStr.trim().length() <= 0) {
                    continue; // no field value, so ignore
                }
                if (bufferedStr.startsWith(version) == true) {
                    continue; // line contains version of the elf file
                }
                if (bufferedStr.startsWith(fieldName) == true) {
                    continue; // header already collected, ignore it
                }
                // temporary field holder
                ArrayList listOfFields = new ArrayList();
                listOfFields = this.getFields(bufferedStr, isSourceData);
                String[] spltStrArr = null;

                if (columnIndices != null) {
                    spltStrArr = new String[columnIndices.size()];
                } else {
                    spltStrArr = new String[listOfFields.size()];
                }

                int j = 0;

                for (int i = 0; i < listOfFields.size(); i++) {
                    if (columnIndices != null) {
                        if (columnIndices.contains(Integer.toString(i))) {
                            spltStrArr[j++] = (String) listOfFields.get(i);
                        }
                    } else {
                        spltStrArr[i] = (String) listOfFields.get(i);
                    }
                }

                int rec_size = localListOfValidRecords.size();

                if ((queryChriteria == null) ||
                        (this.applyQuery(spltStrArr) == true)) {
                    if (this.maxNoOfRecs != LogQuery.ALL_RECORDS) {
                        if (localNumRecs > this.maxNoOfRecs) {
                            localListOfValidRecords.remove(1);
                        }
                    }
                    localListOfValidRecords.add(spltStrArr);
                    localNumRecs++;
                }
            }

            //
            //  might have to do something about max most recent...
            //
            if (localNumRecs > this.maxNoOfRecs) {
                localNumRecs = this.maxNoOfRecs;
            }

            String[] tmpxx = null;
            for (int i = 0; i < localNumRecs; i++) {
                tmpxx = (String[]) localListOfValidRecords.get(i);
                listOfValidRecords.add(tmpxx);
            }

            flRead.close();

        } catch (RuntimeException e) {
            String msg = "Problem in reading " + logFileName;
            throw new IOException(msg);
        } catch (Exception e) {
            String msg = "Problem in reading " + logFileName;
            throw new RuntimeException(msg);
        }

        return true;
    }

    // applies query to find out whether the record matches or not
    private boolean applyQuery(String[] recordToBeQueried) {
        ArrayList queries = (ArrayList) queryChriteria.getQueries();
        if (queries == null) {
            return (true);
        }
        int qrySz = queries.size();
        if (qrySz == 0) {
            return (true);
        }
        int queryCondition = queryChriteria.getGlobalOperand();
        boolean isMatch = false;
        for (int i = 0; i < qrySz; i++) {
            isMatch = false;
            isMatch = doMatch(recordToBeQueried, (QueryElement) queries.get(i));

            if (queryCondition ==
                    com.sun.identity.log.LogQuery.MATCH_ALL_CONDITIONS) {
                if (isMatch == false) {
                    return (isMatch);
                }
            } else if (queryCondition ==
                    com.sun.identity.log.LogQuery.MATCH_ANY_CONDITION) {
                if (isMatch == true) {
                    return (isMatch);
                }
            }
        }
        return (isMatch);
    }

    // checks whether record is a match or not
    private boolean doMatch(String[] record, QueryElement elem) {
        boolean isFound = false;
        int fieldPos = -1;
        String[] fields = (String[]) listOfValidRecords.get(0);
        int fieldNo = fields.length;
        String fldName = elem.getFieldName();
        String fldValue = elem.getFieldValue();
        // find the field position first
        for (int i = 0; i < fieldNo; i++) {
            if (fldName.compareTo(fields[i]) == 0) {
                fieldPos = i;
                break;
            }
        }
        if (fieldPos == -1) {
            return (isFound);
        }
        int result = 0;
        int rel = elem.getRelation();

        switch (rel) {
            case QueryElement.CN:  // contains
                return (record[fieldPos].indexOf(fldValue) != -1);
            case QueryElement.SW:  // starts with
                return (record[fieldPos].startsWith(fldValue));
            case QueryElement.EW:  // ends with
                return (record[fieldPos].endsWith(fldValue));
            default:
                result = record[fieldPos].compareTo(fldValue);
        }

        switch (rel) {
            case QueryElement.EQ:
                if (result == 0) {
                    return (true);
                }
                break;
            case QueryElement.LT:
                if (result < 0) {
                    return (true);
                }
                break;
            case QueryElement.GT:
                if (result > 0) {
                    return (true);
                }
                break;
            case QueryElement.LE:
                if (result <= 0) {
                    return (true);
                }
                break;
            case QueryElement.GE:
                if (result >= 0) {
                    return (true);
                }
                break;
            case QueryElement.NE:
                if (result != 0) {
                    return (true);
                }
                break;
            default:
                break;
        }
        return (false);
    }

    // It collects the records into the internal list
    // takes care of storing latest result, pushing earliest out.
    private void collect(String[] recordValues) {
        int rec_size = listOfValidRecords.size();
        if (this.maxNoOfRecs != LogQuery.ALL_RECORDS) {
            if (rec_size > this.maxNoOfRecs) {
                shiftRecordsUpward();
            }
        }
        appendRecord(recordValues);
    }

    // this method appends the record to the internal data
    private void appendRecord(String[] record) {
        listOfValidRecords.add(record);
    }

    // removes oldest or 1st record and shifts rest of the records upward
    private void shiftRecordsUpward() {
        if (listOfValidRecords.size() <= 1) {
            return;
        }
        listOfValidRecords.remove(1);
    }

    // method to split ELF formatted line into fields
    private ArrayList getFields(String bufferedStr,
            boolean source) {
        String str = bufferedStr.trim();
        ArrayList fields = new ArrayList();

        char quote = '"';
        char new_line = '\n';
        char cr_return = '\r';
        int current_position = 0;
        boolean isNewField = true;
        String tmpStr = new String();
        int str_len = str.length();
        char read_char;

        while (current_position < str_len) {
            read_char = str.charAt(current_position);
            if (read_char == quote) {
                /* Assumptions::
                1st character is a quote. i.e. Substrings starts with quote.
                There could be quote within the desired sub string.
                If the sub string contains a quote,
                it will be precceded with another quote. i.e. the
                substring can contain even number of quotes,
                not odd numbere of quotes.
                The ending position of the substring will be a single quote
                i.e. a detached quote, no quote to follow immediately.
                 */
                String sub_str = str.substring(current_position);
                int current_sub_position = 1;
                char ch;
                boolean quotedFieldAdded = false;
                String buffer = new String();
                if (source == false) {
                    buffer += quote;
                }
                boolean pending_quote = false;
                while (current_sub_position < str_len) {
                    ch = sub_str.charAt(current_sub_position);
                    if ((ch != quote) && (ch != new_line) &&
                            (ch != cr_return)) {
                        buffer += ch;
                        ++current_sub_position;
                        continue;
                    }
                    if (current_sub_position == (sub_str.length() - 1)) {
                        if (source == false) {
                            buffer += ch;
                        } else {
                            if (ch != quote) {
                                buffer += ch;
                            }
                        }
                        ++current_sub_position;
                        fields.add(buffer);
                        current_position += current_sub_position;
                        quotedFieldAdded = true;
                        break;
                    }
                    if ((sub_str.charAt(current_sub_position + 1) != quote) &&
                            (sub_str.charAt(current_sub_position + 1) != new_line) &&
                            (sub_str.charAt(current_sub_position + 1) != cr_return)) {
                        if (source == false) {
                            buffer += ch;
                        }
                        ++current_sub_position;
                        if (pending_quote == true) {
                            pending_quote = false;
                            continue;
                        }
                        fields.add(buffer);
                        current_position += current_sub_position;
                        quotedFieldAdded = true;
                        break;
                    }
                    if (source == false) {
                        buffer += ch;
                        pending_quote = true;
                    } else {
                        if (pending_quote == false) {
                            buffer += ch;
                            pending_quote = true;
                        } else {
                            pending_quote = false;
                        }
                    }
                    ++current_sub_position;
                }
                if (quotedFieldAdded != true) {
                    fields.add(buffer);
                    current_position += current_sub_position;
                }
                continue;
            } else if ((read_char == ' ') ||
                    (read_char == '\t') ||
                    (read_char == '\f') ||
                    (read_char == '\r') ||
                    (read_char == '\n')) {
                if (isNewField == true) {
                    // fields are seperated by whitespace(s)
                    // only String can contain whitespace(s)
                    if (tmpStr.length() != 0) {
                        fields.add(tmpStr);
                    }
                    tmpStr = new String();
                    ++current_position;
                    continue;
                }

                isNewField = true;
                ++current_position;
                continue;
            } else {
                if (isNewField == true) {
                    tmpStr += read_char;
                    ++current_position;
                    if (current_position == str_len) {
                        fields.add(tmpStr);
                    }
                    continue;
                }
            }
        }
        return fields;
    }

    // below method reset previous readings and prepare for new one.
    private void cleaner() {
        this.logFileName = null;
        this.queryChriteria = null;
        this.listOfValidRecords.clear();
        this.queryResult = null;
        return;
    }
}
