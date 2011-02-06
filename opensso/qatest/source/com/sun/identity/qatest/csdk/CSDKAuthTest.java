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
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
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
public class CSDKAuthTest extends AuthenticationCommon {

    private boolean isValidTest = true;
    private CSDKCommon cc;
    private ResourceBundle rb;
    private String moduleServiceName;
    private String moduleSubConfigName;
    private String serviceName;
    private String serviceSubConfigName;
    private String rolename;
    private String user;
    private String password;
    private String libraryPath;
    private String directoryPath;
    private String bootstrapFile;
    private String configurationFile;
    private IDMCommon idmc;
    private SSOToken adminToken;
    private SMSCommon smsc;

    /**
     * Constructor for the class.
     */
    public CSDKAuthTest() {
        super("CSDKAuthTest");
        cc = new CSDKCommon();
        idmc = new IDMCommon();
    }

    /**
     * This method is to configure the initial setup. It does the following:
     * (1) Create a module instance with a given auth level
     * (2) Create an auth service using the auth module
     * (3) Create a new user and assign the auth service to the user
     * (4) Create a new role
     * (5) Assign the auth service and the user to that role
     * This is called only once per auth module.
     */
    @Parameters({"testModule", "testMode"})
    @BeforeClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testModule, String testMode)
            throws Exception {
        Object[] params = {testModule, testMode};
        entering("setup", params);
        Map ldMap = cc.getLibraryPath();
        libraryPath = (String) ldMap.get("libraryPath");
        directoryPath = (String) ldMap.get("directoryPath");
        bootstrapFile = cc.getBootStrapFilePath();
        configurationFile = cc.getConfigurationFilePath();

        try {
            isValidTest = isValidModuleTest(testModule);
            if (isValidTest) {
                rb = ResourceBundle.getBundle("csdk" + fileseparator +
                        "CSDKAuthTest");
                moduleServiceName = (String) rb.getString("CSDKAuthTest" + "."
                        + testModule + ".module-servicename");
                moduleSubConfigName = (String) rb.getString("CSDKAuthTest" +
                        "." + testModule + ".module-subconfigname");
                serviceSubConfigName = (String) rb.getString("CSDKAuthTest" +
                        "." + testModule + ".service-subconfigname");
                String serviceDetails = (String)rb.getString("CSDKAuthTest" +
                        "." + testModule + ".service-details");
                rolename = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".rolename");
                user = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".user");
                password = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".password");

                log(Level.FINEST, "setup", "moduleServiceName: " +
                        moduleServiceName);
                log(Level.FINEST, "setup", "moduleSubConfigName: " +
                        moduleSubConfigName);
                log(Level.FINEST, "setup", "serviceSubConfigName: " +
                        serviceSubConfigName);

                log(Level.FINEST, "setup", "rolename: " + rolename);
                log(Level.FINEST, "setup", "username: " + user);
                log(Level.FINEST, "setup", "userpassword: " + password);

                Reporter.log("ModuleServiceName: " + moduleServiceName);
                Reporter.log("ModuleSubConfigName: " + moduleSubConfigName);
                Reporter.log("ServiceSubConfigName: " + serviceSubConfigName);

                Reporter.log("RoleName: " + rolename);
                Reporter.log("UserName: " + user);
                Reporter.log("UserPassword: " + password);

                Map<String,String> globalUpdateMap = new HashMap();
                globalUpdateMap.put("instances-to-create", testModule + ",1");
                this.setPropsInGlobalAuthInstancesMap(globalUpdateMap);
                createAuthInstances();

                if (!testMode.equals("module") &&
                        !testMode.equals("authlevel")) {
                    String[] configInstances = {serviceDetails};
                    Map configMap = new HashMap();
                    createAuthConfig(realm, serviceSubConfigName,
                            configInstances, configMap);
                }

                StringBuffer attrBuffer = new StringBuffer("sn=" + user).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + user).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + password).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active").
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("iplanet-am-user-auth-config=" +
                        serviceSubConfigName);

                log(Level.FINE, "setup", "Creating user " + user + " ...");
                adminToken = getToken(adminUser, adminPassword, basedn);
                if (!idmc.createID(user, "user", attrBuffer.toString(),
                        adminToken, realm)) {
                    log(Level.SEVERE, "setup",
                            "Failed to create user identity " + user + " ...");
                    assert false;
                }

                smsc = new SMSCommon(adminToken);
                if (testMode.equals("role") && smsc.isPluginConfigured(
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm)) {
                    log(Level.FINE, "setup", "Creating role " + rolename +
                            " ...");
                    if (!idmc.createID(rolename, "role", null, adminToken,
                            realm)) {
                        log(Level.SEVERE, "createUser", "Failed to create role "
                                + rolename + " ...");
                        assert false;
                    }

                    log(Level.FINE, "setup", "Assigning the user " + user +
                            " to role " + rolename + " ...");
                    idmc.addUserMember(adminToken, user, rolename,
                            IdType.ROLE, realm);

                    log(Level.FINE, "setup", "Assigning the service " +
                            serviceName + " to the role " +
                            rolename + "...");
                    idmc.assignSvcIdentity(adminToken, rolename, "role",
                            AUTH_CONFIGURATION_SERVICE_NAME, realm,
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
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup(testModule, testMode);
            throw e;
        } finally {
            if (isValidTest) {
                destroyToken(adminToken);
            }
        }
    }

    /**
     * Tests for successful login into the system using correct
     * credentials
     */
    @Parameters({"testModule", "testMode", "testModeNo"})
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCSDKAuthPositive(String testModule, String testMode,
            String testModeNo)
            throws Exception {
        Object[] params = {testModule, testMode, testModeNo};
        entering("testCSDKAuthPositive", params);
        if (isValidTest) {
            try {
                String results;
                String error;
                String loginUser = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".user");
                String loginPassword = (String) rb.getString("CSDKAuthTest" +
                        "." + testModule +
                        ".password");
                String modevalue = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule +
                        ".modevalue." + testMode);

                Reporter.log("Test Description : " + (String) rb.getString
                        ("CSDKAuthTest" + "." + testModule + ".modevalue." +
                        testMode + ".description") + (String) rb.getString
                        ("CSDKAuthTest.positiveTestMessage"));
                Reporter.log("Execution command :" + " am_auth_test " +
                        " -u " + loginUser + " -p " +
                        loginPassword +
                        " -f " + "  bootstrapFile " + " -o " + realm +
                        " -t " + testModeNo + " -m " + modevalue);
                ProcessBuilder pb = new ProcessBuilder(directoryPath +
                        fileseparator + "am_auth_test", "-u", loginUser,
                        "-p", loginPassword, "-f", bootstrapFile, "-o", realm,
                        "-t", testModeNo,
                        "-m", modevalue);
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
                if (sbResults != null && sbResults.length() > 0) {
                    log(Level.FINEST, "testCSDKAuthPositive",
                            "Result of command = " + sbResults);
                    if (sbResults.toString().contains("Succeeded!")) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else {
                    log(Level.SEVERE, "testCSDKAuthPositive",
                            "Output buffer sbResults is null or empty");
                    assert false;
                }
                exiting("testCSDKAuthPositive");
            } catch (Exception e) {
                log(Level.SEVERE, "testCSDKAuthPositive", e.getMessage());
                cleanup(testModule, testMode);
                throw e;
            }
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testCSDKAuthPositive", se.getMessage());
            throw se;
        }
    }

    /**
     * Tests for unsuccessful login into the system using incorrect
     * credentials
     */
    @Parameters({"testModule", "testMode", "testModeNo"})
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCSDKAuthNegative(String testModule, String testMode,
            String testModeNo)
            throws Exception {
        Object[] params = {testModule, testMode, testModeNo};
        entering("testCSDKAuthNegative", params);
        if (isValidTest) {
            try {
                String error;
                String results;
                String loginUser = (String) rb.getString("CSDKAuthTest" + "."
                        + testModule + ".user");
                String loginPassword = (String) rb.getString("CSDKAuthTest" +
                        "." + testModule +
                        ".password");
                if (testModule.equals("anonymous")) {
                    loginUser = loginUser + "negative";
                }
                String modevalue = (String) rb.getString("CSDKAuthTest" + 
                        "." + testModule + ".modevalue." + testMode);
                Reporter.log("Test Description : " + (String) rb.getString
                        ("CSDKAuthTest" + "." + testModule + ".modevalue." +
                        testMode + ".description") + (String) rb.getString
                        ("CSDKAuthTest.negativeTestMessage"));
                Reporter.log("Execution command :" + " am_auth_test " +
                        " -u " + loginUser + " -p " +
                        loginPassword + "negative" +
                        " -f " + "  bootstrapFile " + " -o " + realm +
                        " -t " + testModeNo + " -m " + modevalue);
                ProcessBuilder pb = new ProcessBuilder(directoryPath +
                        fileseparator + "am_auth_test", "-u", loginUser,
                        "-p", loginPassword + "negative", "-f",
                        bootstrapFile, "-o", realm,
                        "-t", testModeNo,
                        "-m", modevalue);
                pb.environment().put("LD_LIBRARY_PATH", libraryPath);
                pb.directory(new File(directoryPath));
                Process p1 = pb.start();
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p1.getInputStream()));
                StringBuffer sbResults = new StringBuffer();
                while ((results = stdInput.readLine()) != null) {
                    sbResults = sbResults.append(results);
                }
                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(p1.getErrorStream()));
                while ((error = stdError.readLine()) != null) {
                    sbResults = sbResults.append(error);
                }
                if (sbResults != null && sbResults.length() > 0) {
                    log(Level.FINEST, "testCSDKAuthNegative",
                            "Result of command = " + sbResults);
                    if (sbResults.toString().contains("AM_AUTH_FAILURE")) {
                        assert true;
                    } else {
                        assert false;
                    }
                } else {
                    log(Level.SEVERE, "testCSDKAuthNegative",
                            "Output buffer sbResults is null or empty");
                    assert false;
                }
                exiting("testCSDKAuthNegative");
            } catch (Exception e) {
                log(Level.SEVERE, "testCSDKAuthNegative", e.getMessage());
                cleanup(testModule, testMode);
                throw e;
            }
        } else {
            SkipException se = new SkipException("Skipping " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
            log(Level.FINEST, "testCSDKAuthNegative", se.getMessage());
            throw se;        }
    }

    /**
     * This method is to clear the initial setup. It does the following:
     * (1) Delete authentication service
     * (2) Delete authentication instance
     * (3) Delete all users and roles
     * This is called only once per auth module.
     */
    @Parameters({"testModule", "testMode"})
    @AfterClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testModule, String testMode)
            throws Exception {
        Object[] params = {testModule};
        entering("cleanup", params);

        if (isValidTest) {
            try {
                user = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".user");
                rolename = (String) rb.getString("CSDKAuthTest" + "." +
                        testModule + ".rolename");

                log(Level.FINEST, "cleanup", "UserName:" + user);
                log(Level.FINEST, "cleanup", "RoleName:" + rolename);

                Reporter.log("UserName:" + user);
                Reporter.log("RoleName:" + rolename);

                log(Level.FINE, "cleanup", "Deleting user " + user + " ...");
                adminToken = getToken(adminUser, adminPassword, basedn);
                idmc.deleteIdentity(adminToken, realm, IdType.USER, user);

                smsc = new SMSCommon(adminToken);
                if (testMode.equals("role") &&
                        smsc.isPluginConfigured(
                        SMSConstants.UM_DATASTORE_SCHEMA_TYPE_AMSDK, realm)) {
                    rolename = (String)rb.getString(testModule + ".rolename");

                    log(Level.FINEST, "cleanup", "rolename = " + rolename);
                    Reporter.log("RoleName: " + rolename);

                    log(Level.FINE, "cleanup", "Deleting role " + rolename +
                            " ...");
                    idmc.deleteIdentity(adminToken, realm, IdType.ROLE,
                            rolename);
                }

                if (!testMode.equals("module") &&
                        !testMode.equals("authlevel")) {
                    log(Level.FINE, "cleanup",
                            "Deleting authentication configuration " +
                            serviceSubConfigName + " ...");
                    deleteAuthConfig(serviceSubConfigName);
                }
                log(Level.FINE, "cleanup", "Deleting authentication instance " +
                        moduleSubConfigName + " in realm " + realm);
                deleteAuthInstances();
                exiting("cleanup");
            } catch (Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage());
                e.printStackTrace();
                throw e;
            } finally {
                destroyToken(adminToken);
            }
        } else {
            log(Level.FINEST, "cleanup", "Skipping cleanup for " + testModule +
                    " auth module test on unsupported platform or " +
                    "an OpenSSO nightly build.");
        }
    }
}
