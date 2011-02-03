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
 * $Id: SessionUpgrade.java,v 1.13 2009/07/28 13:42:29 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
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
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class <code>SessionUpgrade</code>.
 * Performs Session Upgrade tests.Session Upgrade is Login to the
 * first module instance and use the token create AuthContext and
 * Login to the second module instance should give the AuthType
 * secondModuleinstance|firstModuleinstance and AuthLevel should be equal to
 * the authentication level of the highest authentication level in the modules
 * used for authentication.
 */
public class SessionUpgrade extends AuthenticationCommon {

    private int upgradedAuthLevel;
    private int failedAuthLevel;
    private ResourceBundle testResources;
    private String testModules;
    private String firstModuleName;
    private String firstModuleUserName;
    private String firstModulePassword;
    private String firstModuleLevel;
    private String secondModuleName;
    private String secondModuleUserName;
    private String secondModulePassword;
    private String secondModuleLevel;
    private String absoluteRealm;
    private List<String> testUserList = new ArrayList<String>();
    private List<IdType> idTypeList = new ArrayList<IdType>();
    private String upgradedAuthType;
    private String failedAuthType;
    private boolean useMultipleModules;
    private Set oriAuthAttrValues;
    private SSOToken adminToken;
    private IDMCommon idmc;
    private String strServiceName = "iPlanetAMAuthService";
    private String profileAttrName = "iplanet-am-auth-dynamic-profile-creation";
    private String propertyPrefix;
    private String createUserProfile = "required";
    private boolean usersDontMatch;

    /**
     * Default Constructor
     */
    public SessionUpgrade() {
        super("SessionUpgrade");
        idmc = new IDMCommon();
        testResources = ResourceBundle.getBundle("authentication" +
                fileseparator + "SessionUpgrade");
    }

    /**
     * Set up the system for testing.Create authentication instances
     * create users before testing the session Upgrade feature
     */
    @Parameters({"forceAuth", "testRealm", "useDifferentModules",
            "upgradeTestType"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
            "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String forceAuth, String testRealm,
            String useDifferentModules, String upgradeTestType)
    throws Exception {
        Object[] params = {forceAuth, testRealm, useDifferentModules,
                upgradeTestType};
        entering("setup", params);
        usersDontMatch = upgradeTestType.equals("mismatch");

        try {
            adminToken = getToken(adminUser, adminPassword, realm);
            propertyPrefix = "am-auth-session-" + upgradeTestType + "-test-";
            testModules = testResources.getString(propertyPrefix + "modules");
            String[] moduleTokens = testModules.split(",");
            if (testModules.length() < 2) {
                log(Level.SEVERE, "setup", "The test modules property " +
                        testModules + " does not contain two module tokens!");
                assert false;
            }

            firstModuleName = getAuthInstanceName(moduleTokens[0]);
            firstModuleUserName = testResources.getString(propertyPrefix +
                    "firstuser");
            firstModulePassword = testResources.getString(propertyPrefix +
                    "firstpasswd");
            firstModuleLevel = getAuthInstanceLevel(moduleTokens[0]);
            failedAuthLevel = new Integer(firstModuleLevel).intValue();
            secondModuleName = getAuthInstanceName(moduleTokens[1]);
            secondModuleUserName = testResources.getString(propertyPrefix +
                    "seconduser");
            secondModulePassword = testResources.getString(propertyPrefix +
                    "secondpasswd");
            secondModuleLevel = getAuthInstanceLevel(moduleTokens[1]);
            useMultipleModules = Boolean.parseBoolean(useDifferentModules);

            absoluteRealm = testRealm;
            if (!testRealm.equals("/")) {
                absoluteRealm = realm + testRealm;
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

            log(Level.FINEST, "setup", "OrgName " + testRealm);
            log(Level.FINEST, "setup", "firstModuleName: " + firstModuleName);
            log(Level.FINEST, "setup", "firstModuleUserName: " +
                    firstModuleUserName);
            log(Level.FINEST, "setup", "firstModuleUserPass: " +
                    firstModulePassword);
            log(Level.FINEST, "setup", "forceAuthEnabled: " + forceAuth);
            log(Level.FINEST, "setup", "upgradeTestType: " +
                    upgradeTestType);

            Reporter.log("OrgName: " + testRealm);
            Reporter.log("firstModuleName: " + firstModuleName);
            Reporter.log("firstModuleUserName: " + firstModuleUserName);
            Reporter.log("firstModuleUserPass: " + firstModulePassword);
            Reporter.log("Upgrade Test Type: " + upgradeTestType);

            if (useMultipleModules) {
                log(Level.FINEST, "setup", "secondModuleName: " +
                        secondModuleName);
                log(Level.FINEST, "setup", "secondModuleUserName: " +
                        secondModuleUserName);
                log(Level.FINEST, "setup", "secondModuleUserPass: " +
                        secondModulePassword);
                Reporter.log("secondModuleName: " + secondModuleName);
                Reporter.log("secondModuleUserName: " + secondModuleUserName);
                Reporter.log("secondModuleUserPass: " + secondModulePassword);
                upgradedAuthLevel = Integer.parseInt(secondModuleLevel);
                upgradedAuthType = secondModuleName + "|" + firstModuleName;
            } else {
                String [] tmpArray = new String[1];
                tmpArray[0] = moduleTokens[0];
                moduleTokens = tmpArray;
                upgradedAuthLevel = Integer.parseInt(firstModuleLevel);
                upgradedAuthType = firstModuleName;
            }

            SMSCommon smsc = new SMSCommon(adminToken);
            log(Level.FINE, "setup", "Retrieving the attribute value of " +
                    profileAttrName + " from " + strServiceName + " in realm " +
                    testRealm + "...");
            oriAuthAttrValues = (Set) smsc.getAttributeValue(testRealm,
                    strServiceName, profileAttrName, "Organization");
            log(Level.FINEST, "setup", "Original value of " + profileAttrName +
                    ": "  + oriAuthAttrValues);

            if (upgradeTestType.equals("ignore")) {
                createUserProfile = upgradeTestType;
            }
            String testAttrValue = getProfileAttribute(createUserProfile);
            Set valSet = new HashSet();
            valSet.add(testAttrValue);
            log(Level.FINE, "setup", "Setting authentication attribute " +
                    profileAttrName + " to \'" + testAttrValue + "\'.");
            smsc.updateSvcAttribute(testRealm, strServiceName, profileAttrName,
                    valSet, "Organization");

            log(Level.FINEST, "setup", "testModules " + testModules);
            log(Level.FINEST, "setup", "upgradedAuthLevel: " +
                    upgradedAuthLevel);
            Reporter.log("testModuleNames: " + testModules);
            Reporter.log("ForceAuth Enabled: " + forceAuth);
            Reporter.log("UpgradedAuthLevel: " + upgradedAuthLevel);

            failedAuthType = firstModuleName;
            if (!upgradeTestType.equals("ignore")) {
                createUser(testRealm, firstModuleUserName, firstModulePassword);
                if (!secondModuleUserName.equals(firstModuleUserName)) {
                    createUser(testRealm, secondModuleUserName,
                            secondModulePassword);
                }
            }
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            if (oriAuthAttrValues == null) {
                oriAuthAttrValues = new HashSet();
            }
            e.printStackTrace();
            cleanup(testRealm);
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
            Thread.sleep(notificationSleepTime);
        }
        exiting("setup");
    }

    /**
     * Tests for SessionUpgrade by login into the system using correct
     * credentials for two different modules
     * forceAuth - a <code>String</code> containing "true" or "false"
     * indicating whether the same session will be used on the subsequent
     * authentication.
     * testRealm - the realm in which the session upgrade will take place
     * testMode - the type of authentication (e.g. module-based or level-based)
     * that will be used in the test case.
     */
    @Parameters({"forceAuth", "testRealm", "testMode"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
    "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSessionUpgrade(String forceAuth, String testRealm,
            String testMode)
    throws Exception {
        Object[] params = {forceAuth, testRealm, testMode};

        entering("testSessionUpgrade", params);
        log(Level.FINE, "testSessionUpgrade",
                "Test Description: Testing session upgrade in the realm " +
                testRealm + " with " + testMode +
                "-based authentication, forceAuth " + forceAuth + ", " +
                (usersDontMatch ? "different users, " : "the same user, ") +
                (useMultipleModules ? "multiple modules" : "a single module") +
                ", and user profile creation set to " + createUserProfile);
        Reporter.log("Test Description: Testing session upgrade in the realm " +
                testRealm + " with " + testMode +
                "-based authentication, forceAuth " + forceAuth + ", " +
                (usersDontMatch ? "different users, " : "the same user, ") +
                (useMultipleModules ? "multiple modules" : "a single module") +
                ", and user profile creation set to " + createUserProfile);
        Callback[] callbacks2 = null;
        SSOToken obtainedToken = null;
        SSOToken upgradedToken = null;
        AuthContext.IndexType indexType = null;
        String indexName1 = null;
        String indexName2 = null;

        if (testMode != null) {
            log(Level.FINEST, "testSessionUpgrade", "Auth instance1 name = " +
                    firstModuleName);
            Reporter.log("Auth Instance 1 Name: " + firstModuleName);

            if (testMode.equals("module")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
                indexName1 = firstModuleName;
                indexName2 = secondModuleName;
            } else if (testMode.equals("level")) {
                indexType = AuthContext.IndexType.LEVEL;
                indexName1 = firstModuleLevel;
                indexName2 = secondModuleLevel;
            } else {
                log(Level.SEVERE, "testSessionUpgrade",
                        "Unsupported testMode value from Test NG XML.  " +
                        "Expecting either \"module\" or \"level\".");
                assert false;
            }
        } else {
            log(Level.SEVERE, "testSessionUpgrade",
                        "testMode parameter wa not set in Test NG XML.  " +
                        "Expecting either \"module\" or \"level\".");
            assert false;
        }

        String secondModuleToSelect = secondModuleName;
        if (!useMultipleModules) {
            indexName2 = indexName1;
            secondModuleToSelect = firstModuleName;
            secondModuleUserName = firstModuleUserName;
            secondModulePassword = firstModulePassword;
        }

        log(Level.FINEST, "testSessionUpgrade", "Auth instance2 name = " +
                secondModuleToSelect);
        Reporter.log("Auth Instance 2 Name: " + secondModuleToSelect);

        try {
            obtainedToken = performRemoteLogin(testRealm, testMode, indexName1,
                    firstModuleUserName, firstModulePassword, firstModuleName);
            Thread.sleep(notificationSleepTime);
            if (obtainedToken != null) {
                log(Level.FINEST, "testSessionUpgrade", "forceAuth = " +
                        forceAuth);
                Reporter.log("forceAuth = " + forceAuth);
                boolean useForceAuth = Boolean.parseBoolean(forceAuth);
                AuthContext newlc = new AuthContext(obtainedToken,
                        useForceAuth);
                log(Level.FINE, "testSessionUpgrade",
                    "Invoking AuthContext.login with indexName " + indexName2);
                newlc.login(indexType, indexName2);
                while (newlc.hasMoreRequirements()) {
                    callbacks2 = newlc.getRequirements();
                    if (callbacks2 != null) {
                        log(Level.FINEST, "testSessionUpgrade",
                                "Callback array contains " + callbacks2.length +
                                " callbacks");
                        try {
                            for (int i = 0; i < callbacks2.length; i++){
                                if (callbacks2[i] instanceof NameCallback) {
                                    NameCallback namecallback =
                                            (NameCallback)callbacks2[i];
                                    namecallback.setName(secondModuleUserName);
                                }
                                if (callbacks2[i] instanceof PasswordCallback) {
                                    PasswordCallback passwordcallback =
                                            (PasswordCallback)callbacks2[i];
                                    passwordcallback.setPassword(
                                            secondModulePassword.toCharArray());
                                }
                                if (callbacks2[i] instanceof ChoiceCallback) {
                                    ChoiceCallback choiceCallback =
                                            (ChoiceCallback)callbacks2[i];
                                    String[] strChoices2 =
                                            choiceCallback.getChoices();
                                    int choiceIndex = -1;
                                    for (int k=0; k < strChoices2.length; k++) {
                                        log(Level.FINEST, "testSessionUpgrade",
                                                "choice " + k + " = " +
                                                strChoices2[k]);

                                        if (strChoices2[k].equals(
                                                secondModuleToSelect)) {
                                            choiceIndex = k;
                                            break;
                                        }
                                    }
                                    choiceCallback.setSelectedIndex(
                                            choiceIndex);
                                }
                            }
                            newlc.submitRequirements(callbacks2);
                        } catch (Exception ex1) {
                            log(Level.SEVERE, "testSessionUpgrade",
                                    ex1.getMessage());
                            assert false;
                        }
                    }
                }
                if (newlc.getStatus() == AuthContext.Status.SUCCESS) {
                    upgradedToken = newlc.getSSOToken();
                    SSOTokenManager.getInstance().refreshSession(upgradedToken);
                    String tokenAuthType =
                            upgradedToken.getProperty("AuthType");
                    int tokenAuthLevel = upgradedToken.getAuthLevel();

                    if (tokenAuthType == null) {
                        log(Level.SEVERE, "testSessionUpgradeNegative",
                                "Unable to obtain AuthType from inital " +
                                "SSOToken");
                        assert false;
                    }

                    log(Level.FINEST, "testSessionUpgrade", "AuthType in " +
                            "upgraded token = " + tokenAuthType);
                    log(Level.FINEST, "testSessionUpgrade", "Expected " +
                            "AuthType in upgraded token = " + upgradedAuthType);
                    log(Level.FINEST, "testSessionUpgrade",
                            "AuthLevel in upgraded token = " +
                            upgradedToken.getAuthLevel());
                    log(Level.FINEST, "testSessionUpgrade", "Expected " +
                            "AuthLevel in upgraded token = " +
                            upgradedAuthLevel);
                    String obtainedSessionID =
                            obtainedToken.getTokenID().toString();
                    String upgradedSessionID =
                            upgradedToken.getTokenID().toString();
                    log(Level.FINEST, "testSessionUpgrade",
                            "Obtained token session ID = " + obtainedSessionID);
                    log(Level.FINEST, "testSessionUpgrade",
                            "Upgraded token session ID = " + upgradedSessionID);

                    log(Level.FINEST, "testSessionUpgrade",
                            "AuthType statement = " +
                            upgradedToken.getProperty("AuthType").equals(
                            upgradedAuthType));
                    log(Level.FINEST, "testSessionUpgrade",
                            "Auth level statement = " + (tokenAuthLevel ==
                            upgradedAuthLevel));

                    if (useMultipleModules) {
                        log(Level.FINEST, "testSessionUpgrade",
                                "Session ID statement = " +
                                (useForceAuth ==
                                upgradedSessionID.equals(obtainedSessionID)));
                        assert (upgradedToken.getProperty("AuthType").equals(
                                upgradedAuthType) && (tokenAuthLevel ==
                                upgradedAuthLevel) && (useForceAuth ==
                                (upgradedSessionID.equals(obtainedSessionID))));
                    } else {
                        assert (upgradedToken.getProperty("AuthType").equals(
                                upgradedAuthType) && (tokenAuthLevel ==
                                upgradedAuthLevel) &&
                                (upgradedSessionID.equals(obtainedSessionID)));
                    }
                } else {
                    if (!firstModuleUserName.equals(secondModuleUserName)) {
                        log(Level.FINEST, "testSessionUpgrade",
                                "Expected session upgrade failed with " +
                                "user1 = " +  firstModuleUserName +
                                " and user2 = " + secondModuleUserName);
                        log(Level.FINE, "testSessionUpgrade",
                                "Checking validity of the original SSOToken");
                        SSOTokenManager tokenMgr =
                                SSOTokenManager.getInstance();
                        tokenMgr.refreshSession(obtainedToken);
                        assert tokenMgr.isValidToken(obtainedToken) &&
                                obtainedToken.getProperty("UserId").
                                equals(firstModuleUserName);
                    } else {
                        log(Level.SEVERE, "testSessionUpgrade",
                                "The second authentication with user " +
                                secondModuleUserName + " was not successful.");
                        assert false;
                    }
                }
            } else {
                log(Level.SEVERE, "testSessionUpgrade",
                        "The first authentication with user" +
                        firstModuleUserName + " was not successful.");
                assert false;
            }
            exiting("testSessionUpgrade");
        } catch (Exception e) {
            log(Level.SEVERE, "testSessionUpgrade", e.getMessage());
            e.printStackTrace();
            cleanup(testRealm);
            assert false;
        }

    }

    /**
     * Tests that after a failed authentication with the second module that the
     * user's original session is maintained.
     * forceAuth - a <code>String</code> containing "true" or "false"
     * indicating whether the same session will be used on the subsequent
     * authentication.
     * testRealm - the realm in which the session upgrade will take place
     * testMode - the type of authentication (e.g. module-based or level-based)
     * that will be used in the test case.
     */
    @Parameters({"forceAuth", "testRealm", "testMode"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
    "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSessionUpgradeNegative(String forceAuth,
            String testRealm, String testMode)
    throws Exception {
        Object[] params = {forceAuth, testRealm, testMode};
        entering("testSessionUpgradeNegative", params);
        log(Level.FINE, "testSessionUpgradeNegative",
                "Test Description: Testing failed auth in the realm " +
                testRealm + " with " + testMode +
                "-based authentication, forceAuth " + forceAuth + ", " +
                (usersDontMatch ? "different users, " : "the same user, ") +
                "and " +
                (useMultipleModules ? "multiple modules" : "a single module"));
        Reporter.log("Test Description: Testing failed auth in the realm " +
                testRealm + " with " + testMode +
                "-based authentication, forceAuth " + forceAuth + ", " +
                (usersDontMatch ? "different users, " : "the same user, ") + 
                " and " +
                (useMultipleModules ? "multiple modules" : "a single module"));
        Callback[] callbacks2 = null;
        SSOToken obtainedToken = null;
        AuthContext.IndexType indexType = null;
        String indexName1 = null;
        String indexName2 = null;

        if (testMode != null) {
            if (testMode.equals("module")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
                indexName1 = firstModuleName;
                indexName2 = secondModuleName;
            } else if (testMode.equals("level")) {
                indexType = AuthContext.IndexType.LEVEL;
                indexName1 = firstModuleLevel;
                indexName2 = secondModuleLevel;
            } else {
                log(Level.SEVERE, "testSessionUpgradeNegative",
                        "Unsupported testMode value from Test NG XML.  " +
                        "Expecting either \"module\" or \"level\".");
                assert false;
            }
        } else {
            log(Level.SEVERE, "testSessionUpgradeNegative",
                        "testMode parameter wa not set in Test NG XML.  " +
                        "Expecting either \"module\" or \"level\".");
            assert false;
        }

        if (!useMultipleModules) {
            indexName2 = indexName1;
        }

        try {
            obtainedToken = performRemoteLogin(testRealm, testMode, indexName1,
                    firstModuleUserName, firstModulePassword, firstModuleName);
            Thread.sleep(notificationSleepTime);
            if (obtainedToken != null) {
                log(Level.FINEST, "testSessionUpgradeNegative",
                        "forceAuth = " + forceAuth);
                Reporter.log("ForceAuth = " + forceAuth);
                String failedPassword = "wrong" + secondModulePassword;
                boolean useForceAuth = Boolean.parseBoolean(forceAuth);
                AuthContext newlc = new AuthContext(obtainedToken,
                        useForceAuth);
                newlc.login(indexType, indexName2);
                while (newlc.hasMoreRequirements()) {
                    callbacks2 = newlc.getRequirements();
                    if (callbacks2 != null) {
                        try {
                            for (int i = 0; i < callbacks2.length; i++) {
                                if (callbacks2[i] instanceof NameCallback) {
                                    NameCallback namecallback =
                                            (NameCallback)callbacks2[i];
                                    namecallback.setName(secondModuleUserName);
                                }
                                if (callbacks2[i] instanceof PasswordCallback) {
                                    PasswordCallback passwordcallback =
                                            (PasswordCallback)callbacks2[i];
                                    passwordcallback.setPassword(
                                            failedPassword.toCharArray());
                                }
                                if (callbacks2[i] instanceof ChoiceCallback) {
                                    ChoiceCallback choiceCallback =
                                            (ChoiceCallback)callbacks2[i];
                                    String[] strChoices =
                                            choiceCallback.getChoices();
                                    int choiceIndex = -1;
                                    for (int j=0; j < strChoices.length; j++) {
                                         if (strChoices[j].equals(
                                                 secondModuleName)) {
                                             choiceIndex = j;
                                             break;
                                         }
                                    }
                                    choiceCallback.setSelectedIndex(
                                            choiceIndex);
                                }
                            }
                            newlc.submitRequirements(callbacks2);
                        } catch (Exception e) {
                            log(Level.SEVERE, "testSessionUpgradeNegative",
                                    e.getMessage());
                            cleanup(testRealm);
                            assert false;
                        }
                    }
                }

                AuthContext.Status newStatus = newlc.getStatus();
                SSOTokenManager.getInstance().refreshSession(obtainedToken);
                String tokenAuthType =
                        obtainedToken.getProperty("AuthType");
                int tokenAuthLevel = obtainedToken.getAuthLevel();
                if (tokenAuthType == null) {
                    log(Level.SEVERE, "testSessionUpgradeNegative",
                            "Unable to obtain AuthType from inital " +
                            "SSOToken");
                    assert false;
                }

                log(Level.FINEST, "testSessionUpgradeNegative",
                        "The status of the second login was " + newStatus);
                log(Level.FINEST, "testSessionUpgradeNegative",
                        "AuthType in original token = " + tokenAuthType);
                log(Level.FINEST, "testSessionUpgradeNegative", "Expected "
                        + "AuthType in original token = " + failedAuthType);
                log(Level.FINEST, "testSessionUpgradeNegative",
                        "AuthLevel in original token = " +
                        tokenAuthLevel);
                log(Level.FINEST, "testSessionUpgradeNegative", "Expected "
                        + "AuthLevel in original token = " +
                        failedAuthLevel);

                if (newStatus == AuthContext.Status.FAILED) {
                    assert (tokenAuthType.equals(failedAuthType) &&
                            (tokenAuthLevel == failedAuthLevel));
                } else if (!useMultipleModules &&
                        newStatus == AuthContext.Status.SUCCESS) {
                    long obtainedMaxSessionTime =
                           obtainedToken.getMaxSessionTime();
                    long obtainedMaxIdleTime = obtainedToken.getMaxIdleTime();
                    SSOToken newToken = newlc.getSSOToken();
                    SSOTokenManager.getInstance().refreshSession(newToken);
                    long newMaxSessionTime = newToken.getMaxSessionTime();
                    long newMaxIdleTime = newToken.getMaxIdleTime();

                    log(Level.FINEST, "testSessionUpgradeNegative",
                           "Max session time in original token = " +
                           obtainedMaxSessionTime);
                    log(Level.FINEST, "testSessionUpgradeNegative",
                           "Max session time in new token = " +
                           newMaxSessionTime);
                    log(Level.FINEST, "testSessionUpgradeNegative",
                           "Max idle time in original token = " +
                           obtainedMaxIdleTime);
                    log(Level.FINEST, "testSessionUpgradeNegative",
                            "Max session time in new token = " +
                            newMaxIdleTime);
                    assert ((obtainedMaxSessionTime == newMaxSessionTime) &&
                            (obtainedMaxIdleTime == newMaxIdleTime) &&
                            tokenAuthType.equals(failedAuthType) &&
                            (tokenAuthLevel == failedAuthLevel));
                } else {
                    log(Level.SEVERE, "testSessionUpgradeNegative",
                            "The status of the second login was " + newStatus +
                            " with forceAuth = " + forceAuth + ".");
                    assert false;
                }
                exiting("testSessionUpgradeNegative");
            } else {
                log(Level.SEVERE, "testSessionUpgradeNegative",
                        "The first authentication of user " +
                        firstModuleUserName + " with " + testMode +
                        " = " + indexName1 + ".");
                assert false;
            }
        } catch (Exception ex) {
            log(Level.SEVERE, "testSessionUpgradeNegative",
                    ex.getMessage());
            ex.printStackTrace();
            cleanup(testRealm);
            throw ex;
        } finally {
            if (obtainedToken != null) {
                destroyToken(obtainedToken);
            }
        }
    }

    /**
     * Performs cleanup after tests are done.
     * Deletes the authentication instances and users created
     * by this test scenario
     */
    @Parameters({"testRealm"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("cleanup", params);
        adminToken = getToken(adminUser, adminPassword, realm);

        try {
            if ((testUserList != null) && !testUserList.isEmpty()) {
                log(Level.FINE, "cleanup", "Deleting user(s) " + testUserList +
                        "...");
                idmc.deleteIdentity(adminToken, testRealm,
                        idTypeList, testUserList);
            }

            if (oriAuthAttrValues != null) {
                SMSCommon smsc = new SMSCommon(adminToken);
                log(Level.FINE, "cleanup", "Set " + profileAttrName + " to " +
                        oriAuthAttrValues);
                smsc.updateSvcAttribute(testRealm, strServiceName,
                        profileAttrName,
                        oriAuthAttrValues,
                        "Organization");
            }

            if (!testRealm.equals("/")) {
                if (absoluteRealm.indexOf("/") != 0) {
                    absoluteRealm = "/" + absoluteRealm;
                }
                log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                        absoluteRealm);
                idmc.deleteRealm(adminToken, absoluteRealm);
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
    }

    /**
     * Create a user for the session upgrade testing.
     * @param userRealm - a <code>String</code> containing the realm in which
     * the user should be created.
     * @param user - a <code>String</code> containing the user ID.
     * @param password - a <code>String</code> containing the user's password.
     */
    private void createUser(String userRealm, String user, String password)
    throws Exception {
        log(Level.FINE, "createUser", "Creating the user " + user);
        testUserList.add(user);
        StringBuffer attrBuffer =
                new StringBuffer("sn=" + user).
                append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                append("cn=" + user).
                append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                append("userpassword=" + password).
                append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                append("inetuserstatus=Active");
        if (!idmc.createID(user, "user", attrBuffer.toString(),
                adminToken, userRealm)) {
            log(Level.SEVERE, "setup",
                    "Failed to create user identity " + user + " ...");
            assert false;
        } else {
            idTypeList.add(IdType.USER);
        }
    }
}
