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
 * $Id: CSDKAuthTest.java
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.csdk;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the SSO functions of C SDK 
 */
public class CSDKSSOTest extends TestCommon {

    private CSDKCommon cc;
    private String curTestIdx;
    private SSOToken adminSSOToken;
    private SSOToken userSSOToken;
    private IDMCommon idm;
    private String userId;
    private ResourceBundle testCaseInfo;
    private static String testCaseInfoFileName = "CSDKSSOTest";
    private String libraryPath;
    private String directoryPath;
    private String bootstrapFile;
    private String configurationFile;
    private String restore;

    /**
     * Constructor for the class.
     */
    public CSDKSSOTest()
            throws Exception {
        super("CSDKSSOTest");
        try {
            adminSSOToken = getToken(adminUser, adminPassword, basedn);
            if (!validateToken(adminSSOToken)) {
                log(Level.SEVERE, "CSDKSSOTest", "SSO token is invalid");
                assert false;
            }
            idm = new IDMCommon("CSDKSSOTest");
            cc = new CSDKCommon();
            testCaseInfo = ResourceBundle.getBundle("csdk" + fileseparator +
                    testCaseInfoFileName);
        } catch (Exception ex) {
            log(Level.SEVERE, "CSDKSSOTest", "CSDKSSOTest setup failed");
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * This method is to configure the initial setup. It does the following:   
     *  Create a new user    
     */
    @Parameters({"testIdx", "createUser", "restore"})
    @BeforeClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testIdx, String createUser, String restore)
            throws Exception {
        Object[] params = {testIdx, createUser, restore};
        entering("setup", params);
        Map ldMap = cc.getLibraryPath();
        libraryPath = (String) ldMap.get("libraryPath");
        directoryPath = (String) ldMap.get("directoryPath");
        bootstrapFile = cc.getBootStrapFilePath();
        configurationFile = cc.getConfigurationFilePath();
        try {
            this.restore = restore;
            curTestIdx = testIdx;
            userId = testCaseInfo.getString("CSDKSSOTest.userid");
            if (createUser.equals("true")) {
                if (userId != null) {
                    idm.createID(userId, "user", null, adminSSOToken, realm);
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
     * Tests for successful login into the system using correct
     * credentials
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCSDKSSO()
            throws Exception {
        entering("testCSDKSSO", null);
        try {
            String msg = (String) testCaseInfo.getString("CSDKSSOTest" +
                    "." + curTestIdx + ".passmsg");
            SSOToken token = getToken(userId, userId, realm);
            String results;
            String error;
            Reporter.log("Test Description : " + (String) testCaseInfo.
                    getString("CSDKSSOTest" +
                    "." + curTestIdx + ".description"));
            Reporter.log("Execution command :" + "am_sso_test" +
                    " -u " + userId + " -p " + userId + " -s " +
                    " &lt;amadmin token id&gt; " + " -f " +
                    " bootstrapFile " + " -o " + realm + " -c " +
                    " configurationFile " + " -t " + curTestIdx);
            ProcessBuilder pb = new ProcessBuilder(directoryPath +
                    fileseparator + "am_sso_test", "-u", userId,
                    "-p", userId, "-s",token.getTokenID().toString(),
                    "-o", realm, "-f",bootstrapFile, "-c", configurationFile,
                    "-t", curTestIdx);
            pb.environment().put("LD_LIBRARY_PATH", libraryPath);
            pb.directory(new File(directoryPath));
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));
            StringBuffer sbResults = new StringBuffer();
            while ((results = stdInput.readLine()) != null) {
                sbResults = sbResults.append(results);
            }
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            while ((error = stdError.readLine()) != null) {
                sbResults = sbResults.append(error);
            }
            if (sbResults.toString().contains(msg)) {
                assert true;
            } else {
                assert false;
            }
            log(Level.FINEST, "testCSDKSSO", sbResults);
        } catch (Exception e) {
            log(Level.SEVERE, "testCSDKSSO", e.getMessage());
            cleanUp();
            throw e;
        }
        exiting("testCSDKSSO");
    }

    /**
     * This method deletes the created user, if rsetore is set to true     
     */
    @AfterClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanUp()
            throws Exception {
        entering("cleanUp", null);
        try {
            if (restore.equals("true")) {
                if (userId != null) {
                    if (idm.doesIdentityExists(userId, "user", adminSSOToken,
                            realm)) {
                        destroyToken(userSSOToken);
                        log(Level.FINE, "cleanUp", "Delete test user : " +
                                userId);
                        idm.deleteID(userId, "user", adminSSOToken, realm);                        
                    }
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "cleanUp", "Error occured during test with user"
                    + " id :" + userId);
            log(Level.SEVERE, "cleanUp", "Error writing log :" +
                    ex.getMessage());
            ex.printStackTrace();
        } finally {
            destroyToken(adminSSOToken);
        }
        exiting("cleanUp");
    }
}
