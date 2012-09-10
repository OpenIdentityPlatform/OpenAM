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
 * $Id: DBFormatter.java,v 1.11 2009/08/19 21:12:50 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.log.handlers;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.spi.ITimestampGenerator;

/**
 * This Formatter provides support for formatting LogRecords that will help
 * Database Logging.
 * <p>
 * Typically this Formatter will be associated with a DBHandler(a handler meant
 * to handle Database logging). <tt> DBFormatter </TT> takes a LogRecord and
 * converts it to a Formatted string which DBHandler can understand.
 *
 */
public class DBFormatter extends Formatter {
    
    private LogManager lmanager = LogManagerUtil.getLogManager();
    private ITimestampGenerator secureTimestampGenerator;

    private String dateTimeFormat = null;
    private boolean isMySQL = false;
    /** max length of literal for Oracle */
    private static final int MAX_LITERAL_LENGTH = 4000;
    
    /**
     * Creates <code>DBFormatter</code> object
     */
    public DBFormatter() {
        String timestampGeneratorClass = 
            lmanager.getProperty(LogConstants.SECURE_TIMESTAMP_GENERATOR);
        try {
            Class clz = Class.forName(timestampGeneratorClass);
            secureTimestampGenerator = (ITimestampGenerator)clz.newInstance();
        } catch (ClassNotFoundException cnfe) {
            Debug.error("DBFormatter: TimeStamp Generator Class " +
                "not found", cnfe);
        } catch (InstantiationException ie) {
            Debug.error("DBFormatter: Timestamp Generator Could not " +
                "be Instantiated", ie);
        } catch (IllegalAccessException iae) {
            Debug.error("DBFormatter: Timestamp Generator Could not " +
                "be Instantiated", iae);
        }
        String driver = lmanager.getProperty(LogConstants.DB_DRIVER);
        if ((driver == null) || (driver.length() == 0)) {
            Debug.error("DBFormatter:DB driver not provided; assume not MySQL");
        } else {
            if(driver.toLowerCase().indexOf("oracle") != -1) {
                isMySQL = false;
                dateTimeFormat =
                    lmanager.getProperty(LogConstants.ORA_DBDATETIME_FORMAT);
            } else if (driver.toLowerCase().indexOf("mysql") != -1) {
                isMySQL = true;
                dateTimeFormat =
                    lmanager.getProperty(LogConstants.MYSQL_DBDATETIME_FORMAT);
            } else {
                isMySQL = false;
                dateTimeFormat =
                    lmanager.getProperty(LogConstants.ORA_DBDATETIME_FORMAT);
                Debug.error("DBFormatter:assuming driver: '" + driver +
                    "' is Oracle-compatible.");
            }
        }
    }

    /**
     * Returns the set of all fields converted into a COMMA seperated 
     * string. A typical sql query for logging a record looks like this. <p>
     * insert into table "amSSO_access" (time, data, loginid, domain, level,
     * ipAddress, hostname) values('10:10:10', '10th June, 2002',
     * ..., ..., ...)<p>
     * The getHead method returns back the set of all fields converted into a
     * COMMA seperated string. It is the duty of the formatter to fetch the all
     * field set from the LogManager and convert into a COMMA seperated string.
     * By doing this the handler can be kept independent of the all field and
     * selected field set.
     *
     * @param h The target handler (can be null)
     * @return the set of all fields converted into a COMMA seperated string.
     */
    @Override
    public String getHead(Handler h) {
        String retString = lmanager.getProperty(LogConstants.ALL_FIELDS);
        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter: Returned String from getHead is " 
                + retString);
        }
        return retString;
    }
    
    /**
     * Returns a null string whenever called.
     * @param h The target handler (can be null)
     * @return a null string whenever called.
     */
    @Override
    public String getTail(Handler h) {
        return "";
    }

    /**
      * Format the given LogRecord and return back a formatted String.
      * <p>
      * The formatted String has the values of the fields which are selected and
      * NULL if any field is not selected. All fields are enclosed in single-
      * quotes.
      * <p>
      * A typical formatted string can be given as follows:
      * '10:10:10', '10th June, 2002', 'NULL', 'NULL',
      * 'Session Created Successfull', 'INFO', 'NULL', 'NULL'
      * <p>
      * This formatted string will be enclosed within braces by Handler to
      * construct the query string.
      * 
      * @param logRecord the log record to be formatted.
      * @return formatted string.
      */
    public String format(java.util.logging.LogRecord logRecord) {
        Map logInfoTable = null;
        if ((LogManagerUtil.isAMLoggingMode()) &&
            (logRecord instanceof com.sun.identity.log.ILogRecord)) {
            logInfoTable = ((com.sun.identity.log.ILogRecord) logRecord)
                .getLogInfoMap();
        }
        StringBuilder sbuffer = new StringBuilder();
        String strTime;
        if(secureTimestampGenerator != null) {
            strTime = secureTimestampGenerator.getTimestamp();
        } else {
            strTime ="";
        }
        /*
         *  currently assuming that the date/time comes back in
         *  the "yyyy-mm-dd hh:mn:ss" format (24hr).  if it changes
         *  then there'll need to be a change to the dbdate-format 
         *  attribute.
         */
        String toDate = null;
        if (!isMySQL) {
            toDate = "TO_DATE('";
        } else {
            toDate = "STR_TO_DATE('";
        }
        sbuffer.append(toDate);
        sbuffer.append(strTime);
        sbuffer.append("', '");
        sbuffer.append(dateTimeFormat);
        sbuffer.append("'), ");
        /* Need to check for single-quote in the DATA field to be written
         * to the db
         */
        String tstr = formatMessage(logRecord);

        if ((tstr == null) || (tstr.length() <= 0)) {
            tstr = LogConstants.NOTAVAIL;
        } else if (tstr.length() > 0 ) {
            String str1 = tstr;
            if (tstr.indexOf("'") != -1) {
                str1 = checkEscapes(tstr, "'", "''");
            }
            String str2 = str1;
            if (isMySQL) {
                // MySQL has a problem with backslash
                if (str1.indexOf("\\") != -1) {
                    str2 = checkEscapes(str1, "\\", "\\\\");
                }
            } else {
                // Currently MySQL and Oracle are supported. If isMySQL is not true, we assume it is Oracle. 
                // Split string data since Oracle only accept string literal less than 4000 bytes.
                int splitLength = MAX_LITERAL_LENGTH / 4; // consider multi byte charactor set
                 if (str1.length() >= splitLength){
                    StringBuilder strBuilder = new StringBuilder();
                    int beginIndex = 0;
                    int endIndex = splitLength;
                    if (str1.length() >= splitLength) {
                        strBuilder.append("'");
                        while (str1.length() > beginIndex) {
                            if (endIndex > str1.length()) {
                                endIndex = str1.length();
                            }
                            strBuilder.append(" || TO_CLOB('");
                            strBuilder.append(str1.substring(beginIndex, endIndex));
                            strBuilder.append("')");
                            beginIndex = beginIndex + splitLength;
                            endIndex = endIndex + splitLength;
                        }
                        strBuilder.append(" || '");
                    }
                    str2 = strBuilder.toString();
                }
            }
            tstr = str2;
        }
        sbuffer.append("'").append(tstr).append("', ");
        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter:thisfield3 = #" + sbuffer.toString()
                + "#");
        }
        String[] allFields = lmanager.getAllFields();
        Set selectedFields = lmanager.getSelectedFieldSet();
        int len = 0;
        if (allFields != null) {
            len = allFields.length;
        }
        for (int i = 2; i < len - 1; i ++) { // first 2 fields are compulsory
            if ((logInfoTable != null) &&
            (selectedFields != null) &&
            (selectedFields.contains(allFields[i]))) {
                //
                // if there are any single-quotes in the data, they have to be
                // made double-single-quotes, so it'll pass through sql
                //
                String tempstr = (String)logInfoTable.get(allFields[i]);
                if ((tempstr != null) &&
                    (tempstr.length() > 0) &&
                    (tempstr.indexOf("'") != -1)
                ) {
                    StringTokenizer tmps = new StringTokenizer(tempstr, "'");
                    StringBuilder thisfield = new StringBuilder();
                    if (Debug.messageEnabled()) {
                        Debug.message("DBFormatter:found single-quote in: "
                                      + tempstr);
                    }
                    //
                    // funky case of "'" at the beginning
                    //
                    if (tempstr.indexOf("'") == 0) {
                        thisfield.append("''");
                        if (tmps.hasMoreTokens()) {
                            thisfield.append(tmps.nextToken());
                        }
                    } else {
                        if (tmps.hasMoreTokens()) {
                            thisfield.append(tmps.nextToken());
                        }
                    }

                    while (tmps.hasMoreTokens()) {
                        thisfield.append("''").append(tmps.nextToken());
                    }
                    //
                    // if string ends in "'"
                    //
                    if (tempstr.indexOf("'", tempstr.length()-1) != -1) {
                        thisfield.append("''");
                    }
                    tempstr = thisfield.toString();
                }
                if (tempstr == null) {
                    tempstr = LogConstants.NOTAVAIL;
                }
                sbuffer.append("'").append(tempstr).append("', ");
            } else {
                sbuffer.append("'").append(LogConstants.NOTAVAIL).append(
                    "'").append(", ");
            }
        }

        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter:format1: sbuffer = "
                          + sbuffer.toString());
        }

        if ((selectedFields != null) && (logInfoTable != null) &&
            (selectedFields.contains(allFields[len-1]))) {
            String tmpstr = (String)logInfoTable.get(allFields[len-1]);
            if (tmpstr == null) {
                tmpstr = LogConstants.NOTAVAIL;
            }
            sbuffer.append("'").append(tmpstr).append("'");
        } else {
            sbuffer.append("'").append(LogConstants.NOTAVAIL).append("'");
        }

        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter:format2: sbuffer = "
                    + sbuffer.toString());
        }

        return sbuffer.toString();
    }

    private String checkEscapes(String theString, String charToEscape,
        String doubledChar)
    {
        StringTokenizer tmps = new StringTokenizer(theString, charToEscape);
        StringBuilder thisfield = new StringBuilder();
        if (Debug.messageEnabled()) {
            Debug.message("DBFormatter:looking for " + charToEscape +
            " in data: " + theString);
        }

        /*
         * Weird cases of char at beginning or end of the data
         */
        if (theString.indexOf(charToEscape) == 0) {
            thisfield.append(doubledChar);
            if (tmps.hasMoreTokens()) {
                thisfield.append(tmps.nextToken());
            }
            if (Debug.messageEnabled()) {
                Debug.message("DBFormatter:thisfield1 = #" + 
                    thisfield.toString() + "#");
            }
        } else {
            if (tmps.hasMoreTokens()) {
                thisfield.append(tmps.nextToken());
            }
        }
        while (tmps.hasMoreTokens()) {
            thisfield.append(doubledChar).append(tmps.nextToken());
            if (Debug.messageEnabled()) {
                Debug.message("DBFormatter:thisfield2 = #" + 
                                  thisfield.toString() + "#");
            }
        }

        /*
         *  See if it ends in "'"
         */
        if (theString.indexOf(charToEscape, theString.length()-1) != -1) {
            thisfield.append(doubledChar);
        }

        return (thisfield.toString());
    }

}