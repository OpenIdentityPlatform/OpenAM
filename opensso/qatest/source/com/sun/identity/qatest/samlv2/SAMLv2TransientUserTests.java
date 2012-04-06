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
 * $Id: SAMLv2TransientUserTests.java,v 1.9 2009/01/27 00:14:10 nithyas Exp $
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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This class sets Transient user attribute to anonymous & Runs following:
 * 1. SP Init SSO
 * 2. IDP Init SSO
 * 3. SP Init SSO with Post/SOAP profile
 * 4. IDP Init SSO with Post/SOAP Profile. 
 */
public class SAMLv2TransientUserTests extends TestCommon {
    private String TRANSIENT_USER_DEFAULT = "<Attribute name=\"transientUser" +
            "\">\n" 
            +  "            <Value/>\n" 
            +  "        </Attribute>\n";
    private String TRANSIENT_USER_ANON = "<Attribute name=\"transientUser\">\n" 
            + "            <Value>anonymous</Value>\n" 
            +  "        </Attribute>\n";
    private String ATTRIB_MAP_DEFAULT = "<Attribute name=\""
            + "attributeMap\"/>\n";
    private String ATTRIB_MAP_VALUE = "<Attribute name=\""
            + "attributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";   
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    private String baseDir;
    public WebClient webClient;
    private FederationManager fmIDP;
    private FederationManager fmSP;
    private DefaultTaskHandler task;
    private ArrayList idpuserlist = new ArrayList();
    private HtmlPage page;
    private URL url;
    private String spmetadata;
    private String idpmetadata;
    
    /** Creates a new instance of SAMLv2TransientUserTests */
    public SAMLv2TransientUserTests() {
         super("SAMLv2TransientUserTests");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    @BeforeMethod(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    private void getWebClient() 
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch(Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
  
    /**
     * This is setup method. It creates required users for tests
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup() 
    throws Exception {
        List<String> list;
        String idpurl;
        try {
            ResourceBundle rbAmconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + System.getProperty("file.separator") 
                    + rbAmconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)  
                    + System.getProperty("file.separator") + "built" 
                    + System.getProperty("file.separator") + "classes" 
                    + System.getProperty("file.separator");
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
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
                    "samlv2transientusertests");
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalUsers"));
            for (int i=1; i < totalUsers + 1; i++) {
                //create idp user 
                list.clear();
                list.add("mail=" + usersMap.get("idp_mail" + i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_IDP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                if (FederationManager.getExitCode(fmIDP.createIdentity(
                        webClient, configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        usersMap.get(TestConstants.KEY_IDP_USER + i), "User", 
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                    assert false;
                }
                idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.clear();
            }
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * Change saml2 ext metadata to set transient user to anonymous
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void transientUserSetup()
    throws Exception {
        entering("transientUserSetup", null);
        String idpurl;
        String spurl;
        try {
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            log(Level.FINEST, "transientUserSetup", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + 
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + 
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_IDP_PORT) + 
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        } catch (Exception e) {
            log(Level.SEVERE, "transientUserSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        try {    
            //get sp & idp extended metadata
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            HtmlPage spmetaPage = spfm.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               log(Level.SEVERE, "transientUserSetup", "exportEntity famadm" +
                       " command failed");
               assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            String spmetadataMod = spmetadata.replaceAll(ATTRIB_MAP_DEFAULT,
                    ATTRIB_MAP_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(TRANSIENT_USER_DEFAULT,
                    TRANSIENT_USER_ANON);
            log(Level.FINEST, "transientUserSetup", "Modified metadata:" +
                    spmetadataMod);
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient, 
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "transientUserSetup", "Deletion of Extended" +
                        " entity failed");
                log(Level.SEVERE, "transientUserSetup", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
             
            if (FederationManager.getExitCode(spfm.importEntity(webClient, 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "", 
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "transientUserSetup", "Failed to import " +
                        "extended metadata");
                log(Level.SEVERE, "transientUserSetup", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
           
            HtmlPage idpmetaPage = idpfm.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false, false, 
                    true, "saml2");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
               log(Level.SEVERE, "transientUserSetup", "exportEntity famadm" +
                       " command failed");
               assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(idpmetaPage);
            String idpmetadataMod = idpmetadata.replaceAll(ATTRIB_MAP_DEFAULT,
                    ATTRIB_MAP_VALUE);
            log(Level.FINEST, "transientUserSetup", "Modified IDP metadata:" +
                    idpmetadataMod);
            
            if (FederationManager.getExitCode(idpfm.deleteEntity(webClient, 
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), true, "saml2"))
                    != 0) {
                log(Level.SEVERE, "transientUserSetup", "Deletion of idp " +
                        "Extended entity failed");
                log(Level.SEVERE, "transientUserSetup", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
             
            if (FederationManager.getExitCode(idpfm.importEntity(webClient, 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "", 
                    idpmetadataMod, "", "saml2")) != 0) {
                log(Level.FINEST, "transientUserSetup", "Failed to import" +
                        " idp extended metadata");
               log(Level.SEVERE, "transientUserSetup", "exportEntity famadm" +
                       " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "transientUserSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("transientUserSetup");
    }
    
    /**
     * Run SP initiated federation with transient user
     * @DocTest: SAML2|SP initiated SSO with no SP account.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SSONoSPAccSPInit()
    throws Exception {
        entering("SSONoSPAccSPInit", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, usersMap.get(
                    TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, usersMap.get(
                    TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.get(
                    TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, usersMap.get(
                    TestConstants.KEY_IDP_USER_PASSWORD + 1));
            configMap.put("urlparams", "NameIDFormat=transient");        
            String[] arrActions = {"ssonospaccspinit_ssoinit", 
                    "ssonospaccspinit_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact", 
                    true, false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);            
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "SSONoSPAccSPInit", 
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] 
                        + ".xml");
                page = task.execute(webClient);
            }           
        } catch(Exception e) {
            log(Level.SEVERE, "SSONoSPAccSPInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSONoSPAccSPInit");
    }       

    /**
     * Run IDP initiated federation with transient user
     * @DocTest: SAML2|IDP initiated SSO with no SP account.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SSONoSPAccIDPInit()
    throws Exception {
        entering("SSONoSPAccIDPInit", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, usersMap.get(
                    TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, usersMap.get(
                    TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.get(
                    TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, usersMap.get(
                    TestConstants.KEY_IDP_USER_PASSWORD + 2));
            configMap.put("urlparams", "NameIDFormat=transient");        
             //Now perform SSO
            String[] arrActions = {"ssonospaccidpinit_idplogin",
            "ssonospaccidpinit_sso", "ssonospaccidpinit_slo"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact", 
                    true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");   
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "SSONoSPAccIDPInit", 
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] 
                        + ".xml");
                page = task.execute(webClient);
            }           
        } catch(Exception e) {
            log(Level.SEVERE, "SSONoSPAccIDPInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSONoSPAccIDPInit");
    }       

    /**
     * Run SP initiated federation with transient user
     * @DocTest: SAML2|SP initiated SSO with no SP account with post/soap prof
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SSONoSPAccSPInitPost()
    throws Exception {
        entering("SSONoSPAccSPInitPost", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 3));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 3));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 3));
            configMap.put("urlparams", "NameIDFormat=transient");        
            //Now perform SSO
            String[] arrActions = {"ssonospaccspinitpost_sso", 
            "ssonospaccspinitpost_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post", true, 
                    false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);          
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "SSONoSPAccSPInitPost", 
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] 
                        + ".xml");
                page = task.execute(webClient);
            }           
        } catch(Exception e) {
            log(Level.SEVERE, "SSONoSPAccSPInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("autoFedSPInitPost");
    }       

    /**
     * Run IDP initiated federation with transient user
     * @DocTest: SAML2|IDP initiated SSO with no SP account with post/soap prof
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SSONoSPAccIDPInitPost()
    throws Exception {
        entering("autoFedIDPInit", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 4));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 43));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 4));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 4));
            configMap.put("urlparams", "NameIDFormat=transient");        
             //Now perform SSO
            String[] arrActions = {"ssonospaccidpinitpost_idplogin", 
            "ssonospaccidpinitpost_sso", "ssonospaccidpinitpost_slo"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post", true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");            
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "SSONoSPAccIDPInitPost", 
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] 
                        + ".xml");
                page = task.execute(webClient);
            }           
        } catch(Exception e) {
            log(Level.SEVERE, "SSONoSPAccIDPInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SSONoSPAccIDPInitPost");
    }       

    /**
     * This methods deletes all the users as part of cleanup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        String spurl;
        String idpurl;
        try
        {
            configMap = new HashMap<String, String>();
            getWebClient();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + "://" +
                    configMap.get(TestConstants.KEY_SP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + 
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_IDP_PORT) + 
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
        } catch(Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        try {
            //get sp & idp extended metadata
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            consoleLogin(webClient, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient, 
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of Extended " +
                        "entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
            }
             
            if (FederationManager.getExitCode(spfm.importEntity(webClient, 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),"", 
                    spmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to import extended " +
                        "metadata");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
            }
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            log(Level.FINEST, "cleanup", "Users to delete are : " + idpuserlist, 
                    null);
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient, 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }
            
            if (FederationManager.getExitCode(idpfm.deleteEntity(webClient, 
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME), 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), true, "saml2"))
                    != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of idp Extended " +
                        "entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
                assert(false);
            }
             
            if (FederationManager.getExitCode(idpfm.importEntity(webClient, 
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "", 
                    idpmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to import idp " +
                        "extended metadata");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
                assert(false);
            }
        } catch(Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("cleanup");
    }
}
