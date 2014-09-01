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
 * $Id: LogReadDBHandler.java,v 1.4 2008/06/25 05:43:36 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.handlers;

import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogQuery;
import com.sun.identity.log.QueryElement;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.util.LogRecordSorter;


/**LogReadDBHandler class implements ReadDBHandler interface.
 * This class name will be stored as a configuration parameter.
 * LogReader will instantiate it at run time (when messages are logged
 * into file). This class reads the DB table, applies query if any,
 * sorts records on field name when required, collects most recent records
 * (default option) or all records. It returns result 2D String to the
 * caller (LogReader).
 **/
public class LogReadDBHandler implements ReadDBHandler  {
    // private attributes
    private String databaseURL;
    private String dbDriver;
    private boolean isMySQL = false;
    private String dbUserName;
    private String dbPassWord;
    private String maxRecordsStr = null;
    private int maxRecords = 0;
    private LogRecordSorter sorter =  null;
    private String [][] queryResult;    // will hold the return value
    // internal storage for records
    private ArrayList listOfValidRecords = new ArrayList();

    private Connection conn = null;

    /** 
     * constructor does nothing
     **/
    public LogReadDBHandler() {}
    
    /**
     * LogReader calls this method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param tableName db table name
     * @param logQuery is user specified qury chriteria with sorting requirement
     * @param logMgr the log manager associated with this handler
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws SQLException if it fails to process sql query.
     * @throws Exception if it fails any of operation.
     */
    public String [][]logRecRead(
        String tableName,
        LogQuery logQuery,
        java.util.logging.LogManager logMgr,
        boolean sourceData
    ) throws IOException, NoSuchFieldException,
        IllegalArgumentException, RuntimeException,
        SQLException, Exception
    {
        String sortField = null;

        // if the object is persistence use it otherwise don't
        this.cleaner();
        tableName = tableName.replace('.', '_');

        try {
            this.databaseURL = logMgr.getProperty(LogConstants.LOG_LOCATION);
            this.dbDriver = logMgr.getProperty(LogConstants.DB_DRIVER);
            this.dbUserName = logMgr.getProperty(LogConstants.DB_USER);
            this.dbPassWord = logMgr.getProperty(LogConstants.DB_PASSWORD);
            this.maxRecordsStr = logMgr.getProperty(LogConstants.MAX_RECORDS);
        } catch (Exception e) {
            Debug.error("DBLogRecRead:config: ", e);
            throw e;        // rethrow the exception
        }

        if (this.dbDriver.toLowerCase().indexOf("oracle") != -1) {
            isMySQL = false;
        } else if (this.dbDriver.toLowerCase().indexOf("mysql") != -1) {
            isMySQL = true;
        } else {
            isMySQL = false;
            Debug.warning("DBlogRecRead:assuming driver: '" + this.dbDriver +
                "' is Oracle-compatible.");
        }

        String selectStr;
        if (sourceData == true) {
            String temps = logQuery.getSortingField();
            if (temps != null) {
                    sortField = temps.trim();
            }

            String columns = "*";        // default all
            ArrayList sCol = logQuery.getColumns();
            if (sCol != null) {
                StringBuilder colSB = new StringBuilder();
                int sSize = sCol.size();
                for (int i = 0; i < sSize; i++) {
                    colSB.append((String)sCol.get(i));
                    if ((i+1) < sSize) {
                        colSB.append(", ");
                    }
                }
                columns = colSB.toString();
            }

            selectStr = lq2Select (tableName, columns, logQuery);
            if (Debug.messageEnabled()) {
                Debug.message("logRecRead/4:selectStr = " + selectStr);
            }
        } else {
            selectStr = lq2Select (tableName, null, null);
            if (Debug.messageEnabled()) {
                Debug.message("logRecRead/4.2:selectStr = " + selectStr);
            }
        }

        //
        //  got the LogQuery part converted to SQL.  the "order by"
        //  (sortBy) and limit/rownum parts are different for oracle
        //  and mysql.
        //

        try {
            this.maxRecords = Integer.parseInt(maxRecordsStr);
        } catch (NumberFormatException nfe) {
            if (Debug.warningEnabled()) {
                Debug.warning(
                    "DBlogRecRead: maxRecords error (" + maxRecordsStr +
                    "), set to MAX");
            }
            this.maxRecords = LogConstants.MAX_RECORDS_DEFAULT_INT;
        }

        String [][] tableResults;

        try {
            connectToDatabase (dbUserName, dbPassWord);
        } catch (SQLException sqe) {
            Debug.error("DBlogRecRead:connect:SQE:code=" +
                sqe.getErrorCode() + ", msg=" +
                sqe.getMessage());
            throw sqe; // rethrow for LogReader to catch
        } catch (ClassNotFoundException cnfe) {
            throw cnfe; // rethrow for LogReader to catch
        }

        String selStr = selectStr;

        Statement stmt = null;
        int numberOfRows = 0;

        try {
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                        ResultSet.CONCUR_UPDATABLE);

            if (Debug.messageEnabled()) {
                Debug.message("DBlogRecRead:about to execute: " + selStr);
            }
            ResultSet rs = stmt.executeQuery(selStr);

            //
            //  fetchsize appears to be 10 from rs.getFetchSize();
            //

            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();

            if (Debug.messageEnabled()) {
                Debug.message("DBlogRecRead:#columns = " + numberOfColumns);
            }

            //
            //  these are the column (field) names
            //

            String [] spltHdrStr = new String[numberOfColumns];
            for (int i = 1; i <= numberOfColumns; i++) {
                String tempstr = rsmd.getColumnName(i);
                if (Debug.messageEnabled()) {
                    Debug.message("DBlogRecRead:col #" + i +
                        " name = " + tempstr);
                }
                spltHdrStr[i-1] = tempstr;
            }
            listOfValidRecords.add(spltHdrStr);

            // have to figure out #rows
            while (rs.next()) {
                numberOfRows++;
            }

            if (Debug.messageEnabled()) {
                Debug.message("DBlogRecRead:#rows = " + numberOfRows);
            }

            if (numberOfRows == 0) {
                stmt.close();
                try {
                    conn.close();
                } catch (SQLException ex) {
                    //
                    //  might not care about this too much...?
                    //
                    Debug.error ("DBlogRecRead:rows=0:conn.close (" +
                        ex.getErrorCode() + "): " + ex.getMessage());
                }

                //
                //  should be at least the column names
                //

                int recSize = listOfValidRecords.size();

                //
                // checks whether it has got any record or not
                // weird if it's 0...
                //

                if (recSize <= 0) {
                    // if no record found return null
                    return null;
                }
                queryResult = new String[recSize][];
                for (int i=0; i<recSize; i++) {
                    queryResult[i] = (String [])listOfValidRecords.get(i);
                }

                return queryResult;
            }

            if (numberOfRows > this.maxRecords) {
                 stmt.close();
                try {
                    conn.close();
                } catch (SQLException ex) {
                    //
                    //  don't care about this too much...
                    //
                    Debug.error ("DBlogRecRead:conn.close (" +
                        ex.getErrorCode() + "): " + ex.getMessage());
                }
                throw new AMLogException(AMLogException.LOG_DB_TOOMANYRECORDS);
            }

            //
            //  reset to the beginning
            //
            boolean isFirst = rs.first();
            if (isFirst == false) {
                Debug.error("DBlogRecRead:first() is false!");
            }

            int rowsToAlloc = numberOfRows;
            if (logQuery.getNumRecordsWanted() ==
                LogQuery.MOST_RECENT_MAX_RECORDS) 
            {
                //
                //  remember it's one for the column names, too
                //
                if (numberOfRows > this.maxRecords) {
                    rowsToAlloc = this.maxRecords;
                }
            }
            tableResults = new String[rowsToAlloc][numberOfColumns];

            String result = null;
            int rowCount = 0;

            //
            //  if LogQuery.MOST_RECENT_MAX_RECORDS selected,
            //  then we just have to get the "this.maxRecords" records.
            //

            int skipThisManyRecords = 0;
            if (logQuery.getNumRecordsWanted() ==
                LogQuery.MOST_RECENT_MAX_RECORDS) 
            {
                if (numberOfRows > this.maxRecords) {
                    skipThisManyRecords = numberOfRows - this.maxRecords;
                }
            }

            if (Debug.messageEnabled()) {
                Debug.message("DBlogRecRead:skipThisMany = " +
                    skipThisManyRecords);
            }

            //
            //  always do the first one... the column names
            //

            for (int i = 0; i < numberOfColumns; i++) {
                result = rs.getString(i+1);
                tableResults[0][i] = result;
            }
            rowCount = 1;

            while (rs.next()) {
                if (skipThisManyRecords-- <= 0) {
                    for (int i = 0; i < numberOfColumns; i++) {
                        result = rs.getString(i+1);
                        tableResults[rowCount][i] = result;
                    }
                    rowCount++;
                }
            }

            stmt.close();

        } catch (SQLException se) {
            Debug.error("DBlogRecRead:query:SQE:code=" +
                se.getErrorCode() + ", msg=" +
                se.getMessage());
            throw se; // rethrow for LogReader to catch
        }

        try {
            this.getRecords(tableResults, sourceData);
        } catch (IOException e) {
            throw e; // catch & rethrow it
        } catch (IllegalArgumentException e) {
            throw e; // catch & rethrow it
        } catch (RuntimeException e) {
            throw e; // catch & rethrow it
        } catch (Exception e) {
            throw e; // catch & rethrow it
        }

        int recSize = listOfValidRecords.size();

        // checks whether it has got any record or not
        if (recSize <= 0) {
            // if no record found return null
            return null;
        }
        
        //
        //  sorting already done by DB
        //

        try {
            conn.close();
        } catch (SQLException ex) {
            //
            //  might not care about this too much...?
            //
            Debug.error ("DBlogRecRead:conn.close (" + ex.getErrorCode() +
                "): " + ex.getMessage());
        }

        queryResult = new String[recSize][];
        for (int i=0; i<recSize; i++) {
            queryResult[i] = (String [])listOfValidRecords.get(i);
        }

        return queryResult;
    }

    
    /**
     * LogReader calls this method. It collects header, records,
     * applies query (if any), sorts (if asked) the records on field, checks
     * the max records to return, collects all the recods and returns.
     *
     * @param tableNames db table names
     * @param logQuery is user specified qury chriteria with sorting requirement
     * @param logMgr the log manager associated with this handler
     * @param sourceData it specifies whether return data should be original
     *        data received by logger (source) or formatted data as in file.
     * @return all the matched records with query
     * @throws IOException if it fails to read log records.
     * @throws NoSuchFieldException if it fails to retrieve the name of field.
     * @throws IllegalArgumentException if query has wrong value.
     * @throws RuntimeException if it fails to retrieve log record.
     * @throws SQLException if it fails to process sql query.
     * @throws Exception if it fails any of operation.
     */
    public String [][]logRecRead(
        Set tableNames,
        LogQuery logQuery,
        java.util.logging.LogManager logMgr,
        boolean sourceData
    ) throws IOException, NoSuchFieldException, IllegalArgumentException,
        RuntimeException, SQLException, Exception
    {
        String sortField = null;

        // if the object is persistence use it otherwise don't
        this.cleaner();

        //
        // tblNames is needed for the guaranteed "underscore" form
        // (e.g., "amAuthentication_access") of the table names
        // 
        Set tblNames = new HashSet();
        StringBuilder allTablesSB = new StringBuilder("");
        for (Iterator it = tableNames.iterator(); it.hasNext(); ) {
            String ss = (String)it.next();
            String ss2 = ss.replace('.', '_');
            tblNames.add(ss2);
            allTablesSB.append(ss2);
        }

        //
        //  allTablesSB contains the list of tables for use in the
        //  select statement.
        //
        try {
            this.databaseURL = logMgr.getProperty(LogConstants.LOG_LOCATION);
            this.dbDriver = logMgr.getProperty(LogConstants.DB_DRIVER);
            this.dbUserName = logMgr.getProperty(LogConstants.DB_USER);
            this.dbPassWord = logMgr.getProperty(LogConstants.DB_PASSWORD);
            this.maxRecordsStr = logMgr.getProperty(LogConstants.MAX_RECORDS);
        } catch (Exception e) {
            Debug.error("DBLogRecReadSet:config: ", e);
            throw e;        // rethrow the exception
        }

        //
        //  see if we're using Oracle or MySQL, as there are
        //  some differences between SQL for Oracle and MySQL.
        //

        if (this.dbDriver.toLowerCase().indexOf("oracle") != -1) {
            isMySQL = false;
        } else if (this.dbDriver.toLowerCase().indexOf("mysql") != -1) {
            isMySQL = true;
        } else {
            isMySQL = false;
            Debug.warning("DBlogRecRead:assuming driver: '" + this.dbDriver +
                "' is Oracle-compatible.");
        }

        try {
            this.maxRecords = Integer.parseInt(maxRecordsStr);
        } catch (NumberFormatException nfe) {
            if (Debug.warningEnabled()) {
                Debug.warning(
                    "DBlogRecRead(s): maxRecords error (" + maxRecordsStr +
                    "), set to MAX");
            }
            this.maxRecords = LogConstants.MAX_RECORDS_DEFAULT_INT;
        }

        //
        //  kind of a bad situation here between Oracle and MySQL.
        //  pre-v4 MySQL doesn't support the "union" operator, so multi-
        //  table selects has to be affected as multiple single-table
        //  selects and combining their results.
        //
        //  the Oracle case is more straightforward, and if we decide
        //  to only support >= V4 MySQL, can just use the !isMySQL
        //  branch.
        //

        String selectStr;
        if (!isMySQL) {
            if (sourceData == true) {
                String temps = logQuery.getSortingField();
                if (temps != null) {
                        sortField = temps.trim();
                }

                String columns = "*";        // default all
                ArrayList sCol = logQuery.getColumns();
                if (sCol != null) {
                    StringBuilder colSB = new StringBuilder();
                    int sSize = sCol.size();
                    for (int i = 0; i < sSize; i++) {
                        colSB.append((String)sCol.get(i));
                        if ((i+1) < sSize) {
                            colSB.append(", ");
                        }
                    }
                    columns = colSB.toString();
                }

                selectStr = lq2Select (tblNames, columns, logQuery);
                if (Debug.messageEnabled()) {
                    Debug.message("logRecRead/4:selectStr = " + selectStr);
                }
            } else {
                String columns = "*";        // default all
                selectStr = lq2Select (tblNames, columns, null);
                if (Debug.messageEnabled()) {
                    Debug.message("logRecRead/4.2:selectStr = " + selectStr);
                }
            }


            String [][] tableResults;

            try {
                connectToDatabase (dbUserName, dbPassWord);
            } catch (SQLException sqe) {
                Debug.error("DBlogRecRead:connect:SQE:code=" +
                    sqe.getErrorCode() + ", msg=" +
                    sqe.getMessage());
                throw sqe; // rethrow for LogReader to catch
            } catch (ClassNotFoundException cnfe) {
                throw cnfe; // rethrow for LogReader to catch
            }

            String selStr = selectStr;

            Statement stmt = null;
            int numberOfRows = 0;

            try {
                stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                        ResultSet.CONCUR_UPDATABLE);

                if (Debug.messageEnabled()) {
                    Debug.message("DBlogRecRead:about to execute: " + selStr);
                }
                ResultSet rs = stmt.executeQuery(selStr);

                //
                //  fetchsize appears to be 10 from rs.getFetchSize();
                //

                ResultSetMetaData rsmd = rs.getMetaData();
                int numberOfColumns = rsmd.getColumnCount();

                if (Debug.messageEnabled()) {
                    Debug.message("DBlogRecRead:#columns = " +
                        numberOfColumns);
                }

                //
                //  these are the column (field) names
                //

                String [] spltHdrStr = new String[numberOfColumns];
                for (int i = 1; i <= numberOfColumns; i++) {
                    String tempstr = rsmd.getColumnName(i);
                    if (Debug.messageEnabled()) {
                        Debug.message("DBlogRecRead:col #" + i +
                            " name = " + tempstr);
                    }
                    spltHdrStr[i-1] = tempstr;
                }
                listOfValidRecords.add(spltHdrStr);

                // have to figure out #rows
                while (rs.next()) {
                    numberOfRows++;
                }

                if (Debug.messageEnabled()) {
                    Debug.message("DBlogRecRead:#rows = " + numberOfRows);
                }

                //
                //  possible to have no records
                //

                if (numberOfRows == 0) {
                    stmt.close();
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        //
                        //  might not care about this too much...?
                        //
                        Debug.error("DBlogRecRead:rows=0:conn.close (" +
                            ex.getErrorCode() + "): " + ex.getMessage());
                    }

                    //
                    //  should be at least the column names
                    //

                    int recSize = listOfValidRecords.size();

                    //
                    //  weird if it's 0...
                    //

                    if (recSize <= 0) {
                        return null;
                    }
                    queryResult = new String[recSize][];
                    for (int i=0; i<recSize; i++) {
                        queryResult[i] = (String [])listOfValidRecords.get(i);
                    }
                    return queryResult;
                }

                if (numberOfRows > this.maxRecords) {
                     stmt.close();
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        //
                        //  don't care about this too much...
                        //
                        Debug.error ("DBlogRecRead:conn.close (" +
                            ex.getErrorCode() + "): " + ex.getMessage());
                    }
                    throw new AMLogException(
                        AMLogException.LOG_DB_TOOMANYRECORDS);
                }

                //
                //  reset to the beginning
                //
                boolean isFirst = rs.first();
                if (isFirst == false) {
                    Debug.error("DBlogRecRead:first() is false!");
                }

                //
                //  think we're not going to allow MOST_RECENT_MAX_RECORDS
                //  with multi-table query...
                //

                int rowsToAlloc = numberOfRows;
                tableResults = new String[rowsToAlloc][numberOfColumns];

                String result = null;
                int rowCount = 0;
                //
                //  always do the first one... the column names
                //

                for (int i = 0; i < numberOfColumns; i++) {
                    result = rs.getString(i+1);
                    tableResults[0][i] = result;
                }
                rowCount = 1;
                while (rs.next()) {
                    for (int i = 0; i < numberOfColumns; i++) {
                        result = rs.getString(i+1);
                        tableResults[rowCount][i] = result;
                    }
                    rowCount++;
                }

                 stmt.close();

            } catch (SQLException se) {
                Debug.error("DBlogRecRead:query:SQE:code=" +
                    se.getErrorCode() + ", msg=" +
                    se.getMessage());
                throw se; // rethrow for LogReader to catch
            }

            try {
                this.getRecords(tableResults, sourceData);
            } catch (IOException e) {
                throw e; // catch & rethrow it
            } catch (IllegalArgumentException e) {
                throw e; // catch & rethrow it
            } catch (RuntimeException e) {
                throw e; // catch & rethrow it
            } catch (Exception e) {
                throw e; // catch & rethrow it
            }

            int recSize = listOfValidRecords.size();

            // checks whether it has got any record or not
            if (recSize <= 0) {
                // if no record found return null
                return null;
            }
        
            //
            //  sorting already done by DB
            //

            try {
                conn.close();
            } catch (SQLException ex) {
                //
                //  might not care about this too much...?
                //
                Debug.error ("DBlogRecRead:conn.close (" + ex.getErrorCode() +
                    "): " + ex.getMessage());
            }

            queryResult = new String[recSize][];
            for (int i=0; i<recSize; i++) {
                queryResult[i] = (String [])listOfValidRecords.get(i);
            }

        } else {        // else (isMySQL case)
            //
            //  Multi-table select for <V4 MySQL is essentially
            //  looping through the tables and combining the results.
            //
            String columns = null;

            if (sourceData == true) {
                String temps = logQuery.getSortingField();
                if (temps != null) {
                        sortField = temps.trim();
                }

                columns = "*";        // default all
                ArrayList sCol = logQuery.getColumns();
                if (sCol != null) {
                    StringBuilder colSB = new StringBuilder();
                    int sSize = sCol.size();
                    for (int i = 0; i < sSize; i++) {
                        colSB.append((String)sCol.get(i));
                        if ((i+1) < sSize) {
                            colSB.append(", ");
                        }
                    }
                    columns = colSB.toString();
                }
            } else {
                columns = "*";        // default all
            }

            //
            //  do same select on each table
            //

            boolean isFirstTable = true;
            int totalNumberOfRows = 0;
            int recSize = 0;

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                String thisTable = (String)it.next();
                if (sourceData == true) {
                    selectStr = lq2Select (thisTable, columns, logQuery);
                    if (Debug.messageEnabled()) {
                        Debug.message("logRecRead/5:selectStr = " + selectStr);
                    }
                } else {
                    selectStr = lq2Select (thisTable, columns, null);
                    if (Debug.messageEnabled()) {
                        Debug.message("logRecRead/5.2:selectStr = " +
                            selectStr);
                    }
                }

                String [][] tableResults = null;

                try {
                    connectToDatabase (dbUserName, dbPassWord);
                } catch (SQLException sqe) {
                    Debug.error("DBlogRecRead:connect:SQE:code=" +
                        sqe.getErrorCode() + ", msg=" +
                        sqe.getMessage());
                    throw sqe; // rethrow for LogReader to catch
                } catch (ClassNotFoundException cnfe) {
                    throw cnfe; // rethrow for LogReader to catch
                }

                String selStr = selectStr;

                Statement stmt = null;
                int numberOfRows = 0;

                //
                //  send the select statement
                //
                try {
                    stmt = conn.createStatement(
                                ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_UPDATABLE);

                    if (Debug.messageEnabled()) {
                        Debug.message("DBlogRecRead:about to execute: " +
                            selStr);
                    }
                    ResultSet rs = stmt.executeQuery(selStr);

                    //
                    //  fetchsize appears to be 10 from rs.getFetchSize();
                    //

                    ResultSetMetaData rsmd = rs.getMetaData();
                    int numberOfColumns = rsmd.getColumnCount();

                    if (Debug.messageEnabled()) {
                        Debug.message("DBlogRecRead:#columns = " +
                            numberOfColumns);
                    }

                    //
                    //  get the column (field) names
                    //
                    //  put them into the listOfValidRecords, but only
                    //  the for the first table... don't need or want
                    //  them scattered about later.  and they should be
                    //  the same for all the tables...
                    //

                    if (isFirstTable) {
                        String [] spltHdrStr = new String[numberOfColumns];
                        for (int i = 1; i <= numberOfColumns; i++) {
                            String tempstr = rsmd.getColumnName(i);
                            if (Debug.messageEnabled()) {
                                Debug.message("DBlogRecRead:col #" + i +
                                    " name = " + tempstr);
                            }
                            spltHdrStr[i-1] = tempstr;
                        }
                        listOfValidRecords.add(spltHdrStr);
                    }

                    // have to figure out #rows
                    numberOfRows = 0;
                    while (rs.next()) {
                        numberOfRows++;
                    }

                    totalNumberOfRows += numberOfRows;

                    if (totalNumberOfRows > this.maxRecords) {
                         stmt.close();
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            //
                            //  don't care about this too much...
                            //
                            Debug.error ("DBlogRecRead:conn.close (" +
                                ex.getErrorCode() + "): " + ex.getMessage());
                        }
                        throw new AMLogException(
                            AMLogException.LOG_DB_TOOMANYRECORDS);
                    }

                    if (numberOfRows > 0) {
                        //
                        //  reset to the "beginning"
                        //

                        boolean isFirst = rs.first();
                        if (isFirst == false) {
                            Debug.error("DBlogRecRead:first() is false!");
                        }

                        //
                        //  think we're not going to allow 
                        //  MOST_RECENT_MAX_RECORDS with multi-table query...
                        //

                        tableResults =
                            new String[numberOfRows][numberOfColumns];

                        String result = null;
                        int rowCount = 0;

                        do {
                            for (int i = 0; i < numberOfColumns; i++) {
                                result = rs.getString(i+1);
                                tableResults[rowCount][i] = result;
                            }
                            rowCount++;
                        } while (rs.next());
                    }

                    //
                    //  print the actual results in the debug log
                    //
                     stmt.close();

                } catch (SQLException se) {
                    Debug.error("DBlogRecRead:query:SQE:code=" +
                        se.getErrorCode() + ", msg=" +
                        se.getMessage());
                    throw se; // rethrow for LogReader to catch
                }

                if (numberOfRows > 0) {
                    try {
                        this.getRecords(tableResults, sourceData);
                    } catch (IOException e) {
                        throw e; // catch & rethrow it
                    } catch (IllegalArgumentException e) {
                        throw e; // catch & rethrow it
                    } catch (RuntimeException e) {
                        throw e; // catch & rethrow it
                    } catch (Exception e) {
                        throw e; // catch & rethrow it
                    }
                }

                if (isFirstTable) {
                    isFirstTable = false;
                }

            } // the for loop for the set of tables

            try {
                conn.close();
            } catch (SQLException ex) {
                //
                //  might not care about this too much...?
                //
                Debug.error ("DBlogRecRead:conn.close (" +
                    ex.getErrorCode() + "): " + ex.getMessage());
            }

            //
            //  probably have to sort again
            //

            if (logQuery != null) {
                String sortByField = logQuery.getSortingField();
                if (sortByField != null) {
                    try {
                        this.sorter = new LogRecordSorter(sortByField,
                            listOfValidRecords);
                        queryResult = this.sorter.getSortedRecords();
                    } catch (NoSuchFieldException e) {
                        Debug.error("DBlogRecRead/5:sort:nsfe: " +
                                e.getMessage());
                        throw e;
                    } catch (IllegalArgumentException e) {
                        Debug.error("DBlogRecRead/5:sort:iae: " +
                                e.getMessage());
                        throw e;
                    } catch (RuntimeException e) {
                        Debug.error("DBlogRecRead/5:sort:rte: " +
                                e.getMessage());
                        throw e;
                    } catch (Exception e) {
                        Debug.error("DBlogRecRead/5:sort:ex: " +
                                e.getMessage());
                        throw e;
                    }
                    return (queryResult);
                }
            }

            recSize = listOfValidRecords.size();

            // checks whether it has got any record or not
            if (recSize <= 0) {
                // if no record found return null
                return null;
            }

            //
            //  can we use the toArray() converter?
            //

            queryResult = new String[recSize][];
            for (int i=0; i<recSize; i++) {
                queryResult[i] = (String [])listOfValidRecords.get(i);
            }
        }

        return queryResult;
    }

    /**
     * Return table names for each logger
     * @param logMgr Log Manager that is maintaing table names
     * @return table names for each logger
     */
    public String [][] getTableNames(java.util.logging.LogManager logMgr) {
        try {
            this.databaseURL = logMgr.getProperty(LogConstants.LOG_LOCATION);
            this.dbDriver = logMgr.getProperty(LogConstants.DB_DRIVER);
            this.dbUserName = logMgr.getProperty(LogConstants.DB_USER);
            this.dbPassWord = logMgr.getProperty(LogConstants.DB_PASSWORD);
            this.maxRecordsStr = logMgr.getProperty(LogConstants.MAX_RECORDS);
        } catch (Exception e) {
            return null;
        }

        try {
            connectToDatabase (dbUserName, dbPassWord);
        } catch (SQLException sqe) {
            Debug.error("DBgetTableNames:connect:SQE:code=" +
                sqe.getErrorCode() + ", msg=" +
                sqe.getMessage());
            return null;
        } catch (ClassNotFoundException cnfe) {
            Debug.error("DBgetTableNames:connect:CNFE: " +
                cnfe.getMessage());
            return null;
        }

        isMySQL = false;
        String queryString = null;
        if (this.dbDriver.toLowerCase().indexOf("oracle") != -1) {
            isMySQL = false;
            //
            //  gonna be something like:
            //  select table_name from dba_all_tables where owner = 'AMADMIN';
            //

            queryString =
                "select table_name from dba_all_tables where owner = '" +
                    (this.dbUserName).toUpperCase() + "'";

        } else if (this.dbDriver.toLowerCase().indexOf("mysql") != -1) {
            isMySQL = true;
            //
            //  gonna be:
            //  show tables
            //
            queryString = "show tables";
        }

        Statement stmt = null;
        ResultSet rs = null;
        int numberOfColumns = 0;
        ResultSetMetaData rsmd = null;
        String [][] tableResults = null;
        try {
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                        ResultSet.CONCUR_UPDATABLE);

            if (Debug.messageEnabled()) {
                Debug.message("DBgetTableNames:about to execute: " +
                    queryString);
            }

            rs = stmt.executeQuery(queryString);

            rsmd = rs.getMetaData();
            numberOfColumns = rsmd.getColumnCount();

            if (Debug.messageEnabled()) {
                Debug.message("DBgetTableNames:#columns = " + numberOfColumns);
            }

            String [] spltHdrStr = new String[numberOfColumns];
            for (int i = 1; i <= numberOfColumns; i++) {
                String tempstr = rsmd.getColumnName(i);
                spltHdrStr[i-1] = tempstr;
            }

            // have to figure out #rows

            int numberOfRows = 0;
            while (rs.next()) {
                numberOfRows++;
            }

            if (Debug.messageEnabled()) {
                Debug.message("DBgetTableNames:#rows = " + numberOfRows);
            }

            //
            //  reset to the beginning
            //
            boolean isFirst = rs.first();
            if (isFirst == false) {
                Debug.error("DBgetTableNames:first() is false!");
            }

            String result = null;
            tableResults = new String[numberOfRows][numberOfColumns];

            for (int i = 0; i < numberOfColumns; i++) {
                result = rs.getString(i+1);
                tableResults[0][i] = result;
            }
            int rowCount = 1;


            while (rs.next()) {
                for (int i = 0; i < numberOfColumns; i++) {
                    result = rs.getString(i+1);
                    tableResults[rowCount][i] = result;
                    rowCount++;
                }
            }

            stmt.close();
        } catch (SQLException se) {
            Debug.error("DBgetTableNames:query:SQE:code=" +
                se.getErrorCode() + ", msg=" +
                se.getMessage());
            return null;
        }

        try {
            conn.close();
        } catch (SQLException ex) {
            //
            //  might not care about this too much...?
            //
            Debug.error ("DBgetTableNames:conn.close (" + ex.getErrorCode() +
                "): " + ex.getMessage());
        }

        return tableResults;

    }

    /**
     * Return number of records in each table
     * @param logMgr Log Manager that is maintaing table names
     * @return number of records in each table
     */
    public long getNumberOfRows(java.util.logging.LogManager logMgr,
        String fileName)
    {
        long li = 0;

        try {
            this.databaseURL = logMgr.getProperty(LogConstants.LOG_LOCATION);
            this.dbDriver = logMgr.getProperty(LogConstants.DB_DRIVER);
            this.dbUserName = logMgr.getProperty(LogConstants.DB_USER);
            this.dbPassWord = logMgr.getProperty(LogConstants.DB_PASSWORD);
            this.maxRecordsStr = logMgr.getProperty(LogConstants.MAX_RECORDS);
        } catch (Exception e) {
            return 0;
        }


        try {
            connectToDatabase (dbUserName, dbPassWord);
        } catch (SQLException sqe) {
            Debug.error("DBgetNumberOfRows:connect:SQE:code=" +
                sqe.getErrorCode() + ", msg=" +
                sqe.getMessage());
            return 0;
        } catch (ClassNotFoundException cnfe) {
            Debug.error("DBgetgetNumberOfRows:connect:CNFE: " +
                cnfe.getMessage());
            return 0;
        }

        String fName = fileName.replace('.', '_');
        String queryString = "select count(*) from " + fName;

        Statement stmt = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        String result = null;
        try {
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                        ResultSet.CONCUR_UPDATABLE);

            if (Debug.messageEnabled()) {
                Debug.message("DBgetgetNumberOfRows:about to execute: " +
                    queryString);
            }

            rs = stmt.executeQuery(queryString);

            rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();

            if (Debug.messageEnabled()) {
                Debug.message("DBgetNumberOfRows:#columns = " +
                    numberOfColumns);
            }

            while (rs.next()) {
                for (int i = 0; i < numberOfColumns; i++) {
                    result = rs.getString(i+1);
                }
            }

            stmt.close();
        } catch (SQLException se) {
            Debug.error("DBgetNumberOfRows:query:SQE:code=" +
                se.getErrorCode() + ", msg=" +
                se.getMessage());
            return 0;
        }

        try {
            conn.close();
        } catch (SQLException ex) {
            //
            //  might not care about this too much...?
            //
            Debug.error ("DBgetNumberOfRows:conn.close (" +
                ex.getErrorCode() + "): " + ex.getMessage());
        }

        try {
            Long longval = new Long(result);
            li = longval.longValue();
        } catch (NumberFormatException nfe) {
            Debug.error("DBgetNumberOfRows:got " + result +
                " as number of rows, returning 0.");
        }
        return li;
    }

    //
    //  private, mostly debugging method to display results
    //  should be called after the ResultSet has been gone
    //  through already, as you can't point the cursor to before
    //  the beginning once you've made it move.
    //

    private void displayResultSet (ResultSet myrs) {
        try {
            ResultSetMetaData rsmd = myrs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            Debug.error("displayRS:#columns = " + numberOfColumns);

            StringBuffer sbtemp = new StringBuffer(80);
            for (int i = 1; i <= numberOfColumns; i++) {
                String tempstr = rsmd.getColumnName(i);
                sbtemp.append(tempstr).append("\t");
            }
            Debug.error("displayRS:columns =\n" + sbtemp.toString());

            boolean isFirst = myrs.first();

            int rowNum = 1;
            do {
                sbtemp = new StringBuffer(80);
                for (int i = 1; i <= numberOfColumns; i++) {
                    sbtemp.append(myrs.getString(i)).append("\t");
                }
                Debug.error("displayRS:row #" + rowNum + " = " +
                    sbtemp.toString());
                rowNum++;
            } while (myrs.next());

            //
            // put cursor back at beginning
            //
            isFirst = myrs.first();

        } catch (SQLException ex) {
            Debug.error ("displayRS:got SQLException: " + ex.getMessage());
        }
    }


    //
    //  ELF version reads file, splits into fields, validates
    //  field values and collects it.
    //  DB (this) version already has received the rows and put
    //  them into tableResults, a 2D String array, which is passed
    //  in.

    private boolean getRecords(String [][] tblResults, boolean isSourceData)
        throws IOException, RuntimeException
    {
        //
        //  could pass in the number of rows, or make it global,
        //  but for now, just figure it out.
        //

        int numRows = Array.getLength(tblResults);

        //
        //  process the rows of columns
        //  query to DB already executed the "query"
        //
        for (int i=0; i < numRows; i++) {
            listOfValidRecords.add(tblResults[i]);
        }
        return true;
    }
    
    // below method reset previous readings and prepare for new one.
    private    void cleaner() {
        this.listOfValidRecords.clear();
        this.queryResult = null;
        return;
    }


    //
    //  need:
    //  userName = lmanager.getProperty(LogConstants.DB_USER);
    //  password = lmanager.getProperty(LogConstants.DB_PASSWORD);
    //

    private void connectToDatabase(String userName, String password)
        throws SQLException, ClassNotFoundException
    {
        try {
            Class.forName(dbDriver);
            this.conn = 
                DriverManager.getConnection(databaseURL, userName, password);
        }
        catch (ClassNotFoundException e) {
            Debug.error("DBlogRecRead:connect:ClassNotFoundException: " +
                e.getMessage());
            throw e; // rethrow
        }
        catch (SQLException sqle) {
            //
            //  if start up with Oracle DB down, can get sqle.getErrorCode()
            //  == 1034, "ORA-01034: ORACLE not available"
            //  MySQL returns error code 0, message:
            //  "unable to connect to any hosts due to
            //  exception: java.net.ConnectException: Connection refused
            //
            Debug.error("DBlogRecRead:connect:SQLEx: " +
                sqle.getErrorCode() + "): " + sqle.getMessage());
            throw sqle; // rethrow
        }
    }

    //
    //  LogQuery-to-SQL-Select converter
    //
    //  tblName is the table to select on
    //  columns contains the comma-separated column names to return...
    //    e.g., "time, data, LoginID"
    //  lq is the LogQuery specified.  if no logquery, then
    //    lq2Select returns "select <columns> from <tblName>;"
    //

    private String lq2Select (String tblName, String columns, LogQuery lq) {
        ArrayList queries;
        StringBuffer selectSBuf = null;
        String whatSBuf = "*";  // select what from tblName? 
        StringBuffer whereSBuf = null; // where ...
        String opStr = " and "; // default to "and"; could be "or"
        int lqMatchAny = com.sun.identity.log.LogQuery.MATCH_ANY_CONDITION;
        int qrySize = 0;
        String sortStr = null;
        boolean getAllRecs = false;

        selectSBuf = new StringBuffer("select ");
        //
        // check if query
        //

        if ((columns != null) && (columns.length() > 0)) {
            whatSBuf = columns;
        }

        //
        //  if LogQuery.MOST_RECENT_MAX_RECORDS or
        //  LogQuery.ALL_RECORDS, need to retrieve all records
        //

        int numRecs = 0;
        getAllRecs = false;
        String numRecsStr = null;
        String sortByField = null;

        if (lq != null) {
            numRecs = lq.getNumRecordsWanted();
            if ((numRecs == LogQuery.MOST_RECENT_MAX_RECORDS) ||
                (numRecs == LogQuery.ALL_RECORDS))
            {
                getAllRecs = true;
            } else {
                numRecsStr = Integer.toString(numRecs);
            }
            sortByField = lq.getSortingField();
        } else {
            getAllRecs = true;
        }

        if (Debug.messageEnabled()) {
            Debug.message("lq2Select:getAllRecs = " + getAllRecs +
                ", numRecs = " + numRecs + ", sortByField = " +
                sortByField + ", numRecsStr = " + numRecsStr);
        }

        if (lq == null) {
            selectSBuf.append(whatSBuf).append
                (" from ").append(tblName);
            return selectSBuf.toString();
        }

        queries = (ArrayList)lq.getQueries();

        if ((queries == null) || (qrySize = queries.size()) == 0) {
            selectSBuf.append(whatSBuf).append
                (" from ").append(tblName);
        
            if (!getAllRecs) {
                if (isMySQL) {
                    selectSBuf.append(" limit ").append(numRecsStr);
                } else {
                    selectSBuf.append(" where rownum < ").append
                        (numRecsStr);
                }
            }
            return selectSBuf.toString();
        }

        if (lq.getGlobalOperand() == lqMatchAny) {
            opStr = " or ";
        }

        selectSBuf.append(whatSBuf).append(" from ").append
            (tblName).append(" where ");

        //
        //  limiting for oracle comes first after "where"
        //

        if (!getAllRecs && !isMySQL) {
            selectSBuf.append("rownum < ").append(numRecsStr).append
            (" and ");
        }

        //
        //  get the columns/values to search on
        //
        QueryElement qe;
        whereSBuf = new StringBuffer();

        boolean moreThanOneQuery = false;

        //
        //  if more than one query, then they all should be with
        //  a set of parens, and each should be within parens, e.g.:
        //  ((loginid like 'uid=user2%') or (loginid like 'uid=amAdmin%'))
        //

        if (qrySize > 1) {
            moreThanOneQuery = true;
            whereSBuf.append("(");
        }

        for (int i = 0; i < qrySize; i++) {
            qe = (QueryElement)queries.get(i);
            String fldName = qe.getFieldName();
            String fldValue = qe.getFieldValue();
            String relation;
            int iRelation = qe.getRelation();

            //
            //  both have to be non-null
            //

            switch (iRelation) {
                case QueryElement.GT:
                    relation = " > ";
                    break;
                case QueryElement.LT:
                    relation = " < ";
                    break;
                case QueryElement.EQ:
                    relation = " = ";
                    break;
                case QueryElement.NE:
                    relation = " != ";
                    break;
                case QueryElement.GE:
                    relation = " >= ";
                    break;
                case QueryElement.LE:
                    relation = " <= ";
                    break;
                case QueryElement.CN:  // contains
                case QueryElement.SW:  // starts with
                case QueryElement.EW:  // ends with
                    relation = " like ";
                    break;
                default:
                    relation = " = ";        // for now, anyway
            }

            if ((fldName != null) && (fldName.length() > 0) &&
                (fldValue != null) && (fldValue.length() > 0))
            {
                if (moreThanOneQuery) {
                    whereSBuf.append(" (");
                }
                whereSBuf.append(fldName).append(relation);
                if (iRelation == QueryElement.CN) {
                    whereSBuf.append(" '%").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.SW) {
                    whereSBuf.append(" '").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.EW) {
                    whereSBuf.append(" '%").append(fldValue).append("'");
                } else {
                    whereSBuf.append(" '").append(fldValue).append("'");
                }
                if (moreThanOneQuery) {
                    whereSBuf.append(") ");
                }
            }
            //
            //  if not last element, then
            //  append operator in prep for next element
            //
            if ((i+1) < qrySize) {
                whereSBuf.append(opStr);
            }
        }

        if (moreThanOneQuery) {
            whereSBuf.append(")");
        }
        selectSBuf.append(whereSBuf.toString());

        //
        //  add the "order by" part
        //

        sortStr = lq.getSortingField();
        if ((sortStr != null) && (sortStr.length() > 0)) {
            selectSBuf.append(" order by ").append(sortStr);
        }

        //
        //  for MySQL, the limit comes last
        //

        if (!getAllRecs && isMySQL) {
            selectSBuf.append(" limit ").append(numRecsStr);
        }

        if (Debug.messageEnabled()) {
            Debug.message("lq2Select:select = " + selectSBuf.toString());
        }

        return (selectSBuf.toString());
    }


    //
    //  LogQuery-to-SQL-Select converter for multiple tables
    //
    //  tblNames is the Set of tables to select on
    //  columns contains the comma-separated column names to return...
    //    e.g., "time, data, LoginID", or "*"
    //  lq is the LogQuery specified.  if no logquery, then
    //    lq2Select returns "select <columns> from <tblName>;"
    //

    private String lq2Select (Set tblNames, String columns, LogQuery lq) {
        ArrayList queries;
        StringBuffer selectSBuf = null;
        String whatSBuf = "*";  // select what from tblName? 
        StringBuffer whereSBuf = null; // where ...
        StringBuffer tempSBuf = null;
        String opStr = " and "; // default to "and"; could be "or"
        int lqMatchAny = com.sun.identity.log.LogQuery.MATCH_ANY_CONDITION;
        int qrySize = 0;
        String sortStr = null;
        boolean getAllRecs = false;
        final String CR = " ";

        selectSBuf = new StringBuffer("select ");
        tempSBuf = new StringBuffer("select ");

        //
        //  columns is at least "*"...
        //

        whatSBuf = columns;

        //
        //  if LogQuery.MOST_RECENT_MAX_RECORDS or
        //  LogQuery.ALL_RECORDS, need to retrieve all records
        //

        int numRecs = 0;
        getAllRecs = false;
        String numRecsStr = null;
        String sortByField = null;

        //
        //  if no LogQuery, assume all records
        //
        if (lq != null) {
            numRecs = lq.getNumRecordsWanted();
            if ((numRecs == LogQuery.MOST_RECENT_MAX_RECORDS) ||
                (numRecs == LogQuery.ALL_RECORDS))
            {
                getAllRecs = true;
            } else {
                numRecsStr = Integer.toString(numRecs);
            }
            //
            //  sortByField has to explicitly be in the the
            //  columns requested ("columns"); "columns" can't just
            //  be "*".
            //
            if (!columns.equals("*")) {
                String ssbf = lq.getSortingField();
                if ((ssbf != null) && (ssbf.length() > 0)) {
                    if (columns.indexOf(ssbf) != -1) {
                        //
                        //  found sorting field in columns specified
                        //
                        sortByField = ssbf;
                    }
                }
            }

        } else {
            getAllRecs = true;
        }

        if (Debug.messageEnabled()) {
            Debug.message("lq2Select:getAllRecs = " + getAllRecs +
                ", numRecs = " + numRecs + ", sortByField = " +
                sortByField + ", numRecsStr = " + numRecsStr);
        }

        //
        //  no LogQuery
        //  for Oracle, which supports the union directive:
        //  "select <whatSBuf> from tbl1
        //   union
        //   select <whatSBuf> from tbl2
        //   union
        //   ...
        //   union
        //   select <whatSBuf> from tblx
        //   [order by sortByField]
        //
        //  no "order by" or maxrecords if no LogQuery
        //

        if (lq == null) {
            //
            //  this is the Oracle version (using "union")
            //
            //  no query, so the select should look like:
            //  selectSBuf currently contains "select "
            //
            String baseSelect = "select " + whatSBuf + " from ";

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                selectSBuf.append(baseSelect).append(it.next());
                if (it.hasNext()) {
                    selectSBuf.append(" union ");
                }
            }
            return selectSBuf.toString();
        }


        queries = (ArrayList)lq.getQueries();

        tempSBuf.append(whatSBuf).append(" from ");
        selectSBuf = new StringBuffer();

        //
        //  LogQuery, but no QueryElement(s).
        //
        //  tempSBuf contains the "select <columns> from " part, which will
        //  be the beginning of each select.
        //
        //
        //  oracle can use "union", but mysql < v4 cannot.
        //

        if ((queries == null) || (qrySize = queries.size()) == 0) {
            //
            //  if no query elements, then just the number of records
            //  to return is applied.  select should look something like
            //  (for oracle):
            //  "select * from tbl1 where rownum < [numRecsStr]
            //   union
            //   select * from tbl2 where rownum < [numRecsStr]
            //   union
            //   ...
            //   union
            //   select * from tblx where rownum < [numRecsStr]
            //
            //   can add the " order by <sortByField>" to the end,
            //   if there is a <sortByField> value.
            //
            //   for mysql, can't do:
            //  "select * from tbl1,tb2,...,tblN"
            //   as you get N*(#cols)-wide rows, and
            //   #rows(tbl1)*#rows(tbl2)*...*#rows(tblN) rows
            //   returned.
            //

            String sStr = tempSBuf.toString();

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                //
                //  add tablename to "select * from "
                //
                selectSBuf.append(sStr).append(it.next());
        
                if (!isMySQL) {
                    if (!getAllRecs) {
                        selectSBuf.append(" where rownum < ").
                            append(numRecsStr);
                    }
                    if (it.hasNext()) {
                        selectSBuf.append(CR).append("union").append(CR);
                    } else {
                        if (sortByField != null) {
                            selectSBuf.append(" order by ").
                                append(sortByField);
                        }
                    }
                }
            }
            return selectSBuf.toString();
        }

        //
        //  got table(s), LogQuery, and QueryElement(s).
        //  select (for oracle) will look something like:
        //    select <columns> from tbl1 where [rownum < <numRecsStr> and ]
        //      ((fld1 = 'val1') <opStr>
        //      (fld2 = 'val2') <opStr>
        //      ...
        //      (fldx = 'valx'))
        //    union
        //    select <columns> from tbl2 where [rownum < <numRecsStr> and ]
        //      ((fld1 = 'val1') <opStr>
        //      (fld2 = 'val2') <opStr>
        //      ...
        //      (fldx = 'valx'))
        //    union
        //    ...
        //    union
        //    select <columns> from tblN where [rownum < <numRecsStr> and ]
        //      ((fld1 = 'val1') <opStr>
        //      (fld2 = 'val2') <opStr>
        //      ...
        //      (fldx = 'valx'))
        //    [order by <sortByField>]
        //
        //
        //  for mysql (<v4 doesn't support union), this is incorrect:
        //    select * from tbl1, tbl2, ..., tbln where ((tbl1.fld1 = 'val1')
        //    and (tbl1.fldx = 'valx')) or ((tbl2.fld1 = 'val1') and
        //    (tbl2.fld2 = 'val2')) or ... ((tbln.fld1 = 'val1') and
        //    (tbln.fld2 = 'val2))
        //
        //  the operator was changed from "or" to "and" in the mysql
        //  example to illustrate that the "or" between the tables'
        //  clauses will be fixed (not specifiable).
        //
        //

        if (lq.getGlobalOperand() == lqMatchAny) {
            opStr = " or ";
        }

        //
        //  differences between oracle and mysql to be observed here...
        //

        if (isMySQL) {
            //
            //  the structure of the select with all the tables' names
            //  first, then the where clauses needing the tables' names
            //  sequentially doesn't make for efficient coding...
            //
            //  tempSBuf has the "select <columns> from " part.
            //

            String sStr = tempSBuf.toString();
            selectSBuf.append(sStr);

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                selectSBuf.append((String)it.next());
                if (it.hasNext()) {
                    selectSBuf.append(", ");
                }
            }
            selectSBuf.append(" where ");

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                String tblStr = (String)it.next();
                String wherePart = doMySQLQueryElement(tblStr, queries, opStr);
                selectSBuf.append(wherePart);
                if (it.hasNext()) {
                    selectSBuf.append(" or ");
                }
            }

            //
            //  for MySQL, the limit comes last
            //

            if (!getAllRecs && isMySQL) {
                selectSBuf.append(" limit ").append(numRecsStr);
            }

        } else {        // Oracle
            //
            //  get the part that goes after "select * from "
            //

            //
            //  use sortByField, not lq.getSortingField()... 
            //  checked above that it's explicitly in the columns
            //  requested.
            //
            String wherePart = doOracleQueryElements (queries, numRecsStr,
                opStr, getAllRecs, sortByField);

            //
            //  tempSBuf has the "select <columns> from " part
            //

            String sStr = tempSBuf.toString();

            for (Iterator it = tblNames.iterator(); it.hasNext(); ) {
                selectSBuf.append(sStr).append(it.next()).append(" where ").
                    append(wherePart);
                if (it.hasNext()) {
                    selectSBuf.append(CR).append("union").append(CR);
                    } else {
                    //
                    // this is the attempted fix for order by
                    //
                    if (sortByField != null) {
                        selectSBuf.append(" order by ").
                        append(sortByField);
                    }
                }
            }
        } // !isMySQL

        if (Debug.messageEnabled()) {
            Debug.message("lq2Select:select = " + selectSBuf.toString());
        }

        return (selectSBuf.toString());
    }

    //
    //  create the "where..." clause from the QueryElements...
    //  the part that comes after "where ".
    //
    //  select (for oracle) will look something like:
    //    select * from tbl1 where [rownum < n] and ((fld1 = 'val1') and
    //      (fldx = 'valx')) union
    //    select * from tbl2 where [rownum < n] and ((fld1 = 'val1') and
    //      (fldx = 'valx')) union
    //    ...
    //    select * from tbln where [rownum < n] and ((fld1 = 'val1') and
    //      (fldx = 'valx'))
    //

    private String doOracleQueryElements (ArrayList qes, String numRecsStr,
        String opStr, boolean getAllRecs, String sortStr)
    {
        //
        //  get the columns/values to search on
        //
        int qrySize = qes.size();
        QueryElement qe;
        StringBuilder whereSBuf = new StringBuilder();

        boolean moreThanOneQuery = false;

        //
        //  limiting for oracle comes first after "where"
        //
        //  numRecStr is null if getAllRecs is true.  could have
        //  passed that, but...
        //

        if ((numRecsStr != null) && (numRecsStr.length() > 0)) {
            whereSBuf.append("rownum < ").append(numRecsStr).append(" and ");
        }

        //
        //  if more than one query, then they all should be with
        //  a set of parens, and each should be within parens, e.g.:
        //  ((loginid like 'uid=user2%') or (loginid like 'uid=amAdmin%'))
        //

        if (qrySize > 1) {
            moreThanOneQuery = true;
            whereSBuf.append("(");
        }

        for (int i = 0; i < qrySize; i++) {
            qe = (QueryElement)qes.get(i);
            String fldName = qe.getFieldName();
            String fldValue = qe.getFieldValue();
            String relation;
            int iRelation = qe.getRelation();

            //
            //  both have to be non-null
            //

            switch (iRelation) {
                case QueryElement.GT:
                    relation = " > ";
                    break;
                case QueryElement.LT:
                    relation = " < ";
                    break;
                case QueryElement.EQ:
                    relation = " = ";
                    break;
                case QueryElement.NE:
                    relation = " != ";
                    break;
                case QueryElement.GE:
                    relation = " >= ";
                    break;
                case QueryElement.LE:
                    relation = " <= ";
                    break;
                case QueryElement.CN:  // contains
                case QueryElement.SW:  // starts with
                case QueryElement.EW:  // ends with
                    relation = " like ";
                    break;
                default:
                    relation = " = ";        // for now, anyway
            }

            if ((fldName != null) && (fldName.length() > 0) &&
                (fldValue != null) && (fldValue.length() > 0))
            {
                if (moreThanOneQuery) {
                    whereSBuf.append(" (");
                }
                whereSBuf.append(fldName).append(relation);
                if (iRelation == QueryElement.CN) {
                    whereSBuf.append(" '%").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.SW) {
                    whereSBuf.append(" '").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.EW) {
                    whereSBuf.append(" '%").append(fldValue).append("'");
                } else {
                    whereSBuf.append(" '").append(fldValue).append("'");
                }
                if (moreThanOneQuery) {
                    whereSBuf.append(") ");
                }
            }
            //
            //  if not last element, then
            //  append operator in prep for next element
            //
            if ((i+1) < qrySize) {
                whereSBuf.append(opStr);
            }
        }

        if (moreThanOneQuery) {
            whereSBuf.append(")");
        }

        //
        //  add the "order by" part, if there should be one
        //

        if (Debug.messageEnabled()) {
            Debug.message("doQueryElements:returning " +
                whereSBuf.toString());
        }

        return (whereSBuf.toString());
    }

    //
    //  create the "where..." clause from the QueryElements...
    //  the part that comes after "where ".
    //
    //  for mysql (<v4 doesn't support union):
    //    select * from tbl1, tbl2, ..., tbln where ((tbl1.fld1 = 'val1')
    //    and (tbl1.fldx = 'valx')) or ((tbl2.fld1 = 'val1') and
    //    (tbl2.fld2 = 'val2')) or ... ((tbln.fld1 = 'val1') and
    //    (tbln.fld2 = 'val2)) [limit n]
    //

    private String doMySQLQueryElement(String tblname, ArrayList qes,
        String opStr)
    {
        //
        //  get the columns/values to search on
        //
        int qrySize = qes.size();
        QueryElement qe;
        boolean moreThanOneQuery = false;
        StringBuilder whereSBuf = new StringBuilder();

        //
        //  if more than one query element, then they all should be with
        //  a set of parens, and each should be within parens, e.g.:
        //  ((loginid like 'uid=user2%') or (loginid like 'uid=amAdmin%'))
        //

        if (qrySize > 1) {
            moreThanOneQuery = true;
            whereSBuf.append("(");
        }

        //
        //  now for the "(tblname.col <relation> val) <opStr> (tblname.col..)"
        //
        for (int i = 0; i < qrySize; i++) {
            qe = (QueryElement)qes.get(i);
            String fldName = tblname + "." + qe.getFieldName();
            String fldValue = qe.getFieldValue();
            String relation;
            int iRelation = qe.getRelation();

            //
            //  both have to be non-null
            //

            switch (iRelation) {
                case QueryElement.GT:
                    relation = " > ";
                    break;
                case QueryElement.LT:
                    relation = " < ";
                    break;
                case QueryElement.EQ:
                    relation = " = ";
                    break;
                case QueryElement.NE:
                    relation = " != ";
                    break;
                case QueryElement.GE:
                    relation = " >= ";
                    break;
                case QueryElement.LE:
                    relation = " <= ";
                    break;
                case QueryElement.CN:  // contains
                case QueryElement.SW:  // starts with
                case QueryElement.EW:  // ends with
                    relation = " like ";
                    break;
                default:
                    relation = " = ";        // for now, anyway
            }

            if ((fldName != null) && (fldName.length() > 0) &&
                (fldValue != null) && (fldValue.length() > 0))
            {
                if (moreThanOneQuery) {
                    whereSBuf.append(" (");
                }
                whereSBuf.append(fldName).append(relation);
                if (iRelation == QueryElement.CN) {
                    whereSBuf.append(" '%").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.SW) {
                    whereSBuf.append(" '").append(fldValue).append("%'");
                } else if (iRelation == QueryElement.EW) {
                    whereSBuf.append(" '%").append(fldValue).append("'");
                } else {
                    whereSBuf.append(" '").append(fldValue).append("'");
                }
                if (moreThanOneQuery) {
                    whereSBuf.append(") ");
                }
            }
            //
            //  if not last element, then
            //  append operator in prep for next element
            //
            if ((i+1) < qrySize) {
                whereSBuf.append(opStr);
            }
        }

        if (moreThanOneQuery) {
            whereSBuf.append(")");
        }

        return (whereSBuf.toString());
    }
}

