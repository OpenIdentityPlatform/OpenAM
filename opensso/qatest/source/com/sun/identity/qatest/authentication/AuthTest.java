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
 * $Id: AuthTest.java,v 1.22 2009/07/02 19:32:38 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.authentication;

import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.util.HashMap;
import java.util.HashSet;
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
 * This class does the following:
 * (1) Create a module instance with a given auth level
 * (2) Create an auth service using the auth module
 * (3) Create a new user and assign the auth service to the user
 * (4) Create a new role
 * (5) Assign the auth service and the user to that role
 * (6) Do a module, role, service, level and user based authentication
 * using htmlunit and zero page login with URL parameters. 
 * (7) Repeat the following scenarios for Active Directory, LDAP, 
 * Membership, Anonymous, NT and JDBC modules)
 */
public class AuthTest extends AuthenticationCommon {
    private IDMCommon idmc;
    private ResourceBundle rb;
    private SMSCommon smsc;
    private SSOToken idToken;
    private String modeValue;
    private String serviceName;
    private String serviceSubConfigName;
    private String rolename;
    private String loginUser;
    private String loginPassword;
    private String svcName;
    private String logoutURL;
    private String userRealm;
    private boolean isValidTest = true;
    private WebClient webClient;

    /**
     * Constructor for the class.
     */
    public AuthTest() {
        super();
        idmc = new IDMCommon("authentication");
    }

    /**
     * This method is to configure the initial setup. It does the following:
     * (1) Creates an authentication configuration  using the auth module for
     *     user, realm, role, and service based authentication.
     * (2) Create a new user.
     * (3) For user based authentication, assign the authentication
     *     configuration to the user.
     * (4) For role based authentication, create a new role and assign the auth
     *     service and the user to that role
     * (5) For realm based authentication, create a new realm and assign an
     *     authentication configuration to the new realm.
     * @param testModule - the type of authentication module instance which
     * will be used for authentication.
     * @param testMode - the type of authentication which should be performed
     * (e.g. "module", "user", "service", "realm", "role", "authlevel").
     */
    @Parameters({"testModule", "testMode"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testModule, String testMode)
    throws Exception {
        Object[] params = {testModule, testMode};
        entering("setup", params);
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";

        try {
            //Checks for incompatible module/OS types (e.g. NT auth on UNIX)
            // TODO Move to an if (!validModuleTest) {log issue; assert false;}
            isValidTest = isValidModuleTest(testModule);
            if (isValidTest) {
                //Load config from resources/authentication/AuthTest.properties
                rb = ResourceBundle.getBundle("authentication" + fileseparator +
                        "AuthTest");

                serviceSubConfigName = (String)rb.getString(testModule +
                        ".service-subconfigname");
                String serviceDetails = (String)rb.getString(testModule +
                        ".service-details");

                rolename = (String)rb.getString(testModule + ".rolename");
                loginUser = (String)rb.getString(testModule + ".user");
                loginPassword = (String)rb.getString(testModule + ".password");

                modeValue = (String)rb.getString(testModule + ".modevalue." +
                        testMode);

                //Log details using logger
                log(Level.FINEST, "setup", "rolename: " + rolename);
                log(Level.FINEST, "setup", "username: " + loginUser);
                log(Level.FINEST, "setup", "userpassword: " + loginPassword);

                //Output details with testng
                Reporter.log("RoleName: " + rolename);
                Reporter.log("UserName: " + loginUser);
                Reporter.log("UserPassword: " + loginPassword);

                //Log in using AMSDK and get SSOToken
                idToken = getToken(adminUser, adminPassword, basedn);

                //Create an instance of Service Management containing Token
                smsc = new SMSCommon(idToken);
                
                userRealm = realm;
                if (testMode.equals("realm")) {
                    userRealm = modeValue;
                    Map realmAttrMap = new HashMap();
                    Set realmSet = new HashSet();
                    realmSet.add("Active");
                    realmAttrMap.put("sunOrganizationStatus", realmSet);
                    log(Level.FINE, "setup", "Creating the realm " + userRealm);
                    AMIdentity amid = idmc.createIdentity(idToken,
                            realm, IdType.REALM, userRealm, realmAttrMap);
                    log(Level.FINE, "setup", 
                            "Verifying the existence of sub-realm " +
                            userRealm);
                    if (amid == null) {
                        log(Level.SEVERE, "setup", "Creation of sub-realm " +
                                userRealm + " failed!");
                        assert false;
                    }
                }

                log(Level.FINEST, "setup", "modeValue: " + modeValue);
                Reporter.log("ModeValue: " + modeValue);

                if (!testMode.equals("module") &&
                        !testMode.equals("authlevel")) {
                    String[] configInstances = {serviceDetails};
                    Map configMap = new HashMap();
                    createAuthConfig(userRealm, serviceSubConfigName,
                            configInstances, configMap);
                }

                //Build a string containing the attributes for Identity
                StringBuffer attrBuffer = new StringBuffer("sn=" + loginUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + loginUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + loginPassword).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("iplanet-am-user-auth-config=" + 
                        serviceSubConfigName);

                log(Level.FINE, "setup", "Creating user " + loginUser + " ...");

                //Create identity, and if it returns false, log and assert false
                if (!idmc.createID(loginUser, "user", attrBuffer.toString(),
                        idToken, userRealm)) {
                    log(Level.SEVERE, "setup",
                            "Failed to create user identity " + 
                            loginUser + " ...");
                    assert false;
                }

                // TODO 2nd if (testMode.equals("realm")) - check if necessary
                if (testMode.equals("realm")) {
                    log(Level.FINE, "setup",
                            "Setting the authentication configuration of realm "
                            + userRealm + " to " + serviceSubConfigName);
                    Map realmAttrs = new HashMap();
                    Set attrValue = new HashSet();
                    attrValue.add(serviceSubConfigName);
                    realmAttrs.put("iplanet-am-auth-org-config", attrValue);
                    smsc.updateServiceAttrsRealm(
                            "iPlanetAMAuthService", userRealm, realmAttrs);
                }

                if (testMode.equals("role") && smsc.isPluginConfigured(
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm)) {           
                    log(Level.FINE, "setup", "Creating role " + rolename + 
                            " ...");
                    if (!idmc.createID(rolename, "role", null, idToken,
                            userRealm)) {
                        log(Level.SEVERE, "createUser", "Failed to create role "
                                + rolename + " ...");
                        assert false;
                    }

                    log(Level.FINE, "setup", "Assigning the user " + loginUser +
                            " to role " + rolename + " ...");
                    idmc.addUserMember(idToken, loginUser, rolename,
                            IdType.ROLE, userRealm);

                    log(Level.FINE, "setup", "Assigning the service " + 
                            serviceSubConfigName + " to the role " +
                            rolename + "...");
                    idmc.assignSvcIdentity(idToken, rolename, "role",
                            AUTH_CONFIGURATION_SERVICE_NAME, userRealm,
                            "iplanet-am-auth-configuration=" +
                            serviceSubConfigName);
                } else {
                    log(Level.FINEST, "setup", 
                            "Creation of a role, assignment of user to role, " +
                            "and assignment of service to role skipped for " + 
                            "non amsdk plugin ...");
                }
            } else {
                throw new SkipException("Skipping setup for " + testModule +
                        " auth module test on unsupported platform or " +
                        "an OpenSSO nightly build.");
            }
            exiting("setup");
        } catch (SkipException se) {
            log(Level.FINEST, "setup", se.getMessage());
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup(testModule, testMode);
            throw e;
        } finally {
            if (isValidTest) {
                if (idToken != null) {
                    destroyToken(idToken);
                }
            }
        } 
    }

    /**
     * Tests for successful login into the system using correct credentials.
     * @param testModule - the type of authentication module instance which
     * will be used for authentication.
     * @param testMode - the type of authentication which should be performed
     * (e.g. "module", "user", "service", "realm", "role", "authlevel").
     * @param instanceIndex - an instance index to specify which authentication
     * module instance should be used for this test.
     */
    @Parameters({"testModule", "testMode", "instanceIndex"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testLoginPositive(String testModule, 
            String testMode,
            String instanceIndex)
    throws Exception {
        //Log Parameters used in running method
        Object[] params = {testModule, testMode, instanceIndex};
        entering("testLoginPositive", params);

        //Reset userToken
        SSOToken userToken = null;

        //Log to System logging
        log(Level.FINEST, "testLoginPositive", "testModule = " + testModule);
        log(Level.FINEST, "testLoginPositive", "testMode = " + testMode);
        log(Level.FINEST, "testLoginPositive", "instanceIndex = " +
                instanceIndex);

        //Log to TestNG
        Reporter.log("Test Module: " + testModule);
        Reporter.log("Test Mode: " + testMode);
        Reporter.log("Instance Index: " + instanceIndex);
        
        if (isValidTest) {
            if (!testMode.equals("authlevel")) {
                webClient = new WebClient();
                try {
                    String msg = (String)rb.getString(testModule + ".passmsg");
                    if (testModule.startsWith("anonymous")) {
                        testZeroPageLogin(webClient, loginUser, testMode,
                                modeValue, msg);
                    } else {
                        testZeroPageLogin(webClient, loginUser, loginPassword,
                                testMode, modeValue, msg);
                    }
                    exiting("testLoginPositive");
                } catch (Exception e) {
                    log(Level.SEVERE, "testLoginPositive", e.getMessage());
                    e.printStackTrace();
                    cleanup(testModule, testMode);
                    throw e;
                } finally {
                    consoleLogout(webClient, logoutURL);
                }
            } else {
                try {
                    String instanceName = getAuthInstanceName(
                            testModule, instanceIndex);
                    userToken = performRemoteLogin(userRealm, testMode,
                            modeValue, loginUser, loginPassword, instanceName);
                    if (userToken != null) {
                        log(Level.FINEST, "testLoginPositive",
                                "userToken principal = " +
                                userToken.getPrincipal());
                    }
                    assert (userToken != null);
                    exiting("testLoginPositive");
                } catch (Exception e) {
                    log(Level.SEVERE, "testLoginPositive", e.getMessage());
                    e.printStackTrace();
                    cleanup(testModule, testMode);
                    throw e;
                } finally {
                    if (userToken != null) {
                        destroyToken(userToken);
                    }
                }
            }
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
    }

    /**
     * Tests for unsuccessful login into the system using incorrect
     * credentials.
     * @param testModule - the type of authentication module instance which
     * will be used for authentication.
     * @param testMode - the type of authentication which should be performed
     * (e.g. "module", "user", "service", "realm", "role", "authlevel").
     * @param instanceIndex - an instance index to specify which authentication
     * module instance should be used for this test.
     */
    @Parameters({"testModule", "testMode", "instanceIndex"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testLoginNegative(String testModule, 
            String testMode,
            String instanceIndex)
    throws Exception {
        Object[] params = {testModule, testMode, instanceIndex};
        entering("testLoginNegative", params);
        SSOToken userToken = null;

        log(Level.FINEST, "testLoginNegative", "testModule = " + testModule);
        log(Level.FINEST, "testLoginNegative", "testMode = " + testMode);
        log(Level.FINEST, "testLoginNegative", "instanceIndex = " +
                instanceIndex);

        Reporter.log("Test Module: " + testModule);
        Reporter.log("Test Mode: " + testMode);
        Reporter.log("Instance Index: " + instanceIndex);

        if (isValidTest) {
            if (!testMode.equals("authlevel")) {
                webClient = new WebClient();
                try {
                    String modevalue = (String)rb.getString(testModule +
                            ".modevalue." + testMode);
                    String msg = (String)rb.getString(testModule + ".failmsg");
                    if (!testModule.startsWith("anonymous")) {
                        testZeroPageLogin(webClient, loginUser, "not" +
                                loginPassword, testMode, modevalue, msg);
                    } else {
                        testZeroPageLogin(webClient, loginUser + "negative",
                                testMode, modevalue, msg);
                    }
                    exiting("testLoginNegative");
                } catch (Exception e) {
                    log(Level.SEVERE, "testLoginNegative", e.getMessage());
                    e.printStackTrace();
                    cleanup(testModule, testMode);
                    throw e;
                } finally {
                    consoleLogout(webClient, logoutURL);
                }
            } else {
                try {
                    String instanceName = getAuthInstanceName(
                            testModule, instanceIndex);
                    if (!testModule.startsWith("anonymous")) {
                        userToken = performRemoteLogin(userRealm, testMode,
                                modeValue, loginUser, "not" + loginPassword,
                                instanceName);
                    } else {
                        userToken = performRemoteLogin(userRealm, testMode,
                                modeValue, "not" + loginUser, loginPassword,
                                instanceName);
                    }
                    assert (userToken == null);
                    exiting("testLoginNegative");
                } catch (Exception e) {
                    log(Level.SEVERE, "testLoginNegative", e.getMessage());
                    e.printStackTrace();
                    cleanup(testModule, testMode);
                    throw e;
                } finally {
                    if (userToken != null) {
                        destroyToken(userToken);
                    }
                }
            }
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testLoginPositive", se.getMessage());
            throw se;
        }
    }

    /**
     * This method is to clear the initial setup. It does the following:
     * (1) Delete authentication service
     * (2) Delete all users and roles
     * @param testModule - the type of authentication module instance which
     * will be used for authentication.
     * @param testMode - the type of authentication which should be performed
     * (e.g. "module", "user", "service", "realm", "role", "authlevel").
     */
    @Parameters({"testModule", "testMode"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testModule, String testMode)
    throws Exception {
        Object[] params = {testModule, testMode};
        entering("cleanup", params);
        webClient = new WebClient();
        if (isValidTest) {
            try {
                log(Level.FINEST, "cleanup", "UserName: " + loginUser);
                log(Level.FINEST, "cleanup", "TestMode: " + testMode);

                Reporter.log("UserName:" + loginUser);
                Reporter.log("TestMode: " + testMode);

                log(Level.FINE, "cleanup", "Deleting user " +
                        loginUser + " ...");
                idToken = getToken(adminUser, adminPassword, basedn);
                idmc.deleteIdentity(idToken, userRealm, IdType.USER, loginUser);

                smsc = new SMSCommon(idToken);
                if (testMode.equals("role") &&
                        smsc.isPluginConfigured(
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm)) {
                    rolename = (String)rb.getString(testModule + ".rolename");

                    log(Level.FINEST, "cleanup", "rolename = " + rolename);
                    Reporter.log("RoleName: " + rolename);

                    log(Level.FINE, "cleanup", "Deleting role " + rolename + 
                            " ...");
                    idmc.deleteIdentity(idToken, userRealm, IdType.ROLE,
                            rolename);
                }

                if (!testMode.equals("module") &&
                        !testMode.equals("authlevel")) {
                    log(Level.FINE, "cleanup",
                            "Deleting service sub-configuration " +
                            serviceSubConfigName + " ...");
                    deleteAuthConfig(userRealm, serviceSubConfigName);
                }

                if (testMode.equals("realm")) {
                    log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                            userRealm);
                    idmc.deleteRealm(idToken, realm + userRealm);
                }
                exiting("cleanup");
            } catch (Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage());
                e.printStackTrace();
                throw e;
            } finally {
                if (idToken != null) {
                    destroyToken(idToken);
                }      
            }
        } else {
            log(Level.FINEST, "cleanup", "Skipping cleanup for " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
        }
    }
}
