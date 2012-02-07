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
 * $Id: SAMLv2IDPProxyAutoFederationTests.java,v 1.4 2009/01/27 00:15:32 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2idpproxy;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the following: 
 * 1. First it sets the autofederation attribute to true. 
 * Sets the autofederation attribute & attribute map in the sp & idp extended 
 * metadata. It creates the users with same mail address. 
 * 2. Tests cover sp init SSO
 * 3. SP init SSO with POST/SOAP profiles.
 * 4. IDP init SSO
 * 5. IDP init SSO with POST/SOAP profiles. 
 */
public class SAMLv2IDPProxyAutoFederationTests extends TestCommon {
    
    public WebClient webClient;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    private String ssoProfile;
    private String sloProfile;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    ArrayList idpproxyuserlist = new ArrayList();
    private String  baseDir;
    private HtmlPage page;
    private String spmetadata;
    private String idpmetadata;
    private String idpproxymetadata;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private FederationManager fmIDPProxy;
    private String AUTO_FED_ENABLED_FALSE = "<Attribute name=\""
            + "autofedEnabled" + "\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>\n";
    private  String AUTO_FED_ENABLED_TRUE = "<Attribute name=\""
            + "autofedEnabled" + "\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>\n";
    
    private String AUTO_FED_ATTRIB_DEFAULT = "<Attribute name=\""
            + "autofedAttribute\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    private String AUTO_FED_ATTRIB_VALUE = "<Attribute name=\""
            + "autofedAttribute\">\n"
            + "            <Value>mail</Value>\n"
            + "        </Attribute>";
    
    private String ATTRIB_MAP_DEFAULT_1 = "<Attribute name=\""
            + "attributeMap\"/>\n";
     private String ATTRIB_MAP_DEFAULT_2 = "<Attribute name=\""
            + "attributeMap\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
   private String ATTRIB_MAP_VALUE = "<Attribute name=\""
            + "attributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";
    private String TRANSIENT_USER_DEFAULT =
            "<Attribute name=\"transientUser\">\n"
            +  "            <Value/>\n"
            +  "        </Attribute>\n";
    private String TRANSIENT_USER_ANON = "<Attribute name=\"transientUser\">\n"
            + "            <Value>anonymous</Value>\n"
            +  "        </Attribute>\n";
    
    
    /** Creates a new instance of SAMLv2AutoFederation */
    public SAMLv2IDPProxyAutoFederationTests() {
        super("SAMLv2AutoFederation");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    @BeforeMethod(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
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
    @Parameters({"ssoprofile", "sloprofile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String strSSOProfile, String strSLOProfile) 
    throws Exception {
        List<String> list;
        try {
            ssoProfile = strSSOProfile;
            sloProfile = strSLOProfile;
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + System.getProperty("file.separator")
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + System.getProperty("file.separator") + "built"
                    + System.getProperty("file.separator") + "classes"
                    + System.getProperty("file.separator");
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            log(Level.FINEST, "setup: Config Map is ", configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            idpproxyurl = configMap.get(TestConstants.KEY_IDP_PROXY_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_PROXY_HOST) + 
                    ":" + configMap.get(TestConstants.KEY_IDP_PROXY_PORT) +
                    configMap.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
            } catch (Exception e) {
                log(Level.SEVERE, "setup", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        try {
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));

            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            fmIDPProxy = new FederationManager(idpproxyurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2idpproxy" + fileseparator + 
                    "SAMLv2IDPProxyAutoFederationTests");
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalUsers"));
            for (int i = 1; i < totalUsers + 1; i++) {
                //create sp user first
                list.clear();
                list.add("mail=" + usersMap.get(TestConstants.KEY_SP_USER_MAIL +
                        i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_SP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINEST, "setup", "SP user to be created is " +
                        list);
                if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                        configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_SP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed at SP");
                    assert false;
                }
                spuserlist.add(usersMap.get(TestConstants.KEY_SP_USER + i));
                
                //create idp user
                list.clear();
                list.add("mail=" + usersMap.get(
                        TestConstants.KEY_IDP_USER_MAIL + i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_IDP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINEST, "setup", "IDP user to be created is " + list,
                        null);
                if (FederationManager.getExitCode(fmIDP.createIdentity(
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_IDP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed at IDP");
                    assert false;
                }
                idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.clear();

                //create idp proxy user
                list.clear();
                list.add("mail=" + usersMap.get(
                        TestConstants.KEY_IDP_PROXY_USER_MAIL + i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_PROXY_USER + 
                        i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_PROXY_USER + 
                        i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_IDP_PROXY_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINEST, "setup", "IDP user to be created is " + list,
                        null);
                if (FederationManager.getExitCode(fmIDPProxy.createIdentity(
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), usersMap.get(
                        TestConstants.KEY_IDP_PROXY_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed at IDP Proxy");
                    assert false;
                }
                idpproxyuserlist.add(usersMap.get(TestConstants.
                        KEY_IDP_PROXY_USER + i));
                list.clear();
            }

            //Get SP Metadata
            HtmlPage spmetaPage = fmSP.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               log(Level.SEVERE, "autoFedSetup", "exportEntity famadm command" +
                       " failed at SP");
               assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
           log(Level.FINEST, "autoFedSetup", "sp metadata is \n" + spmetadata);

            //Get IDP Metadata
            HtmlPage idpmetaPage = fmIDP.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false, 
                    false, true, "saml2");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
               log(Level.SEVERE, "autoFedSetup", "exportEntity famadm command" +
                       " failed at IDP");
               assert false;
            }
            idpmetadata = MultiProtocolCommon.getExtMetadataFromPage(
                    idpmetaPage);

            //Get IDP Proxy Metadata
            HtmlPage idpproxymetaPage = fmIDPProxy.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(idpproxymetaPage) != 0) {
               log(Level.SEVERE, "autoFedSetup", "exportEntity famadm command" +
                       " failed at IDP Proxy");
               assert false;
            }
            idpproxymetadata = MultiProtocolCommon.getExtMetadataFromPage(
                    idpproxymetaPage);
            
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * Run SP initiated auto federation in SAMLv2 IDP Proxy setup
     * The auto federation is turned on end to end.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void autoFedSPInit1()
    throws Exception {
        entering("autoFedSPInit1", null);
        try {
            Reporter.log("Test Description: Run SP initiated auto federation" +
                    " SSO with " + ssoProfile + " & SLO with " + sloProfile + 
                    " in SAMLv2 IDP Proxy setup. The auto federation is " +
                    " turned on end to end.");
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            setupAutoFedAtSP();
            setupAutoFedAtIDPProxy();
            setupAutoFedAtIDP();
        } catch (Exception e) {
            log(Level.SEVERE, "seautoFedSPInit1tup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        try {
            getWebClient();
            configMap.put(TestConstants.KEY_SP_USER,
            usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER, usersMap.
                    get(TestConstants.KEY_IDP_PROXY_USER + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 1));
            //Now perform SSO & SLO
            String[] arrActions = {"autofedSAMLv2IDPProxySPInitSSOInit1",
            "autofedSAMLv2IDPProxySPInitSLO1"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    true, true);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "autoFedSPInit" + ssoProfile + 1,
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInit1", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("autoFedSPInit1");
    }
    
    /**
     * Run SP initiated auto federation in SAMLv2 IDP Proxy setup
     * Auto federation is turned only between IDP Proxy & IDP.
     * In this case SP & IDP credentials will be asked.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"autoFedSPInit1"})
    public void autoFedSPInit2()
    throws Exception {
        entering("autoFedSPInit2", null);
        try {
            Reporter.log("Test Description: Run SP initiated auto federation" +
                    " SSO with " + ssoProfile + " & SLO with " + sloProfile + 
                    " in SAMLv2 IDP Proxy setup. Auto federation is turned " +
                    "only between IDP Proxy & IDP.");
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            setupAutoFedAtIDPProxy();
            setupAutoFedAtIDP();
            setOriMetadataAtSP();
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInit2", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        try {
            getWebClient();
            String idpEntityName = configMap.get(TestConstants.
                    KEY_IDP_ENTITY_NAME);
            configMap.put(TestConstants.KEY_IDP_ENTITY_NAME, configMap.
                    get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME));
            
            configMap.put(TestConstants.KEY_SP_USER,
            usersMap.get(TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER, usersMap.
                    get(TestConstants.KEY_IDP_PROXY_USER + 2));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 2));
            //Now perform SSO & SLO
            String[] arrActions = {"autofedSAMLv2IDPProxySPInitSSOInit2",
            "autofedSAMLv2IDPProxySPInitSLO2"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    false, false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "autoFedSPInit2" + 2,
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }

            configMap.put(TestConstants.KEY_IDP_ENTITY_NAME, idpEntityName);
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInit2", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("autoFedSPInit2");
    }
    
    /**
     * Run SP initiated transient SSO auto federation in SAMLv2 IDP Proxy setup
     * Auto federation is turned between IDP Proxy & IDP and SP & IDP Proxy.
     * In this case IDP credentials will be asked.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"autoFedSPInit2"})
    public void autoFedSPInitTransient()
    throws Exception {
        entering("autoFedSPInitTransient", null);
        try {
            Reporter.log("Test Description: Run SP initiated transient SSO " +
                    "with " + ssoProfile + " auto federation in SAMLv2 IDP " +
                    "Proxy setup. Auto federation is turned  between IDP " +
                    "Proxy & IDP and SP & IDP Proxy.");
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            setupAutoFedAtIDPProxy();
            setupAutoFedAtIDP();
            setupAutoFedAtSP();
            setupTransientUserAtSP();
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInit2", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        try {
            getWebClient();
            
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 3));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER, usersMap.
                    get(TestConstants.KEY_IDP_PROXY_USER + 3));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 3));
            configMap.put("urlparams", "NameIDFormat=transient");

            //Now perform SSO & SLO
            String[] arrActions = {"autofedSAMLv2IDPProxySPInitSSOTransient",
            "autofedSAMLv2IDPProxySPInitSLOTransient"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    true, true);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "autoFedSPInitTransient" + 2,
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }

            configMap.remove("urlparams");
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitTransient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("autoFedSPInitTransient");
    }
    
    /**
     * Run SP initiated auto federation in SAMLv2 IDP Proxy setup where 
     * Dynamic user creation is turned on at SP end.
     * After auto federation is succeeded new user will be created at the SP.
     * In this case IDP credentials will be asked.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"autoFedSPInitTransient"})
    public void autoFedSPInitDynamicUserCreation()
    throws Exception {
        entering("autoFedSPInitTransient", null);
        try {
            Reporter.log("Test Description: Run SP initiated auto federation" +
                    " SSO with " + ssoProfile + " & SLO with " + sloProfile +
                    " in SAMLv2 IDP Proxy setup where Dynamic user creation " +
                    " is turned on at SP end. After auto federation is " +
                    " succeeded new user will be created at the SP.");
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            setupAutoFedAtIDPProxy();
            setupAutoFedAtIDP();
            setupAutoFedAtSP();
            //Set Dynamic user creation to true at SP end. 
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
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        try {
            getWebClient();
            
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 4));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 4));
            //Now perform SSO & SLO
            String[] arrActions = {"autofedSAMLv2IDPProxySPInitSSODynUserSP",
            "autofedSAMLv2IDPProxySPInitSLOTDynUserSP"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    true, true);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "autoFedSPInitDynamicUserCreation" ,
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        exiting("autoFedSPInitDynamicUserCreation");
    }

    /**
     * Run SP initiated auto federation in SAMLv2 IDP Proxy setup where
     * Dynamic user creation is turned on at SP end and at IDP Proxy.
     * After auto federation is succeeded new user will be created at the SP as
     * we as at the IDP Proxy.
     * In this case IDP credentials will be asked.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"autoFedSPInitDynamicUserCreation"})
    public void autoFedSPInitDynamicUserCreationAtProxy()
    throws Exception {
        entering("autoFedSPInitDynamicUserCreationAtProxy", null);
        try {
            Reporter.log("Test Description: Run SP initiated auto federation" +
                    " SSO with " + ssoProfile + " & SLO with " + sloProfile +
                    " in SAMLv2 IDP Proxy setup where Dynamic user creation " +
                    "is turned on at SP end and at IDP Proxy.After auto " +
                    "federation  is succeeded new user will be created at the" +
                    " SP as well as at the IDP Proxy.");
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            //Set Dynamic user creation to true at SP end. 
            List listDyn = new ArrayList();
            listDyn.add("iplanet-am-auth-dynamic-profile-creation=createAlias");
            if (FederationManager.getExitCode(fmIDPProxy.setSvcAttrs(webClient, 
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
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
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        try {
            getWebClient();
            
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 5));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 5));
            //Now perform SSO & SLO
            String[] arrActions = {"autofedSAMLv2IDPProxySPInitSSODynUserSP",
            "autofedSAMLv2IDPProxySPInitSLOTDynUserSP"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    true, true);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "autoFedSPInitDynamicUserCreation" ,
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        try {
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            List listDyn = new ArrayList();
            listDyn.add("iplanet-am-auth-dynamic-profile-creation=false");
            if (FederationManager.getExitCode(fmIDPProxy.setSvcAttrs(webClient, 
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "iPlanetAMAuthService", listDyn)) == 0) {
                log(Level.FINE, "autoFedSPInitDynamicUserCreation", 
                        "Successfully disabled Dynamic user creation");
            } else {
                log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                        "Couldn't disable Dynamic user creation");
                log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                        "setSvcAttrs famadm command failed");
                assert(false);
            }
            if (FederationManager.getExitCode(fmSP.setSvcAttrs(webClient, 
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "iPlanetAMAuthService", listDyn)) == 0) {
                log(Level.FINE, "autoFedSPInitDynamicUserCreation", 
                        "Successfully disabled Dynamic user creation");
            } else {
                log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                        "Couldn't disable Dynamic user creation");
                log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                        "setSvcAttrs famadm command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSPInitDynamicUserCreation", 
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }

        exiting("autoFedSPInitDynamicUserCreation");
    }

    /**
     * This methods deletes all the users, restores original metadata
     * as part of cleanup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    spuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }
            
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmIDPProxy.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM),
                    idpproxyuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            setOriMetadataAtSP();
            setOriMetadataAtIDP();
            setOriMetadataAtIDPProxy();
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        exiting("cleanup");
    }
    
    /**
     * Setup auto federation at SP side
     */
    private void setupAutoFedAtSP() 
    throws Exception {
        try {
            String spmetadataMod = spmetadata.replaceAll(AUTO_FED_ENABLED_FALSE,
                    AUTO_FED_ENABLED_TRUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ATTRIB_DEFAULT,
                    AUTO_FED_ATTRIB_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(ATTRIB_MAP_DEFAULT_1,
                    ATTRIB_MAP_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(ATTRIB_MAP_DEFAULT_2,
                    ATTRIB_MAP_VALUE);
            log(Level.FINEST, "setupAutoFedAtSP: Modified metadata:",
                    spmetadataMod);
            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "setupAutoFedAtSP", "Deletion of Extended " +
                        "entity failed");
                log(Level.SEVERE, "setupAutoFedAtSP", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.FINEST, "setupAutoFedAtSP", "Failed to import " +
                        "extended metadata");
                log(Level.FINEST, "setupAutoFedAtSP", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setupAutoFedAtSP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Setup auto federation at IDP side
     */
    private void setupAutoFedAtIDP() 
    throws Exception {
        try {
            String idpmetadataMod = idpmetadata.replaceAll(
                    AUTO_FED_ENABLED_FALSE, AUTO_FED_ENABLED_TRUE);
            idpmetadataMod = idpmetadataMod.replaceAll(
                    AUTO_FED_ATTRIB_DEFAULT, AUTO_FED_ATTRIB_VALUE);
            idpmetadataMod = idpmetadataMod.replaceAll(ATTRIB_MAP_DEFAULT_1,
                    ATTRIB_MAP_VALUE);
            idpmetadataMod = idpmetadataMod.replaceAll(ATTRIB_MAP_DEFAULT_2,
                    ATTRIB_MAP_VALUE);
            log(Level.FINEST, "setupAutoFedAtIDP: Modified metadata:",
                    idpmetadataMod);
            
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), true, 
                    "saml2")) != 0) {
                log(Level.SEVERE, "setupAutoFedAtIDP", "Deletion of idp " +
                        "Extended entity failed");
                log(Level.SEVERE, "setupAutoFedAtIDP", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                    idpmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "setupAutoFedAtIDP", "Failed to import idp " +
                        "extended metadata");
                log(Level.SEVERE, "setupAutoFedAtIDP", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setupAutoFedAtIDP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Setup auto federation at IDP Proxy side
     */
    private void setupAutoFedAtIDPProxy() 
    throws Exception {
        try {
            //Modify IDP Proxy Metadata
            String idpproxymetadataMod = idpproxymetadata.replaceAll(
                    AUTO_FED_ENABLED_FALSE, AUTO_FED_ENABLED_TRUE);
            idpproxymetadataMod = idpproxymetadataMod.replaceAll(
                    AUTO_FED_ATTRIB_DEFAULT, AUTO_FED_ATTRIB_VALUE);
            idpproxymetadataMod = idpproxymetadataMod.replaceAll(
                    ATTRIB_MAP_DEFAULT_1, ATTRIB_MAP_VALUE);
            idpproxymetadataMod = idpproxymetadataMod.replaceAll(
                    ATTRIB_MAP_DEFAULT_2, ATTRIB_MAP_VALUE);
            log(Level.FINEST, "setupAutoFedAtIDPProxy: Modified IDP Proxy" +
                    " metadata:\n", idpproxymetadataMod);
            
            if (FederationManager.getExitCode(fmIDPProxy.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "setupAutoFedAtIDPProxy", "Deletion of idp " +
                        "proxy Extended entity failed");
                log(Level.SEVERE, "setupAutoFedAtIDPProxy", "deleteEntity " +
                        "famadm command failed at IDP Proxy");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmIDPProxy.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "", idpproxymetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "setupAutoFedAtIDPProxy", "Failed to " +
                        "import idp extended metadata");
                log(Level.SEVERE, "setupAutoFedAtIDPProxy", "importEntity " +
                        "famadm command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setupAutoFedAtIDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Setup auto federation at SP side
     */
    private void setupTransientUserAtSP() 
    throws Exception {
        try {
            String spmetadataMod = spmetadata.replaceAll(TRANSIENT_USER_DEFAULT,
                    TRANSIENT_USER_ANON);
            log(Level.FINEST, "setupTransientUserAtSP: Modified metadata:",
                    spmetadataMod);
            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "setupTransientUserAtSP", "Deletion of Extended " +
                        "entity failed");
                log(Level.SEVERE, "setupTransientUserAtSP", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.FINEST, "setupTransientUserAtSP", "Failed to import " +
                        "extended metadata");
                log(Level.FINEST, "setupTransientUserAtSP", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setupTransientUserAtSP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    /**
     * Restore original metadata at SP side
     */
    private void setOriMetadataAtSP() 
    throws Exception {
        try {
            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "setOriMetadataAtSP", "Deletion of SP " +
                        "Extended entity failed at SP");
                log(Level.SEVERE, "setOriMetadataAtSP", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadata, "", "saml2")) != 0) {
                log(Level.FINEST, "setOriMetadataAtSP", "Failed to import" +
                        " extended metadata at SP");
                log(Level.FINEST, "setOriMetadataAtSP", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setOriMetadataAtSP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Restore original metadata at IDP side
     */
    private void setOriMetadataAtIDP() 
    throws Exception {
        try {
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), true, 
                    "saml2")) != 0) {
                log(Level.SEVERE, "setOriMetadataAtIDP", "Deletion of idp " +
                        "Extended entity failed at IDP");
                log(Level.SEVERE, "setOriMetadataAtIDP", "deleteEntity famadm" +
                        " command failed");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                    idpmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "setOriMetadataAtIDP", "Failed to import " +
                        "idp extended metadata");
                log(Level.SEVERE, "setOriMetadataAtIDP", "importEntity famadm" +
                        " command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setOriMetadataAtIDP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Restore original metadata at IDP Proxy side
     */
    private void setOriMetadataAtIDPProxy() 
    throws Exception {
        try {
            if (FederationManager.getExitCode(fmIDPProxy.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "setOriMetadataAtIDPProxy", "Deletion " +
                        "of idp proxy Extended entity failed");
                log(Level.SEVERE, "setOriMetadataAtIDPProxy", "deleteEntity " +
                        "famadm command failed at IDP Proxy");
                assert(false);
            }
            
            if (FederationManager.getExitCode(fmIDPProxy.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "", idpproxymetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "setOriMetadataAtIDPProxy", "Failed to " +
                        "import idp extended metadata");
                log(Level.SEVERE, "setOriMetadataAtIDPProxy", "importEntity " +
                        "famadm command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setOriMetadataAtIDPProxy", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /*
     * This method generates the xml for SSO. It is similar to getxmlSPInitSSO
     * method from SAMLv2Common, but this sends the user id/password for IDP &
     * IDP Proxy. 
     */
    private static void getxmlSPInitSSO(String xmlFileName, Map m,
            String bindingType)
    throws Exception {
        FileWriter fstream = new FileWriter(xmlFileName);
        BufferedWriter out = new BufferedWriter(fstream);
        String sp_proto = (String)m.get(TestConstants.KEY_SP_PROTOCOL);
        String sp_port = (String)m.get(TestConstants.KEY_SP_PORT);
        String sp_host = (String)m.get(TestConstants.KEY_SP_HOST);
        String sp_deployment_uri = (String)m.get(
                TestConstants.KEY_SP_DEPLOYMENT_URI);
        String sp_alias = (String)m.get(TestConstants.KEY_SP_METAALIAS);
        String idp_entity_name = "";
        String idp_proxy_user = "";
        String idp_proxy_userpw = "";
            idp_entity_name = (String)m.get(
                    TestConstants.KEY_IDP_PROXY_ENTITY_NAME);
            idp_proxy_user = (String)m.get(TestConstants.
                    KEY_IDP_PROXY_USER);
            idp_proxy_userpw = (String)m.get(TestConstants.
                    KEY_IDP_PROXY_USER_PASSWORD);
        String idp_user = (String)m.get(TestConstants.KEY_IDP_USER);
        String idp_userpw = (String)m.get(TestConstants.KEY_IDP_USER_PASSWORD);
        String strResult = (String)m.get(TestConstants.KEY_SSO_INIT_RESULT);
        
        out.write("<url href=\"" + sp_proto +"://" + sp_host + ":"
                + sp_port + sp_deployment_uri
                + "/saml2/jsp/spSSOInit.jsp?metaAlias=" + sp_alias
                + "&amp;idpEntityID=" + idp_entity_name );
        if (bindingType == "post") {
            out.write("&amp;binding=HTTP-POST");
        }
        if (m.get("urlparams") != null) {
            out.write("&amp;" + m.get("urlparams"));
        }
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_userpw + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("<form name=\"Login\" buttonName=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + idp_proxy_user + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\""
                + idp_proxy_userpw + "\" />");
        out.write(newline);
        out.write("<result text=\"" + strResult + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
}
