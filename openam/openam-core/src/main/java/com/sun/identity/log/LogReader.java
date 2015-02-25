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
 * $Id: LogReader.java,v 1.7 2008/10/30 04:11:06 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.log.spi.Debug;

/**LogReader class provides mechanism to read a log file to the caller.
 * It does the authorization check, reads line from the file, applies the
 * query (if any), collects most recent records, sorts the records, and
 * returns the result in a two dimensional String. Where columns in the
 * the first row, i.e. 0th row, always holds the header info (field names)
 * present in the ELF formatted file.
 * Other rows hold the value present under those columns.
 * @supported.all.api
 */
public class LogReader {
    /* private attributes */
    private static int maxReordToReturn = 1;
    private static final String FILE_SOURCE = "File";
    private static java.util.logging.LogManager manager;
    private static String [][] queryResult = null;
    private static String logFileName = null;
    private static String logPathName = null;
    private static String logFields = null;
    private static String fileHandlerClass = null;
    private static String maxRecStr = null;
    private static String dbHandlerClass = null;
    private static String logStorageType = null;
    private static String logSecurity = null;
    private static String securityPrefix = "_secure";
    private static String loggerName;   /* name of the logger object */
    private static com.sun.identity.log.handlers.LogReadHandler currentHandler
        = null;
    private static com.sun.identity.log.handlers.LogReadDBHandler
        currentDBHandler = null;
    private static boolean logTypeIsFile = true;  /* default "File" */

    /* private constructor. Only assigns manager. */
    private LogReader() {
        this.manager = LogManagerUtil.getLogManager();
    }
    

    /**
     * Returns the units (LogConstants.NUM_BYTES or LogConstants.NUM_RECORDS)
     * that applies to the value returned by getSize(logName).
     *
     * @return the units applying to the return value of getSize(logName),
     *         LogConstants.NUM_BYTES (in the case of File logging), or
     *         LogConstants.NUM_RECORDS (in the case of DB logging).
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/

    public static int getSizeUnits()
        throws Exception
    {
        LogReader lr = new LogReader();
        try {
            lr.readConfiguration();
        } catch (Exception ex) {
            Debug.error("LogReader.getSizeUnits:could not read configuration");
            throw ex;
        }

        int i = LogConstants.NUM_RECORDS;
        if (logTypeIsFile) {
            return (LogConstants.NUM_BYTES);
        }
        return (i);
    }

    /**
     * Returns the number of LogRecords in the specified table in the DB.
     * In the case where logging is to a file, the number of bytes in
     * the specified file is returned.  Use getSizeUnits() to get which
     * units apply.
     *
     * @param logName the name of the Table or File.
     * @return the number of LogRecords (in the DB table), or number of
     *         bytes (in the file).
     * @throws IOException if file does not exist.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static long getSize(String logName)
        throws IOException, Exception
    {
        LogReader lr = new LogReader();
        try {
            lr.readConfiguration();
        } catch (Exception ex) {
            Debug.error("LogReader.getSize: could not read configuration");
            throw ex;
        }

        long li;
        if (logTypeIsFile) {
            String file = logPathName + logName;
            File thisFile = new File (file);
            if (thisFile.exists()) {
                li = thisFile.length();
            } else {
                throw new IOException(logName + " does not exist.");
            }
        } else {
            li = currentDBHandler.getNumberOfRows(manager, logName);
        }
        return li;
    }

    /**
     * Returns the names of the Log Files or Tables. The Set of names is
     * what is found in the directory or DB, filtered through a default
     * list of names.  Thus, log files or tables with custom names will
     * not be included.  This does not preclude querying those log files
     * or tables with custom names.
     *
     * @return the Log File/Table names in a Set.
     **/

    public static Set getLogNames() {
        LogReader lr = new LogReader();
        Set logNames = new HashSet();

        try {
            lr.readConfiguration();
        } catch (Exception ex) {
            return logNames;
        }

        if (logTypeIsFile) {
            File dir = new File(logPathName);
            String [] dirList = dir.list();
            if (dirList != null) {
                int numFiles = Array.getLength(dirList);
                int numDefFiles = Array.getLength(LogConstants.LOGFILENAMES);
                if (numFiles > 0) {
                    for (int i = 0; i < numFiles; i++) {
                        for (int j = 0; j < numDefFiles; j++) {
                            String nmToCompare = LogConstants.LOGFILENAMES[j];
                            /*
                             *  want to keep out the files that start with:
                             *    _secure.log. and _secure.ver..  just want
                             *  the _secure.am* files.
                             */

                            if (logSecurity.equalsIgnoreCase("ON")) {
                                nmToCompare = securityPrefix + "." +
                                    nmToCompare;
                            }
                            if (dirList[i].indexOf(nmToCompare) > -1) {
                                logNames.add(dirList[i]);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            queryResult = currentDBHandler.getTableNames(manager);
            int szOfList = Array.getLength(queryResult);
            int numDefFiles = Array.getLength(LogConstants.LOGFILENAMES);

            /*
             *  for commonality, make both sides uppercase because
             *  oracle makes the tablenames uppercase; mysql doesn't.
             */
            for (int i = 0; i < szOfList; i++) {
                String tFile = queryResult[i][0].replace('_', '.');
                String thisFile = tFile.toUpperCase();
                String thatFile = null;
                for (int j = 0; j < numDefFiles; j++) {
                    thatFile = LogConstants.LOGFILENAMES[j].toUpperCase();
                    if (thatFile.indexOf(thisFile) > -1) {
                        logNames.add(queryResult[i][0]);  /* real tblname */
                        break;
                    }
                }
            }
        }
        return logNames;
    }

    /**
     * Returns the names of the Log Fields that are selected for logging
     * in the Logging Service template, plus the mandatory "time" and
     * "Data" fields.
     *
     * @return the Field/Column names in an ArrayList.
     **/

    public static ArrayList getLogFields() {
        ArrayList lFHS = new ArrayList();
        LogReader lr = new LogReader();

        try {
            lr.readConfiguration();
        } catch (Exception ex) {
            return lFHS;
        }

        lFHS.add("time");
        lFHS.add(LogConstants.DATA);
        if ((logFields != null) && (logFields.length() != 0)) {
            StringTokenizer stok = new StringTokenizer(logFields, ", ");
            while (stok.hasMoreElements()) {
                lFHS.add(stok.nextToken());
            }
        }
        return lFHS;
    }

    /**
     * Reads the specified log file provided the user has the authorization.
     * It reads all records and returns them without any modification.
     * This query ignores the max record parameter set through configuration.
     * This API is present to support log verifier requirement.
     *
     * @param fileName the filename without path to be read.
     * @param userCrdential user credential to check authorization.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static synchronized String [][] read(
        String fileName,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /*
         *  READ METHOD 1
         *
         * read the configuration from LogManager to use updated info.
         */
        LogReader lr = new LogReader();
        lr.readConfiguration();
        if (fileName == null) {
            throw new IllegalArgumentException("filename can't be null");
        }
        lr.setLoggerName(fileName);
        /* check whether user is authorized or not */
        if (lr.isAllowed(userCrdential) != true) {
            throw new AMLogException(fileName + ":" +
                AMLogException.LOG_RD_AUTH_FAILED);
        }
        /* form the full file name */
        String fullFileName = logPathName + fileName;
        /* get all the records through file handler */
        LogQuery qry = new LogQuery(LogQuery.ALL_RECORDS);
        if (logTypeIsFile) {
            queryResult = currentHandler.logRecRead(fullFileName,qry,false);
        } else {
            queryResult = currentDBHandler.logRecRead(fileName, qry,
                manager, false);
        }
        return queryResult;
    }
    
    /**
     * Retrieves records from a log file provided the user has the required
     * authorization. It identifies the filename using <code>logname</code> and
     * <code>type</code>.
     * It reads all records from the file but returns the maximum number
     * of most recent records set through configuration.
     *
     * @param logName an identifier and is a part of file name to be read.
     * @param logType the components of file name that will be read. it could
     *        be either of "access", "error" or "system".
     * @param userCrdential user credential to check authorization.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static synchronized  String [][] read(String logName,
        String logType,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /*
         * READ METHOD 2
         *
         * pass the call to the CORE logName based read api to collect data
         */
        queryResult = read(logName, logType, (String)null, (LogQuery)null,
            userCrdential);
        return queryResult;
    }
    
    /**
     * Reads a log file provided the user has the authorization. It reads all
     * records but returns the maximum number of most recent records set
     * through configuration.
     *
     * @param logName an identifier and is a part of file name to be read.
     * @param logType the components of filename to be read, not null.
     * @param timeStamp last component of filename to be read and not null.
     * @param userCrdential user credential for authorization check.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static synchronized String [][] read(String logName,
        String logType,
        String timeStamp,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException,
        Exception
    {
        /*
         *  READ METHOD 3
         *
         * pass the call to the CORE logName based read api to collect data
         */
        queryResult = read(logName,logType,timeStamp,null,userCrdential);
        return queryResult;
    }
    
    /**
     * Retrieves records from log file provided it has
     * the required authorization. It reads all records applies query
     * and returns the result as asked by the caller.
     *
     * @param logName an identifier and is a part of file name to be read.
     * @param logType the components of filename to be read.
     * @param logQuery contains search criteria details.
     * @param userCrdential user credential for authorization check.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     */
    public static synchronized String [][] read(String logName,
        String logType,
        LogQuery logQuery,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /*
         *  READ METHOD 4
         *
         * pass the call to the CORE logName based read api to collect data
         */
        queryResult = read(logName,logType,null,logQuery,userCrdential);
        return queryResult;
    }
    
    /**
     * Reads a log file provided it has the required authorization.
     * It reads all records but returns the maximum number of most recent
     * records (if asked) those meet the caller's requirement as specified
     * through query.
     *
     * @param logname an identifier and is a part of file name to be read.
     * @param logtype the components of filename to be read.
     * @param timeStamp is the last component of filename to be read and not
     *        null.
     * @param logQuery contains search criteria details.
     * @param userCrdential user credential for authorization check.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static String [][] read(String logname,
        String logtype,
        String timeStamp,
        LogQuery logQuery,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /*
         *  READ METHOD 5
         *
         * Create the actual file name out of the components
         */
        String mainFileName = new String();
        
        /* set logger name */
        setLoggerName(logname,logtype);
        
        mainFileName = loggerName;
        if (timeStamp != null) {
            if (mainFileName.length() == 0) {
                /* atleast one of logname or logtype must be present */
                return null;
            }
            mainFileName += "." + timeStamp;
        }
        queryResult = read(mainFileName,logQuery,userCrdential);
        return queryResult;
    }
    
    /**
     * Retrieves specific records in a given sorted order on
     * specific field (if user specifies valid sorting by field). The API
     * also needs the user has a successful authorization.
     *
     * @param fileName filename without path that will be read.
     * @param logQuery contains search criteria details.
     * @param userCrdential user credential for authorization check.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static String [][] read(String fileName,
        LogQuery logQuery,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /* READ METHOD 6 */
        LogReader lr = new LogReader();
        /* this is the CORE read api, used by all other read apis */
        lr.readConfiguration();
        /* pass the request to the file handler & collect all required data */
        if (fileName == null) {
            throw new IllegalArgumentException("filename can't be null");
        }
        if (maxReordToReturn <= 0) {
            maxReordToReturn = 1;
        }
        if (logQuery != null) {
            if (logQuery.getNumRecordsWanted() < LogQuery.ALL_RECORDS) {
                logQuery.setMaxRecord(maxReordToReturn);
            } else if ((logQuery.getNumRecordsWanted() ==
                LogQuery.MOST_RECENT_MAX_RECORDS) && (logTypeIsFile))
            {
                /*  MOST_RECENT_MAX_RECORDS processed in DB reader */
                logQuery.setMaxRecord(maxReordToReturn);
            }
        } else {
            logQuery = new LogQuery(maxReordToReturn);
        }
        /* sets logger name */
        setLoggerName(fileName);
        
        /* check whether user is authorized or not */
        if (lr.isAllowed(userCrdential) != true) {
            throw new AMLogException(fileName + ":" +
                AMLogException.LOG_RD_AUTH_FAILED);
        }
        if (logTypeIsFile) {
            String fullFileName = logPathName + fileName;
            queryResult = currentHandler.logRecRead(fullFileName,
                logQuery, true);
        } else {
            queryResult = currentDBHandler.logRecRead(fileName, logQuery,
                manager, true);
        }
        return queryResult;
    }
    
    /**
     * Retrieves specific records in a given sorted order on
     * specific field (if user specifies valid sorting by field). The API
     * also needs the user has a successful authorization.
     *
     * @param fileNames set of filenames without path that will be read
     * @param logQuery contains search criteria details
     * @param userCrdential user credential for authorization check.
     * @return results in a two dimensional String, where columns in the
     *         the first row always hold the field names present in the
     *         file. Other rows hold the values present under those column.
     * @throws IOException if interrupted or failed to do I/O.
     * @throws NoSuchFieldException if invalid field has been specified.
     * @throws IllegalArgumentException when inappropriate argument specified.
     * @throws RuntimeException when it has been caught in any phase.
     * @throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     **/
    public static String [][] read(
        Set fileNames,
        LogQuery logQuery,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        /* READ METHOD 7 */
        LogReader lr = new LogReader();
        /* this is the CORE read api, used by all other read apis */
        lr.readConfiguration();
        /* pass the request to the file handler & collect all required data */
        if (fileNames == null) {
            throw new IllegalArgumentException("filenames can't be null");
        }
        if (fileNames.isEmpty()) {
            throw new IllegalArgumentException("filenames can't be empty");
        } else {
            /*  check all filenames in set */
            for (Iterator it = fileNames.iterator(); it.hasNext(); ) {
                String ss = (String)it.next();
                if (ss != null) {
                    ss = ss.trim();
                }
                if ((ss == null) || (ss.length() == 0)) {
                    throw new IllegalArgumentException(
                        "filename cannot be null");
                }
            }
        }

        if (maxReordToReturn <= 0) {
            maxReordToReturn = 1;
        }
        if (logQuery != null) {
            if (logQuery.getNumRecordsWanted() < LogQuery.ALL_RECORDS) {
                logQuery.setMaxRecord(maxReordToReturn);
            } else if ((logQuery.getNumRecordsWanted() ==
                LogQuery.MOST_RECENT_MAX_RECORDS) && (logTypeIsFile))
            {
                /* MOST_RECENT_MAX_RECORDS processed in DB reader */
                logQuery.setMaxRecord(maxReordToReturn);
            }
        } else {
            logQuery = new LogQuery(maxReordToReturn);
        }

        String tmpF = getAllFilenames (fileNames);
        /* sets logger name */
        setLoggerName(tmpF);
        
        /* check whether user is authorized or not */
        if (lr.isAllowed(userCrdential) != true) {
            throw new AMLogException(tmpF + ":" +
                AMLogException.LOG_RD_AUTH_FAILED);
        }
        if (logTypeIsFile) {
            Set fullFileNames = new HashSet();
            for (Iterator it = fileNames.iterator(); it.hasNext(); ) {
                String ss = (String)it.next();
                ss = logPathName + ss;
                fullFileNames.add(ss);
            }
            queryResult =
                currentHandler.logRecRead(fullFileNames, logQuery, true);
        } else {
            queryResult =
                currentDBHandler.logRecRead(fileNames, logQuery, manager,true);
        }
        return queryResult;
    }

    private static String getAllFilenames (Set fileNames) {
        StringBuilder fsSB = new StringBuilder();

        if (fileNames == null) {
            return null;
        }
        if (fileNames.isEmpty()) {
            return "";
        }

        String ts = null;
        for (Iterator it=fileNames.iterator(); it.hasNext(); ) {
            ts = (String)it.next();
            fsSB.append(ts);
        }
        ts = fsSB.toString();
        return (ts);
    }

    /*
     * This method collects the current file name used by this Logger.
     *
     *@param logName an identifier and is a part of file name to be read.
     *@param logType the components of filename to be read.
     *@param userCrdential user credential for authorization check.
     *@return name of the current logfile associated with this logname.
     *        returns null, if there is no file or invalid input params.
     *@throws IOException if interrupted or failed to do I/O.
     *@throws NoSuchFieldException if invalid field has been specified.
     *@throws IllegalArgumentException when inappropriate argument specified.
     *@throws RuntimeException when it has been caught in any phase.
     *@throws Exception if unrecoverable problem occurs, that is beyond
     *         its control.
     */
    private static String getCurrentFile(String logname,
        String logtype,
        Object userCrdential
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException,
        RuntimeException, Exception
    {
        // sets loggerName
        setLoggerName(logname,logtype);
        Logger logInstance =
            (com.sun.identity.log.Logger)Logger.getLogger(loggerName);
        try {
            logFileName = logInstance.getCurrentFile();
        } catch ( RuntimeException e) {
            Debug.error("LogReader:getCurrentFile:" +
                "RuntimeException: ", e);
            logFileName = null;
            throw e;
        }
        return logFileName;
    }
    
    /*
     *This method checks whether this user/ role does have the required
     *(here reading) permission on the resourse.
     *It uses spi based interface to check it. It returns true when allowed
     *else false.
     *
     *@param userCredential contains the user/ role details required to check
     *       the authorization.
     */
    private boolean isAllowed(Object userCredential) throws Exception {
        boolean isAuthorized = false;
        try {
            isAuthorized =
            com.sun.identity.log.spi.Authorizer.isAuthorized(
            loggerName,"READ",
            userCredential);
        } catch (Exception e) {
            Debug.error("LogReader:isAllowed:" + "Exception: ", e);
            throw e;
        }
        return isAuthorized;
    }
    
    /*
     *This method is to read configuration details it needs to perform
     *its task. LogManager always has the updated configuration information.
     *So this method goes to read its well defined property from LogManager.
     *This method should be called before each read request to ensure the
     *latest configuration has been used.
     */
    private void readConfiguration() throws Exception {
        cleanup();  // first cleans up all previous setting
        try {
            logPathName = manager.getProperty(LogConstants.LOG_LOCATION);
            if (logPathName == null) {
                Debug.error("LogReader:readConfiguration:" +
                    "unable to get log location");
                return;
            }

            logStorageType = manager.getProperty(LogConstants.BACKEND);
            maxRecStr = manager.getProperty(LogConstants.MAX_RECORDS);

            if ((maxRecStr == null) || (maxRecStr.length() == 0)) {
                maxRecStr = LogConstants.MAX_RECORDS_DEFAULT;
            }

            logFields = manager.getProperty(LogConstants.LOG_FIELDS);

            /* depending on whether "File" or "DB" backend... */
            logTypeIsFile = true;
            if (logStorageType.equals("File")) {
                fileHandlerClass =
                    manager.getProperty(LogConstants.FILE_READ_HANDLER);
                if (!(logPathName.endsWith(File.separator))) {
                    logPathName = logPathName + File.separator;
                }
                logSecurity =
                    manager.getProperty(LogConstants.SECURITY_STATUS);
            } else {
                logTypeIsFile = false;
                dbHandlerClass =
                    manager.getProperty(LogConstants.DB_READ_HANDLER);
            }
        } catch (Exception e) {
            Debug.error("LogReader:readConfiguration:" + "Exception: ", e);
            throw e; // rethrow the exception as it is
        }
        try {
            /* check type of log backend and instantiate appropriate object */
            if (logStorageType.compareToIgnoreCase(FILE_SOURCE) == 0 ) {
                Class clz = Class.forName(fileHandlerClass);
                currentHandler =(com.sun.identity.log.handlers.LogReadHandler)
                clz.newInstance();
            } else if (logStorageType.compareToIgnoreCase("DB") == 0 ) {
                /*  FOR DB Handler */
                Class clz = Class.forName(dbHandlerClass);
                // Following type casting has to be changed for DB support
                currentDBHandler =
                    (com.sun.identity.log.handlers.LogReadDBHandler)
                        clz.newInstance();
            }
        } catch (Exception e) {
            Debug.error("LogReader:readConfiguration:" + "Exception: ", e);
            throw e; /* rethrow the exception as it is */
        }
        try {
            if (maxRecStr != null) {
                maxRecStr = maxRecStr.trim();
                maxReordToReturn = Integer.parseInt(maxRecStr);
            }
        } catch (Exception ex) {
            Debug.error("LogReader:readConfiguration:" + "Exception: ", ex);
            throw ex; // rethrow the exception as it is
        }
        return;
    }
    
    /*
     *setLoggerName method set the logname i.e. logger name from filename.
     *@param filename name of the file to read.
     */
    private static void setLoggerName(String name) {
        /* checks for security prefix, if present removes it. */
        if (name.startsWith(securityPrefix) == true) {
            name = name.substring(securityPrefix.length()+1,name.length());
        }
        
        /* check for time stamp, if present removes it. */
        int datePos = name.lastIndexOf(".");
        int strLen  = name.length();
        ++datePos;
        String dateCheck = name.substring(datePos+1,strLen);
        try {
            Long longTm = new Long(dateCheck);
            long dateValue = longTm.longValue();
            if (dateValue >= 1) {
                /* valid time present so remove it to get file name */
                name = name.substring(0,datePos-1);
            }
        } catch (Exception e) {
            /*
             * exception suggests the log file name doesn't have time stamp
             * attached so loggerName = name.
             * do nothing
             */
        }
        loggerName = name;
    }
    
    /*
     *setLoggerName method set the logname i.e. logger name from filename.
     *@param logname 1st part (if has 2 parts) of the logger name.
     *@param logtype 2nd part (if any) of the logger name
     */
    private static void setLoggerName(String logname, String logtype) {
        /* checks for security prefix, if present removes it. */
        if (logname != null) {
            loggerName = logname;
        }
        
        if (logtype != null) {
            if (logname != null) {
                loggerName += ".";
            }
            loggerName += logtype;
        }
    }
    
    /*This method clears all the stored information and previous result
     *to make sure there is no effect from previous query. It should be
     *be called before reading configuration.
     */
    private void cleanup() {
        this.maxReordToReturn = LogQuery.MOST_RECENT_MAX_RECORDS;
        this.queryResult = null;
        this.currentHandler = null;
        return;
    }

    public static boolean isLogSecure() {
        return (logSecurity.equalsIgnoreCase("ON"));
    }
}
