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
 * Portions Copyrighted 2011-2013 ForgeRock AS.
 */
package com.sun.identity.log.handlers;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

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
 * table name which will be created if it doesn't already exists.
 * <p>
 * By default DBFormatter is used for formatting the logRecords.
 */
public class DBHandler extends Handler {

    private LogManager lmanager = LogManagerUtil.getLogManager();
    private String driver;
    private String databaseURL;
    private Connection conn = null;
    private String tableName;
    private String userName;
    private String password;
    private int recCountLimit;
    private int recMaxDBMem = 2;
    private LinkedList<LogRecord> recordBuffer;
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

    private void configure() throws NullLocationException, FormatterInitException {
        setFilter(null);
        try {
            setEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Debug.error(tableName + ":DBHandler: unsupportedEncodingException ", e);
        }

        String bufferSize = lmanager.getProperty(LogConstants.BUFFER_SIZE);
        if (bufferSize != null && bufferSize.length() > 0) {
            try {
                recCountLimit = Integer.parseInt(bufferSize);
            } catch (NumberFormatException nfe) {
                Debug.warning(tableName + ":DBHandler: NumberFormatException ", nfe);
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + ":DBHandler: Setting buffer size to 1");
                }
                recCountLimit = 1;
            }
        } else {
            Debug.warning(tableName + ":DBHandler: Invalid buffer size: " + bufferSize);
            if (Debug.messageEnabled()) {
                Debug.message(tableName + ":DBHandler: Setting buffer size to 1");
            }
            recCountLimit = 1;
        }

        String recMaxDBMemStr = lmanager.getProperty(LogConstants.DB_MEM_MAX_RECS);
        if (recMaxDBMemStr != null && recMaxDBMemStr.length() > 0) {
            try {
                recMaxDBMem = Integer.parseInt(recMaxDBMemStr);
            } catch (NumberFormatException nfe) {
                Debug.warning(tableName + ":DBHandler:recMaxDBMem ("
                        + recMaxDBMemStr + "): NumberFormatException ", nfe);
                if (Debug.messageEnabled()) {
                    Debug.message(tableName +
                        ":DBHandler: Setting Max DB Mem Buffer Size to 2x (" +
                        2*recCountLimit + ") the Buffer Size (" +
                        recCountLimit + ")");
                }
                recMaxDBMem = 2*recCountLimit;
            }
        } else {
            Debug.warning(tableName + ":DBHandler: Invalid buffer size: " + bufferSize);
            if (Debug.messageEnabled()) {
                Debug.message(tableName + ":DBHandler: Defaulting Max DB Mem Buffer Size to 2x Buffer Size");
            }
            recMaxDBMem = 2*recCountLimit;
        }

        if (recMaxDBMem < recCountLimit) {
            Debug.warning (tableName +
                ":DBHandler:Maximum DB memory buffer size < Buffer Size, " +
                "setting to buffer size (" + recCountLimit + ")");
            recMaxDBMem = recCountLimit;
        }

        String status = lmanager.getProperty(LogConstants.TIME_BUFFERING_STATUS);

        if ( status != null && status.equalsIgnoreCase("ON")) {
            timeBufferingEnabled = true;
        }

        oraDataType = lmanager.getProperty(LogConstants.ORA_DBDATA_FIELDTYPE);
        mysqlDataType = lmanager.getProperty(LogConstants.MYSQL_DBDATA_FIELDTYPE);

        databaseURL = lmanager.getProperty(LogConstants.LOG_LOCATION);
        if ((databaseURL == null) || (databaseURL.length() == 0)) {
            throw new NullLocationException("Database URL location is null");
        }

        String strFormatter = com.sun.identity.log.LogManager.FORMATTER;
        if ((strFormatter == null) || (strFormatter.length() == 0)) {
            throw new FormatterInitException("Unable To Initialize DBFormatter");
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
            Debug.warning(tableName + ":DBHandler:configure:assuming driver: '" + driver + "' is Oracle-compatible.");
        }

        try {
            Class clz = Class.forName(strFormatter);
            Formatter formatter = (Formatter) clz.newInstance();
            setFormatter(formatter);
        } catch (Exception e) {    // should be Invalid Formatter Exception
            Debug.error(tableName + ":DBHandler: Could not load Formatter", e);
            throw new FormatterInitException("Unable To Initialize DBFormatter " + e.getMessage());
        }
    }

    private void connectToDatabase(String userName, String password) throws ConnectionException, DriverLoadException {
        //Monit start
        if (MonitoringUtil.isRunning()) {
            if (dbLogHandlerForMonitoring == null) {
                logServiceImplForMonitoring = Agent.getLoggingSvcMBean();
                dbLogHandlerForMonitoring =
                        logServiceImplForMonitoring.getHandler(SsoServerLoggingSvcImpl.DB_HANDLER_NAME);
            }
            if (dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionRequests(1);
            }
        }
        //Monit end
        try {
            Class.forName(driver);
            this.conn = DriverManager.getConnection(databaseURL, userName, password);
        } catch (ClassNotFoundException e) {
            Debug.error(tableName + ":DBHandler: ClassNotFoundException " + e.getMessage());
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
            Debug.error(tableName + ":DBHandler: ConnectionException ("
                    + sqle.getErrorCode() + "): " + sqle.getMessage());
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
    private void reconnectToDatabase() throws ConnectionException, DriverLoadException {
        //Monit start
        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
            dbLogHandlerForMonitoring.incHandlerConnectionRequests(1);
        }
        //Monit end

        try {
            Class.forName(driver);
            this.conn = DriverManager.getConnection(databaseURL, userName, password);
        } catch (ClassNotFoundException e) {
            //Monit start
            if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                dbLogHandlerForMonitoring.incHandlerConnectionsFailed(1);
            }
            //Monit end
            throw new DriverLoadException(e.getMessage());
        } catch (SQLException sqle) {
            Debug.error (tableName + ":DBHandler:reconnect (" + sqle.getErrorCode() + "): " + sqle.getMessage());
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
            Debug.error(tableName + ":DBHandler: Unable to Initialize formatter", fie);
        }

        String stat = lmanager.getProperty(LogConstants.LOG_STATUS_ATTR);
        if (Debug.messageEnabled()) {
            Debug.message("DBHandler:tableName = " + tableName + ", LOG_STATUS = " + stat);
        }

        if (stat != null & !(stat.startsWith("INACTIVE"))) {
            connectionToDBLost = true;
            try {
                connectToDatabase(userName, password);
                connectionToDBLost = false;
                createTable(tableName);
            } catch (SQLException sqe) {
                Debug.error(tableName + ":DBHandler: sql operation unsuccessful ("
                        + sqe.getErrorCode() + "): " + sqe.getMessage());
            } catch (ConnectionException ce) {
                Debug.error(tableName + ":DBHandler: Could not connect to database:" + ce.getMessage());
            } catch (DriverLoadException dle) {
                Debug.error(tableName + ":DBHandler: Could not load driver", dle);
            } catch (UnsupportedEncodingException use) {
                Debug.error(tableName + ":DBHandler: Unsupported Encoding: " + use.getMessage());
            }
            connectionToDBLost = false;
        }

        recordBuffer = new LinkedList<LogRecord>();
        if (timeBufferingEnabled) {
            startTimeBufferingThread();
        }

        if (MonitoringUtil.isRunning()) {
            logServiceImplForMonitoring = Agent.getLoggingSvcMBean();
            dbLogHandlerForMonitoring = logServiceImplForMonitoring.getHandler(SsoServerLoggingSvcImpl.DB_HANDLER_NAME);
        }
    }

    /**
     * Publishes the provided LogRecord.
     * @param logRecord the log record to be published.
     *
     */
    @Override
    public void publish(LogRecord logRecord) {
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
                        + recordBuffer.size() + " records, Limit "
                        + recCountLimit + " writing all");
                }
                nonBlockingFlush();
            }
        }
    }

    /**
     * Construct a PreparedStatement that can be used to insert the passed values
     * into the table that this DBHandler represents.
     * The first two values are always a timestamp string and the main data block, rest is optional based on the
     * database logging configuration.
     * @param values The values to be added as part of the INSERT statement
     * @return A PreparedStatement representing the INSERT statement for the provided values.
     * @throws SQLException if there is an issue creating or preparing the statement values.
     */
    private PreparedStatement getInsertPreparedStatement(List<String> values) throws SQLException {

        StringBuilder insertStringBuffer = new StringBuilder(2000);
        insertStringBuffer.append("INSERT INTO ").append(tableName);
        // This returns a comma separated String representing the column names
        String columns = getFormatter().getHead(this);
        if (columns != null) {
            insertStringBuffer.append(" (").append(columns).append(")");
        }
        insertStringBuffer.append(" VALUES (");

        // Inset a placeholder for every value we have.
        for (int i = 0; i < (values.size() - 1); i++) {
           insertStringBuffer.append("?,");
        }
        // Finish with remaining value placeholder less the ,
        insertStringBuffer.append("?)");

        String preparedStatementString = insertStringBuffer.toString();
        if (Debug.messageEnabled()) {
            Debug.message(tableName +
                ":DBHandler:getInsertPreparedStatement preparedStatementString is " + preparedStatementString);
        }

        PreparedStatement preparedStatement = conn.prepareStatement(preparedStatementString);

        // Column numbers in PreparedStatements start at 1 not 0.
        // Set the time and data values directly, these are always the first two columns.
        preparedStatement.setTimestamp(1, new Timestamp(Long.parseLong(values.get(0))));
        if (isMySQL) {
            preparedStatement.setString(2, values.get(1));
        } else {
            preparedStatement.setClob(2, new StringReader(values.get(1)));
        }
        // The remaining values are all strings
        for (int i = 2; i < values.size(); i++) {
            preparedStatement.setString(i + 1, values.get(i));
        }

        return preparedStatement;
    }

    /**
     * For the given LogRecord, return a String List of all the of values to be inserted into the database.
     * The first two values are always the timestamp representing when the LogRecord was captured and data value which
     * represents the main block of information to be logged.
     * @param logRecord The LogRecord to extract values from
     * @return A List of String values
     */
    private List<String> getValues(LogRecord logRecord) {

        List<String> result = new ArrayList<String>();

        Map logInfoTable = null;
        if ((LogManagerUtil.isAMLoggingMode()) && (logRecord instanceof com.sun.identity.log.ILogRecord)) {
            logInfoTable = ((com.sun.identity.log.ILogRecord) logRecord).getLogInfoMap();
        }

        // This value will be used to create a Timestamp when persisting this record to the DB
        result.add(String.valueOf(logRecord.getMillis()));

        // Next field is the data block, this call will do any substitutions based on Java util.text formatting rules
        String data = getFormatter().formatMessage(logRecord);

        if ((data == null) || (data.length() <= 0)) {
            data = LogConstants.NOTAVAIL;
        }
        result.add(data);
        if (Debug.messageEnabled()) {
            Debug.message("DBHandler.getValues: time and data fields = " + result);
        }
        String[] allFields = lmanager.getAllFields();
        Set selectedFields = lmanager.getSelectedFieldSet();
        int len = 0;
        if (allFields != null) {
            len = allFields.length;
        }
        for (int i = 2; i < len; i ++) { // first 2 fields are compulsory
            if ((logInfoTable != null) && (selectedFields != null) && (selectedFields.contains(allFields[i]))) {
                String tempstr = (String)logInfoTable.get(allFields[i]);
                if (tempstr == null) {
                    tempstr = LogConstants.NOTAVAIL;
                }
                result.add(tempstr);
            } else {
                result.add(LogConstants.NOTAVAIL);
            }
        }

        if (Debug.messageEnabled()) {
            Debug.message("DBHandler.getValues: values: " + result);
        }

        return result;
    }

    /**
     * Flush any buffered messages.
     */
    protected void nonBlockingFlush() {
        LinkedList<LogRecord> tempBuffer = null;
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + ":DBHandler:nonBlockingFlush: no records in buffer to write");
                }
                return;
            }

            String tableName = getTableName();
            if (tableName == null) {
                Debug.error(tableName + ":DBHandler:nonBlockingFlush:NullLocationException: table name is null");
                int recordsToBeDropped = recordBuffer.size();
                recordBuffer.clear();
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(recordsToBeDropped);
                }
                //Monit end
                return;
            } else {
                tempBuffer = recordBuffer;
                recordBuffer = new LinkedList<LogRecord>();
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

    @Override
    public void flush() {
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                return;
            }

            String tableName = getTableName();
            if (tableName == null) {
                int recordsToBeDropped = recordBuffer.size();
                recordBuffer.clear();
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(recordsToBeDropped);
                }
                //Monit end
                return;
            }
            logRecords(recordBuffer);
            recordBuffer.clear();
        }
    }
    
    /**
     * Flush any buffered messages and close the current output stream.
     */
    @Override
    public void close() {
        try {
            flush();
        } catch (AMLogException ale) {
            Debug.error(tableName + ":DBHandler:close/flush error: " + ale.getMessage());
        }
        if(conn != null) {
            try {
                conn.close();
            }
            catch (SQLException ce) {
                Debug.error(tableName + ":DBHandler: Unable To Close Connection", ce);
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

    private void clearBuffer(LinkedList<LogRecord> buffer) {
        synchronized (this) {
            int reccnt = recordBuffer.size();
            if (buffer != recordBuffer) {
                reccnt += buffer.size();
            }
            if (reccnt > recMaxDBMem) {            
                int removeCount = reccnt - recMaxDBMem;
                    Debug.error(tableName + ":DBHandler:dropping " + removeCount + " records.");
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
                    dbLogHandlerForMonitoring.incHandlerDroppedCount(removeCount);
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
     * independent of the fields(we need not hard core the fields) and space.
     */
    private void createTable(String tableName) throws SQLException, UnsupportedEncodingException
    {
        StringBuffer sbuffer = new StringBuffer();
        // form a query string to check if the table exists.
        String oracleTableName = new String(tableName.getBytes("UTF-8")).toUpperCase();

        try {
            if(!isMySQL){
                /*
                 *  unlike MySQL, Oracle seems to make the table name
                 *  all uppercase.  wonder if you can query using the
                 *  original case?
                 */
                sbuffer.append("SELECT COUNT(table_name) FROM all_tables WHERE table_name = ");
                sbuffer.append("'")
                       .append(oracleTableName)
                       .append("'");
            } else {
                /*
                 *  MySQL makes the table (at least running on solaris)
                 *  the same case as specified in the create, so the
                 *  query needs to specify the same case.
                 */
                sbuffer.append("SHOW TABLES LIKE ");
                sbuffer.append("'")
                       .append((new String(tableName.getBytes("UTF-8"))))
                       .append("'");
            }

            boolean foundTable = false;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sbuffer.toString());
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
            } finally {
                closeResultSet(rs);
                closeStatement(stmt);
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
                    getColNames = "SHOW COLUMNS from " + tableName;
                } else {
                    getColNames = "SELECT column_name FROM USER_TAB_COLUMNS WHERE Table_Name = '" + oracleTableName + "'";
                }

                Set<String> colSet = new HashSet<String>();
                try {
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
                    }
                } finally {
                    closeResultSet(rs);
                    closeStatement(stmt);
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
                        colList.append(allFields[i]).append(" VARCHAR(255)");
                        addedOne = true;
                    }
                }
                if (colList.length() > 0) {
                    String altStr = "ALTER TABLE " + tableName + " ADD (" + colList.toString() + ")";
                
                    try {
                        stmt = conn.createStatement();
                        stmt.execute(altStr);
                    } catch (SQLException sqle) {
                        Debug.error("DBHandler:createTable: '" + altStr + "'; error ("
                                + sqle.getErrorCode() + "); msg = " + sqle.getMessage());
                        /*
                         *  guess we'll have to just return here, and
                         *  let the flush handle the error...
                         *
                         *  there's a return right after this, so just
                         *  fall through.
                         */
                    } finally {
                        closeStatement(stmt);
                    }
                }
                return;
            }
        } catch (SQLException e) {
            Debug.error(tableName +
                    ":DBHandler:createTable:Query:SQLException (" + e.getErrorCode() + "): " + e.getMessage());
            //  rethrow the exception
            throw e;
        } catch (UnsupportedEncodingException use) {
            Debug.error(tableName + ":DBHandler:createTable:Query:UE: "+ use.getMessage());
            //  rethrow the exception
            throw use;
        }

        //  didn't find the table in the DB, so create it.

        sbuffer = new StringBuffer();
        try {
            sbuffer.append("CREATE TABLE ").append(new String(tableName.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException uee) {
            Debug.error(tableName + ":DBHandler: unsupported encoding exception uee", uee);
        }

        String varCharX = "VARCHAR2"; // default for Oracle

        if (isMySQL) {
            sbuffer.append(" (TIME DATETIME, ");
        } else {
            sbuffer.append(" (TIME DATE, ");
        }

        /*
         * next column is the DATA column, which will be a non-VARCHAR*
         * type, since it *can* contain a large quantity of 'character'
         * (note not binary) data.  Oracle uses "CLOB", while MySQL
         * uses "LONGTEXT".
         */

        if(isMySQL) {
            varCharX = "VARCHAR";
            sbuffer.append(" DATA ").append(mysqlDataType).append(", ");
        } else {
            sbuffer.append(" DATA ").append(oraDataType).append(", ");
        }

        String [] allFields = lmanager.getAllFields();
        int i = 0;
        for (i = 2; i < allFields.length - 1; i++) {
            sbuffer.append(allFields[i]).append(" ").append(varCharX).append(" (255), ");
        }
        sbuffer.append(allFields[i]).append(" ").append(varCharX).append(" (255)) ");
        String createString = sbuffer.toString();
        Statement stmt = null;
        try {
             stmt = conn.createStatement();
            if (Debug.messageEnabled()) {
                Debug.message(tableName + ":DBHandler: the query string for creating is " + createString);
            }
            stmt.executeUpdate(createString);
        } catch (SQLException sqe) {
            Debug.error(tableName +
                ":DBHandler:createTable:Execute:SQLEx (" + sqe.getErrorCode() + "): " + sqe.getMessage());
            //
            //  rethrow the exception
            //
            throw sqe;
        } finally {
            closeStatement(stmt);
        }
    }

    private void logRecords(LinkedList<LogRecord> records) {

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
                Debug.error (tableName + ":DBHandler:logRecords:reconnectToDatabase successful.");
            } catch (DriverLoadException dle) {
                Debug.error(tableName + ":DBHandler:logRecords:reconnectToDatabase:DLE: " + dle.getMessage());
                //
                //  if the max mem buffer is exceeded, dump the records
                //
                clearBuffer(records);
                throw new AMLogException(AMLogException.LOG_DB_DRIVER + "'" + driver + "'");
            } catch (ConnectionException ce) {
                Debug.error(tableName + ":DBHandler:logRecords:reconnectToDatabase:CE: " + ce.getMessage());
                //
                //  if the max mem buffer is exceeded, dump the records
                //
                clearBuffer(records);
                throw new AMLogException(AMLogException.LOG_DB_CONNECT_FAILED);
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
                    Debug.message(tableName + ":DBHandler:logRecords:reconnect:cTable:SQLE ("
                            + se.getErrorCode() + "): " + se.getMessage());
                }
            } catch (UnsupportedEncodingException usee) {
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + ":DBHandler:logRecords:reconnect:cTable:UE: " + usee.getMessage());
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
        Statement testConnectionStatement = null;
        try {
            testConnectionStatement = conn.createStatement();
        } catch (SQLException se) {
            //
            //  observed that when Oracle's down, it's detected here.
            //  error code 1034.
            //
            Debug.error(tableName + ":DBHandler:logRecords:cStatement:SQLE (" + se.getErrorCode() + "): " + se.getMessage());

            //
            // try reconnecting to DB once.  if can't, dump the record
            // and wait for the next attempt.
            //
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                //
                //  ignore exception and continue
                //
                if (Debug.messageEnabled()) {
                    Debug.error (tableName + ":DBHandler:logRecords:cStatement:close:SQLE (" + ex.getErrorCode() + ")" + ex.getMessage());
                }
            }

            connectionToDBLost = true;
            try {
                reconnectToDatabase();
                Debug.error (tableName + ":DBHandler:logRecords:cStatement:reconnect successful.");
            } catch (DriverLoadException dle) {
                Debug.error(tableName + ":DBHandler:logRecords:cStatement:reconnect:DLE: " + dle.getMessage());
                //
                //  if the max mem buffer is exceeded, dump the records
                //
                clearBuffer(records);
                throw new AMLogException(AMLogException.LOG_DB_DRIVER +  "'" + driver + "'");
            } catch (ConnectionException ce) {
                Debug.error(tableName + ":DBHandler:logRecords:cStatement:reconnect:CE: " + ce.getMessage());
                //
                //  if the max mem buffer is exceeded, dump the records
                //
                clearBuffer(records);
                throw new AMLogException(AMLogException.LOG_DB_CONNECT_FAILED);
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
                        ":DBHandler:logRecords:cStatement:reconnect:cTable:SQLE (" + sqle.getErrorCode() + "): " +
                        sqle.getMessage());
                }
            } catch (UnsupportedEncodingException usee) {
                if (Debug.messageEnabled()) {
                    Debug.message(tableName + ":DBHandler:logRecords:cStatement:reconnect: cTable:UE: " + usee.getMessage());
                }
            }

            try {
                testConnectionStatement = conn.createStatement();
            } catch (SQLException sqle) {
                //
                //  second time this failed (note that this whole block
                //  started with the createStatement()).
                //  log the error message, and continue on (for now)
                //
                Debug.error(tableName +
                    ":DBHandler:logRecords:cStatement:reconnect:cSt:SQLE ("
                    + sqle.getErrorCode() + "): " + sqle.getMessage());
                throw new AMLogException(AMLogException.LOG_DB_CSTATEMENT);
            }
        } finally {
            closeStatement(testConnectionStatement);
        }

        PreparedStatement insertStatement = null;
        for (LogRecord record : records) {
            List<String> values = getValues(record);
            try {
                insertStatement = getInsertPreparedStatement(values);
                insertStatement.executeUpdate();
                //Monit start
                if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                    dbLogHandlerForMonitoring.incHandlerSuccessCount(1);
                }
                //Monit end
            } catch (SQLException sqle) {
                // Attempt to close this just in case it is holding resources.
                closeStatement(insertStatement);
                insertStatement = null;

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
                        ":DBHandler:logRecords:SQLException (" + sqleErrCode + "): " + sqle.getMessage());
                }

                /*
                 *  unfortunately have to check which db and specific
                 *  error codes...
                 *  see if table's missing
                 *  MySQL: 1146
                 *  Oracle: 942
                 */
                if ((isMySQL && (sqleErrCode == 1146)) || (!isMySQL && (sqleErrCode == 942))) {
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
                        Debug.error(tableName + ":DBHandler:logRecords:execUpdate:cTable:SQLE ("
                            + se.getErrorCode() + "): " + se.getMessage());
                    } catch (UnsupportedEncodingException usee) {
                        //  just log the message and continue, for now
                        Debug.error(tableName + ":DBHandler:logRecords:execUpdate:cTable:UE: " + usee.getMessage());
                    }

                    try {
                        insertStatement = getInsertPreparedStatement(values);
                        insertStatement.executeUpdate();
                    } catch (SQLException sqle2) {
                        //  guess NOW it's an error
                        Debug.error(tableName +
                            ":DBHandler:flush:logRecords:exUpdate:SQLE (" + sqle2.getErrorCode() + "): " +
                            sqle2.getMessage());
                        throw new AMLogException (AMLogException.LOG_DB_EXECUPDATE);
                    } finally {
                        closeStatement(insertStatement);
                        insertStatement = null;
                    }
                } else if ((isMySQL && (sqleErrCode == 0))
                        || (!isMySQL && ((sqleErrCode == 17002) || (sqleErrCode == 17410)))) {
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
                                ":DBHandler:logRecords:execUpdate:close:SQLE (" + ex.getErrorCode() + "): " +
                                ex.getMessage());
                        }
                    }

                    connectionToDBLost = true;
                    try {
                        reconnectToDatabase();
                        Debug.error (tableName + ":DBHandler:logRecords:execUpdate:reconnect successful.");
                    } catch (DriverLoadException dle) {
                        if (Debug.messageEnabled()) {
                            Debug.message(tableName + ":DBHandler:logRecords:execUpdate:reconnect:DLE: " + dle.getMessage());
                        }
                        /*
                         * if the max mem buffer is exceeded,
                         * dump the records
                         */
                        clearBuffer(records);
                        throw new AMLogException (AMLogException.LOG_DB_RECONNECT_FAILED);
                    } catch (ConnectionException ce) {
                        if (Debug.messageEnabled()) {
                            Debug.message(tableName + ":DBHandler:logRecords:execUpdate:reconnect:CE: " + ce.getMessage());
                        }
                        /*
                         * if the max mem buffer is exceeded,
                         * dump the records
                         */
                        clearBuffer(records);
                        throw new AMLogException (AMLogException.LOG_DB_RECONNECT_FAILED);
                    } finally {
                        closeStatement(insertStatement);
                        insertStatement = null;
                    }
                    connectionToDBLost = false;

                    /*
                     *  bunch the createTable, createStatement, and
                     *  executeUpdate together because if any of these
                     *  fail, throw an exception.
                     */
                    try {
                        createTable (tableName);
                        insertStatement = getInsertPreparedStatement(values);
                        insertStatement.executeUpdate();
                        //Monit start
                        if (MonitoringUtil.isRunning() && dbLogHandlerForMonitoring != null) {
                            dbLogHandlerForMonitoring.incHandlerSuccessCount(1);
                        }
                        //Monit end
                    } catch (SQLException sqe) {
                        Debug.error (tableName +
                            ":DBHandler:logRecords:executeUpd:reconnect:stmt:SQE: (" + sqe.getErrorCode() + "): "
                            + sqe.getMessage());
                        /*
                         *  if the max mem buffer is exceeded,
                         *  dump the records
                         */
                        clearBuffer(records);
                        throw new AMLogException (AMLogException.LOG_DB_EXECUPDATE);
                    } catch (UnsupportedEncodingException usee) {
                        Debug.error (tableName + ":DBHandler:logRecords:execUpd:reconnect:stmt:UE: " + usee.getMessage());
                        /*
                         *  if the max mem buffer is exceeded,
                         *  dump the records
                         */
                        clearBuffer(records);
                        throw new AMLogException (AMLogException.LOG_DB_EXECUPDATE);
                    } finally {
                        closeStatement(insertStatement);
                        insertStatement = null;
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
                    // if the max mem buffer is exceeded, dump the
                    // records
                    clearBuffer(records);
                    throw new AMLogException (AMLogException.LOG_DB_EXECUPDATE);
                }
            } finally {
                closeStatement(insertStatement);
                insertStatement = null;
            }
        }
    }

    private void closeResultSet(ResultSet resultSet) {

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException se) {
                if (Debug.warningEnabled()) {
                    Debug.warning(tableName + ":DBHandler:closeResultSet:error closing resultSet: SQLException ("
                            + se.getErrorCode() + "): ", se);
                }
            }
        }
    }

    private void closeStatement(Statement statement) {

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException se) {
                if (Debug.warningEnabled()) {
                    Debug.warning(tableName + ":DBHandler:closeStatement:error closing statement: SQLException ("
                            + se.getErrorCode() + "): ", se);
                }
            }
        }
    }

    private class LogTask implements Runnable {

        private LinkedList<LogRecord> buffer;

        public LogTask(LinkedList<LogRecord> buffer) {
            this.buffer = buffer;
        }

        public void run() {

            logRecords(buffer);
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
                Debug.message(tableName + ":DBHandler:TimeBufferingTask.run() called");
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
        if ((period != null) && (period.length() != 0)) {
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
                Debug.error (tableName + ":DBHandler:BuffTimeArg: " + e.getMessage());
            } catch (IllegalStateException e) {
                if (Debug.messageEnabled()) {
                    Debug.message (tableName + ":DBHandler:BuffTimeState: " + e.getMessage());
                }
            }
            if (Debug.messageEnabled()) {
                Debug.message(tableName + ":DBHandler: Time Buffering Thread Started");
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
