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
 * $Id: AuthContextLogout.java,v 1.4 2009/06/02 17:08:18 cmwesley Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
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
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Automates the authentication test case Core_19.
 */

/**
 * The class AuthContextLogout is used to test whether 
 * <code>AuthContext.logout</code> destroys the underlying session for a 
 * successfully authenticated user.
 */
public class AuthContextLogout extends AuthenticationCommon {
    private IDMCommon idmc;
    private ResourceBundle testResources;
    private String moduleName;
    private SSOToken adminToken;
    private String moduleSubConfig;
    private String userName;
    private String password;
    private String absoluteRealm;
    private List<String> testUserList = new ArrayList<String>();
    private String testDescription;
    
    /**
     * Default Constructor
     */
    public AuthContextLogout() {
        super("AuthContextLogout");
        idmc = new IDMCommon();
        testResources = ResourceBundle.getBundle("authentication" + 
                fileseparator + "AuthContextLogout");
    }
    
    /**
     * Set up the system for testing.  Create an authentication instance,
     * a user, and a realm before the logout test.
     * @param testRealm - the realm in which the test should be executed.  If
     * this realm is not the top-level realm then it will be created.
     */
    @Parameters({"testRealm"}) 
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testRealm) 
    throws Exception {
        Object[] params = {testRealm};
        entering("setup", params);

        try {
            adminToken = getToken(adminUser, adminPassword, realm);
            moduleName = testResources.getString("am-auth-logout-module");
            userName = testResources.getString("am-auth-logout-user");
            password = testResources.getString("am-auth-logout-password");
            testDescription = testResources.getString(
                    "am-auth-logout-test-description");

            moduleSubConfig = getAuthInstanceName(moduleName);
            if (!testRealm.equals("/")) {
                if (realm.endsWith("/")) {
                    absoluteRealm = realm + testRealm;
                } else {
                    absoluteRealm = realm + "/" + testRealm;
                }
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
            
            log(Level.FINEST, "setup", "moduleName = " + moduleName);
            log(Level.FINEST, "setup", "Realm = " + testRealm);
            log(Level.FINEST, "setup", "User = " + userName);
            log(Level.FINEST, "setup", "Password = " + password);
            
            Reporter.log("ModuleName: " + moduleName);
            Reporter.log("Realm: " + testRealm);
            Reporter.log("User: " + userName);
            Reporter.log("Password: " + password);

            StringBuffer attrBuffer =  new StringBuffer("sn=" + userName).
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
                log(Level.SEVERE, "createUser", "Failed to create user " +
                        "identity " + userName + "...");
                assert false;
            }
            exiting("setup");
        } catch(Exception e) {
            cleanup(testRealm);
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }
    }
    
    /**
     * Tests <code>AuthContext.logout</code> by authenticating a user via remote
     * module-based authentication, retrieving the user's <code>SSOToken</code>,
     * invoking <code>AuthContext.logout</code> on the token, and verifying that
     * the token is no longer valid.
     * @param testRealm - the realm in which the logout test will take place
     */
    @Parameters({"testRealm"}) 
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testAuthContextLogout(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("testAuthContextLogout", params);  
        log(Level.FINE, "testAuthContextLogout", "Test Description: " + 
                testDescription);
        Reporter.log("Test Description: " + testDescription);
        AuthContext lc = null;
        Callback[] callbacks = null;
        SSOToken userToken = null;
        boolean sessionValid = false;

        try {
            lc = new AuthContext(testRealm);
            log(Level.FINE, "testAuthContextLogout", 
                    "Calling AuthContext.login with the " + moduleSubConfig + 
                    " module.");
            lc.login(AuthContext.IndexType.MODULE_INSTANCE, moduleSubConfig);
        } catch (AuthLoginException le) {
            log(Level.SEVERE, "testAuthContextLogout", 
                    "An exception occurred during AuthContext.login()");
            log(Level.SEVERE, "testAuthContextLogout", le.getMessage());
            le.printStackTrace();
            assert false;
        }
        
        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            if (callbacks != null) {
                try {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            NameCallback namecallback =
                                    (NameCallback)callbacks[i];
                            namecallback.setName(userName);
                        }
                        if (callbacks[i] instanceof PasswordCallback) {
                            PasswordCallback passwordcallback =
                                    (PasswordCallback)callbacks[i];
                            passwordcallback.setPassword(
                                    password.toCharArray());
                        }
                    }
                    lc.submitRequirements(callbacks);
                } catch (Exception e) {
                    log(Level.SEVERE, "testAuthContextLogout", e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        }
        
        if (lc.getStatus() == AuthContext.Status.SUCCESS) {
            try {
                userToken = lc.getSSOToken();                
                log(Level.FINE, "testAuthContextLogout", 
                        "Verifying expected behavior that the user token is " +
                        "valid after successful authentication ...");                
                sessionValid = 
                        SSOTokenManager.getInstance().isValidToken(userToken);
                if (!sessionValid) {
                    log(Level.SEVERE, "testAuthContextLogout", 
                            "Token for user " + userName + " is not valid");
                    assert false;
                }

                log(Level.FINE, "testAuthContextLogout", 
                        "Calling AuthContext.logout for user " + userName);
                lc.logout();

                log(Level.FINE, "testAuthContextLogout", 
                        "Verifying expected behavior that the user token is " +
                        "no longer valid after logout");
                try {
                    SSOTokenManager.getInstance().refreshSession(userToken);                
                } catch (SSOException ssoe) {
                    log(Level.FINEST, "testAuthContextLogout", 
                            "SSOException message = " + ssoe.getMessage());
                    log(Level.FINEST, "testAuthContextLogout", 
                            "An SSOException was thrown when calling " +
                            "refereshSession for an invalid session");
                    sessionValid = false;
                }

                if (sessionValid) {
                    log(Level.SEVERE, "testAuthContextLogout", 
                            "Token for user " + userName + 
                            " is still valid after logout");
                }
                assert !sessionValid;  
                exiting("testAuthContextLogout");
            } catch (Exception ex) {
                log(Level.SEVERE, "testAuthContextLogout", ex.getMessage());
                ex.printStackTrace();
                cleanup(testRealm);
                throw ex;
            } finally {
                if (sessionValid) {
                    destroyToken(userToken);
                }
            }
        } else {
            log(Level.SEVERE, "testAuthContextLogout", "The user " + userName + 
                    " did not authenticate successfully.");
            assert false;
        }
    }
    
    /**
     * Performs cleanup after tests are done.
     * Deletes the authentication instances, users, and realms created by this 
     * test scenario.
     * @param testRealm - the realm which will be deleted
     */
    @Parameters({"testRealm"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testRealm)
    throws Exception {
        Object[] params = {testRealm};
        entering("cleanup", params);

        try {
            adminToken = getToken(adminUser, adminPassword, realm);
            log(Level.FINE, "cleanup", "Deleting user " + testUserList + "...");
            idmc.deleteIdentity(adminToken, testRealm, IdType.USER, userName);
            
            if (!testRealm.equals("/")) {
                if (!absoluteRealm.startsWith("/")) {
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
}
