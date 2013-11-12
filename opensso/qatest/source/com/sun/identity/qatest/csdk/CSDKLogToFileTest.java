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
import com.sun.identity.qatest.common.LogCommon;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.log.LogTestConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.ResourceBundle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class will test writing logs to file and modifying the log service
 * attribute values
 **/
public class CSDKLogToFileTest extends LogCommon implements LogTestConstants {
    
    private Map logConfig;
    private SSOToken adminSSOToken;
    private SSOToken userSSOToken;
    private IDMCommon idm;
    private String userId;
    private ResourceBundle testCaseInfo;
    private static String testCaseInfoFileName = "CSDKLogToFileTest";
    private String curTestName;
    private String restore;
    private String modifyServConfig;
    private CSDKCommon cc;
    private String libraryPath;
    private String directoryPath;
    private String bootstrapFile;
    private String configurationFile;
    
    /** Creates a new instance of CSDKLogToFileTest */
    public CSDKLogToFileTest()
    throws Exception {
        super("CSDKLogToFileTest");
        try {
            adminSSOToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(adminSSOToken)) {
                log(Level.SEVERE, "CSDKLogToFileTest", "SSO token is invalid");
                assert false;
            }
            idm = new IDMCommon("CSDKLogToFileTest");
            cc = new CSDKCommon();
            logConfig = getLogConfig(adminSSOToken);
            testCaseInfo = ResourceBundle.getBundle("csdk" + fileseparator +
                    testCaseInfoFileName);
        } catch (Exception ex) {
            log(Level.SEVERE, "CSDKLogToFileTest", "LogTest setup failed");
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
        Map ldMap = cc.getLibraryPath();
        libraryPath = (String) ldMap.get("libraryPath");
        directoryPath = (String) ldMap.get("directoryPath");
        bootstrapFile =  cc.getBootStrapFilePath();
        configurationFile = cc.getConfigurationFilePath();
        try {
            this.restore = restore;
            this.modifyServConfig = modifyServConfig;
            curTestName = testName;            
            if (createUser.equals("true")) {
                userId = testCaseInfo.getString("CSDKLogToFileTest" + "." +
                        curTestName + "." +
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
            if (modifyServConfig.equals("true")) {                
                String attrValPair =  testCaseInfo.getString
                        ("CSDKLogToFileTest" +
                        "." + curTestName + "." +
                        LOGTEST_KEY_ATTR_VAL_PAIR);
                Map logSvcMap = attributesToMap(attrValPair);
                log(Level.FINEST, "modifyLogConfig",
                        "Updating service config " + logSvcMap);
                assert (updateLogConfig(adminSSOToken, logSvcMap));
            } else {
                log(Level.FINEST, "modifyLogConfig",
                        "Using previours testcase service configuration. ");
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "setup", "Setup failed :" + ex.getMessage());
            cleanUp();
            throw ex;
        }
        exiting("setup");
    }
    
  
    /**
     * This method tests writing messages to the log files.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void logMessage()
    throws Exception {
        entering("logMessage", null);
        try {
            String msgStr = testCaseInfo.getString("CSDKLogToFileTest" + "." +
                    curTestName + "." +
                    LOGTEST_KEY_MESSAGE);
            String moduleName = testCaseInfo.getString("CSDKLogToFileTest" +
                    "." + curTestName + "." + LOGTEST_KEY_MODULE_NAME);
            String fileName = testCaseInfo.getString("CSDKLogToFileTest" + "."
                    + curTestName + "." + LOGTEST_KEY_FILE_NAME);
            String results;
            String error;
            Reporter.log("Test Description : " + testCaseInfo.getString(
                    "CSDKLogToFileTest" + "." + curTestName + "." +
                    LOGTEST_KEY_DESCRIPTION));            
            Reporter.log("Test log message : " + msgStr);
            Reporter.log("Execution command :" + "am_log_test" + " -u " +
                        " &lt;user tokenid&gt; " +  " -n " +
                        fileName + " -l " +  " &lt;amadmin tokenid&gt; " +
                        " -m " + msgStr +  " -f " +
                        bootstrapFile);
            log(Level.FINE, "logMessage", "Logging message " + msgStr);
            ProcessBuilder pb = new ProcessBuilder(directoryPath + 
                    fileseparator + "am_log_test","-u",
                    userSSOToken.getTokenID().toString(), "-n" , fileName ,
                    "-l" , adminSSOToken.getTokenID().toString(), "-m" ,
                    msgStr , "-f" , bootstrapFile );
            pb.environment().put("LD_LIBRARY_PATH", libraryPath);
            pb.directory(new File(directoryPath));
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader
                    (new InputStreamReader(p.getInputStream()));            
            StringBuffer sbResults = new StringBuffer();
            while ((results = stdInput.readLine()) != null) {                
                sbResults = sbResults.append(results);
            }
            BufferedReader stdError = new BufferedReader
                    (new InputStreamReader(p.getErrorStream()));
            while ((error = stdError.readLine()) != null) {
                sbResults = sbResults.append(error);
            }    
            if (sbResults.toString().contains("Logging Completed!")){
                assert true;
            } else {
                assert false;
            }
                log(Level.FINEST, "logMessage", sbResults);                 
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
                if (idm.doesIdentityExists(userId, "user", adminSSOToken,
                        realm)) {                    
                    log(Level.FINE, "cleanUp", "Delete test user : " + userId);
                    idm.deleteID(userId, "user", adminSSOToken, realm);
                }
            }
            if (restore.equals("true")) {
                log(Level.FINE, "cleanUp", "Resetting logService config.");
                updateLogConfig(adminSSOToken, logConfig);                
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "cleanUp", "Error occured during test with " +
                    "user id :" +
                    userId);
            log(Level.SEVERE, "cleanUp", "Error writing log :" + 
                    ex.getMessage());
            ex.printStackTrace();
        } finally {
            destroyToken(userSSOToken);
            destroyToken(adminSSOToken);
        }
        exiting("cleanUp");
    }
}
