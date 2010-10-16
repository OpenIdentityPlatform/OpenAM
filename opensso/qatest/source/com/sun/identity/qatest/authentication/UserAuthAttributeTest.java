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
 * $Id: UserAuthAttributeTest.java,v 1.6 2009/06/02 17:08:18 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class performs the tests for setting up different user authentication
 * attibutes and verifies the login status and also verifies that the attributes
 * are set in the user entry correctly. The login is set to the module based
 * login
 */
public class UserAuthAttributeTest extends AuthenticationCommon {

    private ResourceBundle testResources;
    private IDMCommon idmc;
    String testUserName;
    String testCaseDesc;
    String testUserPass;
    String testUserStatus;
    String testAttrName;
    String testAttrValue;
    String testPassMsg;
    int noOfAttributes;
    Map userAttrMap;

    /**
     * Default Constructor
     */
    public UserAuthAttributeTest() {
        super("UserAuthAttributeTest");
        testResources = ResourceBundle.getBundle("authentication" +
                fileseparator + "UserAuthAttributeTest");
        idmc = new IDMCommon();
    }

    /**
     * Reads the necessary test configuration and prepares the system
     * for Authentication related properties for testing
     * @param 
     */
    @Parameters({"testCaseName", "testRealm"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testCaseName, String testRealm)
    throws Exception {
        Object[] params = {testCaseName, testRealm};
        entering("setup", params);
        SSOToken adminToken = null;
        String expireDate = null;

        try {
            testUserName = testResources.getString("am-auth-" + testCaseName +
                    "-" + "username");
            testUserPass = testResources.getString("am-auth-" + testCaseName +
                    "-" + "userpassword");
            testPassMsg = testResources.getString("am-auth-" + testCaseName +
                    "-" + "passmessage");
            noOfAttributes = new Integer(testResources.getString("am-auth-" +
                    testCaseName + "-" + "noofattributes")).intValue();
            testCaseDesc = testResources.getString("am-auth-" + testCaseName +
                    "-" + "description");

            adminToken = getToken(adminUser, adminPassword, realm);
            if (!testRealm.equals("/")) {
                Map realmAttrMap = new HashMap();
                Set realmSet = new HashSet();
                realmSet.add("Active");
                realmAttrMap.put("sunOrganizationStatus", realmSet);
                log(Level.FINE, "setup", "Creating the realm " + testRealm);
                AMIdentity amid = idmc.createIdentity(adminToken,
                        realm, IdType.REALM, testRealm, realmAttrMap);
                log(Level.FINE, "setup",
                        "Verifying the existence of sub-realm " +
                        testRealm);
                if (amid == null) {
                    log(Level.SEVERE, "setup", "Creation of sub-realm " +
                            testRealm + " failed!");
                    assert false;
                }
            }

            userAttrMap = new HashMap();
            Set valSet = new HashSet();
            valSet.add(testUserPass);
            userAttrMap.put("userpassword", valSet);
            for (int i = 0; i < noOfAttributes; i++) {
                testAttrName = (testResources.getString("am-auth-" +
                        testCaseName + "-" + "attr" + i + "-name")).trim();
                testAttrValue = (testResources.getString("am-auth-" +
                        testCaseName + "-" + "attr" + i + "-value")).trim();
                if (testAttrName.equals("iplanet-am-user-account-life")) {
                    if (testAttrValue.equals("positive")) {
                        Calendar rightNow = Calendar.getInstance();
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Calendar working = (Calendar) rightNow.clone();
                        working.add(Calendar.DAY_OF_YEAR, +2);
                        expireDate = (formatter.format(working.getTime()));
                    } else if (testAttrValue.equals("negative")) {
                        Calendar rightNow = Calendar.getInstance();
                        SimpleDateFormat formatter =
                                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                        Calendar working = (Calendar) rightNow.clone();
                        working.add(Calendar.DAY_OF_YEAR, - 2);
                        expireDate = (formatter.format(working.getTime()));
                    }
                    testAttrValue = expireDate;
                }
                valSet = new HashSet();
                valSet.add(testAttrValue);
                userAttrMap.put(testAttrName, valSet);
            }

            idmc.createIdentity(adminToken, testRealm, IdType.USER,
                    testUserName, userAttrMap);
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup(testRealm);
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }

    }

    /**
     * This test verifies the account login status for the users with 
     * different account management attributes.
     */
    @Parameters({"testCaseName", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void runTest(String testCaseName, String testRealm)
    throws Exception {
        Object[] params = {testCaseName, testRealm};
        entering("runTest", params);
        try {
            log(Level.FINE, "runTest", "testCaseName" + testUserName);
            log(Level.FINE, "runTest", "testuserpassword" + testPassMsg);
            log(Level.FINE, "runTest", "testCaseName" + testCaseName);

            Reporter.log("TestcaseName" + testUserName);
            Reporter.log("Testuserpassword" + testPassMsg);
            Reporter.log("TestCaseDescription:" + testCaseDesc);

            Map executeMap = new HashMap();
            executeMap.put("realm", testRealm);
            executeMap.put("users", testUserName + ":" + testUserPass);
            executeMap.put("successMsg", testPassMsg);
            String testModule = testResources.getString(
                    "am-auth-userauthattributetest-module");
            String authInstanceName = getAuthInstanceName(testModule);
            String uniqueIdentifier;
            String redirectURL;
            if (!testRealm.equals("/")) {
                uniqueIdentifier = testRealm + "_" + testCaseName;
                redirectURL = getLoginURL(testRealm) + "&amp;" +
                        "module=" + authInstanceName;
            } else {
                redirectURL = getLoginURL(testRealm) + "?module=" +
                        authInstanceName;
                uniqueIdentifier = "rootrealm" + "_" + testCaseName;
            }
            executeMap.put("redirectURL", redirectURL);
            executeMap.put("uniqueIdentifier", uniqueIdentifier);
            testUserLoginAuthAttribute(executeMap, userAttrMap);
            exiting("runTest");
        } catch (Exception e) {
            log(Level.SEVERE, "runTest", e.getMessage());
            e.printStackTrace();
            cleanup(testRealm);
            throw e;
        } 
    }

    /**
     * performs cleanup after tests are done.
     */
    @Parameters({"testRealm"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("cleanup", params);
        SSOToken adminToken = null;
        try {
            adminToken = getToken(adminUser, adminPassword, realm);
            log(Level.FINE, "cleanup", "Deleting user " + testUserName + 
                    " in realm " + testRealm + " ...");
            idmc.deleteIdentity(adminToken, testRealm, 
                    IdType.USER, testUserName);
            if (!testRealm.equals("/")) {
                log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                        testRealm);
                idmc.deleteRealm(adminToken, realm + testRealm);
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }
    }
}
