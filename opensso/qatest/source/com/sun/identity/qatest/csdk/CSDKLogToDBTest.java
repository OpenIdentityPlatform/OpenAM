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
 * $Id: 
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.csdk;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.LogCommon;
import com.sun.identity.qatest.log.LogTestConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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

/**
 * This class will test writing logs to datatbase and modifying the log service
 * attribute values
 **/
public class CSDKLogToDBTest extends LogCommon implements LogTestConstants {

    private Connection dbConn;
    private CSDKCommon cc;
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
    private static String testCaseInfoFileName = "CSDKLogToDBTest";
    private String curTestName;
    private String restore;
    private String modifyServConfig;
    private String tableName;
    private String libraryPath;
    private String directoryPath;
    private String bootstrapFile;
    private String configurationFile;

    /** Creates a new instance of CSDKLogToDBTest */
    public CSDKLogToDBTest()
            throws Exception {
        super("CSDKLogToDBTest");
        try {
            idm = new IDMCommon("CSDKLogToDBTest");
            cc = new CSDKCommon();
            adminSSOToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(adminSSOToken)) {
                log(Level.SEVERE, "CSDKLogToDBTest", "SSO token is invalid");
                assert false;
            }
            logConfig = getLogConfig(adminSSOToken);
            dbConfRb = ResourceBundle.getBundle("log" + fileseparator +
                    LOGTEST_DB_CONF_FILE);
            testCaseInfo = ResourceBundle.getBundle("csdk" + fileseparator +
                    testCaseInfoFileName);
            location = dbConfRb.getString(LOGTEST_KEY_LOG_LOCATION);
            int lastIdx = location.lastIndexOf("/");
            String reqDBURL = location.substring(0, lastIdx);
            SimpleDateFormat simple = new SimpleDateFormat(
                    "yyyyMMddhhmmssSS");
            Date date = new Date();
            if (dataBaseName.equals("")) {
                dataBaseName = location.substring(lastIdx + 1) +
                        simple.format(date);
                log(Level.FINEST, "CSDKLogToDBTest", "New dataBaseName :" +
                        dataBaseName);
            }
            dbUser = dbConfRb.getString(LOGTEST_KEY_DB_USER);
            dbPassword = dbConfRb.getString(LOGTEST_KEY_DB_PASSWORD);
            driver = dbConfRb.getString(LOGTEST_KEY_DRIVER);
            if (dbConn == null) {
                log(Level.FINE, "CSDKLogToDBTest", "Getting db connection");
                dbConn = LogCommon.getConnection(dbUser, dbPassword, driver,
                        reqDBURL + "/" + dataBaseName, dataBaseName);
                dbInit = true;
            }
        } catch (SQLException sqexp) {
            log(Level.SEVERE, "CSDKLogToDBTest ", "Couldn't get connection " +
                    sqexp.getMessage());
            dbInit = false;
        } catch (Exception ex) {
            log(Level.SEVERE, "CSDKLogToDBTest ", "Initialization failed " +
                    ex.getMessage());
            throw ex;
        }
    }

    /**
     * This method configures the log service to user "DB" and configures the
     * database.
     */
    @BeforeClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    @Parameters({"testName", "createUser", "restore", "modifyServConfig"})
    public void setup(String testName, String createUser, String restore,
            String modifyServConfig)
            throws Exception {
        Object[] params = {testName, createUser, restore, modifyServConfig};
        entering("setup", params);
        Map ldMap = cc.getLibraryPath();
        libraryPath = (String) ldMap.get("libraryPath");
        directoryPath = (String) ldMap.get("directoryPath");
        bootstrapFile = cc.getBootStrapFilePath();
        configurationFile = cc.getConfigurationFilePath();
        try {
            if (dbInit) {
                if (!configured) {
                    configureLogToDB();
                }
                this.restore = restore;
                this.modifyServConfig = modifyServConfig;
                curTestName = testName;
                if (createUser.equals("true")) {
                    userId = testCaseInfo.getString("CSDKLogToDBTest" + "." +
                            curTestName + "." + LOGTEST_KEY_USER_ID);
                    if (userId != null) {
                        idm.createID(userId, "user", null, adminSSOToken,
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
            if (modifyServConfig.equals("true")) {
                Reporter.log("Test description : " + testCaseInfo.getString
                        ("CSDKLogToDBTest.testCSDKModifyLogConfig." +
                        "description"));
                String attrValPair = testCaseInfo.getString("CSDKLogToDBTest" +
                        "." + curTestName + "." +
                        LOGTEST_KEY_ATTR_VAL_PAIR);
                Map logSvcMap = attributesToMap(attrValPair);
                log(Level.FINEST, "testCSDKModifyLogConfig",
                        "Updating service config : " + logSvcMap);
                assert (updateLogConfig(adminSSOToken, logSvcMap));
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "setup", "Setup failed :" + ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("setup");
    }

    /**
     * This method tests writing messages to the db.
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCSDKLogMessageToDB()
            throws Exception {
        entering("testCSDKLogMessageToDB", null);
        try {
            String msgStr = testCaseInfo.getString("CSDKLogToDBTest" + "." +
                    curTestName + "." + LOGTEST_KEY_MESSAGE);
            String moduleName = testCaseInfo.getString("CSDKLogToDBTest" + "." 
                    + curTestName + "." + LOGTEST_KEY_MODULE_NAME);
             String error;
             String results;
             String errorDummy;
             String resultsDummy;
            tableName = testCaseInfo.getString("CSDKLogToDBTest" + "." +
                    curTestName + "." + LOGTEST_KEY_TABLE_NAME);
            Reporter.log("Test description : " + testCaseInfo.getString(
                    "CSDKLogToDBTest" + "." +
                    curTestName + "." + LOGTEST_KEY_DESCRIPTION));
            Reporter.log("Test log message : " + msgStr);
            Reporter.log("Test table name : " + tableName);
            Reporter.log("Execution command :" + "am_log_test" + " -u " +
                    " &lt;user tokenid&gt " + " -n " + tableName +
                    " -l " + " &lt;amadmin tokenid&gt; " + " -m " +
                    msgStr + " -f " + bootstrapFile);
            if (modifyServConfig.equals("true")) {
                ProcessBuilder pbDummy = new ProcessBuilder(directoryPath +
                        fileseparator + "am_log_test", "-u",
                        userSSOToken.getTokenID().toString(), "-n", tableName,
                        "-l", adminSSOToken.getTokenID().toString(), "-m",
                        "dummy message", "-f", bootstrapFile);
                pbDummy.environment().put("LD_LIBRARY_PATH", libraryPath);
                pbDummy.directory(new File(directoryPath));
                Process pDummy = pbDummy.start();
                BufferedReader stdInputDummy = new BufferedReader(new
                        InputStreamReader(pDummy.getInputStream()));
                StringBuffer sbResultsDummy = new StringBuffer();
                while ((resultsDummy = stdInputDummy.readLine()) != null) {
                    sbResultsDummy = sbResultsDummy.append(resultsDummy);
                }
                BufferedReader stdErrorDummy = new BufferedReader(new
                        InputStreamReader(pDummy.getErrorStream()));
                while ((errorDummy = stdErrorDummy.readLine()) != null) {
                    sbResultsDummy = sbResultsDummy.append(errorDummy);
                }
                if (sbResultsDummy.toString().contains("Logging Completed!")) {
                    assert true;
                } else {
                    assert false;
                }
                log(Level.FINEST, "testCSDKLogMessageToDB", sbResultsDummy);
                Thread.sleep(notificationSleepTime);
            }            
            log(Level.FINE, "testCSDKLogMessageToDB", "Logging message : " +
                    msgStr);

            ProcessBuilder pb = new ProcessBuilder(directoryPath +
                    fileseparator + "am_log_test", "-u",
                    userSSOToken.getTokenID().toString(), "-n", tableName,
                    "-l", adminSSOToken.getTokenID().toString(), "-m",
                    msgStr, "-f", bootstrapFile);
            pb.environment().put("LD_LIBRARY_PATH", libraryPath);
            pb.directory(new File(directoryPath));
            Process p1 = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p1.getInputStream()));
            StringBuffer sbResults = new StringBuffer();
            while ((results = stdInput.readLine()) != null) {
                sbResults = sbResults.append(results);
            }
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p1.getErrorStream()));
            while ((error = stdError.readLine()) != null) {
                sbResults = sbResults.append(error);
            }
            if (sbResults.toString().contains("Logging Completed!")) {
                assert true;
            } else {
                assert false;
            }
            log(Level.FINEST, "testCSDKLogMessageToDB", sbResults);
        } catch (Exception ex) {
            log(Level.SEVERE, "testCSDKLogMessageToDB", "Error writing log : "
                    + ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("testCSDKLogMessageToDB");
    }

    /**
     * This method tests reading messages from db.
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"testCSDKLogMessageToDB"})
    public void testCSDKReadLog()
            throws Exception {
        entering("testCSDKReadLog", null);
        try {
            int i = 0;
            String lastRec;
            String expMsg = testCaseInfo.getString("CSDKLogToDBTest" + "." +
                    curTestName + "." +
                    LOGTEST_KEY_EXPECTED_MESSAGE);
            Reporter.log("Test description : " + testCaseInfo.getString
                    ("CSDKLogToDBTest.testCSDKReadLog." +
                    "description"));
            lastRec = readLastLogEntry(dbConn, tableName);
            while (i < 5) {
                i++;
                lastRec = readLastLogEntry(dbConn, tableName);
                log(Level.FINEST, "testCSDKReadLog", "Last Rec :" + lastRec);
                if (lastRec.indexOf(expMsg) == -1) {
                    log(Level.FINEST, "testCSDKReadLog",
                            "Last Rec doesnt match. " +
                            "Wait for " + notificationSleepTime +
                            " milisecs & read again");
                    Thread.sleep(notificationSleepTime);
                } else {
                    break;
                }
            }
            if (lastRec.indexOf(expMsg) == -1) {
                log(Level.SEVERE, "testCSDKReadLog", "Record doesn't contain "
                        + "expected message");
                assert false;
            } else {
                assert true;
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "testCSDKReadLog", "Error reading last record "
                    + ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("testCSDKReadLog");
    }

    /**
     * This method deletes the created user and if restore is set to true
     * retores the log service configuration to the backup version and releases
     * the connection.
     */
    @AfterClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanUp()
            throws Exception {
        entering("cleanUp", null);
        try {
            if (idm.doesIdentityExists(userId, "user", adminSSOToken, realm)) {               
                log(Level.FINE, "cleanUp", "Delete test user : " + userId);
                idm.deleteID(userId, "user", adminSSOToken, realm);
            }
            if (restore.equals("true")) {
                if (dbInit) {
                    log(Level.FINE, "cleanUp", "Resetting...");
                    updateLogConfig(adminSSOToken, logConfig);                    
                    LogCommon.deleteDB(dbConn, dataBaseName);
                    int lastIdx = location.lastIndexOf("/");
                    String reqDBURL = location.substring(0, lastIdx);
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
        } finally {
            destroyToken(userSSOToken);
            destroyToken(adminSSOToken);
        }
        exiting("cleanUp");
    }

    /**
     * This method configures the log service.
     */
    private void configureLogToDB()
            throws Exception {
        entering("configureLogToDB", null);
        try {
            Map cfgMap = new HashMap();
            Set locSet = new HashSet();
            int lastIdx = location.lastIndexOf("/");
            String reqDBURL = location.substring(0, lastIdx);
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
            log(Level.SEVERE, "configureLogToDB",
                    "Log service configuration failed : " + ex.getMessage());
            throw ex;
        }
        exiting("configureLogToDB");
    }
}
