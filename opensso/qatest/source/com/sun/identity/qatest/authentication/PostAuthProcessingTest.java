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
 * $Id: PostAuthProcessingTest.java,v 1.8 2009/08/20 11:20:58 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
 * This class performs the following tests:
 * 1) setting various user profile attributes in the <code>SSOToken</code> using
 * the "User Attribute Mapping to Session Attributes" feature of the 
 * Authentication service.  After a successful authentication, the attributes
 * can be retrieved using <code>SSOToken.getProperty()</code>.
 * 2) attempting to modify user profile attributes which have already been set 
 * in the <code>SSOToken</code>.  This should result in an 
 * <code>SSOException</code>.
 */
public class PostAuthProcessingTest extends AuthenticationCommon {

    private ResourceBundle testResources;
    private Set oriAuthAttrValues;
    private Set userProfileValSet; 
    private SSOToken adminToken;
    private String moduleInstance;
    private String strServiceName = "iPlanetAMAuthService";
    private String mappingAttrName = "sunAMUserAttributesSessionMapping";
    private IDMCommon idmc;
    private String testUserName;
    private String testUserPass;
    private int noOfAttributes;
    private Map userAttrMap;

    /**
     * Default Constructor
     */ 
    public PostAuthProcessingTest() {
        super("PostAuthProcessingTest");
        testResources = ResourceBundle.getBundle("authentication" +
                fileseparator + "PostAuthProcessingTest");
        idmc = new IDMCommon();
        userProfileValSet = new HashSet();
    }

    /**
     * Reads the necessary test configuration and prepares the system
     * for Authentication related properties for testing.
     * Create module instance and create users with required attributes.
     */
    @Parameters({"testModule", "testRealm"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testModule, String testRealm) 
    throws Exception {
        Object[] params = {testModule, testRealm};
        entering("setup", params);
        SSOToken realmToken = null;
        SSOToken idToken = null;
        SSOToken serviceToken = null;
        
        try {
            testUserName = testResources.getString("am-auth-postprocessing-" + 
                    "username");
            testUserPass = testResources.getString("am-auth-postprocessing-" +
                    "password");
            
            log(Level.FINEST, "setup", "testRealm: " + testRealm);
            log(Level.FINEST, "setup", "testModule: " + testModule);
            log(Level.FINEST, "setup", "userName: " + testUserName);
            log(Level.FINEST, "setup", "userPassword: " + testUserPass);

            Reporter.log("TestRealm: " + testRealm);
            Reporter.log("TestModule: " + testModule);
            Reporter.log("UserName: " + testUserName);
            Reporter.log("UserPassword: " + testUserPass);
            
            String absoluteRealm = testRealm;
            if (!testRealm.equals("/")) {
                if (testRealm.indexOf("/") != 0) {
                    absoluteRealm = "/" + testRealm;
                }
                log(Level.FINE, "setup", "Creating the sub-realm " + testRealm);
                realmToken = getToken(adminUser, adminPassword, realm);
                String childRealm = absoluteRealm.substring(
                        absoluteRealm.lastIndexOf("/") + 1);
                String parentRealm = idmc.getParentRealm(absoluteRealm);
                Map realmAttrMap = new HashMap();
                Set realmSet = new HashSet();
                realmSet.add("Active");
                realmAttrMap.put("sunOrganizationStatus", realmSet);
                if (realmToken != null) {
                    log(Level.FINEST, "setup",
                            "SSOToken used for realm creation = " + realmToken);
                } else {
                    log(Level.SEVERE, "setup",
                            "The SSOToken for realm creation is null!");
                }
                log(Level.FINEST, "setup",
                        "Parent realm for realm creaetion = " + parentRealm);
                log(Level.FINEST, "setup", "Realm entity name to create = " +
                        childRealm);
                AMIdentity amid = idmc.createIdentity(realmToken, parentRealm,
                        IdType.REALM, childRealm, realmAttrMap);
                log(Level.FINE, "setup", "Searching for the sub-realm " +
                        testRealm);
                if (amid == null) {
                    log(Level.SEVERE, "setup", "Creation of sub-realm " +
                            testRealm + " failed!");
                    assert false;
                }
            }
            
            noOfAttributes = new Integer(testResources.getString(
                    "am-auth-postprocessing-noofattributes")).intValue();
            userAttrMap = new HashMap();
            Set valSet = new HashSet();
            valSet.add(testUserPass);
            userAttrMap.put("userpassword", valSet);

            StringBuffer userAttrs = new StringBuffer("userpassword=");
            userAttrs.append(testUserPass);
            userAttrs.append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
            for (int i = 0; i < noOfAttributes; i++) {
                String testAttrName = (testResources.getString(
                        "am-auth-postprocessing-attr" + i + "-name")).trim();
                String testAttrValue = (testResources.getString(
                        "am-auth-postprocessing-attr" + i + "-value")).trim();
                userAttrs.append(testAttrName).append("=");
                userAttrs.append(testAttrValue);
                if (i < noOfAttributes - 1) {
                    userAttrs.append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
                }
                valSet = new HashSet();
                valSet.add(testAttrValue);
                log(Level.FINE, "setup", "Setting user attribute " +
                        testAttrName + " to \'" + testAttrValue + "\'.");
                userAttrMap.put(testAttrName, valSet);
                userProfileValSet.add(testAttrName + "|user." + testAttrName);
            }

            moduleInstance = getAuthInstanceName(testModule);
            Thread.sleep(notificationSleepTime);
            log(Level.FINE, "setup", "Creating user " + testUserName + 
                    " in realm " + testRealm + " with attributes " +
                    userAttrs + " ...");
            idToken = getToken(adminUser, adminPassword, realm);
            if (!idmc.createID(testUserName, "user", userAttrs.toString(),
                    idToken, testRealm)) {
                log(Level.SEVERE, "setup",
                        "Failed to create user identity " +
                        testUserName + " ...");
                assert false;
            }

            serviceToken = getToken(adminUser, adminPassword, realm);
            SMSCommon smsc = new SMSCommon(serviceToken);
            log(Level.FINE, "setup", "Retrieving the attribute value of " + 
                    mappingAttrName + " from " + strServiceName + " in realm " +
                    testRealm + "...");
            oriAuthAttrValues = (Set) smsc.getAttributeValue(testRealm, 
                    strServiceName, mappingAttrName, "Organization");
            log(Level.FINEST, "setup", "Original value of " + mappingAttrName +
                    ": "  + oriAuthAttrValues);
            log(Level.FINE, "setup", "Set " + mappingAttrName + " to " + 
                    userProfileValSet);
            smsc.updateSvcAttribute(testRealm, strServiceName, mappingAttrName,
                    userProfileValSet, "Organization");
            exiting("setup");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", "Exception message = " + e.getMessage());
            e.printStackTrace();
            if (oriAuthAttrValues == null) {
                oriAuthAttrValues = new HashSet();
            }            
            cleanup(testRealm);
            throw e;
        } finally {
            if (realmToken != null) {
                destroyToken(realmToken);
            }
            if (idToken != null) {
                destroyToken(idToken);
            }
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }
    }

    /**
     * This test sets user profile attributes in the user's SSOToken.  The
     * test then retrieves these attributes from the <code>SSOToken</code>
     * using the prefix "am.protected".
     * @param testModule - the authentication module instance type which will
     * be used by the user to be authenticated.
     * @param testRealm - the name of the realm in which the user will be
     * authenticated.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUserProfileMapping(String testModule, String testRealm) 
    throws Exception {
        boolean userPropsFound = true;
        SSOToken userToken  = null;
        String testCaseName = testResources.getString(
                "am-auth-postprocessing-test1-name");
        String testCaseDesc = testResources.getString(
                "am-auth-postprocessing-test1-description");
        log(Level.FINEST, "setup", "testCaseName: " + testCaseName);
        log(Level.FINEST, "setup", "testCaseDescription: " + testCaseDesc);
        Reporter.log("TestCaseName: " + testCaseName);
        Reporter.log("TestCaseDescription: " + testCaseDesc);
        
        Object[] params = {testRealm};
        entering("testUserProfileMapping", params);

        try {
            userToken = performRemoteLogin(testRealm, "module",
                            moduleInstance, testUserName, testUserPass);

            if (userToken != null) {
                for (Iterator i = userProfileValSet.iterator(); i.hasNext(); ) {
                    String mapping = (String) i.next();
                    log(Level.FINE, "testUserProfileMapping", 
                            "Obtained mapping " + mapping + 
                            " from user profile mapping set");
                    String[] mappingAttrs = mapping.split("\\|");
                    String profileAttrName = mappingAttrs[0];
                    String sessionAttrName = "am.protected." + mappingAttrs[1];
                    log(Level.FINE, "testUserProfileMapping", 
                            "Retrieving user profile attribute " + 
                            profileAttrName + " ...");
                    String expectedAttrValue = 
                            (String) ((Set)userAttrMap.get(profileAttrName)).
                            iterator().next();
                    log(Level.FINEST, "testUserProfileMapping", 
                            "User profile attribute " + profileAttrName + 
                            " = " + expectedAttrValue);
                    log(Level.FINE, "testUserProfileMapping",
                            "Retrieving session attribute " + sessionAttrName);
                    String retrievedValue =
                            userToken.getProperty(sessionAttrName);
                    log(Level.FINEST, "testUserProfileMapping",
                            "SSOToken attribute " + sessionAttrName + " = " +
                            retrievedValue);
                    if (retrievedValue.equals(expectedAttrValue)) {
                        log(Level.FINEST, "testUserProfileMapping",
                                "Session attribute " + sessionAttrName +
                                " has the value \'" + expectedAttrValue + "\'");
                    } else {
                        log(Level.SEVERE, "testUserProfileMapping",
                                "Session attribute " + sessionAttrName +
                                " had a value of \'" + retrievedValue +
                                "\' instead of expected value \'" +
                                expectedAttrValue + "\'.");
                        userPropsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "testUserProfileMapping",
                        "SSOToken for user " + testUserName +
                        " was not retrieved!");
                userPropsFound = false;
            }
            exiting("testUserProfileMapping");
        } catch (Exception e) {
            log(Level.SEVERE, "loginModule", "Exception message: " +
                    e.getMessage());
            log(Level.SEVERE, "loginModule", testUserName +
                    " login resulted in an exception.");
            cleanup(testRealm);
            throw e;
        } finally {
            if (userToken != null) {
                destroyToken(userToken);
            }
            assert userPropsFound;
        }
    }
    
    /**
     * Attempt to modify the value of a protected user profile attribute in
     * the <code>SSOToken</code>.
     * @param testModule - the authentication module instance type which will
     * be used by the user to be authenticated.
     * @param testRealm - the name of the realm in which the user will be
     * authenticated.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSessionAttrModification(String testModule, String testRealm) 
    throws Exception {
        boolean propertyUnchanged = false;
        String sessionAttrName = null;
        String expectedAttrValue = null;
        SSOToken userToken = null;
        Object[] params = {testRealm};

        entering("testSessionAttrModification", params);
        String testCaseName = testResources.getString(
                "am-auth-postprocessing-test2-name");
        String testCaseDesc = testResources.getString(
                "am-auth-postprocessing-test2-description");
        log(Level.FINEST, "setup", "testCaseName: " + testCaseName);
        log(Level.FINEST, "setup", "testCaseDescription: " + testCaseDesc);
        Reporter.log("TestCaseName: " + testCaseName);
        Reporter.log("TestCaseDescription: " + testCaseDesc);

        try {
            userToken = performRemoteLogin(testRealm, "module",
                            moduleInstance, testUserName, testUserPass);

            if (userToken != null) {
                Iterator i = userProfileValSet.iterator();
                String mapping = (String) i.next();
                String[] mappingAttrs = mapping.split("\\|");
                String profileAttrName = mappingAttrs[0];
                sessionAttrName = "am.protected." + mappingAttrs[1];
                expectedAttrValue = (String) ((Set) userAttrMap.get(
                        profileAttrName)).iterator().next();
                log(Level.FINE, "testUserProfileMapping",
                        "Attempting to set session attribute " +
                        sessionAttrName);
                userToken.setProperty(sessionAttrName, "wrongvalue");
            } else {
                log(Level.SEVERE, "testSessionAttrModification",
                        "SSOToken for user " + testUserName +
                        " was not retrieved!");
            }
        } catch (SSOException ssoe) {
            log(Level.FINEST, "testSessionAttrModification", ssoe.getMessage());
            log(Level.FINE, "testSessionAttrModification",
                    "Verifying the value of the session attribute " +
                    sessionAttrName);
            String retrievedValue = userToken.getProperty(sessionAttrName);
            if (retrievedValue.equals(expectedAttrValue)) {
                log(Level.FINEST, "testSessionAttrModification",
                        "Session attribute " + sessionAttrName +
                        " has the value \'" + expectedAttrValue + "\'");
                propertyUnchanged = true;
            } else {
                log(Level.SEVERE, "testSessionAttrModification",
                        "Session attribute " + sessionAttrName +
                        " had a value of \'" + retrievedValue +
                        "\' instead of expected value \'" +
                        expectedAttrValue + "\'.");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testSessionAttrModification",
                    "Exception message: " + e.getMessage());
            log(Level.SEVERE, "testSessionAttrModification", testUserName +
                    " login resulted in an exception.");
            cleanup(testRealm);
            throw e;
        } finally {
            destroyToken(userToken);
            assert propertyUnchanged;
        }
    }

    /**
     * Attempt to modify the authentication related attributes in
     * the <code>SSOToken</code>.  Such modifications should result in an
     * <code>SSOException</code> being thrown.
     * @param testModule - the authentication module instance type which will
     * be used by the user to be authenticated.
     * @param testRealm - the name of the realm in which the user will be
     * authenticated.
     */
    @Parameters({"testModule", "testRealm"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAuthCredModification(String testModule, String testRealm)
    throws Exception {
        SSOToken userToken = null;
        boolean credsModified = false;
        Object[] params = {testRealm};
        Principal userPrincipal = null;
        int userAuthLevel = -1;
        String tokenUserID = null;
        String userAuthType = null;

        entering("testAuthCredModification", params);
        String testCaseName = "SSOTokenAuthCredentialModification";
        String testCaseDesc = "Attepmt to modify auth credentials " +
                "(e.g. AuthLevel, UserID, AuthType, Principal) in an " +
                "an authenticated user's SSOToken.";
        log(Level.FINEST, "setup", "testCaseName: " + testCaseName);
        log(Level.FINEST, "setup", "testCaseDescription: " + testCaseDesc);
        Reporter.log("TestCaseName: " + testCaseName);
        Reporter.log("TestCaseDescription: " + testCaseDesc);

        try {
            userToken = performRemoteLogin(testRealm, "module",
                            moduleInstance, testUserName, testUserPass);

            if (userToken != null) {
                userPrincipal = userToken.getPrincipal();
                userAuthLevel = userToken.getAuthLevel();
                tokenUserID = userToken.getProperty("UserId");
                userAuthType = userToken.getProperty("AuthType");
                // attempt to change the token principal to the amadmin user
                try {
                    String badPrincipal = "uid=amadmin,ou=people," + basedn;
                    log(Level.FINE, "testAuthCredModification",
                            "Attempting to change the principal in the user " +
                            "token to " + badPrincipal + " ...");
                    userToken.setProperty("Principal", badPrincipal);
                    log(Level.SEVERE, "testAuthCredModification",
                            "Attempt to modify token principal did not " +
                            "result in an SSOException");
                    credsModified = true;
                } catch (SSOException ssoe) {
                    log(Level.FINEST, "testAuthCredModification",
                            "Attempt to modify token principal resulted in " +
                            "SSOException as expected");
                    log(Level.FINEST, "testAuthCredModification",
                            "SSOException message = " + ssoe.getMessage());
                    Principal currentPrincipal = userToken.getPrincipal();
                    if (!currentPrincipal.equals(userPrincipal)) {
                        log(Level.FINEST, " testAuthCredModification",
                                "The principal in the user token was " +
                                "modified to " + currentPrincipal);
                        credsModified = true;
                    }
                }

                // attempt to change the token auth level to a higher value
                try {
                    int badLevel = userAuthLevel + 50;
                    log(Level.FINE, "testAuthCredModification",
                            "Attempting to change the auth level in the user " +
                            "token to " + badLevel + " ...");
                    userToken.setProperty("AuthLevel",
                            Integer.toString(badLevel));
                    log(Level.SEVERE, "testAuthCredModification",
                            "Attempt to modify token auth level did not " +
                            "result in an SSOException");
                    credsModified = true;
                } catch (SSOException ssoe) {
                    log(Level.FINEST, "testAuthCredModification",
                            "Attempt to modify token auth level resulted in " +
                            "SSOException as expected");
                    log(Level.FINEST, "testAuthCredModification",
                            "SSOException message = " + ssoe.getMessage());
                    int currentAuthLevel = userToken.getAuthLevel();
                    if (currentAuthLevel != userAuthLevel) {
                        log(Level.FINEST, " testAuthCredModification",
                                "The auth level in the user token was " +
                                "modified to " + currentAuthLevel);
                        credsModified = true;
                    }
                }

                // attempt to change the token user ID to another user
                try {
                    log(Level.FINE, "testAuthCredModification",
                            "Attempting to change the user ID in the user " +
                            "token to " + adminUser + " ...");
                    userToken.setProperty("UserId", adminUser);
                    log(Level.SEVERE, "testAuthCredModification",
                            "Attempt to modify token user ID did not " +
                            "result in an SSOException");
                    credsModified = true;
                } catch (SSOException ssoe) {
                    log(Level.FINEST, "testAuthCredModification",
                            "Attempt to modify token user ID resulted in " +
                            "SSOException as expected");
                    log(Level.FINEST, "testAuthCredModification",
                            "SSOException message = " + ssoe.getMessage());
                    String currentUserID = userToken.getProperty("UserId");
                    if (!currentUserID.equals(tokenUserID)) {
                        log(Level.FINEST, " testAuthCredModification",
                                "The user ID in the user token was " +
                                "modified to " + currentUserID);
                        credsModified = true;
                    }
                }

                // attempt to change the token auth type to another value
                try {
                    if ((userToken != null) &&
                            SSOTokenManager.getInstance().isValidToken(
                            userToken)) {
                        log(Level.FINE, "testAuthCredModification",
                                "Attempting to change the auth type in the " +
                                "user token to badAuthType ...");
                        userToken.setProperty("AuthType", "badAuthType");
                        log(Level.SEVERE, "testAuthCredModification",
                                "Attempt to modify token auth type did not " +
                                "result in an SSOException");
                        credsModified = true;
                    } else {
                        log(Level.SEVERE, "testAuthCredModification",
                                "The token for user " + testUserName +
                                " is no longer valid!");
                        assert false;
                    }
                } catch (SSOException ssoe) {
                    log(Level.FINEST, "testAuthCredModification",
                            "Attempt to modify token auth type resulted in " +
                            "SSOException as expected");
                    log(Level.FINEST, "testAuthCredModification",
                            "SSOException message = " + ssoe.getMessage());
                    String currentAuthType = userToken.getProperty("AuthType");
                    if (!currentAuthType.equals(userAuthType)) {
                        log(Level.FINEST, " testAuthCredModification",
                                "The user ID in the user token was " +
                                "modified to " + currentAuthType);
                        credsModified = true;
                    }
                }
            } else {
                log(Level.SEVERE, "testAuthCredModification",
                        "The user " + testUserName +
                        " failed to be authenticated!");
                assert false;
            }
         } catch (Exception e) {
            log(Level.SEVERE, "testAuthCredModification",
                    "Unexpected Exception : " + e.toString());
            cleanup(testRealm);
            throw e;
        } finally {
            destroyToken(userToken);
            assert !credsModified;
        }
    }
    
    /**
     * performs cleanup after tests are done.
     * @param testRealm - if not the top-level realm, this realm will be removed
     */
    @Parameters({"testRealm"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testRealm) 
    throws Exception {
        Object[] params = {testRealm};
        entering("cleanup", params);
        SSOToken realmToken = null;
        SSOToken idToken = null;
        SSOToken serviceToken = null;
        
        try {
            log(Level.FINE, "cleanup", "Deleting user " + testUserName + "...");
            idToken = getToken(adminUser, adminPassword, realm);
            idmc.deleteIdentity(idToken, testRealm, IdType.USER, testUserName);
            
            log(Level.FINE, "cleanup", "Restoring original value of " +
                    "sunAMUserAttributesSessionMapping");
            serviceToken = getToken(adminUser, adminPassword, realm);
            SMSCommon smsc = new SMSCommon(serviceToken);
            log(Level.FINE, "setup", "Set " + mappingAttrName + " to " + 
                    oriAuthAttrValues);
            smsc.updateSvcAttribute(testRealm, strServiceName, mappingAttrName,
                    oriAuthAttrValues, "Organization");            

            String absoluteRealm = testRealm;
            if (!absoluteRealm.equals("/")) {
                if (absoluteRealm.indexOf("/") != 0) {
                    absoluteRealm = "/" + testRealm;
                }
                log(Level.FINE, "cleanup", "Deleting the sub-realm " + 
                        absoluteRealm);
                realmToken = getToken(adminUser, adminPassword, realm);
                idmc.deleteRealm(realmToken, absoluteRealm);
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (realmToken != null) {
                destroyToken(realmToken);
            }
            if (idToken != null) {
                destroyToken(idToken);
            }
            if (serviceToken != null) {
                destroyToken(serviceToken);
            }
        }
    }
}
