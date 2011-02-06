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
 * $Id: SAMLv2AutoFedDynUserCreationTests.java,v 1.12 2009/07/10 04:38:48 mrudulahg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This class tests auto federation with Dymanic user creation set to true at SP
 * 1. SP Init SSO
 * 2. IDP Init SSO
 * 3. SP Init SSO with Post/SOAP profile
 * 4. IDP Init SSO with Post/SOAP Profile.
 */
public class SAMLv2AutoFedDynUserCreationTests extends TestCommon {
    private String AUTO_FED_ENABLED_FALSE = "<Attribute name=\""
            + "autofedEnabled" + "\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>\n";
    private  String AUTO_FED_ENABLED_TRUE = "<Attribute name=\""
            + "autofedEnabled" + "\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>\n";
    private String AUTO_FED_ATTRIB_VALUE = "<Attribute name=\""
            + "autofedAttribute\">\n"
            + "            <Value>cn</Value>\n"
            + "        </Attribute>";
    private String AUTO_FED_ATTRIB_DEFAULT = "<Attribute name=\""
            + "autofedAttribute\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    private String ATTRIB_MAP_DEFAULT = "<Attribute name=\""
            + "attributeMap\"/>\n";
    private String ATTRIB_MAP_VALUE = "<Attribute name=\""
            + "attributeMap\">\n"
            + "            <Value>cn=cn</Value>\n"
            + "        </Attribute>";
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    private Map<String, String> spusersMap;
    private FederationManager fmIDP;
    private FederationManager fmSP;
    private String baseDir;
    public WebClient webClient;
    private DefaultTaskHandler task;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    private HtmlPage page;
    private String spmetadata;
    private String idpmetadata;
    private String spurl;
    private String idpurl;
    
    /** Creates a new instance of SAMLv2AutoFedDynUserCreationTests */
    public SAMLv2AutoFedDynUserCreationTests() {
        super("SAMLv2AutoFedDynUserCreationTests");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    @BeforeMethod(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    private void getWebClient() 
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This is setup method. It creates required users for test
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup() 
    throws Exception {
        List<String> list;
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestData"));
            log(Level.FINEST, "setup", "Config map is " + configMap);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        try {   
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2" + fileseparator + 
                    "samlv2AutoFedDynUserCreationTests");
            log(Level.FINEST, "setup", "Users map is " + usersMap);
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalUsers"));
            for (int i = 1; i < totalUsers + 1; i++) {
                //create idp user
                list.clear();
                list.add("mail=" + usersMap.get(
                        TestConstants.KEY_IDP_USER_MAIL + i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_IDP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINEST, "setup", "IDP user to be created is " + list);
                if (FederationManager.getExitCode(fmIDP.createIdentity(
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_EXECUTION_REALM), usersMap.get(TestConstants.
                        KEY_IDP_USER + i), "User", list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                    assert false;
                }
                idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.clear();
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * This is setup method. It enables auto federation. 
     * It also sets Dynamic user creation to true in iplanetAMAuthService.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void autoFedDynamicUserCreationSetup()
    throws Exception {
        entering("autoFedDynamicUserCreationSetup", null);
        try {
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestData"));
            log(Level.FINEST, "autoFedDynamicUserCreationSetup", "Map:" + 
                    configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + 
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_SP_PORT) + 
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }

        try {
            //get sp & idp extended metadata
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            //Set Dynamic user creation to true. 
            List listDyn = new ArrayList();
            listDyn.add("iplanet-am-auth-dynamic-profile-creation=createAlias");
            if (FederationManager.getExitCode(fmSP.setSvcAttrs(webClient, 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "iPlanetAMAuthService", listDyn)) == 0) {
                log(Level.FINE, "autoFedDynamicUserCreationSetup", 
                        "Successfully enabled Dynamic user creation");
            } else {
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup", 
                        "Couldn't enable Dynamic user creation");
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                        "setSvcAttrs famadm command failed");
                assert(false);
            }
            
            //Enable AutoFederation
            HtmlPage spmetaPage = fmSP.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                       "exportEntity famadm command failed");
               assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            String spmetadataMod = spmetadata.replaceAll(ATTRIB_MAP_DEFAULT,
                    ATTRIB_MAP_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ENABLED_FALSE,
                    AUTO_FED_ENABLED_TRUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ATTRIB_DEFAULT,
                    AUTO_FED_ATTRIB_VALUE);
            log(Level.FINEST, "autoFedDynamicUserCreationSetup", "Modified " +
                    "metadata:" + spmetadataMod);
            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup", 
                        "Deletion of Extended entity failed");
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                        "deleteEntity famadm command failed");
                assert(false);
            } else {
                 log(Level.FINE, "autoFedDynamicUserCreationSetup", "Deleted " +
                         "SP Ext entity");
            }
            
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "", 
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup", "Failed " +
                        "to importextended metadata");
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                        "importEntity famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "autoFedDynamicUserCreationSetup", "Imported " +
                        "SP extended metadata"); 
            }
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            HtmlPage idpmetaPage = fmIDP.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                    false, true, "saml2");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
               log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                       "exportEntity famadm command failed");
               assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(idpmetaPage);
            String idpmetadataMod = idpmetadata.replaceAll(ATTRIB_MAP_DEFAULT,
                    ATTRIB_MAP_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ENABLED_FALSE,
                    AUTO_FED_ENABLED_TRUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ATTRIB_DEFAULT,
                    AUTO_FED_ATTRIB_VALUE);
            log(Level.FINEST, "autoFedDynamicUserCreationSetup", "Modified " +
                    "IDP metadata:" + idpmetadataMod);
            
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup", 
                        "Deletion of idp Extended entity failed");
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                        "deleteEntity famadm command failed");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "", 
                    idpmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup", "Failed " +
                        "to import idp extended metadata");
                log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                        "importEntity famadm command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedDynamicUserCreationSetup",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("autoFedDynamicUserCreationSetup");
    }
    
    /**
     * Run SP initiated auto federation with Dynamic user creation at sp side 
     * @DocTest: SAML2| SP initiated SSO with Dynamic user creation.
     * TestCase ID: SAMLv2_usecase_8_1
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SSOWithDynUserSPInit()
    throws Exception {
        entering("SSOWithDynUserSPInit", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            String[] arrActions = {"ssowithdynuserspinit_ssoinit", 
                    "ssowithdynuserspinit_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact",
                    true, false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SSOWithDynUserSPInit",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
            spuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + 1));
        } catch (Exception e) {
            log(Level.SEVERE, "SSOWithDynUserSPInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSOWithDynUserSPInit");
    }
    
    /**
     * Run IDP initiated auto federation with Dynamic user creation at sp side 
     * @DocTest: SAML2| IDP initiated SSO with Dynamic user creation at SP.
     * TestCase ID: SAMLv2_usecase_8_2
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SSOWithDynUserIDPInit()
    throws Exception {
        entering("SSOWithDynUserIDPInit", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 2));
            //Now perform SSO
            String[] arrActions = {"ssowithdynuseridpinit_idplogin",
                    "ssowithdynuseridpinit_sso", 
                    "ssowithdynuseridpinit_slo"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact",
                    true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SSOWithDynUserIDPInit",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
            spuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + 2));
        } catch (Exception e) {
            log(Level.SEVERE, "SSOWithDynUserIDPInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSOWithDynUserIDPInit");
    }
    
    /**
     * Run SP initiated auto federation with Dynamic user creation at sp side 
     * using Post/SOAP Profile
     * @DocTest: SAML2| SP initiated SSO with dynamic user creation Post/SOAP
     * Profile
     * TestCase ID: SAMLv2_usecase_8_3
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SSOWithDynUserSPInitPost()
    throws Exception {
        entering("SSOWithDynUserSPInitPost", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 3));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 3));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 3));
            //Now perform SSO
            String[] arrActions = {"ssowithnodbwritesspinitpost_sso",
            "ssowithnodbwritesspinitpost_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post", true, 
                    false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SSOWithDynUserSPInitPost",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
            spuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + 3));
        } catch (Exception e) {
            log(Level.SEVERE, "SSOWithDynUserSPInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSOWithDynUserSPInitPost");
    }
    
    /**
     * Run IDP initiated auto federation with Dynamic user creation at sp side 
     * using Post/SOAP Profile
     * @DocTest: SAML2| IDP initiated Autofederation with Dynamic User creation
     * Post/SOAP Profile.
     * TestCase ID: SAMLv2_usecase_8_4
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SSOWithDynUserIDPInitPost()
    throws Exception {
        entering("SSOWithDynUserIDPInitPost", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 4));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 4));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 4));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 4));
            //Now perform SSO
            String[] arrActions = {"ssowithdymuseridpinitpost_idplogin",
                    "ssowithdymuseridpinitpost_sso", 
                    "ssowithdymuseridpinitpost_slo"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post", true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SSOWithDynUserIDPInitPost",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
            spuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + 4));
        } catch (Exception e) {
            log(Level.SEVERE, "SSOWithDynUserIDPInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSOWithDynUserIDPInitPost");
    }
    
    /**
     * This methods deletes all the users as part of cleanup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
         try {
            getWebClient();
            ArrayList list;
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            //Set Dynamic user creation to false (default). 
            List listDyn = new ArrayList();
            listDyn.add("iplanet-am-auth-dynamic-profile-creation=false");
            if (FederationManager.getExitCode(fmSP.setSvcAttrs(webClient, 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "iPlanetAMAuthService", listDyn)) == 0) {
                log(Level.FINE, "cleanup", "Successfully disabled Dynamic " +
                        "user creation");
            } else {
                log(Level.SEVERE, "cleanup", "Couldn't disable Dynamic user " +
                        "creation");
                log(Level.SEVERE, "cleanup", "setSvcAttrs famadm command" +
                        " failed");
                assert(false);
            }

            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of Extended " +
                        "entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
                assert(false);
            }
            spusersMap = new HashMap<String, String>();
            spusersMap = getMapFromResourceBundle("samlv2" + fileseparator + 
                    "samlv2AutoFedDynUserCreationTests");
            log(Level.FINEST, "cleanup", "SP users to delete : " + spuserlist);
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    spuserlist , "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command" +
                        " failed");
            }
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "", 
                    spmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to import extended " +
                        "metadata");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
            }
            
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            log(Level.FINE, "cleanup", "Users to delete are" + idpuserlist);
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(
                    TestConstants.KEY_IDP_EXECUTION_REALM),
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
            }
            
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of idp Extended " +
                        "entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
            }
            
            if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "", 
                    idpmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to import idp " +
                        "extended metadata");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
        }
        exiting("cleanup");
    }
}
