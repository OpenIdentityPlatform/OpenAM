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
 * $Id: ProfileAttributesTest.java,v 1.12 2009/06/02 17:08:18 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
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
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class is called by the <code>ProfileAttributesTest</code>.
 * Performs the tests for User profile attributes .Test cases
 * coveres are Global Test cases
 * AMSubRealmAuth,Core_5,Core_7
 * 
 */
public class ProfileAttributesTest extends AuthenticationCommon {

    private IDMCommon idmc;
    private SMSCommon smsc;
    private ResourceBundle testResources;
    private String testModule;
    private String testAttribute;
    private boolean createTestUser;
    private String testUser;
    private String testUserpass;
    private String testPassmsg;
    private SSOToken adminToken;
    private String strServiceName = "iPlanetAMAuthService";
    private String profileAttrName = "iplanet-am-auth-dynamic-profile-creation";
    private Set oriAuthAttrValues;
    private List<String> testUserList = new ArrayList<String>();
    private List<IdType> idTypeList = new ArrayList<IdType>();
    
    /**
     * Default Constructor
     **/
    public ProfileAttributesTest() {
        super("ProfileAttributesTest");
        idmc = new IDMCommon();
    }
    
    /**
     * Reads the necessary test configuration and prepares the system
     * for user profile testing
     * - Sets the profile attribute
     * - Create Users , If needed
     * @param testProfile - a String used to set the "User Profile" attribute in
     * the Authentication service.
     */
    @Parameters({"testProfile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testProfile)
    throws Exception {
        Object[] params = {testProfile};
        entering("setup", params);
        try {
            testAttribute = "am-auth-" + testProfile ;
            testResources = ResourceBundle.getBundle("authentication" +
                    fileseparator + "ProfileAttributesTest");
            testModule = testResources.getString(testAttribute + 
                    "-test-module");
            String createUserProp = testResources.getString(testAttribute
                    + "-test-createUser");
            createTestUser = new Boolean(createUserProp).booleanValue();
            testUser = testResources.getString(testAttribute + 
                    "-test-username");
            testUser.trim();
            testUserpass = testResources.getString(testAttribute
                    + "-test-userpassword");
            testUserpass.trim();
            testPassmsg = testResources.getString(testAttribute + 
                    "-test-passmsg");
            
            log(Level.FINEST, "setup", "profileAttribute: " + testProfile);
            log(Level.FINEST, "setup", "testModule: " + testModule);
            log(Level.FINEST, "setup", "createTestUser: " + createTestUser);
            log(Level.FINEST, "setup", "testUser: " + testUser);
            log(Level.FINEST, "setup", "testUserPassword: " + testUserpass);
            log(Level.FINEST, "setup", "testPassmsg: " + testPassmsg);

            Reporter.log("Profile Creation Attribute: " + testProfile);
            Reporter.log("Auth Module: " + testModule);
            Reporter.log("Test Creates User: " + createTestUser);
            Reporter.log("User: " + testUser);
            Reporter.log("User Password: " + testUserpass);
            Reporter.log("Test Passed Msg: " + testPassmsg);
            
            adminToken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(adminToken);
            log(Level.FINE, "setup", "Retrieving the attribute value of " +
                    profileAttrName + " from " + strServiceName + " in realm " +
                    realm + "...");
            oriAuthAttrValues = (Set) smsc.getAttributeValue(realm,
                    strServiceName, profileAttrName, "Organization");
            log(Level.FINEST, "setup", "Original value of " + profileAttrName +
                    ": "  + oriAuthAttrValues);

            String testAttrValue = getProfileAttribute(testProfile);
            Set valSet = new HashSet();
            valSet.add(testAttrValue);
            log(Level.FINE, "setup", "Setting authentication attribute " +
                    profileAttrName + " to \'" + testAttrValue + "\'.");
            smsc.updateSvcAttribute(realm, strServiceName, profileAttrName,
                    valSet, "Organization");
            
            testUserList.add(testUser);
            if (createTestUser) {
                idTypeList.add(IdType.USER);
                StringBuffer attrBuffer = new StringBuffer("sn=" + testUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("cn=" + testUser).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("userpassword=" + testUserpass).
                        append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                        append("inetuserstatus=Active");

                log(Level.FINE, "setup", "Creating user " + testUser + " ...");
                if (!idmc.createID(testUser, "user", attrBuffer.toString(),
                        adminToken, realm)) {
                    log(Level.SEVERE, "setup",
                            "Failed to create user identity " + testUser);
                    assert false;
                }
            }
            exiting("setup");
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup(testProfile);
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }

    }
    
    /**
     * Validate the profile tests
     * @param testProfile - a String used to set the "User Profile" attribute in
     * the Authentication service.
     * @param instanceIndex - the index of the auth module instance from
     * AuthenticationConfig.properties
     */
    @Parameters({"testProfile", "instanceIndex"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testProfile(String testProfile, String instanceIndex)
    throws Exception {
        Object[] params = {testProfile, instanceIndex};
        entering("testProfile", params);

        try {
            log(Level.FINEST, "testProfile",
                    "Description: Test authentication " +
                    "with iplanet-am-auth-dynamic-profile-creation set to " +
                    testProfile);
            Reporter.log("Description: Test authentication with " +
                    "iplanet-am-auth-dynamic-profile-creation set to " +
                    testProfile);
            Map executeMap = new HashMap();
            String moduleSubConfig = getAuthInstanceName(testModule,
                    instanceIndex);
            executeMap.put("redirectURL",
                    getLoginURL("/") + "?module=" + moduleSubConfig);
            executeMap.put("users", testUser + ":" + testUserpass);
            executeMap.put("successMsg", testPassmsg);
            executeMap.put("uniqueIdentifier", testProfile + "-test");
            executeMap.put("Loginuser", testUser);
            executeMap.put("Loginpassword", testUserpass);
            executeMap.put("Passmsg", testPassmsg);
            executeMap.put("profileattr", testProfile);
            testFormBasedAuth(executeMap);
            exiting("testProfile");
        } catch (Exception e) {
            log(Level.SEVERE, "testProfile", e.getMessage());
            e.printStackTrace();
            cleanup(testProfile);
            throw e;
        }
    }
    
    /**
     * performs cleanup after tests are done.
     * @param testProfile - testProfile - a String used to set the
     * "User Profile" attribute in the Authentication service.
     */
    @Parameters({"testProfile"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testProfile)
    throws Exception {
        Object[] params = {testProfile};
        entering("cleanup", params);

        try {
            adminToken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(adminToken);
            log(Level.FINE, "cleanup", "Set " + profileAttrName + " to " +
                    oriAuthAttrValues);
            smsc.updateSvcAttribute(realm, strServiceName, profileAttrName,
                    oriAuthAttrValues, "Organization");

            if (!testProfile.equals("ignored")) {
                idmc.deleteIdentity(adminToken, realm, IdType.USER, testUser);
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
