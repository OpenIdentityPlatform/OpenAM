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
 *$Id: AccountLockoutTest.java,v 1.9 2009/06/02 17:08:18 cmwesley Exp $*
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
 * This class is called by the <code>AccountLockoutTest</code>.
 * Performs the tests for User Account Lockout and warnings
 * for the number of set login failure attempts. Test cases covered
 * AccountLock_2, AccountLock_3, AccountLock_4, AccountLock_6, AccountLock_9,
 * AccountLock_10, AccountLock_11, AccountLock_13, AccountLock_14
 */
public class AccountLockoutTest extends AuthenticationCommon {
    private IDMCommon idmc;
    private List idTypeList = new ArrayList<IdType>();
    private ResourceBundle testResources;
    private String testModule;
    private String createUserProp;
    private boolean userExists;
    private String lockUser;
    private String lockUserpass;
    private String nslockUser;
    private String nslockUserpass;
    private String lockStatusUser;
    private String lockStatusUserpass;
    private String lockAttrUser;
    private String lockAttrUserpass;
    private String warnUser;
    private String warnUserpass;
    private String lockAttrName;
    private String lockAttrValue;
    private String lockStatusAttrName;
    private String lockStatusAttrValue;
    private String nslockAttrName;
    private String nslockAttrValue;
    private String lockoutAttempts;
    private String warningAttempts;
    private String testURL;
    private String serviceName = "iPlanetAMAuthService";
    private String lockoutPassmsg;
    private String warnPassmsg;
    private String failpage;
    private String url;
    private List<String> testUserList = new ArrayList<String>();
    private Map authServiceAttrs;
    private SMSCommon smsc;
    private SSOToken serviceToken;

    /**
     * Default Constructor
     **/
    public AccountLockoutTest() {
        super();
        idmc = new IDMCommon();
    }

    /**
     * Reads the necessary test configuration and prepares the system
     * for Account Lockout testing
     * - Enables the Account Lockout
     * - Sets the lockout attributes
     * - Create Users , If needed
     * @param testRealm - the realm in which the account lockout testing should
     * be exected.
     */
    @Parameters({"testRealm"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("setup", params);
        url = getLoginURL("/");
        Map authAttrMap = new HashMap();

        try {
            testResources = ResourceBundle.getBundle("authentication" +
                    fileseparator + "AccountLockoutTest");
            testModule = testResources.getString("am-auth-lockout-test-module");
            createUserProp = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-createTestUser");
            userExists = new Boolean(createUserProp).booleanValue();
            lockUser = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockusername");
            lockUser.trim();
            lockUserpass = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockuserpassword");
            lockUserpass.trim();
            lockoutPassmsg = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lock-passmsg");
            warnUser = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-warnusername");
            warnUser.trim();
            warnUserpass = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-warnuserpassword");
            warnUserpass.trim();
            warningAttempts = testResources.getString(
                    "am-auth-lockout-test-warning-attempts");
            lockoutAttempts = testResources.getString(
                    "am-auth-lockout-test-lockout-attempts");
            warnPassmsg = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-warn-passmsg");
            failpage = testResources.getString("am-auth-lockout-test-" +
                    testModule + "-fail-page");

            testURL = url + "?module=" + testModule;

            serviceToken = getToken(adminUser, adminPassword, realm);
            if (!testRealm.equals("/")) {
                testURL = url + "?realm=" + testRealm +
                        "&amp;module=" + testModule;
                Map realmAttrMap = new HashMap();
                Set realmSet = new HashSet();
                realmSet.add("Active");
                realmAttrMap.put("sunOrganizationStatus", realmSet);
                log(Level.FINE, "setup", "Creating the realm " + testRealm);
                AMIdentity amid = idmc.createIdentity(serviceToken,
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
 
            Set valSet = new HashSet();
            valSet.add("true");
            authAttrMap.put("iplanet-am-auth-login-failure-lockout-mode", 
                    valSet);
            valSet = new HashSet();
            valSet.add(warningAttempts);
            authAttrMap.put("iplanet-am-auth-lockout-warn-user", valSet);
            valSet = new HashSet();
            valSet.add(lockoutAttempts);
            authAttrMap.put("iplanet-am-auth-login-failure-count", valSet);            

            log(Level.FINE, "setup", "Retrieving attribute values in " + 
                    serviceName + " ...");

            smsc = new SMSCommon(serviceToken);
            authServiceAttrs = smsc.getAttributes(serviceName, testRealm,
                    "Organization");
           
            log(Level.FINE, "setup", "Setting the " +
                    "iplanet-am-auth-login-failure-lockout-mode, " +
                    "iplanet-am-auth-lockout-warn-user, and " +
                    "iplanet-am-auth-login-failure-count attributes in the" +
                    serviceName + " service ...");
            smsc.updateServiceAttrsRealm(serviceName, testRealm, authAttrMap);

            StringBuffer attrBuffer = new StringBuffer();
            if (!userExists) {
                log(Level.FINE, "setup", "Creating the user " + lockUser);
                attrBuffer.append("sn=" + lockUser).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("cn=" + lockUser).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("userpassword=" + lockUserpass).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("inetuserstatus=Active");

                log(Level.FINE, "setup", "Creating user " +
                        lockUser + " ...");
                if (!idmc.createID(lockUser, "user", attrBuffer.toString(),
                        serviceToken, testRealm)) {
                    log(Level.SEVERE, "setup",
                            "Failed to create user identity " +
                            lockUser + " ...");
                    assert false;
                } else {
                    testUserList.add(lockUser);
                    idTypeList.add(IdType.USER);
                }
                log(Level.FINE, "setup", "Creating the user " + warnUser);
                attrBuffer = new StringBuffer("sn=" + warnUser).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("cn=" + warnUser).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("userpassword=" + warnUserpass).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("inetuserstatus=Active");

                log(Level.FINE, "setup", "Creating user " +
                        warnUser + " ...");
                if (!idmc.createID(warnUser, "user", attrBuffer.toString(),
                        serviceToken, testRealm)) {
                    log(Level.SEVERE, "setup",
                            "Failed to create user identity " +
                            warnUser + " ...");
                    assert false;
                } else {
                    testUserList.add(warnUser);
                    idTypeList.add(IdType.USER);
                }
            }
            nslockUser = (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-nslockusername")).trim();
            nslockUserpass =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-nslockuserpassword")).trim();
            nslockAttrName =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-nslockoutattrname")).trim();
            nslockAttrValue =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-nslockoutattrvalue")).trim();
            attrBuffer = new StringBuffer("sn=" + nslockUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + nslockUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + nslockUserpass).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append(nslockAttrName).append("=").
                        append(nslockAttrValue);

            log(Level.FINE, "setup", "Creating user " + nslockUser + " ...");
            if (!idmc.createID(nslockUser, "user", attrBuffer.toString(),
                    serviceToken, testRealm)) {
                log(Level.SEVERE, "setup",
                        "Failed to create user identity " +
                        nslockUser + " ...");
                assert false;
            } else {
                testUserList.add(nslockUser);
                idTypeList.add(IdType.USER);
            }
            lockStatusUser = (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockstatususer")).trim();
            lockStatusUserpass =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockstatususerpass")).trim();
            lockStatusAttrName =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockstatusattrname")).trim();
            lockStatusAttrValue =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockstatusattrvalue")).trim();
            attrBuffer = new StringBuffer("sn=" + lockStatusUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + lockStatusUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + lockStatusUserpass).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append(lockStatusAttrName).append("=").
                        append(lockStatusAttrValue);

            log(Level.FINE, "setup", "Creating user " + 
                    lockStatusUser + " ...");
            if (!idmc.createID(lockStatusUser, "user", attrBuffer.toString(),
                    serviceToken, testRealm)) {
                log(Level.SEVERE, "setup",
                        "Failed to create user identity " +
                        lockStatusUser + " ...");
                assert false;
            } else {
                testUserList.add(lockStatusUser);
                idTypeList.add(IdType.USER);
            }
            lockAttrUser = (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockAttrUser")).trim();
            lockAttrUserpass =
                    (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockAttrUserpass")).trim();
            lockAttrName = (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockoutattrname")).trim();
            lockAttrValue = (testResources.getString("am-auth-lockout-test-" +
                    testModule + "-lockoutattrvalue")).trim();
            attrBuffer = new StringBuffer("sn=" + lockAttrUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + lockAttrUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + lockAttrUserpass).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append(lockAttrName).append("=").append(lockAttrValue);

            log(Level.FINE, "setup", "Creating user " + lockAttrUser + " ...");
            if (!idmc.createID(lockAttrUser, "user", attrBuffer.toString(),
                    serviceToken, testRealm)) {
                log(Level.SEVERE, "setup",
                        "Failed to create user identity " +
                        lockAttrUser + " ...");
                assert false;
            } else {
                testUserList.add(lockAttrUser);
                idTypeList.add(IdType.USER);
            }
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup(testRealm);
            throw e;
        } finally {
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }
    }

    /**
     * Validate that the user account is locked after the configured number
     * of failed authentication attempts have been made.
     * @param testRealm - the realm in which the testing should be exected.
     */
    @Parameters({"testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateAccountLockTest(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("validateAccountLockTest", params);
        Map executeMap = new HashMap();
        try {
            log(Level.FINEST, "validateAccountLockTest",
                    "lockUser = " + lockAttrUser);
            log(Level.FINEST, "validateAccountLockTest",
                    "testCaseName = validateAccountLockTest");
            log(Level.FINEST, "validateAccountLockTest",
                    "loginURL = " + testURL);
            log(Level.FINEST, "validateAccountLockTest",
                    "test case description = This test is to verify the " +
                    "basic account lockout functionality");

            Reporter.log("Loginuser: " + warnUser);
            Reporter.log("TestCaseName: validateAccountLockTest");
            Reporter.log("LoginUrl: " + testURL);
            Reporter.log("TestCaseDescription: This test is to verify the " +
                    "basic account lockout functionality");

            executeMap.put("Loginuser", lockUser);
            executeMap.put("Loginpassword", lockUserpass);
            executeMap.put("Loginattempts", lockoutAttempts);
            executeMap.put("Passmsg", lockoutPassmsg);
            executeMap.put("loginurl", testURL);
            executeMap.put("failpage", failpage);

            testAccountLockout(executeMap);
            exiting("validateAccountLockTest");
        } catch (Exception e) { 
            log(Level.FINE, "setup", e.getMessage());
            e.printStackTrace();
            assert false;
            cleanup(testRealm);
            throw e;
        } 
    }

    /**
     * Validate that the account lockout warning is issued prior to lockout
     * @param testRealm - the realm in which the test should be executed
     */
    @Parameters({"testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateWarningTest(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("validateWarningTest", params);
        Map executeMap = new HashMap();
        try {
            log(Level.FINEST, "validateWarningTest",
                    "lockUser = " + lockAttrUser);
            log(Level.FINEST, "validateWarningTest",
                    "testCaseName = validateAccountLockUserStatusTest");
            log(Level.FINEST, "validateWarningTest", "loginURL = " + testURL);
            log(Level.FINEST, "validateWarningTest",
                    "test case description = This test is to verify the " +
                    "warnings after the authentication failure ");

            Reporter.log("Loginuser: " + warnUser);
            Reporter.log("TestCaseName: validateWarningTest");
            Reporter.log("LoginUrl: " + testURL);
            Reporter.log("TestCaseDescription: This test is to verify the " +
                    "warnings after the authentication failure  ");

            executeMap.put("Loginuser", warnUser);
            executeMap.put("Loginpassword", warnUserpass);
            executeMap.put("Loginattempts", warningAttempts);
            executeMap.put("Passmsg", warnPassmsg);
            executeMap.put("loginurl", testURL);
            executeMap.put("failpage", failpage);

            testAccountLockWarning(executeMap);
            exiting("validateWarningTest");
        } catch (Exception e) {
            log(Level.FINE, "validateWaringTest", e.getMessage());
            e.printStackTrace();
            assert false;
            cleanup(testRealm);
            throw e;
        } 
    }

    /**
     * Validate the inetuserstatus attribute value after the lockout
     * @param testRealm - the realm in which the testing should be exected.
     */
    @Parameters({"testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateAccountLockUserStatusTest(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("validateAccountLockUserStatusTest", params);
        Map executeMap = new HashMap();
        try {
            log(Level.FINEST, "validateAccountLockUserStatusTest",
                    "lockUser = " + lockAttrUser);
            log(Level.FINEST, "validateAccountLockUserStatusTest",
                    "testCaseName = validateAccountLockUserStatusTest");
            log(Level.FINEST, "validateAccountLockUserStatusTest",
                    "loginURL = " + testURL);
            log(Level.FINEST, "validateAccountLockUserStatusTest",
                    "test case description = This test is to verify the " +
                    "inetuserstatus attribute change after lockout");

            Reporter.log("Loginuser: " + lockAttrUser);
            Reporter.log("TestCaseName: validateAccountLockUserStatusTest");
            Reporter.log("LoginUrl: " + testURL);
            Reporter.log("TestCaseDescription: This test is to verify the " +
                    "inetuserstatus attribute change after lockout ");

            executeMap.put("Loginuser", lockStatusUser);
            executeMap.put("Loginpassword", lockStatusUserpass);
            executeMap.put("Loginattempts", lockoutAttempts);
            executeMap.put("Passmsg", lockoutPassmsg);
            executeMap.put("loginurl", testURL);
            executeMap.put("failpage", failpage);

            testAccountLockoutUserStatus(executeMap, lockStatusUser);
            exiting("validateAccountLockUserStatusTest");
        } catch (Exception e) {
            log(Level.FINE, "validateAccountLockUserStatusTest",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } 
    }

    /**
     * Validate the Custom attribute value change after the lockout
     * @param testRealm - the realm in which the test should be executed
     */
    @Parameters({"testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateAccountLockUserAttrTest(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("validateAccountLockUserAttrTest", params);
        Map executeMap = new HashMap();
        try {
            log(Level.FINEST, "validateAccountLockUserAttrTest", "lockUser = " +
                    lockAttrUser);
            log(Level.FINEST, "validateAccountLockUserAttrTest",
                    "testCaseName = validateAccountLockUserAttrTest");
            log(Level.FINEST, "validateAccountLockUserAttrTest", "loginURL = " +
                    testURL);
            log(Level.FINEST, "validateAccountLockUserAttrTest",
                    "test case description = This test is to verify the " +
                    "nsaccountlockout attribute change after lockout");

            Reporter.log("Loginuser: " + lockAttrUser);
            Reporter.log("TestCaseName: validateAccountLockUserAttrTest");
            Reporter.log("LoginUrl: " + testURL);
            Reporter.log("TestCaseDescription: This test is to verify the " +
                    "custom attribute change after lockout ");

            Map authAttrMap = new HashMap();
            serviceToken = getToken(adminUser, adminPassword, realm);
            Set valSet = new HashSet();
            valSet.add(lockAttrName);
            authAttrMap.put("iplanet-am-auth-lockout-attribute-name", valSet);
            valSet = new HashSet();
            valSet.add(lockAttrValue);
            smsc = new SMSCommon(serviceToken);
            smsc.updateServiceAttrsRealm(serviceName, testRealm, authAttrMap);

            executeMap.put("Loginuser", lockAttrUser);
            executeMap.put("Loginpassword", lockAttrUserpass);
            executeMap.put("Loginattempts", lockoutAttempts);
            executeMap.put("Passmsg", lockoutPassmsg);
            executeMap.put("loginurl", testURL);
            executeMap.put("failpage", failpage);

            testAccountLockoutUserAttr(executeMap, lockAttrUser,
                    lockAttrName, lockAttrValue);
            exiting("validateAccountLockUserAttrTest");
        } catch (Exception e) {
            log(Level.FINE, "validateAccountLockUserAttrTest", e.getMessage());
            e.printStackTrace();
            assert false;
            cleanup(testRealm);
            throw e;
        } finally {
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }

    }

    /**
     * Validate the nsaccountlock attribute will not change the value after 
     * lock out
     * @param testRealm - the realm in which the testing should be exected.
     */
    @Parameters({"testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateNsAccountLockTest(String testRealm)
    throws Exception {
        entering("validateNsAccountLockTest", null);
        Map executeMap = new HashMap();
        try {
            log(Level.FINEST, "validateNsAccountLockTest", "lockUser = " +
                    lockUser);
            log(Level.FINEST, "validateNsAccountLockTest",
                    "testCaseName = validateNsAccountLockTest");
            log(Level.FINEST, "validateNsAccountLockTest", "loginURL = " +
                    testURL);
            log(Level.FINEST, "validateNsAccountLockTest",
                    "test case description = This test is to verify the " +
                    "nsaccountlockout attribute change after lockout");

            Reporter.log("Loginuser: " + lockUser);
            Reporter.log("TestCaseName: validateNsAccountLockTest");
            Reporter.log("LoginUrl: " + testURL);
            Reporter.log("TestCaseDescription: This test is to verify the " +
                    "nsaccountlockout attribute change after lockout ");

            executeMap.put("Loginuser", nslockUser);
            executeMap.put("Loginpassword", nslockUserpass);
            executeMap.put("Loginattempts", lockoutAttempts);
            executeMap.put("Passmsg", lockoutPassmsg);
            executeMap.put("loginurl", testURL);
            executeMap.put("failpage", failpage);

            testAccountLockoutUserAttr(executeMap, nslockUser,
                    nslockAttrName, nslockAttrValue);
            exiting("validateNsAccountLockTest");
        } catch (Exception e) {
            log(Level.FINE, "validateNsAccountLockTest", e.getMessage());
            e.printStackTrace();
            assert false;
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

        try {
            serviceToken = getToken(adminUser, adminPassword, realm);

            Iterator listIter = testUserList.iterator();
            while (listIter.hasNext()) {
                String userToDelete = (String)listIter.next();
                log(Level.FINE, "cleanup", "Deleting user " +
                        userToDelete + " in realm " + testRealm + "...");
                idmc.deleteIdentity(serviceToken, testRealm, IdType.USER,
                        userToDelete);
            }

            smsc = new SMSCommon(serviceToken);
            log(Level.FINE, "setup", "Restoring attribute values in " + 
                    serviceName + " to " + authServiceAttrs);
            smsc.updateServiceAttrsRealm(serviceName, testRealm,
                    authServiceAttrs);
            if (!testRealm.equals("/")) {
                log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                        testRealm);
                idmc.deleteRealm(serviceToken, realm + testRealm);
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }
    }
}
