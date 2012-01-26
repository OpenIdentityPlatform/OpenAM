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
 * $Id: ChainTest.java,v 1.12 2009/06/02 17:08:18 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * This class automates the following test cases:
 * AccessManager_AuthModule(JAASSS)_1, AccessManager_AuthModule(JAASSS)_2,
 * AccessManager_AuthModule(JAASSS)_3, AccessManager_AuthModule(JAASSS)_4,
 * AccessManager_AuthModule(JAASSS)_5, AccessManager_AuthModule(JAASSS)_6,
 * AccessManager_AuthModule(JAASSS)_7, AccessManager_AuthModule(JAASSS)_8,
 * AccessManager_AuthModule(JAASSS)_9, AccessManager_AuthModule(JAASSS)_10,
 * AccessManager_AuthModule(JAASSS)_11, AccessManager_AuthModule(JAASSS)_12,
 * AccessManager_AuthModule(JAASSS)_13, AccessManager_AuthModule(JAASSS)_14
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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class drives the chaining tests, Each Chain can have the number of
 * module instances.
 * Creates the chain test setup based on the test chain
 * mentioned in the test resource bundle, The following are performed in order
 * to test chaining.
 * - Create the module instances by the given module instance names.
 * - Create services (Chain) for the number of modules involved in the chain.
 * - Validates each chain
 */
public class ChainTest extends AuthenticationCommon {
    private IDMCommon idmc;
    private ResourceBundle testResources;
    private SSOToken idToken;
    private String chainService;
    private String chainUserNames;
    private String chainModInstances;
    private String chainSuccessURL;
    private String chainFailureURL;
    private String testDescription;
    private String redirectURL;
    private static String SUCCESS_URL_PROPERTY_NAME =
            "iplanet-am-auth-login-success-url";
    private static String FAILURE_URL_PROPERTY_NAME =
            "iplanet-am-auth-login-failure-url";
    private int numOfSufficientUsers = -1;
    private List<String> testUserList = new ArrayList<String>();
    private List<IdType> idTypeList = new ArrayList<IdType>();
    
    /**
     * Default Constructor
     **/
    public ChainTest() {
       super("ChainTest");
       idmc = new IDMCommon();
    }
    
    /**
     * Creates the necessary configuration for the chain suchs as 
     * creating module instances and chain service by the given service name
     * before performing the tests.
     */
    @Parameters({"testChainName", "testRealm"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
       "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testChainName, String testRealm)
    throws Exception {
        Object[] params = {testChainName, testRealm};
        entering("setup", params);

        testResources = ResourceBundle.getBundle("authentication" +
                fileseparator + "ChainTest");
        chainService = testResources.getString("am-auth-test-" +
                testChainName + "-servicename");
        chainUserNames = testResources.getString("am-auth-test-" +
                testChainName+ "-users");
        chainModInstances = testResources.getString("am-auth-test-" +
                testChainName+ "-instances");
        chainSuccessURL = testResources.getString("am-auth-test-" + 
                testChainName + "-successURL");
        chainFailureURL = testResources.getString("am-auth-test-" + 
                testChainName + "-failureURL");
        testDescription = testResources.getString("am-auth-test-" + 
                testChainName + "-description");
        try {
            numOfSufficientUsers = Integer.parseInt(
                    testResources.getString("am-auth-test-" +
                    testChainName + "-num-of-sufficient-users"));
        } catch (MissingResourceException mre) {
            log(Level.FINEST, "setup", "Parameter am-auth-test-" +
                    testChainName + "-num-of-sufficient-users was not used.");
        }

        log(Level.FINEST, "setup", "testChainName: " + testChainName);
        log(Level.FINEST, "setup", "Description of Chain Test: " + 
                testDescription);
        log(Level.FINEST, "setup", "Service Name for Chain: " + chainService);
        log(Level.FINEST, "setup", "Users for the Chain: " +
                chainUserNames);
        log(Level.FINEST, "setup", "Module Instances for Chain: " +
                chainModInstances);
        log(Level.FINEST, "setup", "SuccessURL for the Chain: " 
                + chainSuccessURL);
        log(Level.FINEST, "setup", "Failure URL for the Chain: " 
                + chainFailureURL);

        Reporter.log("ChainName: " + testChainName);
        Reporter.log("Test Description: " + testDescription);
        Reporter.log("Service Name for Chain: " + chainService);
        Reporter.log("Users for the Chain: " + chainUserNames);
        Reporter.log("Module Intances for Chain: " + chainModInstances);
        Reporter.log("SuccessURL for the Chain: " + chainSuccessURL);
        Reporter.log("Failure URL for the Chain: " + chainFailureURL);
        
        try {
            idToken = getToken(adminUser, adminPassword, basedn);
            if (!testRealm.equals("/")) {
                Map realmAttrMap = new HashMap();
                Set realmSet = new HashSet();
                realmSet.add("Active");
                realmAttrMap.put("sunOrganizationStatus", realmSet);
                log(Level.FINE, "setup", "Creating the realm " + testRealm);
                AMIdentity amid = idmc.createIdentity(idToken,
                        realm, IdType.REALM, testRealm, realmAttrMap);
                log(Level.FINE, "setup",
                        "Verifying the existence of sub-realm " +
                        testRealm);
                if (amid == null) {
                    log(Level.SEVERE, "setup", "Creation of sub-realm " +
                            testRealm + " failed!");
                    assert false;
                }
                redirectURL = getLoginURL(testRealm) +
                        "&amp;service=" + chainService;
            } else {
                redirectURL = getLoginURL(testRealm) +
                        "?service=" + chainService;

            }

            Map configMap = new HashMap();
            configMap.put(SUCCESS_URL_PROPERTY_NAME, chainSuccessURL);
            configMap.put(FAILURE_URL_PROPERTY_NAME, chainFailureURL);
            String[] configInstances = chainModInstances.split("\\|");
            createAuthConfig(testRealm, chainService,
                    configInstances, configMap);

            String[] userTokens = chainUserNames.split("\\|");
            for (String userEntry: userTokens){
                int uLength = userEntry.length();
                int uIndex = userEntry.indexOf(":");
                String userName = userEntry.substring(0, uIndex);
                String userPass = userEntry.substring(uIndex + 1, uLength);
                testUserList.add(userName);
                idTypeList.add(IdType.USER);
                String aliasName = testResources.getString("am-auth-test-" +
                        testChainName + "-" + userName + "-alias");
                StringBuffer attrBuffer = new StringBuffer("sn=" + userName).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + userName).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + userPass).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("iplanet-am-user-alias-list=" + aliasName);

                log(Level.FINE, "setup", "Creating user " + userName + " ...");
                if (!idmc.createID(userName, "user", attrBuffer.toString(),
                        idToken, testRealm)) {
                    log(Level.SEVERE, "setup", "Failed to create user " +
                            "identity " + userName + " in realm " + testRealm +
                            "...");
                    assert false;
                }
            }
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", "Exception stack msg = " +
                    e.getMessage());
            e.printStackTrace();
            cleanup(testChainName, testRealm);
            throw e;            
        } finally {
            if (idToken != null) {
                destroyToken(idToken);
            }
        }
        
    }

    /**
     * Peform the test validation for this chain/service. This method
     * performs the positive test validation by calling
     * <code>AuthenticationCommon.testFormBasedAuth()</code>.
     * @param testChainName - the name of the authentication configuration that
     * will be used for authentication.
     */
    @Parameters({"testChainName", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validatePositiveTests(String testChainName, String testRealm)
    throws Exception {
        Object[] params = {testChainName, testRealm};
        entering("validatePositiveTests", params);

        try {
            Map executeMap = new HashMap();
            log(Level.FINEST, "validatePositiveTests", "redirectURL = " +
                    redirectURL);
            executeMap.put("redirectURL", redirectURL);
            executeMap.put("successMsg", testResources.getString(
                    "am-auth-test-" + testChainName + "-passmsg"));
            executeMap.put("uniqueIdentifier", testChainName + "-positive");
            String testUsers = testResources.getString("am-auth-test-" +
                    testChainName + "-users");
            if (numOfSufficientUsers == -1) {
                executeMap.put("users", testUsers);
            } else {
                String[] sufficientUsers = testUsers.split("\\|");
                StringBuffer userBuffer = new StringBuffer();
                for (int i=0; i < numOfSufficientUsers &&
                        i < sufficientUsers.length; ) {
                    userBuffer.append(sufficientUsers[i++]);
                    if (i < numOfSufficientUsers) {
                        userBuffer.append("|");
                    }
                }
                executeMap.put("users", userBuffer.toString());
            }
            executeMap.put("servicename", testResources.getString(
                    "am-auth-test-" + testChainName + "-servicename"));
            log(Level.FINEST, "validatePositiveTests",
                    "ExecuteMap: " + executeMap);
            log(Level.FINEST, "validatePositiveTests", "TestDescription: " +
                    testDescription);
            Reporter.log("Test Description: " + testDescription);

            testServicebasedPositive(executeMap);
            exiting("validatePositiveTests");
        } catch (Exception e) {
            log(Level.SEVERE, "validatePositiveTests", e.getMessage());
            e.printStackTrace();
            cleanup(testChainName, testRealm);
            throw e;
        }
    }
    
    /**
     * Peform the test validation for this chain/service. This method
     * performs the Negative test validation by calling
     * <code>AuthenticationCommon.testFormBasedAuth()</code>.
     * @param testChainName - the name of the authentication configuration that
     * will be used for authentication.
     */
    @Parameters({"testChainName", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void validateNegativeTests(String testChainName, String testRealm)
    throws Exception {
        Object[] params = {testChainName};
        entering("validateNegativeTests", params);

        try {
            Map executeMap = new HashMap();
            log(Level.FINEST, "validateNegativeTests", "redirectURL = " +
                    redirectURL);
            executeMap.put("redirectURL", redirectURL);
            executeMap.put("successMsg", testResources.getString(
                    "am-auth-test-" + testChainName + "-failmsg"));
            executeMap.put("uniqueIdentifier", testChainName + "-negative");

            String configUsers = testResources.getString("am-auth-test-" +
                    testChainName + "-users");
            String[] userEntries = configUsers.split("\\|");
            StringBuffer updatedUsers = new StringBuffer();
            for (int i=0; i < userEntries.length; i++) {
                String userEntry = userEntries[i];
                String[] userTokens = userEntry.split(":");
                String negativePassword = "not" + userTokens[1];
                updatedUsers.append(userTokens[0]).append(":");
                updatedUsers.append(negativePassword);
                if (i < (userEntries.length - 1)) {
                    updatedUsers.append("|");
                }
            }
            executeMap.put("users", updatedUsers.toString());
            executeMap.put("servicename", testResources.getString(
                    "am-auth-test-" + testChainName + "-servicename"));
            log(Level.FINEST, "validateNegativeTests", "ExecuteMap:" +
                    executeMap);
            log(Level.FINEST, "validateNegativeTests", "TestDescription: " +
                    testDescription);
            Reporter.log("Test Description: " + testDescription);

            testServicebasedNegative(executeMap);
            exiting("validateNegativeTests");
        } catch (Exception e) {
            log(Level.SEVERE, "validateNegativeTests", e.getMessage());
            e.printStackTrace();
            cleanup(testChainName, testRealm);
            throw e;
        }
    }

    /**
     * Clean up is called post execution of each chain before entering into
     * the next chain, This is done to maintain the system in a clean state
     * after executing each test scenario, in this case each chain/service
     * authentication is done.This processed in this method are:
     * 1. Delete the authentication service/Chain
     * 2. Delete the users involved/create for this chain.
     */
    @Parameters({"testChainName", "testRealm"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testChainName, String testRealm)
        throws Exception {
        Object[] params = {testChainName, testRealm};
        entering("cleanup", params);

        try {
            idToken = getToken(adminUser, adminPassword, basedn);

            log(Level.FINEST, "cleanup", "chainName: " + testChainName);
            log(Level.FINEST, "cleanup", "testRealm: " + testRealm);
            
            Reporter.log("chainName:" + testChainName);
            Reporter.log("Realm: " + testRealm);
 
            log(Level.FINE, "cleanup", "Deleting auth configuration " + 
                    chainService + " ...");
            deleteAuthConfig(testRealm, chainService);
            
            log(Level.FINE, "cleanup", "Deleting user(s) " + testUserList + 
                    " ...");
            idmc.deleteIdentity(idToken, testRealm, idTypeList, testUserList);

            if (!testRealm.equals("/")) {
                log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                        testRealm);
                idmc.deleteRealm(idToken, realm + testRealm);
            }
            exiting("cleanup");
        } catch(Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (idToken != null) {
                destroyToken(idToken);
            }
        }
    }
}
