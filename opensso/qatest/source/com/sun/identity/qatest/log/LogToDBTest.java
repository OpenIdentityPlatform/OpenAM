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
 * $Id: LogToDBTest.java,v 1.8 2009/01/27 00:07:20 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.log;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.LogCommon;
import java.text.SimpleDateFormat;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class LogToDBTest extends LogCommon implements LogTestConstants {
    
    private Connection dbConn;
    private IDMCommon idm;
    private SSOToken adminSSOToken;
    private SSOToken userSSOToken;
    private String userId;
    private Map logConfig;
    private String location;
    private static String dataBaseName = "";
    private String dbUser;
    private String dbPassword;
    private String driver;
    private static boolean configured = false;
    private static boolean dbInit = false;
    private ResourceBundle dbConfRb;
    private ResourceBundle testCaseInfo;
    private static String testCaseInfoFileName = "LogToDBTest";
    private String curTestName;
    private String restore;
    private String modifyServConfig;
    private String tableName;
    
    /** Creates a new instance of LogToDBTest */
    public LogToDBTest() 
    throws Exception {
        super("LogToDBTest");
        try {
            idm = new IDMCommon("LogToDBTest");
            adminSSOToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(adminSSOToken)) {
                log(Level.SEVERE, "LogToDBTest", "SSO token is invalid");
                assert false;
            }
            logConfig = getLogConfig(adminSSOToken);
            dbConfRb = ResourceBundle.getBundle("log" + fileseparator +
                    LOGTEST_DB_CONF_FILE);
            testCaseInfo = ResourceBundle.getBundle("log" + fileseparator +
                    testCaseInfoFileName);
            location = dbConfRb.getString(LOGTEST_KEY_LOG_LOCATION);
            int lastIdx = location.lastIndexOf("/");
            String reqDBURL= location.substring(0, lastIdx);                        
            SimpleDateFormat simple = new SimpleDateFormat(
                    "yyyyMMddhhmmssSS");
            Date date = new Date();
            if (dataBaseName.equals("")) {
                dataBaseName = location.substring(lastIdx + 1) + 
                        simple.format(date);
                log(Level.FINEST, "LogToDBTest", "New dataBaseName :" + 
                        dataBaseName);                
            }
            dbUser = dbConfRb.getString(LOGTEST_KEY_DB_USER);
            dbPassword = dbConfRb.getString(LOGTEST_KEY_DB_PASSWORD);
            driver = dbConfRb.getString(LOGTEST_KEY_DRIVER);
            if (dbConn == null) {
                log(Level.FINE, "LogToDBTest", "Getting db connection");
                dbConn = LogCommon.getConnection(dbUser, dbPassword, driver,
                        reqDBURL + "/" + dataBaseName, dataBaseName);
                dbInit = true;
            }
        } catch (SQLException sqexp) {
            log(Level.SEVERE, "LogToDBTest ", "Couldn't get connection " +
                    sqexp.getMessage());
            dbInit = false;
        } catch (Exception ex) {
            log(Level.SEVERE, "LogToDBTest ", "Initialization failed " +
                    ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * This method configures the log service to user "DB" and configures the
     * database.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"testName", "createUser", "restore", "modifyServConfig"})
    public void setup(String testName, String createUser, String restore, 
            String modifyServConfig)
    throws Exception {
        Object[] params = {testName, createUser, restore, modifyServConfig};
        entering("setup", params);
        try {
            if (dbInit) {
                if (!configured) {
                    configure();
                }
                this.restore = restore;
                this.modifyServConfig = modifyServConfig;
                curTestName = testName;
                if (createUser.equals("true")) {
                    userId = testCaseInfo.getString(curTestName + "." + 
                            LOGTEST_KEY_USER_ID);
                    if (userId != null) {
                        idm.createID(userId ,"user", null, adminSSOToken, 
                                realm);
                        userSSOToken = getToken(userId, userId, basedn);
                        if (!validateToken(userSSOToken)) {
                            log(Level.SEVERE, "setup", "SSO token is invalid");
                            assert false;
                        }
                    }
                }
                assert configured;
            } else {
                log(Level.SEVERE, "setup", "DB is not initialized. ");
                assert false;
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "setup", "Setup failed :" + ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("setup");
    }
    
    /**
     * This method modifys the log service configuration.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void modifyLogConfig() 
    throws Exception {
        entering("modifyLogConfig", null);
        try {
            Reporter.log("Test Name : " + curTestName);
            Reporter.log("Test Description : " + testCaseInfo.getString(
                    curTestName + "." + LOGTEST_KEY_DESCRIPTION));
            if (modifyServConfig.equals("true")) {
                Reporter.log("Test action :  modifyLogConfig");
                String attrValPair =  testCaseInfo.getString(curTestName + "." +
                        LOGTEST_KEY_ATTR_VAL_PAIR);
                Map logSvcMap = attributesToMap(attrValPair);
                Reporter.log("Test configuration : " + logSvcMap);
                log(Level.FINEST, "modifyLogConfig",
                        "Updating service config : " + logSvcMap);
                assert (updateLogConfig(adminSSOToken, logSvcMap));
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "modifyLogConfig", 
                    "Error Modifying log service : " + ex.getMessage());
            cleanUp();
            throw ex;
        } 
        exiting("modifyLogConfig");
    }
    
    /**
     * This method tests writing messages to the db.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"modifyLogConfig"})
    public void logMessageToDB()
    throws Exception {
        entering("logMessageToDB", null);
        try {
            String msgStr = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_MESSAGE);
            String moduleName = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_MODULE_NAME);
            tableName = testCaseInfo.getString(curTestName + "." + 
                    LOGTEST_KEY_TABLE_NAME);
            String loggerLevel = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_LOGGER_LEVEL);
            String recordLevel = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_RECORD_LEVEL);
            if (modifyServConfig.equals("true")) 
                assert(writeLog(adminSSOToken, userSSOToken, tableName, "This is dummy msg", 
                        moduleName, getLevel(loggerLevel), 
                        getLevel(recordLevel)));
            Thread.sleep(4000);
            Reporter.log("Test Name : " + curTestName);
            Reporter.log("Test Description : " + testCaseInfo.getString(
                    curTestName + "." + LOGTEST_KEY_DESCRIPTION));
            Reporter.log("Test action :  logMessageToDB");
            Reporter.log("Test logger log level : " + loggerLevel);
            Reporter.log("Test record log level : " + recordLevel);
            Reporter.log("Test log message : " + msgStr);
            Reporter.log("Test table name : " + tableName);
            log(Level.FINE, "logMessageToDB", "Logging message : " + msgStr);
            assert(writeLog(adminSSOToken, userSSOToken, tableName, msgStr, 
                    moduleName, getLevel(loggerLevel), getLevel(recordLevel)));
        } catch (Exception ex) {
             log(Level.SEVERE, "logMessageToDB", "Error writing log : " + 
                     ex.getMessage());
             cleanUp();
             throw ex;
        }
        exiting("logMessageToDB");
    }
    
    /**
     * This method tests reading messages from db.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"logMessageToDB"})
    public void readLog() 
    throws Exception {
        entering("readLog", null);
        try {
            int i = 0;
            String lastRec;
            String expMsg = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_EXPECTED_MESSAGE);
            lastRec = readLastLogEntry(dbConn, tableName);
            while (i < 5) {
                i++;
                lastRec = readLastLogEntry(dbConn, tableName);
                log(Level.FINEST, "readLog", "Last Rec :" + lastRec);
                if (lastRec.indexOf(expMsg) == -1) {
                    log(Level.FINEST, "readLog", "Last Rec doesnt match. " +
                            "Wait for " + notificationSleepTime + 
                            " milisecs & read again");
                    Thread.sleep(notificationSleepTime);
                } else 
                    break;
            }
            if (lastRec.indexOf(expMsg) == -1) {
                log(Level.SEVERE, "readLog", "Record doesn't contain " +
                        "expected message");
                assert false;
            } else {
                assert true;
            }
        } catch (Exception ex){
            log(Level.SEVERE, "readLog", "Error reading last record " +
                    ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("readLog");
    }
    
    /**
     * This method deletes the created user and if restore is set to true
     * retores the log service configuration to the backup version and releases
     * the connection.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanUp() 
    throws Exception {
        try {
            if (idm.doesIdentityExists(userId, "user", adminSSOToken, realm)) {
                destroyToken(userSSOToken);
                log(Level.FINE, "cleanUp", "Delete test user : " + userId);
                idm.deleteID(userId, "user", adminSSOToken, realm);
            }
            if (restore.equals("true")) {
                if (dbInit){
                    log(Level.FINE, "cleanUp", "Resetting...");
                    updateLogConfig(adminSSOToken, logConfig);
                    destroyToken(adminSSOToken);
                    LogCommon.deleteDB(dbConn, dataBaseName);
                    int lastIdx = location.lastIndexOf("/");
                    String reqDBURL= location.substring(0, lastIdx);                        
                    if (dbConn != null) {
                        LogCommon.releaseConn(reqDBURL + "/" + dataBaseName);
                    }
                    dbInit = false;
                    configured = false;
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "cleanUp", "Error cleaning : " + ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * This method configures the log service.
     */
    private void configure() 
    throws Exception {
        try {
            Map cfgMap = new HashMap();
            Set locSet = new HashSet();
            int lastIdx = location.lastIndexOf("/");
            String reqDBURL= location.substring(0, lastIdx);                        
            locSet.add(reqDBURL + "/" + dataBaseName);
            cfgMap.put(LOGTEST_KEY_LOG_LOCATION, locSet);
            Set dbUserSet = new HashSet();
            dbUserSet.add(dbUser);
            cfgMap.put(LOGTEST_KEY_DB_USER, dbUserSet);
            Set dbPasswordSet = new HashSet();
            dbPasswordSet.add(dbPassword);
            cfgMap.put(LOGTEST_KEY_DB_PASSWORD, dbPasswordSet);
            Set driverSet = new HashSet();
            driverSet.add(driver);
            cfgMap.put(LOGTEST_KEY_DRIVER, driverSet);
            Set typeSet = new HashSet();
            typeSet.add("DB");
            cfgMap.put(LOGTEST_KEY_LOGTYPE, typeSet);
            Set statusSet = new HashSet();
            statusSet.add(dbConfRb.getString(LOGTEST_KEY_TIME_BUFFER_STATUS));
            cfgMap.put(LOGTEST_KEY_TIME_BUFFER_STATUS, statusSet);
            Set buffSizeSet = new HashSet();
            buffSizeSet.add(dbConfRb.getString(LOGTEST_KEY_BUFF_SIZE));
            cfgMap.put(LOGTEST_KEY_BUFF_SIZE, buffSizeSet);
            configured = updateLogConfig(adminSSOToken, cfgMap);
        } catch (Exception ex) {
            log(Level.SEVERE, "configure", 
                    "Log service configuration failed : " + ex.getMessage());
            throw ex;
        }
    }
}
