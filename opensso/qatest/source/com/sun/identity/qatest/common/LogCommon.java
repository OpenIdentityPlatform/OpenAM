/* The contents of this file are subject to the terms
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
 * $Id: LogCommon.java,v 1.7 2008/10/28 23:49:12 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * This class contains common function for logging the messages to file and db.
 */
public class LogCommon extends TestCommon {
    
    private static boolean isConInit = false;
    private static HashMap connMap = new HashMap();
    private static String logServiceName = "iPlanetAMLoggingService";

    /** Creates a new instance of LogCommon */
    public LogCommon(String compName) {
        super(compName);
    }
    
    /**
     * This method gets log service configuration data.
     * @param adminToken Admin SSO token.
     * @return map containing the service configuration information.
     */
    public Map getLogConfig(SSOToken adminToken)
    throws Exception {
        try {
            ServiceSchemaManager schemaManager =
                    new ServiceSchemaManager(logServiceName, adminToken);
            ServiceSchema smsLogSchema = schemaManager.getGlobalSchema();
            Map configMap = smsLogSchema.getAttributeDefaults();
            log(Level.FINEST, "getLogConfig", 
                    "Log config values : " + configMap);
            return configMap;
        } catch (Exception ex) {
            log(Level.SEVERE, "getLogConfig", "Error getting log config : " +
                    ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * This method modifys log service configuration data.
     * @param adminToken Admin SSO token.
     * @param attrValMap service configuation information.
     * @return true if modification is success else false.
     */
    public boolean updateLogConfig(SSOToken adminToken, Map attrValMap) 
    throws Exception {
        try {
            SMSCommon sm = new SMSCommon(adminToken);
            log(Level.FINE, "updateLogConfig", "Modifying service conf " + 
                    attrValMap);
            return sm.updateSvcSchemaAttribute(logServiceName, attrValMap, 
                    "Global");
        } catch (Exception ex) {
            log(Level.SEVERE, "updateLogConfig", 
                    "Error updating log config : " + ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * This method writes the log to the server using default logger level
     * as Level.INFO.
     * @param loggerToken Admin token who have logging permissions.
     * @param userToken user token who wants to generate log message.
     * @param logFileName File name where logs has to be written.
     * @param message Log message to be written.
     * @param moduleName Module to name which is generating logs.
     * @param recordLevel Level to be set for log record.
     * @return true if modification is success else false.
     */
    public boolean writeLog(SSOToken loggerToken, SSOToken userToken, 
            String logFileName, String message, String moduleName, 
            Level recordLevel) 
    throws Exception {
        return writeLog(loggerToken, userToken, logFileName, message, 
                moduleName, Level.INFO, recordLevel);
    }
    
    /**
     * This method writes the log to the server.
     * @param loggerToken Admin token who have logging permissions.
     * @param userToken user token who wants to generate log message.
     * @param logFileName File name where logs has to be written.
     * @param message Log message to be written.
     * @param moduleName Module to name which is generating logs.
     * @param loggerLevel Level to set for the logger.
     * @param recordLevel Level to set for the log record.
     * @return true if modification is success else false.
     */
    public boolean writeLog(SSOToken loggerToken, SSOToken userToken, 
            String logFileName, String message, String moduleName, 
            Level loggerLevel, Level recordLevel)
    throws Exception {
        try {
            Thread.sleep(4000);
            LogRecord logRecord = 
                new LogRecord(recordLevel, message, userToken);
            logRecord.addLogInfo("ModuleName", moduleName);
            Logger logger = (Logger)Logger.getLogger(logFileName);
            log(Level.FINE, "writeLog", "Writing message : " + message);
            logger.setLevel(loggerLevel);
            logger.log(logRecord, loggerToken);
        } catch (Exception ex) {
            log (Level.SEVERE, "writeLog", "Writing log failed : " +
                    ex.getMessage());
            throw ex;
        }
        return true;
    }
    
    /**
     * This method creates a test database with the default name from 
     * input parameter location
     * and returns connection for this database.
     * @param dbUserName Database user Name.
     * @param password Database user Password.
     * @param driver Database Driver to be used.
     * @param location Location of the database.
     * @return connection to the database.
     */
    public static Connection getConnection(String dbUserName, String password, 
            String driver, String location) 
    throws Exception, SQLException {
        try {
            int lastIdx = location.lastIndexOf("/");
            String dataBaseName = location.substring(lastIdx + 1);            
            return getConnection(dbUserName, password, driver, location, 
                    dataBaseName);
        } catch (Exception ex) {
            log(Level.SEVERE, "getConnection", 
                    "Error creating db connection : " + ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Deletes the database 
     * @param con Connection to the database.
     * @param dataBaseName Name of the database to be deleted.
     * @return true if the database deleted.
     */
    public static boolean deleteDB(Connection con, String dataBaseName) 
    throws Exception {
        try {
            Statement createSt = con.createStatement();
            log (Level.FINE, "deleteDB", "Deleting database : " + dataBaseName);
            return createSt.execute("DROP DATABASE " + dataBaseName);
        } catch (Exception ex) {
            log(Level.SEVERE, "deleteDB", "Error deleting db : " +
                    ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Returns recently inserted row from db.
     * @param con Connection to the database.
     * @param logName Table name
     * @return last record as string.
     */ 
    public String readLastLogEntry(Connection con, String logName) 
    throws Exception {
        StringBuffer lastRec = null;
        try {
            Thread.sleep(4000);
            Statement selectSt = con.createStatement();
            ResultSet results = selectSt.executeQuery("SELECT * FROM " + 
                    logName + " ORDER BY TIME DESC LIMIT 1");
            if (results.last()) {
                lastRec = new StringBuffer(results.getString("TIME"));
                lastRec.append(" ");
                lastRec.append(results.getString("DATA"));
                lastRec.append(" ");
                lastRec.append(results.getString("MODULENAME"));
                lastRec.append(" ");
                lastRec.append(results.getString("DOMAIN"));
                lastRec.append(" ");
                lastRec.append(results.getString("LOGLEVEL"));
                lastRec.append(" ");
                lastRec.append(results.getString("LOGINID"));
                lastRec.append(" ");
                lastRec.append(results.getString("IPADDR"));
                lastRec.append(" ");
                lastRec.append(results.getString("LOGGEDBY"));
                lastRec.append(" ");
                lastRec.append(results.getString("HOSTNAME"));
                lastRec.append(" ");
                lastRec.append(results.getString("MESSAGEID"));
                lastRec.append(" ");
                lastRec.append(results.getString("CONTEXTID"));
                return lastRec.toString();
            }
        } catch (Exception ex){
            log(Level.SEVERE, "readLastLogEntry", 
                    "Error getting last record : " + ex.getMessage());
            throw ex;
        }
        return null;
    }
    
    /**
     * Return java.util.logging.Level 
     */
    public Level getLevel(String level) 
    throws Exception {
        if (level.equalsIgnoreCase("info")) {
            return Level.INFO;
        } else if (level.equalsIgnoreCase("config")) {
            return Level.CONFIG;
        } else if (level.equalsIgnoreCase("fine")) {
            return Level.FINE;
        } else if (level.equalsIgnoreCase("finest")) {
            return Level.FINEST;
        } else if (level.equalsIgnoreCase("finer")) {
            return Level.FINER;
        } else if (level.equalsIgnoreCase("off")) {
            return Level.OFF;
        } else if (level.equalsIgnoreCase("severe")) {
            return Level.SEVERE;
        } else if (level.equalsIgnoreCase("warning")) {
            return Level.WARNING;
        } else {
           throw new RuntimeException("Level type " + level + 
                   " not supported.");
         }
    }
    
    /**
     * Close the connection.
     */
    public static boolean releaseConn(String location)
    throws Exception {
        try {
            log(Level.FINE, "releaseConn", 
                    "Releasing connection : " + location);
            Connection con = (Connection)connMap.get(location);
            con.close();
            connMap.remove(location);
        } catch (Exception ex) {
            log(Level.SEVERE, "releaseConn", "Error releasing connection. " +
                    ex.getMessage());
            throw ex;
        }
        return true;
    }
    
    /**
     * This method creates a test database and returns connection for this
     * database.
     * @param dbUserName Database user Name.
     * @param password Database user Password.
     * @param driver Database Driver to be used.
     * @param location Location of the database.
     * @param dataBaseName Name of the database.
     * @return connection to the database.
     */
    public static Connection getConnection(String dbUserName, String password, 
            String driver, String location, String dataBaseName) 
    throws Exception, SQLException {
        try {
            int lastIdx = location.lastIndexOf("/");
            String reqDBURL= location.substring(0, lastIdx);
            if (!connMap.containsKey(location)) {
                log (Level.FINE, "getConnection", "Loading driver : " + driver);
                Class.forName(driver);
                log (Level.FINE, "getConnection", "Getting connection for : " + 
                        reqDBURL);
                Connection con = DriverManager.getConnection(reqDBURL, 
                        dbUserName, password);
                Statement createSt = con.createStatement();
                log (Level.FINE, "getConnection", "Creating database : " + 
                        dataBaseName);
                createSt.execute("CREATE DATABASE " + dataBaseName);
                con.close();
                con = DriverManager.getConnection(reqDBURL + "/" + dataBaseName,
                        dbUserName, password);
                connMap.put(location, con);
                log (Level.FINE, "getConnection", "Added connection to " +
                        "connMap: " + location);
                return con;
            } else {
                return (Connection)connMap.get(location);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "getConnection", 
                    "Error creating db connection : " + ex.getMessage());
            throw ex;
        }
    }
}
