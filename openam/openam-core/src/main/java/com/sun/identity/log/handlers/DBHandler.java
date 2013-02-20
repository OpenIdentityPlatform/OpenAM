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
 * $Id: DBHandler.java,v 1.19 2009/12/15 17:59:16 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.log.handlers;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import com.iplanet.am.util.ThreadPoolException;
import com.iplanet.log.ConnectionException;
import com.iplanet.log.DriverLoadException;
import com.iplanet.log.NullLocationException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.AMLogException;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;

/**
 * DBHandler takes log messages from the Logger and exports
 * them to a specified Database. DBHandler reads LogManager's
 * configuration to get information about Database userid,
 * password, Database Driver, Database location. It takes from the caller the
 * table name which will be created if it doesnt already exists.
 * <p>
 * By default DBFormatter is used for formatting the logRecords.
 */
public class DBHandler extends Handler {
    private LogManager lmanager = LogManagerUtil.getLogManager();
    private String driver;
    private String databaseURL;
    private Connection conn = null;
    private String tableName;
    private Formatter formatter;
    private String userName;
    private String password;
    private int recCountLimit;
    private int recMaxDBMem = 2;
    private LinkedList recordBuffer;
    private TimeBufferingTask bufferTask;
    private boolean timeBufferingEnabled = false;
    private SsoServerLoggingSvcImpl logServiceImplForMonitoring = null;
    private SsoServerLoggingHdlrEntryImpl dbLogHandlerForMonitoring = null;
    //
    //  this is to keep track when the connection to the DB
    //  is lost so that debug messages are logged only on
    //  transitions.
    //
    private boolean connectionToDBLost = false;
    private boolean isMySQL = false;

    private String oraDataType;
    private String mysqlDataType;
    private int dbFieldMax = 0;

    private void configure() throws NullLocationException,
        FormatterInitException
    {
        String cname = DBHandler.class.getName();
        setFilter(null);
        try {
            setEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Debug.error(tableName +
                ":DBHandler: unsupportedEncodingException ", e);
        }

        String bufferSize = lmanager.getProperty(LogConstants.BUFFER_SIZE);
        if (bufferSize != null && bufferSize.length() > 0) {
            try {
                recCountLimit = Integer.parseInt(bufferSize);
            } catch (NumberFormatException nfe) {
                Debug.warning(tableName +
                    ":DBHandler: NumberFormatException ", nfe);
                if (Debug.messageEnabled()) {
                    Debug.message(tableName +
                        ":DBHandler: Setting buffer size to 1");
                }
                recCountLimit = 1;
            }
        } else {
            Debug.warning(tableName +
                ":DBHandler: Invalid buffer size: " + bufferSize);
            if (Debug.messageEnabled()) {
                Debug.message(tableName +
                    ":DBHandler: Setting buffer size to 1");
            }
            recCountLimit = 1;
        }

        String recMaxDBMemStr = lmanager.getProperty(
            LogConstants.DB_MEM_MAX_RECS);
        if (recMaxDBMemStr != null && recMaxDBMemStr.length() > 0) {
            try {
                recMaxDBMem = Integer.parseInt(recMaxDBMemStr);
            } catch (NumberFormatException nfe) {
                Debug.warning(tableName +
                    ":DBHandler:recMaxDBMem (" + recMaxDBMemStr +
                    "): NumberFormatException ", nfe);
                if (Debug.messageEnabled()) {
                    Debug.message(tableName +
                        ":DBHandler: Setting Max DB Mem Buffer Size to 2x (" +
                        2*recCountLimit + ") the Buffer Size (" +
                        recCountLimit + ")");
                }
                recMaxDBMem = 2*recCountLimit;
            }
        } else {
            Debug.warning(tableName +
                ":DBHandler: Invalid buffer size: " + bufferSize);
            if (Debug.messageEnabled()) {
                Debug.message(tableName +
                    ":DBHandler: Defaulting Max DB Mem Buffer Size " +
                    "to 2x Buffer Size");
            }
            recMaxDBMem = 2*recCountLimit;
        }

        if (recMaxDBMem < recCountLimit) {
            Debug.warning (tableName +
                ":DBHandler:Maximum DB memory buffer size < Buffer Size, " +
                "setting to buffer size (" + recCountLimit + ")");
            recMaxDBMem = recCountLimit;
        }

        String status =
            lmanager.getProperty(LogConstants.TIME_BUFFERING_STATUS);

        if ( status != null && status.equalsIgnoreCase("ON")) {
            timeBufferingEnabled = true;
        }

        oraDataType = lmanager.getProperty(LogConstants.ORA_DBDATA_FIELDTYPE);
        mysqlDataType =
            lmanager.getProperty(LogConstants.MYSQL_DBDATA_FIELDTYPE);

        databaseURL = lmanager.getProperty(LogConstants.LOG_LOCATION);
        if ((databaseURL == null) || (databaseURL.length() == 0)) {
            throw new NullLocationException("Database URL location is null");
        }

        String strFormatter = com.sun.identity.log.LogManager.FORMATTER;
        if ((strFormatter == null) || (strFormatter.length() == 0)) {
            throw new FormatterInitException(
                "Unable To Initialize DBFormatter");
        }
        userName = lmanager.getProperty(LogConstants.DB_USER);
        if ((userName == null) || (userName.length() == 0)) {
            throw new NullLocationException("userName is null");
        }
        password = lmanager.getProperty(LogConstants.DB_PASSWORD);
        if ((password == null) || (password.length() == 0)) {
            throw new NullLocationException("password not provided");
        }
        driver = lmanager.getProperty(LogConstants.DB_DRIVER);
        if ((driver == null) || (driver.length() == 0)) {
            throw new NullLocationException("driver not provided");
        }
        //
        //  don't know about drivers other than Oracle and MySQL
        //
        if (driver.toLowerCase().indexOf("oracle") != -1){
            isMySQL = false;
        } else if (driver.toLowerCase().indexOf("mysql") != -1) {
            isMySQL = true;
        } else {
            isMySQL = false;
            Debug.warning(tableName +
                ":DBHandler:configure:assuming driver: '" + driver +
                "' is Oracle-compatible.");
        }

        try {
            Class clz = Class.forName(strFormatter);
            formatter = (Formatter) clz.newInstance();
            setFormatter(formatter);
        } catch (Exception e) {    // should be Invalid Formatter Exception
            Debug.error(tableName + ":DBHandler: Could not load Formatter", e);
            throw new FormatterInitException(
                "Unable To Initialize DBFormatter " + e.getMessage());
        }
    }

    private void connectToDatabase(String userName, String password)
        throws ConnectionException, DriverLoadException 
    {
        //Monit start
        if (MonitoringUtil.isRunning()) {
            if (dbLogHandlerForMonitoring == null) {
                logServiceImplForMonitoring =
                    Agent.getLoggingSvcMBean();
                dbLogHandlerForMonitoring =
                    logServiceImplForMonitoring.getHandler(
                        SsoServerLoggingSvcImpl.DB_HANDLER_NAME);
            }
            if (dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionRequests(1);
            }
        }
        //Monit end
        try {
            Class.forName(driver);
            this.conn = 
                DriverManager.getConnection(databaseURL, userName, password);
        } catch (ClassNotFoundException e) {
            Debug.error(tableName +
                ":DBHandler: ClassNotFoundException " + e.getMessage());
            //Monit start
            if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionsFailed(1);
            }
            //Monit end
            throw new DriverLoadException(e.getMessage());
        } catch (SQLException sqle) {
            //
            //  if start up with Oracle DB down, can get sqle.getErrorCode()
            //  == 1034, "ORA-01034: ORACLE not available"
            //  MySQL returns error code 0, message:
            //  "unable to connect to any hosts due to
            //  exception: java.net.ConnectException: Connection refused
            //
            Debug.error(tableName +
                ":DBHandler: ConnectionException (" + sqle.getErrorCode() +
                "): " + sqle.getMessage());
            //Monit start
            if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionsFailed(1);
            }
            //Monit end
            throw new ConnectionException(sqle.getMessage());
        }

        //Monit start
        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
            dbLogHandlerForMonitoring.incHandlerConnectionsMade(1);
        }
        //Monit end
    }

    //
    //  detected that connection to the DB had failed previously;
    //  this routine reestablishes the connection, and checks that
    //  the table exists (creating it if it doesn't).
    //
    private void reconnectToDatabase()
        throws ConnectionException, DriverLoadException 
    {
        //Monit start
        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
            dbLogHandlerForMonitoring.incHandlerConnectionRequests(1);
        }
        //Monit end

        try {
            Class.forName(driver);
            this.conn = DriverManager.getConnection(databaseURL,
                userName, password);
        } catch (ClassNotFoundException e) {
            //Monit start
            if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionsFailed(1);
            }
            //Monit end
            throw new DriverLoadException(e.getMessage());
        } catch (SQLException sqle) {
            Debug.error (tableName +
                ":DBHandler:reconnect (" + sqle.getErrorCode() + "): " +
                sqle.getMessage());
            //Monit start
            if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionsFailed(1);
            }
            //Monit end
            throw new ConnectionException(sqle.getMessage());
        }

        //Monit start
        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
            dbLogHandlerForMonitoring.incHandlerConnectionsMade(1);
        }
        //Monit end
    }

    /**
     * Constructor takes the tableName as a parameter. Gets the configuration
     * information from LogManager regarding the user name, password, database
     * driver, the log location. Gets the formatter class from the
     * configuration, loads it and sets it as default Formatter. Connects to
     * the database using the username and password. If the table by that name
     * doesnot exists, creates the table.
     * @param tableName Database table name for logger.
     */
    public DBHandler(String tableName) {
        if ((tableName == null) || (tableName.length() == 0)) {
            return;
        }
        tableName = tableName.replace('.', '_');
        this.tableName = tableName;

        try {
            configure();
        } catch (NullLocationException nle) {
            Debug.error(tableName + ":DBHandler: Null Location", nle);
        } catch (FormatterInitException fie) {
            Debug.error(tableName +
                ":DBHandler: Unable to Initialize formatter", fie);
        }

        String stat = lmanager.getProperty(LogConstants.LOG_STATUS_ATTR);
        if (Debug.messageEnabled()) {
            Debug.message("DBHandler:tableName = " + tableName +
                ", LOG_STATUS = " + stat);
        }

        if (stat != null & !(stat.startsWith("INACTIVE"))) {
            connectionToDBLost = true;
            try {
                connectToDatabase(userName, password);
                connectionToDBLost = false;
                createTable(tableName);
            } catch (SQLException sqe) {
                Debug.error(tableName +
                    ":DBHandler: sql operation unsuccessful (" +
                    sqe.getErrorCode() + "): " + sqe.getMessage());
            } catch (ConnectionException ce) {
                Debug.error(tableName +
                    ":DBHandler: Could not connect to database:" +
                    ce.getMessage());
            } catch (DriverLoadException dle) {
                Debug.error(tableName +
                    ":DBHandler: Could not load driver", dle);
            } catch (UnsupportedEncodingException use) {
                Debug.error(tableName +
                    ":DBHandler: Unsupported Encoding: " + use.getMessage());
            }
            connectionToDBLost = false;
        }

        recordBuffer = new LinkedList();
        if (timeBufferingEnabled) {
            startTimeBufferingThread();
        }

        if (MonitoringUtil.isRunning()) {
            logServiceImplForMonitoring =
                Agent.getLoggingSvcMBean();
            dbLogHandlerForMonitoring = logServiceImplForMonitoring.getHandler(
                SsoServerLoggingSvcImpl.DB_HANDLER_NAME);
        }
    }

    /**
     * Formats and publishes the LogRecord.
     * <p>
     * A sql command is to be generated to write a record to the database. A
     * typical sql command for writing a record to the database can be given
     * as follows:
     * <p>
     * insert into "amSSO_access" (time, date, loginid, domain, level, message)
     * values ('10:10:10', '10th June, 2002', 'uid=amadmin,o=sun.com', 
     * 'o=sun.com', 'NULL', 'SESSION CREATE SUCCESSFUL')
     * <p>
     * To construct the above sql query, this publish method uses the 
     * Formatter's getHead method to get the string containing COMMA seperated 
     * all field set, uses format method to get the COMMA seperated field 
     * values corresponding to those fields.
     * 
     * @param logRecord the log record to be published.
     *
     */
    public void publish(java.util.logging.LogRecord logRecord) {
        //Monit start
        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
            dbLogHandlerForMonitoring.incHandlerRequestCount(1);
        }
        //Monit end
        if (!isLoggable(logRecord)) {
            return;
        }
        synchronized (this) {
            recordBuffer.add(logRecord);
            if (recordBuffer.size() >= recCountLimit) {
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + ":DBHandler:.publish(): got "
                        + recordBuffer.size() + " records, Limit " +
                        recCountLimit + " writing all");
                }
                nonBlockingFlush();
            }
        }
    }

    private String getColString() {
        String cols = getFormatter().getHead(this);
        if (Debug.messageEnabled()) {
            Debug.message("cols = " + cols);
        }
        StringBuilder colStrBuffer = new StringBuilder(1000);
        if (cols != null) {
            colStrBuffer.append("insert into ")
                    .append(tableName)
                    .append(" (")
                    .append(cols)
                    .append(")")
                    .append(" values (");
        } else {
            colStrBuffer.append("insert into ")
                    .append(tableName)
                    .append(" values (");
        }
        return colStrBuffer.toString();
    }
    
    /**
     * Flush any buffered messages.
     */
    protected void nonBlockingFlush() {
        String tabelName = null;
        LinkedList tempBuffer = null;
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + 
                        ":DBHandler:flush: no records in buffer to write");
                }
                return;
            }

            tableName = getTableName();
            if (tableName == null) {
                Debug.error(tableName + 
                    ":DBHandler:flush:NullLocationException: table name is" + 
                    " null");
                int recordsToBeDropped = recordBuffer.size();
                recordBuffer.clear();
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(
                    recordsToBeDropped);
                }
                //Monit end
                return;
            } else {
                tempBuffer = recordBuffer;
                recordBuffer = new LinkedList();
            }
        }

        LogTask task = new LogTask(tempBuffer);
        try {
            // Get an instance as required otherwise it can cause issues on container restart.
            LoggingThread.getInstance().run(task);
        } catch (ThreadPoolException ex) {
            // use current thread to flush the data if ThreadPool is shutdown
            synchronized (this) {
                task.run();
            }
        }
    }

    public void flush() {
        String tabelName = null;
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                return;
            }

            tableName = getTableName();
            if (tableName == null) {
                int recordsToBeDropped = recordBuffer.size();
                recordBuffer.clear();
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(
                    recordsToBeDropped);
                }
                //Monit end
                return;
            }
            //
            //  check if the connection to the db had problems before
            //  if so, try to reconnect and then make sure the table's
            //  there.
            //

            if ((conn == null) || connectionToDBLost) {
                //
                //  either the connection was never initially made, or it
                //  was lost somewhere along the line.  try to make the
                //  connection now.
                //
                try {
                    reconnectToDatabase();
                } catch (DriverLoadException dle) {
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(recordBuffer);
                    throw new AMLogException(AMLogException.LOG_DB_DRIVER +
                        "'" + driver + "'");
                } catch (ConnectionException ce) {
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(recordBuffer);
                    throw new AMLogException(
                        AMLogException.LOG_DB_CONNECT_FAILED);
                }

                //
                //  re-established the connection to the DB.  now
                //  check on the table.  
                //

                connectionToDBLost = false;
                try {
                    //
                    //  any exception from createTable() might mean the
                    //  table's not in the DB... just record the error, and
                    //  let the insert let us know if the record didn't get
                    //  logged.
                    //
                    createTable (tableName);
                } catch (SQLException se) {
                    // the error will be handled at later part of the code
                } catch (UnsupportedEncodingException usee) {
                    // the error will be handled at later part of the code
                }
            }

            //
            //  when using oracle, and the db is down, you get an
            //  exception on the createStatement.  unfortunately,
            //  it's a TTC message (e.g. [ORA-]17310... the getErrorCode
            //  returns 17310), a vendor-specific error code.
            //
            //  MySQL db, on the other hand seems to return from
            //  the createStatement() call "ok".  catch it on the
            //  executeUpdate(), below.
            //

            Statement stmt = null;
            try {
                stmt = conn.createStatement();
            } catch (SQLException se) {

                //
                // try reconnecting to DB once.  if can't, dump the record
                // and wait for the next attempt.
                //

                try {
                    conn.close();
                } catch (SQLException ex) {
                }

                connectionToDBLost = true;
                try {
                    reconnectToDatabase();
                } catch (DriverLoadException dle) {
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(recordBuffer);
                    throw new AMLogException(AMLogException.LOG_DB_DRIVER +
                        "'" + driver + "'");
                } catch (ConnectionException ce) {
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(recordBuffer);
                    throw new AMLogException(
                        AMLogException.LOG_DB_CONNECT_FAILED);
                }
                connectionToDBLost = false;

                //
                //  connection's reestablished, now do the table check.
                //

                try {
                    createTable (tableName);
                } catch (SQLException sqle) {
                    // the error will be handled by later part of the code
                } catch (UnsupportedEncodingException usee) {
                    // the error will be handled by later part of the code
                }

                try {
                    stmt = conn.createStatement();
                } catch (SQLException sqle) {
                    //
                    //  second time this failed (note that this whole block
                    //  started with the createStatement()).
                    //  log the error message, and continue on (for now)
                    //
                    throw new AMLogException(
                        AMLogException.LOG_DB_CSTATEMENT);
                }
            }

            String vals = null;                
            int rbsz = recordBuffer.size();
            Formatter formatter = getFormatter();
            for (Iterator iter = recordBuffer.iterator(); iter.hasNext();) {
                vals = formatter.format((java.util.logging.LogRecord)
                    iter.next());
                StringBuilder insertStringBuffer = new StringBuilder(2000);
                insertStringBuffer.append(getColString()).
                   append(vals).append(")");

                String insertStr = insertStringBuffer.toString();
                        
                try {
                    stmt.executeUpdate(insertStr);
                    //Monit start
                    if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring !=
                        null){
                        dbLogHandlerForMonitoring.incHandlerSuccessCount(1);
                    }
                    //Monit end
                } catch (SQLException sqle) {
                    /*
                     *  as mentioned above, connection errors to oracle
                     *  seem to get caught in the createStatement(), while
                     *  with mysql, they get caught here.
                     * 
                     *  the other thing that could happen is the table was
                     *  dropped, but not the connection.
                     */
                    int sqleErrCode = sqle.getErrorCode();
                    boolean tableDoesNotExist = false;

                    /*
                     *  unfortunately have to check which db and specific
                     *  error codes...
                     *  see if table's missing
                     *  MySQL: 1146
                     *  Oracle: 942
                     */
                    if ((isMySQL && (sqleErrCode == 1146)) ||
                        (!isMySQL && (sqleErrCode == 942))) {
                        /*
                         *  connection to DB's there, but table's missing
                         *
                         *  gotta make the table; try the executeUpdate()
                         *  again
                         */
                        try {
                            createTable(tableName);
                        } catch (SQLException se) {
                        // the error will be handled by later part of the code
                        } catch (UnsupportedEncodingException usee) {
                        // the error will be handled by later part of the code
                        }

                        try {
                            stmt.executeUpdate(insertStr);
                        } catch (SQLException sqle2) {
                            //  guess NOW it's an error
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        }
                    } else if ((isMySQL && (sqleErrCode == 0)) ||
                        (!isMySQL && ((sqleErrCode == 17002) ||
                        (sqleErrCode == 17410))))
                    {
                        /*
                         *  connection's probably gone gotta try everything
                         *  up to this point again, starting with
                         *  reconnecting to the db.  any failure along the
                         *  line this time gets an exception.
                         */

                        try {
                            conn.close();
                        } catch (SQLException ex) {
                        // the error will be handled by later part of the code
                        }

                        connectionToDBLost = true;
                        try {
                            reconnectToDatabase();
                        } catch (DriverLoadException dle) {
                            /*
                             * if the max mem buffer is exceeded,
                             * dump the records
                             */
                            clearBuffer(recordBuffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_RECONNECT_FAILED);
                        } catch (ConnectionException ce) {
                            /*
                             * if the max mem buffer is exceeded,
                             * dump the records
                             */
                            clearBuffer(recordBuffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_RECONNECT_FAILED);
                        }
                        connectionToDBLost = false;

                        /*
                         *  bunch the createTable, createStatement, and
                         *  executeUpdate together because if any of these
                         *  fail, throw an exception.
                         */
                        try {
                            createTable (tableName);
                            stmt = conn.createStatement();
                            stmt.executeUpdate(insertStr);
                            //Monit start
                            if (MonitoringUtil.isRunning() &&
                                dbLogHandlerForMonitoring != null)
                            {
                                dbLogHandlerForMonitoring.
                                    incHandlerSuccessCount(1);
                            }
                            //Monit end
                        } catch (SQLException sqe) {
                            /*
                             *  if the max mem buffer is exceeded,
                             *  dump the records
                             */
                            clearBuffer(recordBuffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        } catch (UnsupportedEncodingException usee) {
                            /*
                             *  if the max mem buffer is exceeded,
                             *  dump the records
                             */
                            clearBuffer(recordBuffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        }
                    } else {
                        /*
                         *  not sure what to do here yet.  log the error,
                         *  throw an exception, and see what happens next.
                         *
                         *  just for informational purposes, you get the
                         *  following if the columns don't exist:
                         *    if ((isMySQL && (sqleErrCode == 1054)) ||
                         *        (!isMySQL && ((sqleErrCode == 904) ||
                         *              (sqleErrCode == 913))))
                         */
                        // if the max mem buffer is exceeded, dump the
                        // records
                        clearBuffer(recordBuffer);
                        throw new AMLogException (
                            AMLogException.LOG_DB_EXECUPDATE);
                    }
                }
            }
            recordBuffer.clear();
            try {
                stmt.close();
            } catch (SQLException se) {
                // don't need to handle as the function will be exited.
            }
        }
    }
    
    /**
     * Flush any buffered messages and close the current output stream.
     */
    public void close() {
        try {
            flush();
        } catch (AMLogException ale) {
            Debug.error(tableName + ":DBHandler:close/flush error: " +
                ale.getMessage());
        }
        if(conn != null) {
            try {
                conn.close();
            }
            catch (SQLException ce) {
                Debug.error(tableName + 
                    ":DBHandler: Unable To Close Connection", ce);
            }
        }
        stopBufferTimer();
    }
    
    private void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    private String getTableName() {
        return tableName;
    }

    private void clearBuffer(LinkedList buffer) {
        synchronized (this) {
            int reccnt = recordBuffer.size();
            if (buffer != recordBuffer) {
                reccnt += buffer.size();
            }
            if (reccnt > recMaxDBMem) {            
                int removeCount = reccnt - recMaxDBMem;
                    Debug.error(tableName + ":DBHandler:dropping " +
                        removeCount + " records.");
                if (removeCount >= buffer.size()) {
                    removeCount -= buffer.size();
                    buffer.clear();
                }
                for(int i = 0; i < removeCount; ++i) {
                    if (!buffer.isEmpty()) {
                        buffer.remove(0);
                    } else {
                        recordBuffer.remove(0);
                    }
                }
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(
                        removeCount);
                }
                //Monit end
            }
            if ((buffer != recordBuffer) && (!buffer.isEmpty())) {
                for (int i = 0; i < buffer.size(); i++) {
                    recordBuffer.addFirst(buffer.removeLast());
                }
            }
        }
    }

    /**
     * This method first checks to see if the table already exists
     * if not it creates a table with the fields.
     * Since the field names/types are not known for sure, each field is
     * assigned varchar 255. In case we had idea about whether the field is
     * time or ipaddress or whatever... we could have assigned space
     * accordingly. The trade off is b/w the ability to keep DBHandler
     * indpendent of the fields(we need not hard core the fields) and space.
     */
    private void createTable(String tableName)
        throws SQLException, UnsupportedEncodingException
    {
        StringBuffer sbuffer = new StringBuffer();
        // form a query string to check if the table exists.
        String oracleTableName =
            new String(tableName.getBytes("UTF-8")).toUpperCase();

        try {
            if(!isMySQL){
                /*
                 *  unlike MySQL, Oracle seems to make the table name
                 *  all uppercase.  wonder if you can query using the
                 *  original case?
                 */
                sbuffer.append(
                "select count(table_name) from all_tables where table_name = ");
                sbuffer.append("'")
                        .append(oracleTableName)
                       .append("'");
            } else {
                /*
                 *  MySQL makes the table (at least running on solaris)
                 *  the same case as specified in the create, so the
                 *  query needs to specify the same case.
                 */
                sbuffer.append("show tables like ");
                sbuffer.append("'")
                       .append((new String(tableName.getBytes("UTF-8"))))
                       .append("'");
            }

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sbuffer.toString());
            boolean foundTable = false;
            if(isMySQL) {
                String result = null;
                while (rs.next()) {
                    result = rs.getString(1);
                }
                if (result != null && result.equalsIgnoreCase(tableName)) {
                    foundTable = true;
                }
            } else {
                int result = 0;
                while (rs.next()) {
                    result = rs.getInt(1);
                }
                if (result == 1) {
                    foundTable = true;
                }
            }
            try{
                stmt.close();
                rs.close();
            } catch(SQLException ex){
                Debug.error("DBHandler:createTable: " + ex.getMessage());
            }

            /*
             *  if the table's in the DB, then check if it has all
             *  the columns we're going to want to write.
             *  if table's not in the DB, fall through (as before),
             *  to create it.
             */
            if (foundTable == true) {
                String getColNames = null;
                if (isMySQL) {
                    getColNames = "show columns from " + tableName;
                } else {
                    getColNames =
                        "select column_name from USER_TAB_COLUMNS " +
                        "where Table_Name = '" + oracleTableName + "'";
                }

                stmt = conn.createStatement();
                try {
                    rs = stmt.executeQuery(getColNames);
                } catch (SQLException sqe) {
                    Debug.error("DBHandler:createTable: '" + getColNames +
                        "'; error (" + sqe.getErrorCode() + "); msg = " +
                        sqe.getMessage());
                    /*
                     *  guess we'll have to just return here, and
                     *  let the flush handle the error...
                     */
                    return;
                }
                ResultSetMetaData rsmd = rs.getMetaData();
                int numCols = rsmd.getColumnCount();

                String colName = null;
                int tempj = 0;
                Set colSet = new HashSet();

                /*
                 *  Oracle appears to return column names in
                 *  all uppercase, 1 per cursor position.
                 *  MySQL returns column names in the case created,
                 *  also 1 per cursor position, but also information
                 *  about the column (type ['varchar(255)'], and
                 *  | Null | Key | Default | Extra).  the column name
                 *  is in the first column (#1) of the row.
                 */

                while (rs.next()) {
                    colName = rs.getString(1);
                    colSet.add(colName);
                    for (int i = 0; i < numCols; i++) {
                            colName = rs.getString(i+1);
                    }
                    tempj++;
                }
                try{
                    stmt.close();
                } catch(SQLException ex){
                    Debug.error("DBHandler:createTable: " + ex.getMessage());
                }
                
                /*
                 *  check that the columns we want to write are
                 *  already in the table.  if not, going to issue
                 *  an alter command.  except for Data field, both
                 *  Oracle and MySQL lengths are 255 (see in create, below).
                 *
                 *  Oracle seems to return the column names in uppercase.
                 */

                StringBuilder colList = new StringBuilder();
                String [] allFields = lmanager.getAllFields();
                boolean addedOne = false;
                String tmpx = null;
                for (int i = 2; i < allFields.length - 1; i++) {
                    if (isMySQL) {
                        tmpx = allFields[i];
                    } else {
                        tmpx = allFields[i].toUpperCase();
                    }
                    if (!colSet.contains(tmpx)) {
                        if (addedOne) {
                            colList.append(", ");
                        }
                        colList.append(allFields[i]).append(" varchar(255)");
                        addedOne = true;
                    }
                }
                if (colList.length() > 0) {
                    String altStr = "alter table " + tableName + " add (" +
                        colList.toString() + ")";
                
                    try {
                        stmt = conn.createStatement();
                        stmt.execute(altStr);
                        stmt.close();
                    } catch (SQLException sqle) {
                        Debug.error("DBHandler:createTable: '" + altStr +
                            "'; error (" + sqle.getErrorCode() + "); msg = " +
                            sqle.getMessage());
                        /*
                         *  guess we'll have to just return here, and
                         *  let the flush handle the error...
                         *
                         *  there's a return right after this, so just
                         *  fall through.
                         */
                    }
                }
                return;
            }
        } catch (SQLException e) {
            Debug.error(tableName +
                ":DBHandler:createTable:Query:SQLException (" +
                e.getErrorCode() + "): " + e.getMessage());
            //  rethrow the exception
            throw e;
        } catch (UnsupportedEncodingException use) {
            Debug.error(tableName + 
                ":DBHandler:createTable:Query:UE: "+
                use.getMessage());
            //  rethrow the exception
            throw use;
        }

        //  didn't find the table in the DB, so create it.

        sbuffer = new StringBuffer();
        try {
            sbuffer.append("create table ")
                   .append(new String(tableName.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            Debug.error(tableName + 
                ":DBHandler: unsupported encoding exception uee", uee);
        }

        String varCharX = "varchar2"; // default for Oracle

        if (isMySQL) {
            sbuffer.append(" (time datetime, ");
        } else {
            sbuffer.append(" (time date, ");
        }

        /*
         * next column is the DATA column, which will be a non-VARCHAR*
         * type, since it *can* contain a large quantity of 'character'
         * (note not binary) data.  Oracle uses "CLOB", while MySQL
         * uses "LONGTEXT".
         */

        if(isMySQL) {
            varCharX = "varchar";
            sbuffer.append(" data ").append(mysqlDataType).append(", ");
        } else {
            sbuffer.append(" data ").append(oraDataType).append(", ");
        }

        String [] allFields = lmanager.getAllFields();
        int i = 0;
        for (i = 2; i < allFields.length - 1; i++) {
            sbuffer.append(allFields[i]).append(" ").append(varCharX).append(" (255), ");
        }
        sbuffer.append(allFields[i]).append(" ").append(varCharX).append(" (255)) ");
        String createString = sbuffer.toString();
        try {
            Statement stmt = conn.createStatement();
            if (Debug.messageEnabled()) {
                Debug.message(tableName + 
                    ":DBHandler: the query string for creating is " +
                    createString);
            }
            stmt.executeUpdate(createString);
            stmt.close();
        } catch (SQLException sqe) {
            Debug.error(tableName +
                ":DBHandler:createTable:Execute:SQLEx (" +
                sqe.getErrorCode() + "): " + sqe.getMessage());
            //
            //  rethrow the exception
            //
            throw sqe;
        }
    }

    private class LogTask implements Runnable {

        private LinkedList buffer;

        public LogTask(LinkedList buffer) {
            this.buffer = buffer;
        }

        public void run() {
            //
            //  check if the connection to the db had problems before
            //  if so, try to reconnect and then make sure the table's
            //  there.
            //

            if ((conn == null) || connectionToDBLost) {
                //
                //  either the connection was never initially made, or it
                //  was lost somewhere along the line.  try to make the
                //  connection now.
                //
                try {
                    reconnectToDatabase();
                    Debug.error (tableName +
                        ":DBHandler:flush:reconnectToDatabase" +
                        " successful.");
                } catch (DriverLoadException dle) {
                    Debug.error(tableName +
                        ":DBHandler:flush:reconnectToDatabase:DLE: " +
                        dle.getMessage());
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(buffer);
                    throw new AMLogException(AMLogException.LOG_DB_DRIVER +
                        "'" + driver + "'");
                } catch (ConnectionException ce) {
                    Debug.error(tableName +
                        ":DBHandler:flush:reconnectToDatabase:CE: " +
                        ce.getMessage());
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(buffer);
                    throw new AMLogException(
                        AMLogException.LOG_DB_CONNECT_FAILED);
                }

                //
                //  re-established the connection to the DB.  now
                //  check on the table.  
                //

                connectionToDBLost = false;
                try {
                    //
                    //  any exception from createTable() might mean the
                    //  table's not in the DB... just record the error, and
                    //  let the insert let us know if the record didn't get
                    //  logged.
                    //
                    createTable (tableName);
                } catch (SQLException se) {
                    if (Debug.messageEnabled()) {
                        Debug.message(tableName +
                            ":DBHandler:flush:reconnect:cTable:SQLE (" +
                            se.getErrorCode() + "): " + se.getMessage());
                    }
                } catch (UnsupportedEncodingException usee) {
                    if (Debug.messageEnabled()) {
                        Debug.message(tableName +
                            ":DBHandler:flush:reconnect:cTable:UE: " +
                            usee.getMessage());
                    }
                }
            }

                //
                //  when using oracle, and the db is down, you get an
                //  exception on the createStatement.  unfortunately,
                //  it's a TTC message (e.g. [ORA-]17310... the getErrorCode
                //  returns 17310), a vendor-specific error code.
                //
                //  MySQL db, on the other hand seems to return from
                //  the createStatement() call "ok".  catch it on the
                //  executeUpdate(), below.
                //

                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                } catch (SQLException se) {
                //
                //  observed that when Oracle's down, it's detected here.
                //  error code 1034.
                //
                Debug.error(tableName +
                    ":DBHandler:flush:cStatement:SQLE (" +
                    se.getErrorCode() + "): " + se.getMessage());

                //
                // try reconnecting to DB once.  if can't, dump the record
                // and wait for the next attempt.
                //

                try {
                    conn.close();
                } catch (SQLException ex) {
                    //
                    //  ignore exception and continue
                    //
                    if (Debug.messageEnabled()) {
                        Debug.error (tableName +
                            ":DBHandler:flush:cStatement:close:SQLE (" +
                            ex.getErrorCode() + ")" + ex.getMessage());
                    }
                }

                connectionToDBLost = true;
                try {
                    reconnectToDatabase();
                    Debug.error (tableName +
                        ":DBHandler:flush:cStatement:reconnect" + 
                        " successful.");
                } catch (DriverLoadException dle) {
                    Debug.error(tableName +
                        ":DBHandler:flush:cStatement:reconnect:DLE: " +
                        dle.getMessage());
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(buffer);
                    throw new AMLogException(AMLogException.LOG_DB_DRIVER +
                        "'" + driver + "'");
                } catch (ConnectionException ce) {
                    Debug.error(tableName +
                        ":DBHandler:flush:cStatement:reconnect:CE: " +
                        ce.getMessage());
                    //
                    //  if the max mem buffer is exceeded, dump the records
                    //
                    clearBuffer(buffer);
                    throw new AMLogException(
                        AMLogException.LOG_DB_CONNECT_FAILED);
                }
                connectionToDBLost = false;

                //
                //  connection's reestablished, now do the table check.
                //

                try {
                    createTable (tableName);
                } catch (SQLException sqle) {
                    if (Debug.messageEnabled()) {
                        Debug.message(tableName +
                            ":DBHandler:flush:cStatement:reconnect:" +
                            "cTable:SQLE (" + sqle.getErrorCode() + "): " +
                            sqle.getMessage());
                    }
                } catch (UnsupportedEncodingException usee) {
                    if (Debug.messageEnabled()) {
                        Debug.message(tableName +
                            ":DBHandler:flush:cStatement:reconnect:" + 
                            "cTable:UE: " + usee.getMessage());
                    }
                }

                try {
                    stmt = conn.createStatement();
                } catch (SQLException sqle) {
                    //
                    //  second time this failed (note that this whole block
                    //  started with the createStatement()).
                    //  log the error message, and continue on (for now)
                    //
                    Debug.error(tableName +
                        ":DBHandler:flush:cStatement:reconnect:cSt:SQLE ("
                        + sqle.getErrorCode() + "): " + sqle.getMessage());
                    throw new AMLogException(
                        AMLogException.LOG_DB_CSTATEMENT);
                }
            }

            String vals = null;                
            int rbsz = buffer.size();
            Formatter formatter = getFormatter();
            for (Iterator iter = buffer.iterator(); iter.hasNext();) {
                vals = formatter.format((java.util.logging.LogRecord)
                    iter.next());
                if (Debug.messageEnabled()) {
                    Debug.message("values = " + vals);
                }
                StringBuilder insertStringBuffer = new StringBuilder(2000);
                insertStringBuffer.append(getColString()).
                   append(vals).append(")");

                String insertStr = insertStringBuffer.toString();
                        
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + 
                        ":DBHandler:insertString is: " + insertStr);
                }
                try {
                    stmt.executeUpdate(insertStr);
                    //Monit start
                    if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring !=
                        null){
                        dbLogHandlerForMonitoring.incHandlerSuccessCount(1);
                    }
                    //Monit end
                } catch (SQLException sqle) {
                    /*
                     *  as mentioned above, connection errors to oracle
                     *  seem to get caught in the createStatement(), while
                     *  with mysql, they get caught here.
                     * 
                     *  the other thing that could happen is the table was
                     *  dropped, but not the connection.
                     */
                    int sqleErrCode = sqle.getErrorCode();
                    boolean tableDoesNotExist = false;
                    if (Debug.messageEnabled()) {
                        Debug.message(tableName +
                            "DBHandler:execute:SQLException: insertStr = "
                            + insertStr);
                        Debug.message(tableName +
                            ":DBHandler:execute:SQLException (" +
                            sqleErrCode + "): " + sqle.getMessage());
                    }

                    /*
                     *  unfortunately have to check which db and specific
                     *  error codes...
                     *  see if table's missing
                     *  MySQL: 1146
                     *  Oracle: 942
                     */
                    if ((isMySQL && (sqleErrCode == 1146)) ||
                        (!isMySQL && (sqleErrCode == 942))) {
                        /*
                         *  connection to DB's there, but table's missing
                         *
                         *  gotta make the table; try the executeUpdate()
                         *  again
                         */
                        try {
                            createTable(tableName);
                        } catch (SQLException se) {
                            //  just log the message and continue, for now
                            Debug.error(tableName +
                                ":DBHandler:flush:execUpdate:cTable:SQLE ("
                                + se.getErrorCode() + "): " +
                                se.getMessage());
                        } catch (UnsupportedEncodingException usee) {
                            //  just log the message and continue, for now
                            Debug.error(tableName + 
                                ":DBHandler:flush:execUpdate:cTable:UE: " +
                                usee.getMessage());
                        }

                        try {
                            stmt.executeUpdate(insertStr);
                        } catch (SQLException sqle2) {
                            //  guess NOW it's an error
                            Debug.error(tableName +
                                ":DBHandler:flush:execUpdate:exUpdate:" +
                                "SQLE (" + sqle2.getErrorCode() + "): " +
                                sqle2.getMessage());
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        }
                    } else if ((isMySQL && (sqleErrCode == 0)) ||
                        (!isMySQL && ((sqleErrCode == 17002) ||
                        (sqleErrCode == 17410))))
                    {
                        /*
                         *  connection's probably gone gotta try everything
                         *  up to this point again, starting with
                         *  reconnecting to the db.  any failure along the
                         *  line this time gets an exception.
                         */

                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            //  log and continue
                            if (Debug.messageEnabled()) {
                                Debug.message(tableName +
                                    ":DBHandler:flush:execUpdate:close:" +
                                    "SQLE (" + ex.getErrorCode() + "): " +
                                    ex.getMessage());
                            }
                        }

                        connectionToDBLost = true;
                        try {
                            reconnectToDatabase();
                            Debug.error (tableName +
                               ":DBHandler:flush:execUpdate:" +
                               "reconnect successful.");
                        } catch (DriverLoadException dle) {
                            if (Debug.messageEnabled()) {
                                Debug.message(tableName +
                                    ":DBHandler:flush:execUpdate:" +
                                    "reconnect:DLE: " + dle.getMessage());
                            }
                            /*
                             * if the max mem buffer is exceeded,
                             * dump the records
                             */
                            clearBuffer(buffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_RECONNECT_FAILED);
                        } catch (ConnectionException ce) {
                            if (Debug.messageEnabled()) {
                                Debug.message(tableName +
                                    ":DBHandler:flush:execUpdate:" +
                                    "reconnect:CE: " + ce.getMessage());
                            }
                            /*
                             * if the max mem buffer is exceeded,
                             * dump the records
                             */
                            clearBuffer(buffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_RECONNECT_FAILED);
                        }
                        connectionToDBLost = false;

                        /*
                         *  bunch the createTable, createStatement, and
                         *  executeUpdate together because if any of these
                         *  fail, throw an exception.
                         */
                        try {
                            createTable (tableName);
                            stmt = conn.createStatement();
                            stmt.executeUpdate(insertStr);
                            //Monit start
                            if (MonitoringUtil.isRunning() &&
                                dbLogHandlerForMonitoring != null)
                            {
                                dbLogHandlerForMonitoring.
                                    incHandlerSuccessCount(1);
                            }
                            //Monit end
                        } catch (SQLException sqe) {
                            Debug.error (tableName +
                                ":DBHandler:flush:executeUpd:reconnect:" +
                                "stmt:SQE: (" + sqe.getErrorCode() + "): "
                                + sqe.getMessage());
                            /*
                             *  if the max mem buffer is exceeded,
                             *  dump the records
                             */
                            clearBuffer(buffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        } catch (UnsupportedEncodingException usee) {
                            Debug.error (tableName +
                                ":DBHandler:flush:execUpd:reconnect:" +
                                "stmt:UE: " + usee.getMessage());
                            /*
                             *  if the max mem buffer is exceeded,
                             *  dump the records
                             */
                            clearBuffer(buffer);
                            throw new AMLogException (
                                AMLogException.LOG_DB_EXECUPDATE);
                        }
                    } else {
                        /*
                         *  not sure what to do here yet.  log the error,
                         *  throw an exception, and see what happens next.
                         *
                         *  just for informational purposes, you get the
                         *  following if the columns don't exist:
                         *    if ((isMySQL && (sqleErrCode == 1054)) ||
                         *        (!isMySQL && ((sqleErrCode == 904) ||
                         *              (sqleErrCode == 913))))
                         */
                        Debug.error (tableName +
                            ":DBHandler:flush:executeUpdate failed (" +
                            sqleErrCode + "): " + sqle.getMessage());
                        Debug.error(tableName +
                            ":DBHandler:execute:SQLException: insertStr = "
                            + insertStr);
                        // if the max mem buffer is exceeded, dump the
                        // records
                        clearBuffer(buffer);
                        throw new AMLogException (
                            AMLogException.LOG_DB_EXECUPDATE);
                    }
                }
            }
            try {
                stmt.close();
            } catch (SQLException se) {
                if (Debug.warningEnabled()) {
                    Debug.warning(tableName + ":DBHandler:close:" +
                        "SQLException (" + se.getErrorCode() + "): ", se);
                }
            }
        }
    }

    private class TimeBufferingTask extends GeneralTaskRunnable {

        private long runPeriod;

        public TimeBufferingTask(long runPeriod) {
            this.runPeriod = runPeriod;
        }

        /**
         * The method which implements the GeneralTaskRunnable.
         */
        public void run() {
            if (Debug.messageEnabled()) {
                Debug.message(tableName + 
                    ":DBHandler:TimeBufferingTask.run() called");
            }
            nonBlockingFlush();
        }

        /**
         *  Methods that need to be implemented from GeneralTaskRunnable.
         */
        
        public boolean isEmpty() {
            return true;
        }

        public boolean addElement(Object obj) {
            return false;
        }

        public boolean removeElement(Object obj) {
            return false;
        }

        public long getRunPeriod() {
            return runPeriod;
        }
    }

    private void startTimeBufferingThread() {
        String period = lmanager.getProperty(LogConstants.BUFFER_TIME);
        long interval;
        if((period != null) || (period.length() != 0)) {
            interval = Long.parseLong(period);
        } else {
            interval = LogConstants.BUFFER_TIME_DEFAULT;
        }
        interval *= 1000;
        if(bufferTask == null){
            bufferTask = new TimeBufferingTask(interval);
            try {
                SystemTimer.getTimer().schedule(bufferTask,
                    new Date(((System.currentTimeMillis() + interval) / 1000) *
                    1000));
            } catch (IllegalArgumentException e) {
                Debug.error (tableName + ":DBHandler:BuffTimeArg: " +
                    e.getMessage());
            } catch (IllegalStateException e) {
                if (Debug.messageEnabled()) {
                    Debug.message (tableName + ":DBHandler:BuffTimeState: " +
                        e.getMessage());
                }
            }
            if (Debug.messageEnabled()) {
                Debug.message(tableName + 
                    ":DBHandler: Time Buffering Thread Started");
            }
        }
    }

    private void stopBufferTimer() {
        if(bufferTask != null) {
            bufferTask.cancel();
            bufferTask = null;
            if (Debug.messageEnabled()) {
                Debug.message(tableName + ":DBHandler: Buffer Timer Stopped");
            }
        }
    }
}
