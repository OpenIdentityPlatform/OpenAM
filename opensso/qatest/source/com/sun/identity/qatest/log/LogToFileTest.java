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
 * $Id: LogToFileTest.java,v 1.5 2009/01/27 00:07:20 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.log;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.IDMCommon;
import java.util.Map;
import java.util.logging.Level;
import com.sun.identity.qatest.common.LogCommon;
import java.util.ResourceBundle;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class will test writing logs to file and modifying the log service
 * attribute values
 **/
public class LogToFileTest extends LogCommon implements LogTestConstants {
    
    private Map logConfig;
    private SSOToken adminSSOToken;
    private SSOToken userSSOToken;
    private IDMCommon idm;
    private String userId;
    private ResourceBundle testCaseInfo;
    private static String testCaseInfoFileName = "LogToFileTest";
    private String curTestName;
    private String restore;
    private String modifyServConfig;
    
    /** Creates a new instance of LogToFileTest */
    public LogToFileTest() 
    throws Exception {
        super("LogToFileTest");
        try {
            adminSSOToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(adminSSOToken)) {
                log(Level.SEVERE, "LogToFileTest", "SSO token is invalid");
                assert false;
            }
            idm = new IDMCommon("LogToFileTest");
            logConfig = getLogConfig(adminSSOToken);
            testCaseInfo = ResourceBundle.getBundle("log" + fileseparator +
                    testCaseInfoFileName);
        } catch (Exception ex) {
            log(Level.SEVERE, "LogToFileTest", "LogTest setup failed");
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * This function creates the test user.
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
            this.restore = restore;
            this.modifyServConfig = modifyServConfig;
            curTestName = testName;
            if (createUser.equals("true")) {
                userId = testCaseInfo.getString(curTestName + "." + 
                        LOGTEST_KEY_USER_ID);
                if (userId != null) {
                    idm.createID(userId ,"user", null, adminSSOToken, realm);
                    userSSOToken = getToken(userId, userId, basedn);
                    if (!validateToken(userSSOToken)) {
                        log(Level.SEVERE, "setup", "SSO token is invalid");
                        assert false;
                    }
                }
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
                        "Updating service config " + logSvcMap);
                assert (updateLogConfig(adminSSOToken, logSvcMap));
            } else {
                log(Level.FINEST, "modifyLogConfig",
                        "Using previours testcase service configuration. ");
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "testLogging", "Error Modifying log service :" +
                    ex.getMessage());
            cleanUp();
            throw ex;
        } 
        exiting("modifyLogConfig");
    }
    
    /**
     * This method tests writing messages to the log files.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"modifyLogConfig"})
    public void logMessage()
    throws Exception {
        entering("logMessage", null);
        try {
            String msgStr = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_MESSAGE);
            String moduleName = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_MODULE_NAME);
            String fileName = testCaseInfo.getString(curTestName + "." + 
                    LOGTEST_KEY_FILE_NAME);
            String loggerLevel = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_LOGGER_LEVEL);
            String recordLevel = testCaseInfo.getString(curTestName + "." +
                    LOGTEST_KEY_RECORD_LEVEL);
            Reporter.log("Test Name : " + curTestName);
            Reporter.log("Test Description : " + testCaseInfo.getString(
                    curTestName + "." + LOGTEST_KEY_DESCRIPTION));
            Reporter.log("Test action :  logMessage");
            Reporter.log("Test logger log level : " + loggerLevel);
            Reporter.log("Test record log level : " + recordLevel);
            Reporter.log("Test log message : " + msgStr);
            log(Level.FINE, "logMessage", "Logging message " + msgStr);
            assert(writeLog(adminSSOToken, userSSOToken, fileName, msgStr, 
                    moduleName, getLevel(loggerLevel), getLevel(recordLevel)));
        } catch (Exception ex) {
             log(Level.SEVERE, "logMessage", "Error writing log :" + 
                     ex.getMessage());
             cleanUp();
             throw ex;
        }
        exiting("logMessage");
    }
    
    /**
     * This method deletes the created user and if retore is set to true
     * restores the log configuration service.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanUp() 
    throws Exception {
        entering("cleanUp", null);
        try {
            if (userId != null) {
                if (idm.doesIdentityExists(userId, "user", adminSSOToken, realm)) {
                    destroyToken(userSSOToken);
                    log(Level.FINE, "cleanUp", "Delete test user : " + userId);
                    idm.deleteID(userId, "user", adminSSOToken, realm);
                }
            }
            if (restore.equals("true")) {
                log(Level.FINE, "cleanUp", "Resetting logService config.");
                updateLogConfig(adminSSOToken, logConfig);
                destroyToken(adminSSOToken);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "cleanUp", "Error occured during test with user id :" + 
                    userId);
            log(Level.SEVERE, "cleanUp", "Error writing log :" + 
                    ex.getMessage());
            ex.printStackTrace();
        }
        exiting("cleanUp");
    }
}
