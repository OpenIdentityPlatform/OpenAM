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
 * $Id: RedirectTest.java,v 1.14 2009/07/02 19:32:38 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Each RedirectTest will have a module instance name and the realm
 * associated with it to perform the test.
 * This class does the following :
 * - Create the realm
 * - Create the module instances for that realm
 * - Create users to Login if required to create.
 * - Validates for module based authentication pass and failure case
 * - Validates for the goto and gotoOnFail URL for this instance.
 *
 * RedirectTest automates the following test cases:
 * OpenSSO_AuthModule(LDAP)_30a-d, OpenSSO_AuthModule(LDAP)_31a-d,
 * OpenSSO_AuthModule(NT)_9a-d, OpenSSO_AuthModule(NT)_10a-d,
 * OpenSSO_AuthModule(JDBC)_9a-d, OpenSSO_AuthModule(JDBC)_10a-d
 * OpenSSO_AuthModule(ActiveDirectory)_15a-d,
 * OpenSSO_AuthModule(ActiveDirectory)_16a-d,
 * OpenSSO_AuthModule(DataStore)_7a-d, OpenSSO_AuthModule(DataStore)_8a-d,
 * OpenSSO_AuthModule(RADIUS)_10a-d, OpenSSO_AuthModule(RADIUS)_11a-d,
 * OpenSSO_AuthModule(Membership)_17a-d, OpenSSO_AuthModule(Membership)_18a-d,
 * OpenSSO_AuthModule(Unix)_11a-d, and OpenSSO_AuthModule(Unix)_12a-d.
 */
public class RedirectTest extends AuthenticationCommon {
    private IDMCommon idmc;
    private ResourceBundle testResources;
    private String moduleOnSuccess;
    private String moduleOnFail;
    private String modulePassMsg;
    private String moduleFailMsg;
    private String moduleGotoPassMsg;
    private String moduleGotoOnFailMsg;
    private String createUserProp;
    private boolean userExists;
    private String cleanupFlag;
    private boolean debug;
    private String userName;
    private String password;
    private String redirectURL;
    private String uniqueIdentifier;
    private String moduleSubConfig;
    private List<String> testUserList = new ArrayList<String>();
    private boolean isValidTest = true;
    private SSOToken adminToken;
    
    /**
     * Default Constructor
     **/
    public RedirectTest() {
        super();  
    }
        
    /**
     * Reads the necessary test configuration and prepares the system
     * for module/goto/gotoOnFail redirection tests.The following are done
     * in the setup
     * - Create realm
     * - Create Users , If needed
     */
    @Parameters({"testModule", "testRealm", "instanceIndex"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testModule, String testRealm,
            String instanceIndex)
    throws Exception {
        Object[] params = {testModule, testRealm, instanceIndex};
        entering("setup", params);
        try {
            isValidTest = isValidModuleTest(testModule);
            if (isValidTest) {
                testResources = ResourceBundle.getBundle("authentication" +
                        fileseparator + "RedirectTest");
                moduleOnSuccess = testResources.getString("am-auth-test-" +
                        testModule + "-goto");
                moduleOnFail = testResources.getString("am-auth-test-" +
                        testModule + "-gotoOnFail");
                modulePassMsg = testResources.getString("am-auth-test-" +
                        testModule + "-module-passmsg");
                moduleFailMsg = testResources.getString("am-auth-test-" +
                        testModule + "-module-failmsg");
                moduleGotoPassMsg = testResources.getString("am-auth-test-" +
                        testModule + "-goto-passmsg");
                moduleGotoOnFailMsg = testResources.getString("am-auth-test-" +
                        testModule + "-gotoOnFail-passmsg");
                moduleSubConfig = this.getAuthInstanceName(testModule,
                        instanceIndex);
                userName = testResources.getString("am-auth-test-" +
                        testModule + "-user");
                userName.trim();
                password = testResources.getString("am-auth-test-" +
                        testModule + "-password");
                password.trim();
                cleanupFlag = testResources.getString("am-auth-test-debug");
                debug = new Boolean(cleanupFlag).booleanValue();
                
                log(Level.FINEST, "setup", "ModuleName: " + testModule);
                log(Level.FINEST, "setup", "RealmName: " + testRealm);
                log(Level.FINEST, "setup", "Success URL: " + moduleOnSuccess);
                log(Level.FINEST, "setup", "Failure URL: " + moduleOnFail);
                log(Level.FINEST, "setup", "modulePassMsg: " + modulePassMsg);
                log(Level.FINEST, "setup", "moduleFailMsg: " + moduleFailMsg);
                log(Level.FINEST, "setup", "modulePassMsg: " +
                        moduleGotoPassMsg);
                log(Level.FINEST, "setup", "modulePassMsg: " +
                        moduleGotoOnFailMsg);
                log(Level.FINEST, "setup", "userName: " + userName);
                log(Level.FINEST, "setup", "password: " + password);
                log(Level.FINEST, "setup", "debug: " + debug);
                
                Reporter.log("testModuleName: " + testModule);
                Reporter.log("Realm: " + testRealm);
                Reporter.log("testModuleName: " + moduleOnSuccess);
                Reporter.log("moduleOnFail: " + moduleOnFail);
                Reporter.log("modulePassMsg: " + modulePassMsg);
                Reporter.log("moduleFailMsg: " + moduleFailMsg);
                Reporter.log("moduleGotoPassMsg: " + moduleGotoPassMsg);
                Reporter.log("moduleGotoFailMsg: " + moduleGotoOnFailMsg);
                Reporter.log("moduleuserName: " + userName);
                Reporter.log("modulepassword: " + password);
                Reporter.log("cleanupFlag: " + debug);

                adminToken = getToken(adminUser, adminPassword, basedn);
                idmc = new IDMCommon();
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
                    uniqueIdentifier = testRealm + "_" + testModule;
                    redirectURL = getLoginURL(testRealm) + "&amp;"
                            + "module=" + moduleSubConfig;
                } else {
                    redirectURL = getLoginURL(testRealm) + "?" + "module="
                            + moduleSubConfig;
                    uniqueIdentifier = "rootrealm" + "_" + testModule;
                }

                createUserProp = testResources.getString("am-auth-test-" +
                        testModule + "-createTestUser");
                userExists = new Boolean(createUserProp).booleanValue();
                testUserList.add(userName);
                if (!userExists) {
                    StringBuffer attrBuffer =
                            new StringBuffer("sn=" + userName).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("cn=" + userName).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("userpassword=" + password).
                            append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                            append("inetuserstatus=Active");

                    log(Level.FINE, "setup",
                            "Creating user " + userName + " ...");
                    if (!idmc.createID(userName, "user", attrBuffer.toString(),
                            adminToken, testRealm)) {
                        log(Level.SEVERE, "createUser", "Failed to create " +
                                "user identity " + userName + "...");
                        assert false;
                    }
                }
            } else {
                throw new SkipException("Skipping setup for " + testModule +
                        " auth module test on unsupported platform or " +
                        "an OpenSSO nightly build.");
            }
            exiting("setup");
        } catch (SkipException se) {
            log(Level.FINEST, "setup", se.getMessage());
        } catch (Exception e) {
            log(Level.SEVERE, "setup", 
                    "Unexpected setup failure has occured ...");
            cleanup(testModule, testRealm); 
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }
    }
    
    /**
     * Validate positive module based authentication with the correct user
     * and password.
     * @param testModule - the authentication module used for authentication.
     * @param testRealm - the realm in which the authentication should be
     * performed.
     */
    @Parameters({"testModule", "testRealm"})    
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateModuleTest(String testModule, String testRealm)
    throws Exception {
        Object[] params = {testModule, testRealm};        
        entering("validateModuleTest", params);
        if (isValidTest) {
            Map executeMap = new HashMap();
            executeMap.put("redirectURL", redirectURL);
            executeMap.put("users", userName + ":" + password);
            executeMap.put("successMsg", modulePassMsg);
            executeMap.put("uniqueIdentifier", uniqueIdentifier + "_positive");

            testFormBasedAuth(executeMap);
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
        exiting("validateModuleTest");
    }
    
    /**
     * Validate the module based authentication for the module
     * under test for incorrect user and password behaviour
     * @param testModule - the authentication module used for authentication.
     * @param testRealm - the realm in which the authentication should be
     * performed.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateModuleTestNegative(String testModule, String testRealm)
    throws Exception {
        Object[] params = {testModule, testRealm};
        entering("validateModuleTestNegative", params);
        if (isValidTest) {
            Map executeMap = new HashMap();
            executeMap.put("redirectURL", redirectURL);
            executeMap.put("users", userName + ":not" + password);
            executeMap.put("successMsg", moduleFailMsg);
            executeMap.put("uniqueIdentifier", uniqueIdentifier + "_negative");
            testFormBasedAuth(executeMap);
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
        exiting("validateModuleTestNegative");
    }

    /**
     * Validate the module based authentication for the module
     * under test for "goto" param when auth success behaviour.
     * @param testModule - the authentication module used for authentication.
     * @param testRealm - the realm in which the authentication should be
     * performed.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateGotoTest(String testModule, String testRealm)
    throws Exception {
        Object params[] = {testModule, testRealm};
        entering("validateGotoTests", params);
        if (isValidTest) {
            Map executeMap = new HashMap();
            String gotoURL = redirectURL + "&amp;goto=" + moduleOnSuccess;
            executeMap.put("redirectURL", gotoURL);
            executeMap.put("users", userName + ":" + password);
            executeMap.put("successMsg", moduleGotoPassMsg);
            executeMap.put("uniqueIdentifier", uniqueIdentifier + "_goto");
            testFormBasedAuth(executeMap);
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
        exiting("validateGotoTests");
    }

    /**
     * Validate the module based authentication for the module
     * under test for "GotoOnfail" param with unsuccessful authentication
     * behaviour
     * @param testModule - the authentication module used for authentication.
     * @param testRealm - the realm in which the authentication should be
     * performed.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateGotoOnFailTest(String testModule, String testRealm)
    throws Exception {
        Object[] params = {testModule, testRealm};
        entering("validateGotoOnFailTests", params);
        if (isValidTest) {
            Map executeMap = new HashMap();
            String gotoURL = redirectURL + "&amp;gotoOnFail=" + moduleOnFail;
            executeMap.put("redirectURL", gotoURL);
            executeMap.put("users", userName + ":not" + password);
            executeMap.put("successMsg", moduleGotoOnFailMsg);
            executeMap.put("uniqueIdentifier", 
                    uniqueIdentifier + "_gotoOnFail");
            testFormBasedAuth(executeMap);
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
        exiting("validateGotoOnFailTests");
    }
    
    /**
     * Clean up is called post execution of each module and realm .
     * This is done to maintain the system in a clean state
     * after executing each test scenario, in this case each module based
     * authentication tests for all possible cases.
     * This processed in this method are:
     * 1. Delete the authentication module
     * 2. Delete the realm involved only if it is not root realm
     * 3. Delete the users involved/created for this test if any.
     */
    @Parameters({"testModule", "testRealm"})        
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testModule, String testRealm)
    throws Exception {
        Object[] params = {testModule, testRealm};
        entering("cleanup", params);
        if (isValidTest) {
            if (!debug) {
                try {
                    log(Level.FINEST, "cleanup", "TestRealm: " + testRealm);
                    log(Level.FINEST, "cleanup", "TestModule: " + testModule);

                    Reporter.log("TestRealm: " + testRealm);
                    Reporter.log("TestModule: " + testModule);

                    String delRealm = testRealm;
                    if (!testRealm.equals("/")) {
                        delRealm = "/" + testRealm;
                    }
                    adminToken = getToken(adminUser, adminPassword, basedn);
                    if ((testUserList != null) && !testUserList.isEmpty()) {
                        log(Level.FINE, "cleanup", "Deleting user " + userName + 
                                " from realm " + delRealm + " ...");
                        idmc.deleteIdentity(adminToken, testRealm,
                                IdType.USER, userName);
                    }

                    if (!testRealm.equals("/")) {
                        log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                                testRealm);
                        idmc.deleteRealm(adminToken, "/" + testRealm);
                    }
                    exiting("cleanup");
                } catch(Exception e) {
                    log(Level.SEVERE, "cleanup", e.getMessage());
                    e.printStackTrace();
                    throw e;
                } finally {
                    if (adminToken != null) {
                        destroyToken(adminToken);
                    }
                }
            } else {
                log(Level.FINEST, "cleanup", 
                        "Debug flag was set cleanup method was not performed");
            }
        } else {
            log(Level.FINEST, "setup", "Skipping cleanup for " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
        }
    }
}
