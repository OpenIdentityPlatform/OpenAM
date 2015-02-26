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
 * $Id: AMSamples.java,v 1.12 2009/02/14 00:58:08 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.clientsamples;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import com.sun.identity.wss.provider.ProviderConfig;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.URL;
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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class tests the Acccess Manager Client Samples
 */
public class AMSamples extends TestCommon {
    
    private ResourceBundle rb_client;
    private ResourceBundle rb_ams;
    private ResourceBundle rb_amconfig;
    private String clientURL;
    private String serviceconfigURL;
    private String userprofileURL;
    private String policyURL;
    private String ssovalidationURL;
    private String stsclientvalidationURL;
    private String stswscvalidationURL;
    private String baseDir;
    private String userName;
    private String userPassword;
    private String grpName;
    private String roleName;
    private String resourceName;
    private String clientDomain;
    private String polName = "clientSamplePolicyTest";
    private DefaultTaskHandler task;
    private HtmlPage page;
    private SSOToken admintoken;
    private IDMCommon idmc;
    private PolicyCommon pc;
    private WebClient webClient;
    
    /**
     * Creates a new instance of AMSamples and instantiates common objects and
     * classes.
     */
    public AMSamples()
    throws Exception {
        super("AMSamples");
        rb_amconfig = ResourceBundle.getBundle(
                TestConstants.TEST_PROPERTY_AMCONFIG);
        rb_client = ResourceBundle.getBundle("config" + fileseparator +
                "default" + fileseparator + "ClientGlobal");
        rb_ams = ResourceBundle.getBundle("clientsamples" + fileseparator +
                "AMSamples");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        pc = new PolicyCommon();
        baseDir = getBaseDir() + System.getProperty("file.separator")
        + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
        + System.getProperty("file.separator") + "built"
                + System.getProperty("file.separator") + "classes"
                + System.getProperty("file.separator");
    }
    
    /**
     * Deploy the client sampels war on jetty server and start the jetty
     * server.
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void startServer()
    throws Exception {
        entering("startServer", null);
        clientURL = deployClientSDKWar(rb_client);
        exiting("startServer");
    }

    /**
     *  Creates required users and policy.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);
        try {
            log(Level.FINE, "setup", "Client URL: " + clientURL);
            
            serviceconfigURL = clientURL +
                    rb_ams.getString("serviceconfig_uri");
            log(Level.FINE, "setup", "Service Configuration Sample " +
                    "Servlet URL: " +  serviceconfigURL);
                  
            userprofileURL = clientURL + rb_ams.getString("userprofile_uri");
            log(Level.FINE, "setup", "User Profile (Attributes)" +
                    " Sample Servlet URL: "  + userprofileURL);
            
            policyURL = clientURL + rb_ams.getString("policy_uri");
            log(Level.FINE, "setup", "Policy Evaluator Client Sample Servlet" +
                    " URL: " + policyURL);
            
            ssovalidationURL = clientURL +
                    rb_ams.getString("ssovalidation_uri");
            log(Level.FINE, "setup", "Single Sign On Token " +
                    "Verification Servlet" +  " URL: " + ssovalidationURL);
                  
            stsclientvalidationURL = clientURL +
                    rb_ams.getString("stsclientvalidation_uri");
            log(Level.FINE, "setup", "STS Client Sample with End user " +
                    "  Token JSP URL: " + stsclientvalidationURL);
            
            stswscvalidationURL = clientURL +
                    rb_ams.getString("stswscvalidation_uri");
            log(Level.FINE, "setup", "STS Client Sample for WSC Token JSP" +
                    " URL: " + stswscvalidationURL);
            
	    roleName = rb_ams.getString("cs_rolename");
            grpName = rb_ams.getString("cs_groupname");
            
            Map map = new HashMap();
            Set set = new HashSet();
            userName = rb_ams.getString("cs_username");
            set.add(userName);
            map.put("sn", set);
            set = new HashSet();
            set.add(userName);
            map.put("cn", set);
            set = new HashSet();
            userPassword = rb_ams.getString("cs_password");
            set.add(userPassword);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("inetuserstatus", set);
            
            idmc.createIdentity(admintoken, realm, IdType.USER, userName, map);
            
            resourceName = rb_ams.getString("policy_resource");
            String xmlFile = "client-samples-policy-test.xml";
            createPolicyXML(xmlFile);
            assert(pc.createPolicy(xmlFile, realm));
            
        } catch (Exception e) {
            log(Level.SEVERE, "setup", "Exception in the setup ...");
            e.printStackTrace();
            throw e;
        } 
        exiting("setup");
    }
    
    /**
     * This test validates the user attributes for a super admin user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUserProfileAdminUser()
    throws Exception {
        entering("testUserProfileAdminUser", null);
        try {
            String res = rb_ams.getString("userprofile_pass");
            String xmlFile = "testUserProfileAdminUser.xml";
            generateUserProfileXML(realm, adminUser, adminPassword, xmlFile,
                    res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, adminUser) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a super" +
                    " admin user.");
        }
        exiting("testUserProfileAdminUser");
    }
    
    /**
     * This test validates the user attributes for a user with no admin
     * privilages
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUserProfileNonadminUser()
    throws Exception {
        entering("testUserProfileNonadminUser", null);
        try {
            String res = rb_ams.getString("userprofile_pass");
            String xmlFile = "testUserProfileNonadminUser.xml";
            generateUserProfileXML(realm, userName, userName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, userName) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileNonadminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a user" +
                    " with no admin privilages.");
        }
        exiting("testUserProfileNonadminUser");
    }
    
    /**
     * This test validates the user attributes for a non existing user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUserProfileNonExistingUser()
    throws Exception {
        entering("testUserProfileNonExistingUser", null);
        try {
            String res = rb_ams.getString("userprofile_error");
            String xmlFile = "testUserProfileExistingUser.xml";
            generateUserProfileXML(realm, "doesnotexist", "doesnotexist",
                    xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileNonExistingUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a non" +
                    " existing user.");
        }
        exiting("testUserProfileNonExistingUser");
    }
    
    /**
     * This test validates the user attributes for a user with incorrect
     * credentials (incorrect password).
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testUserProfileInvalidUserCredential()
    throws Exception {
        entering("testUserProfileInvalidUserCredential", null);
        try {
            String res = rb_ams.getString("userprofile_error");
            String xmlFile = "testUserProfileInvalidUserCredential.xml";
            generateUserProfileXML(realm, adminUser, adminPassword + "wrong",
                    xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileInvalidUserCredential",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a user" +
                    " with incorrect credentials (incorrect password).");
        }
        exiting("testUserProfileInvalidUserCredential");
    }
    
    /**
     * This test validates the service configuration for
     * iplanetAMPlatformService service for configuration type set to Schema.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceConfigPlatformSvcSchema()
    throws Exception {
        entering("testServiceConfigPlatformSvcSchema", null);
        try {
            String res = rb_ams.getString("serviceconfig_schema_pass");
            String xmlFile = "testServiceConfigPlatformSvcSchema.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMPlatformService", "globalSchema", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigPlatformSvcSchema",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " iplanetAMPlatformService service for configuration type" +
                    " set to Schema.");
        }
        exiting("testServiceConfigPlatformSvcSchema");
    }
    
    /**
     * This test validates the service configuration for
     * iplanetAMPlatformService service for configuration type set to Config.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceConfigPlatformSvcConfig()
    throws Exception {
        entering("testServiceConfigPlatformSvcConfig", null);
        try {
            String res = rb_ams.getString("serviceconfig_config_pass");
            String xmlFile = "testServiceConfigPlatformSvcConfig.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMPlatformService", "globalConfig", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigPlatformSvcConfig",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " iplanetAMPlatformService service for configuration type" +
                    " set to Config.");
        }
        exiting("testServiceConfigPlatformSvcConfig");
    }
    
    /**
     * This test validates the service configuration for not existing service
     * for configuration type set to Schema.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceConfigSvcNotExistSchema()
    throws Exception {
        entering("testServiceConfigSvcNotExistSchema", null);
        try {
            String res = rb_ams.getString("serviceconfig_error");
            String xmlFile = "testServiceConfigSvcNotExistSchema.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMSvcNotExistSchema", "globalSchema", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigSvcNotExistSchema",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " not existing service for configuration type set to" +
                    " Schema.");
        }
        exiting("testServiceConfigSvcNotExistSchema");
    }
    
    /**
     * This test validates the service configuration for not existing service
     * for configuration type set to Config.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testServiceConfigSvcNotExistConfig()
    throws Exception {
        entering("testServiceConfigSvcNotExistConfig", null);
        try {
            String res = rb_ams.getString("serviceconfig_error");
            String xmlFile = "testServiceConfigSvcNotExistConfig.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMSvcNotExistConfig", "globalConfig", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigSvcNotExistConfig",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " not existing service for configuration type set to" +
                    " Config.");
        }
        exiting("testServiceConfigSvcNotExistConfig");
    }
    
    /**
     * This test validates valid authorization (allow access) to a policy
     * resource for a super admin user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testPolicyEvalPassAdminUser()
    throws Exception {
        entering("testPolicyEvalPassAdminUser", null);
        try {
            String res = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalPassAdminUser.xml";
            generatePolicyEvalXML(realm, adminUser, adminPassword,
                    "iPlanetAMWebAgentService", resourceName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalPassAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (allow access) to a policy resource for a super admin" +
                    " user.");
        }
        exiting("testPolicyEvalPassAdminUser");
    }
    
    /** his test validates valid authorization(deny access) to a policy
     * resource for a super admin user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testPolicyEvalFailAdminUser()
    throws Exception {
        entering("testPolicyEvalFailAdminUser", null);
        try {
            String res = rb_ams.getString("policy_error");
            String resP = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalFailAdminUser.xml";
            generatePolicyEvalXML(realm, adminUser, adminPassword,
                    "iPlanetAMWebAgentService", resourceName + "Fail",
                    xmlFile, "");
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, resP) != -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalFailAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (deny access) to a policy resource for a super admin" +
                    " user.");
        }
        exiting("testPolicyEvalFailAdminUser");
    }
    
    /**
     * This test validates valid authorization (allow access) to a policy
     * resource for a user with no admin privilages.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testPolicyEvalPassTestUser()
    throws Exception {
        entering("testPolicyEvalPassTestUser", null);
        try {
            String res = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalPassTestUser.xml";
            generatePolicyEvalXML(realm, userName, userName,
                    "iPlanetAMWebAgentService", resourceName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalPassTestUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (allow access) to a policy resource for a user with no" +
                    " admin privilages.");
        }
        exiting("testPolicyEvalPassTestUser");
    }
    
    /**
     * This test validates valid authorization (deny access) to a policy
     * resource for a user with no admin privilage.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testPolicyEvalFailTestUser()
    throws Exception {
        entering("testPolicyEvalFailTestUser", null);
        try {
            String res = rb_ams.getString("policy_error");
            String resP = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalFailTestUser.xml";
            generatePolicyEvalXML(realm, userName, userName,
                    "iPlanetAMWebAgentService", resourceName + "Fail",
                    xmlFile, "");
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, resP) != -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalFailTestUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (deny access) to a policy resource for a user with no" +
                    " admin privilage.");
        }
        exiting("testPolicyEvalFailTestUser");
    }
    
    /**
     * This test validates SSO Token for a super admin user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSSOVerificationServletAdminUser()
    throws Exception {
        entering("testSSOVerificationServletAdminUser", null);
        String loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        webClient = new WebClient();
        try {
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            Thread.sleep(5000);
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (getHtmlPageStringIndex(page, adminUser) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletAdminUser",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            Reporter.log("This test validates SSO Token for a super admin" +
                    " user.");
        }
        exiting("testSSOVerificationServletAdminUser");
    }
    
    /**
     * This test validates SSO Token for a user with no admin privilages.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSSOVerificationServletTestUser()
    throws Exception {
        entering("testSSOVerificationServletTestUser", null);
        String loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        webClient = new WebClient();
        try {
            consoleLogin(webClient, loginURL, userName, userName);
            Thread.sleep(5000);
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (getHtmlPageStringIndex(page, userName) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletTestUser",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            Reporter.log("This test validates SSO Token for a user with no" +
                    " admin privilages.");
        }
        exiting("testSSOVerificationServletTestUser");
    }
    
    /**
     * This test validates SSO Token failure when no user token is available.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSSOVerificationServletError()
    throws Exception {
        entering("testSSOVerificationServletError", null);
        try {
            String res = rb_ams.getString("ssovalidation_error");
            webClient = new WebClient();
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (getHtmlPageStringIndex(page, res) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletError",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates SSO Token failure when no user" +
                    " token is available.");
        }
        exiting("testSSOVerificationServletError");
    }
    
    /**
     * This test validates STS Token for a super admin user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSTSClientAdminUser()
    throws Exception {
        entering("testSTSClientAdminUser", null);
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        try {
            String res = adminUser;
            String xmlFile = "testSTSClientAdminUser.xml";
            
            generateSTSClientXML(adminUser, adminPassword, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            log(Level.FINEST, "testSTSClientAdminUser",
                    "testSTSClientAdminUser page after login\n" +
                    page.getWebResponse().getContentAsString());
            
            if (getHtmlPageStringIndex(page, res) == -1)
                assert false;
            
        } catch (Exception e) {
            log(Level.SEVERE, "testSTSClientAdminUser", e.getMessage());
            e.printStackTrace();
        } finally {
            Reporter.log("This test validates valid STS token generated for " +
                    " valid admin user SSOToken");
            consoleLogout(webClient, logoutURL);
        }
        exiting("testSTSClientAdminUser");
    }
    
    /**
     * This test validates STS Token for a normal user.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSTSClientNormalUser()
    throws Exception {
        entering("testSTSClientNormalUser", null);
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        try {
            String res = userName;
            String xmlFile = "testSTSClientNormalUser.xml";
            
            generateSTSClientXML(userName, userPassword, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            log(Level.FINEST, "testSTSClientNormalUser",
                    "testSTSClientNormalUser page after login\n" +
                    page.getWebResponse().getContentAsString());
            if (getHtmlPageStringIndex(page, res) == -1)
                assert false;
            
        } catch (Exception e) {
            log(Level.SEVERE, "testSTSClientNormalUser", e.getMessage());
            e.printStackTrace();
        } finally {
            Reporter.log("This test validates valid STS token generated for " +
                    " valid normal user SSOToken");
            consoleLogout(webClient, logoutURL);
        }
        exiting("testSTSClientNormalUser");
    }
    
    /**
     * This test validates STS Token for a normal user with SAML attribute
     * mapper. It also verifies the Memberships for the user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSTSClientUserSAMLAttribute()
    throws Exception {
        entering("testSTSClientUserSAMLAttribute", null);
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        try {
            String xmlFile = "testSTSClientUserSAMLAttribute.xml";
            AMIdentity amid = new AMIdentity(admintoken, userName,
                    IdType.USER, realm, null);
            assert(amid.isExists());
            
            Map idmAttrMap = new HashMap();
            idmAttrMap = amid.getAttributes();
            idmAttrMap = new HashMap();
            Set set = new HashSet();
            set.add("Cupertino");
            idmAttrMap.put("postaladdress", set);
            set = new HashSet();
            set.add("user@yahoo.com");
            idmAttrMap.put("mail", set);
            idmc.modifyIdentity(amid, idmAttrMap);
            
            //create group and add user to the group
            String grpAttr = null;
            assert(idmc.createID(grpName, "group", grpAttr, admintoken, realm));
            AMIdentity grpid = new AMIdentity(admintoken, grpName,
                    IdType.GROUP, realm, null);
            idmc.addUserMember(admintoken, userName, grpName,
                    IdType.GROUP, realm);
            assert(amid.isMember(grpid));
            
            //create role and add user to the role
            String roleAttr = null;
            assert(idmc.createID(roleName, "role", grpAttr, admintoken, realm));
            AMIdentity roleid = new AMIdentity(admintoken, roleName,
                    IdType.ROLE, realm, null);
            idmc.addUserMember(admintoken, userName, roleName,
                    IdType.ROLE, realm);
            assert(amid.isMember(roleid));
            
            ProviderConfig pc = ProviderConfig.
                    getProvider("wsp", ProviderConfig.WSP);
            assert (ProviderConfig.isProviderExists("wsp", ProviderConfig.WSP));
            Set attributeSet = new HashSet();
            attributeSet.add("address=postaladdress");
            attributeSet.add("emailid=mail");
            pc.setSAMLAttributeMapping(attributeSet);
            boolean include = true;
            pc.setIncludeMemberships(include);
            ProviderConfig.saveProvider(pc);
            
            AMIdentity wspAmid = new AMIdentity(admintoken, "wsp",
                    IdType.AGENTONLY, realm, null);
            assert (wspAmid.isExists());
            Map wspAttrMap;
            wspAttrMap = new HashMap();
            wspAttrMap = wspAmid.getAttributes();
            log(Level.FINEST, "testSTSClientUserSAMLAttribute",
                    "WSP attributes \n" + wspAttrMap);
            
            generateSTSClientXML(userName, userPassword, xmlFile, userName);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            log(Level.FINEST, "testSTSClientUserSAMLAttribute",
                    "testSTSClientuserAttributes page after login\n" +
                    page.getWebResponse().getContentAsString());
            if (getHtmlPageStringIndex(page, userName) == -1)
                assert false;
            else if (getHtmlPageStringIndex(page, "Cupertino") == -1)
                assert false;
            else if (getHtmlPageStringIndex(page, "user@yahoo.com") == -1)
                assert false;
            else if (getHtmlPageStringIndex(page, roleName) == -1)
                assert false;
            else if (getHtmlPageStringIndex(page, grpName) == -1)
                assert false;
            
        } catch (Exception e) {
            log(Level.SEVERE, "testSTSClientUserSAMLAttribute", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid STS token generated for " +
                    " valid normal user SSOToken and the mapping of the" +
                    " SAML attributes. It also verifies the Memberships " +
                    "for the user ");
            consoleLogout(webClient, logoutURL);
        }
        exiting("testSTSClientUserSAMLAttribute");
    }
    
    /**
     * This test validates STS Token for WSC of type SAMLv2
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSTSwscSAMLv2Token()
    throws Exception {
        entering("testSTSwscSAMLv2Token", null);
        
        try {
            String expResult = "SAML:2.0:assertion";
            webClient = new WebClient();
            
            AMIdentity stsAmid = new AMIdentity(admintoken,
                    "SecurityTokenService", IdType.AGENTONLY, realm, null);
            assert(stsAmid.isExists());
            
            AMIdentity amid = new AMIdentity(admintoken, "wsc",
                    IdType.AGENTONLY, realm, null);
            assert(amid.isExists());
            Map idmAttrMap = new HashMap();
            idmAttrMap = amid.getAttributes();
            idmAttrMap = new HashMap();
            Set set = new HashSet();
            set.add("SecurityTokenService");
            idmAttrMap.put("STS", set);
            idmc.modifyIdentity(amid, idmAttrMap);
            idmAttrMap = new HashMap();
            set = new HashSet();
            set.add("urn:sun:wss:sts:security");
            idmAttrMap.put("SecurityMech", set);
            idmc.modifyIdentity(amid, idmAttrMap);
            set = new HashSet();
            set.add("default");
            idmAttrMap = new HashMap();
            idmAttrMap.put("WSPEndpoint", set);
            idmc.modifyIdentity(amid, idmAttrMap);
            
            idmAttrMap = new HashMap();
            idmAttrMap = amid.getAttributes();
            log(Level.FINEST, "testSTSwscSAMLv2Token",
                    "WSC attributes \n" + idmAttrMap);
            
            //Setup the WSP security mechanism to 2.0 token
            
            ProviderConfig pc = ProviderConfig.
                    getProvider("wsp", ProviderConfig.WSP);
            assert (ProviderConfig.isProviderExists("wsp", ProviderConfig.WSP));
            List listSec = new ArrayList();
            listSec = pc.getSecurityMechanisms();
            if ((listSec.contains("urn:sun:wss:security:null:SAMLToken-HK"))) {
                listSec.remove("urn:sun:wss:security:null:SAMLToken-HK");
            }
            if ((listSec.contains("urn:sun:wss:security:null:SAMLToken-SV"))) {
                listSec.remove("urn:sun:wss:security:null:SAMLToken-SV");
            }
            if (!(listSec.contains("urn:sun:wss:security:null:SAML2Token-HK")))
            {
                listSec.add("urn:sun:wss:security:null:SAML2Token-HK");
            }
            if (!(listSec.contains("urn:sun:wss:security:null:SAML2Token-SV")))
            {
                listSec.add("urn:sun:wss:security:null:SAML2Token-SV");
            }
            
            AMIdentity wspAmid = new AMIdentity(admintoken, "wsp",
                    IdType.AGENTONLY, realm, null);
            assert (wspAmid.isExists());
            Map wspAttrMap;
            wspAttrMap = new HashMap();
            pc.setSecurityMechanisms(listSec);
            ProviderConfig.saveProvider(pc);
            wspAttrMap = wspAmid.getAttributes();
            log(Level.FINEST, "testSTSwscSAMLv2Token",
                    "WSP attributes \n" + wspAttrMap);
            
            URL cmdUrl = new URL(stswscvalidationURL);
            HtmlPage page = (HtmlPage) webClient.getPage(cmdUrl);
            HtmlForm form = (HtmlForm) page.getForms().get(0);
            
            HtmlTextInput txtagentname = (HtmlTextInput) form.
                    getInputByName("providerName");
            txtagentname.setValueAttribute("wsc");
            
            HtmlPage returnPage = (HtmlPage) form.getInputByName("Submit").click();
            log(Level.FINEST, "testSTSwscSAMLv2Token",
                    " Page after login\n" +
                    returnPage.getWebResponse().getContentAsString());
            int iIdx = getHtmlPageStringIndex(returnPage, expResult);
            assert (iIdx != -1);
            
        } catch (Exception e) {
            log(Level.SEVERE, "testSTSwscSAMLv2Token", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("Test Description: Verifying the WSC token " +
                    "(of type SAMLv2)from STS  which can be presented to " +
                    "  the WSP for authentication purposes");
        }
        exiting("testSTSwscSAMLv2Token");
    }
    
    /**
     * This test validates STS Token for WSC of type SAMLv1.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"testSTSwscSAMLv2Token"})
    public void testSTSwscSAMLv1Token()
    throws Exception {
        entering("testSTSwscSAMLv1Token", null);
        
        try {
            String expResult = "SAML:1.0:assertion";
            webClient = new WebClient();
            
            ProviderConfig pc = null;
            pc = ProviderConfig.getProvider("wsp", ProviderConfig.WSP);
            assert (ProviderConfig.isProviderExists("wsp", ProviderConfig.WSP));
            List listSec = new ArrayList();
            listSec = pc.getSecurityMechanisms();
            
            if ((listSec.contains("urn:sun:wss:security:null:SAML2Token-HK"))) {
                listSec.remove("urn:sun:wss:security:null:SAML2Token-HK");
            }
            if ((listSec.contains("urn:sun:wss:security:null:SAML2Token-SV"))) {
                listSec.remove("urn:sun:wss:security:null:SAML2Token-SV");
            }
            if (!(listSec.contains("urn:sun:wss:security:null:SAMLToken-HK"))) {
                listSec.add("urn:sun:wss:security:null:SAMLToken-HK");
            }
            if (!(listSec.contains("urn:sun:wss:security:null:SAMLToken-SV"))) {
                listSec.add("urn:sun:wss:security:null:SAMLToken-SV");
            }
            pc.setSecurityMechanisms(listSec);
            ProviderConfig.saveProvider(pc);
            
            AMIdentity wspAmid = new AMIdentity(admintoken, "wsp",
                    IdType.AGENTONLY, realm, null);
            assert (wspAmid.isExists());
            Map wspAttrMap;
            wspAttrMap = new HashMap();
            wspAttrMap = wspAmid.getAttributes();
            log(Level.FINEST, "testSTSwscSAMLv1Token",
                    "WSP attributes \n" +  wspAttrMap);
            
            URL cmdUrl = new URL(stswscvalidationURL);
            HtmlPage page = (HtmlPage) webClient.getPage(cmdUrl);
            HtmlForm form = (HtmlForm) page.getForms().get(0);
            HtmlTextInput txtagentname = (HtmlTextInput) form.
                    getInputByName("providerName");
            txtagentname.setValueAttribute("wsc");
            
            HtmlPage returnPage = (HtmlPage) form.getInputByName("Submit").click();
            log(Level.FINEST, "testSTSwscSAMLv1Token",
                    " Page after login\n" +
                    returnPage.getWebResponse().getContentAsString());
            int iIdx = getHtmlPageStringIndex(returnPage, expResult);
            assert (iIdx != -1);
            
        } catch (Exception e) {
            log(Level.SEVERE, "testSTSwscSAMLv1Token", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("Test Description: Verifying the WSC token " +
                    "(of type SAMLv1)from STS  which can be presented to " +
                    "  the WSP for authentication purposes ");
        }
        exiting("testSTSwscSAMLv1Token");
    }
    
    /**
     * Cleanup method. This method:
     * (a) Delete users
     * (b) Deletes policies
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINEST, "cleanup", "UserName:" + userName);
            log(Level.FINEST, "cleanup", "roleName:" + roleName);
            log(Level.FINEST, "cleanup", "groupName:" + grpName);
            log(Level.FINEST, "cleanup", "PolicyName" + polName);
            Reporter.log("User name:" + userName);
            Reporter.log("Policy name:" + polName);
            Reporter.log("User name:" + roleName);
            Reporter.log("Group name:" + grpName);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, userName);
            idmc.deleteIdentity(admintoken, realm, IdType.ROLE, roleName);
            idmc.deleteIdentity(admintoken, realm, IdType.GROUP, grpName);
            pc.deletePolicy(polName, realm);
            
            //unset the SAML attribute Mapper settings for WSP
            AMIdentity wspid = new AMIdentity(admintoken, "wsp",
                    IdType.AGENTONLY, realm, null);
            Map wspMap= new HashMap();
            Set tempSet = new HashSet();
            tempSet.add("false");
            wspMap.put("includeMemberships", tempSet);
            wspid.setAttributes(wspMap);
            wspid.store();
            wspMap= new HashMap();
            wspMap = wspid.getAttributes() ;
            Set attrSet = new HashSet();
            attrSet.add("SAMLAttributeMapping");
            wspid.removeAttributes(attrSet);
            wspid.store();
            wspMap= new HashMap();
            wspMap = wspid.getAttributes() ;
            log(Level.FINEST, "cleanup", "WSP settings after cleanup: " + 
                    wspMap);
            
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }

    /**
     * Stop the jetty server. This basically undeploys the war.
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void stopServer()
    throws Exception {
        entering("stopServer", null);
        undeployClientSDKWar(rb_client);
        exiting("stopServer");
    }

    /**
     * Generate the XML for User Profile testcases.
     */
    private void generateUserProfileXML(String org, String username,
            String password, String xmlFile, String result)
            throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        log(Level.FINEST, "generateUserProfileXML", "Organization: " + org);
        log(Level.FINEST, "generateUserProfileXML", "Username: " + username);
        log(Level.FINEST, "generateUserProfileXML", "Password: " + password);
        log(Level.FINEST, "generateUserProfileXML", "XML File: " + xmlFile);
        
        out.write("<url href=\"" + userprofileURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\" >");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * Generates XML for Service Configuration testacses
     */
    private void generateServiceConfigXML(String org, String username,
            String password, String svcName, String configType,
            String xmlFile, String result)
            throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        log(Level.FINEST, "generateServiceConfigXML", "Organization: " + org);
        log(Level.FINEST, "generateServiceConfigXML", "Username: " + username);
        log(Level.FINEST, "generateServiceConfigXML", "Password: " + password);
        log(Level.FINEST, "generateServiceConfigXML", "Service Name: " +
                svcName);
        log(Level.FINEST, "generateServiceConfigXML", "Configuration Type: " +
                configType);
        log(Level.FINEST, "generateServiceConfigXML", "XML File: " + xmlFile);
        
        out.write("<url href=\"" + serviceconfigURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\">");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<input name=\"service\" value=\"" + svcName + "\"/>");
        out.write(newline);
        out.write("<dynamicinput name=\"method\" value=\"" + configType);
        out.write("\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * Generates XML for Policy testacses.
     */
    private void generatePolicyEvalXML(String org, String username,
            String password, String svcName, String resName, String xmlFile,
            String result)
            throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        log(Level.FINEST, "generatePolicyEvalXML", "Organization: " + org);
        log(Level.FINEST, "generatePolicyEvalXML", "Username: " + username);
        log(Level.FINEST, "generatePolicyEvalXML", "Password: " + password);
        log(Level.FINEST, "generatePolicyEvalXML", "Service Name: " + svcName);
        log(Level.FINEST, "generatePolicyEvalXML", "Resource: " + resName);
        log(Level.FINEST, "generatePolicyEvalXML", "XML File: " + xmlFile);
        
        out.write("<url href=\"" + policyURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\">");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<input name=\"servicename\" value=\"" + svcName + "\"/>");
        out.write(newline);
        out.write("<input name=\"resource\" value=\"" + resName + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * Generates XML for STS testacses.
     */
    private void generateSTSClientXML( String username,
            String password, String xmlFile, String result)
            throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        log(Level.FINEST, "generateSTSClientXML", "Result: " + result);
        log(Level.FINEST, "generateSTSClientXML", "Username: " + username);
        log(Level.FINEST, "generateSTSClientXML.", "Password: " + password);
        log(Level.FINEST, "generateSTSClientXML", "XML File: " + xmlFile);
        
        out.write("<url href=\"" + stsclientvalidationURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" IDButton=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + username + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\"" + password + "\" />");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    
    /**
     * Generates XML for creating the policy.
     */
    private void createPolicyXML(String xmlFile)
    throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.write(newline);
        out.write("<!DOCTYPE Policies");
        out.write(newline);
        out.write("PUBLIC \"-//Sun Java System Access Manager 7.1 2006Q3");
        out.write("Admin CLI DTD//EN\"");
        out.write(newline);
        out.write("\"jar://com/sun/identity/policy/policyAdmin.dtd\">");
        out.write(newline);
        
        out.write("<Policies>");
        out.write(newline);
        
        out.write("<Policy name=\"" + polName + "\" referralPolicy=\"false\"");
        out.write(" active=\"true\">");
        out.write(newline);
        
        out.write("<Rule name=\"csrule\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"" + resourceName + "\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);
        
        out.write("<Subjects name=\"cssubjects\" description=\"\">");
        out.write(newline);
        out.write("<Subject name=\"cssubj\" type=\"AuthenticatedUsers\"");
        out.write(" includeType=\"inclusive\">");
        out.write(newline);
        out.write("</Subject>");
        out.write(newline);
        out.write("</Subjects>");
        out.write(newline);
        
        out.write("</Policy>");
        out.write(newline);
        out.write("</Policies>");
        out.close();
    }
}
