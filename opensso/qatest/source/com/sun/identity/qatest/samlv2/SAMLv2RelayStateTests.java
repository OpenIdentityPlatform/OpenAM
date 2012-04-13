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
 * $Id: SAMLv2RelayStateTests.java,v 1.9 2009/01/27 00:14:09 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.SAMLv2Common;
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
 * This class tests samlv2 scenarios with relay state
 * 1. SP Initiated SSO with relay state
 * 2. SP Initiated SSO with relay state with POST/SOAP profile
 * 3. IDP Initiated SSO with relay state
 * 4. IDP Initiated SSO with relay state with POST/SOAP profile
 * 5. SP Initiated SLO with relay state
 * 6. SP Initiated SLO with relay state with POST/SOAP profile
 * 7. IDP Initiated SLO with relay state
 * 8. IDP Initiated SLO with relay state with POST/SOAP profile
 * 9. SP Initiated SSO & SLO with relay states
 * 10. SP Initiated SSO & SLO with relay states with POST/SOAP profile
 * 11. IDP Initiated SSO & SLO with relay states
 * 12. IDP Initiated SSO & SLO with relay states with POST/SOAP profile
 */
public class SAMLv2RelayStateTests extends TestCommon {
    
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    private String  baseDir;
    private HtmlPage page;
    private URL url;
    
    /** Creates a new instance of SAMLv2RelayStateTests */
    public SAMLv2RelayStateTests() {
        super("SAMLv2RelayStateTests");
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
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        List<String> list;
        String spurl;
        String idpurl;
        try {
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
            log(Level.FINEST, "setup", "Map is " + configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            } catch (Exception e) {
                log(Level.SEVERE, "getWebClient", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        try {
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            
            fmSP = new FederationManager(spurl);
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2relaystatetests");
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalScenarios"));
            for (int i = 1; i < totalUsers + 1; i++) {
                //create sp user first
                list.clear();
                list.add("sn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_SP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_SP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                    assert false;
                }
                spuserlist.add(usersMap.get(TestConstants.KEY_SP_USER + i));
                
                //create idp user
                list.clear();
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
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * @DocTest: SAML2|SP Init SSO with RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2SPInitArtSSORS()
    throws Exception {
        entering("samlv2SPInitArtSSORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 1));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate1"));
            log(Level.FINEST, "samlv2SPInitArtSSORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitssors_ssoinit", 
                    "samlv2spinitssors_slo", "samlv2spinitssors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact",
                    false, false);
            configMap.remove("urlparams");
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitArtSSORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitArtSSORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitArtSSORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SSO with RelayState with post/soap binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2SPInitPostSSORS()
    throws Exception {
        entering("samlv2SPInitPostSSORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 2));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate2"));
            log(Level.FINEST, "samlv2SPInitPostSSORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitpostssors_ssoinit", 
            "samlv2spinitpostssors_slo", "samlv2spinitpostssors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post", false, 
                    false);
            configMap.remove("urlparams");
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitPostSSORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitPostSSORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitPostSSORS");
    }
    
    /**
     * @DocTest: SAML2|IDP initiated SSO RelayState set.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2IDPInitSSORS()
    throws Exception {
        entering("samlv2IDPInitSSORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 3));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 3));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 3));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate3"));
            log(Level.FINEST, "samlv2IDPInitSSORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitssors_idplogin", 
                    "samlv2idpinitssors_ssoinit", "samlv2idpinitssors_slo", 
                    "samlv2idpinitssors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact",
                    false);
            configMap.remove("urlparams");
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitSSORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitSSORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitSSORS");
    }
    
    /**
     * @DocTest: SAML2|IDP Init SSO with RelayState set with POST/SOAP binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2IDPInitPostSSORS()
    throws Exception {
        entering("samlv2IDPInitPostSSORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 4));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 4));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 4));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 4));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 4));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate4"));
            log(Level.FINEST, "samlv2IDPInitPostSSORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitpostssors_idplogin", 
                    "samlv2idpinitpostssors_ssoinit",
                    "samlv2idpinitpostssors_slo", 
                    "samlv2idpinitpostssors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post", false);
            configMap.remove("urlparams");
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitPostSSORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitPostSSORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitPostSSORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SLO with RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2SPInitHTTPSLORS()
    throws Exception {
        entering("samlv2SPInitHTTPSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 5));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 5));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 5));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 5));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 5));
            log(Level.FINEST, "samlv2SPInitHTTPSLORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitslors_ssoinit", 
                    "samlv2spinitslors_slo", "samlv2spinitslors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact",
                    false, false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate5"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitHTTPSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitHTTPSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitHTTPSLORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SLO with RelayState with post/soap binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2SPInitSOAPSLORS()
    throws Exception {
        entering("samlv2SPInitSOAPSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 6));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 6));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 6));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 6));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 6));
            log(Level.FINEST, "samlv2SPInitSOAPSLORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitpostslors_ssoinit", 
                    "samlv2spinitpostslors_slo",
                    "samlv2spinitpostslors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post", false, 
                    false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate6"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitSOAPSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitSOAPSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitSOAPSLORS");
    }
    
    /**
     * @DocTest: SAML2|IDP initiated SLO with RelayState set.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2IDPInitHTTPSLORS()
    throws Exception {
        entering("samlv2IDPInitHTTPSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 7));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 7));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 7));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 7));
            configMap.put(TestConstants.KEY_IDP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_IDP_SLO_RESULT + 7));
            log(Level.FINEST, "samlv2IDPInitHTTPSLORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitslors_idplogin", 
                    "samlv2idpinitslors_ssoinit", "samlv2idpinitslors_slo", 
                    "samlv2idpinitslors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact",
                    false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate1"));
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitHTTPSLORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitHTTPSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitHTTPSLORS");
    }
    
    /**
     * @DocTest: SAML2|IDP Init SLO with Relay state POST/SOAP binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2IDPInitSOAPSLORS()
    throws Exception {
        entering("samlv2IDPInitSOAPSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 8));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 8));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 8));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 8));
            configMap.put(TestConstants.KEY_IDP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_IDP_SLO_RESULT + 8));
            log(Level.FINEST, "samlv2IDPInitSOAPSLORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitpostslors_idplogin", 
                    "samlv2idpinitpostslors_ssoinit",
                    "samlv2idpinitpostslors_slo", 
                    "samlv2idpinitpostslors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post", false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate8"));
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitSOAPSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitSOAPSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitSOAPSLORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SSO/SLO with diff RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2SPInitArtHTTPSSOSLORS()
    throws Exception {
        entering("samlv2SPInitArtHTTPSSOSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 9));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 9));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 9));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 9));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 9));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 9));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate9"));
            log(Level.FINEST, "samlv2SPInitArtHTTPSSOSLORS", "Map:" +
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitssoslors_ssoinit", 
                    "samlv2spinitssoslors_slo",
                    "samlv2spinitssoslors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact",
                    false, false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate9"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitArtHTTPSSOSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitArtHTTPSSOSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitArtHTTPSSOSLORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SSO/SLO with diff RelayState set POST/SOAP
     * binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2SPInitPostSOAPSSOSLORS()
    throws Exception {
        entering("samlv2SPInitPostSOAPSSOSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 10));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 10));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 10));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 10));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 10));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 10));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate10"));
            log(Level.FINEST, "samlv2SPInitPostSOAPSSOSLORS", "Map:" +
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spinitpostssoslors_ssoinit", 
                    "samlv2spinitpostssoslors_slo",
                    "samlv2spinitpostssoslors_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post",
                    false, false);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate10"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2SPInitPostSOAPSSOSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2SPInitPostSOAPSSOSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2SPInitPostSOAPSSOSLORS");
    }
    
    /**
     * @DocTest: SAML2|IDP initiated SSO/SLO with diff RelayState set.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2IDPInitArtSOAPSSOSLORS()
    throws Exception {
        entering("samlv2IDPInitArtSOAPSSOSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 11));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 11));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 11));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 11));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 11));
            configMap.put(TestConstants.KEY_IDP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_IDP_SLO_RESULT + 11));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate11"));
            log(Level.FINEST, "samlv2IDPInitArtSOAPSSOSLORS", "Map:" +
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitssoslors_idplogin", 
                    "samlv2idpinitssoslors_ssoinit",
                    "samlv2idpinitssoslors_slo", 
                    "samlv2idpinitssoslors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate11"));
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact",
                    false);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitArtSOAPSSOSLORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitArtSOAPSSOSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitArtSOAPSSOSLORS");
    }
    
    /**
     * @DocTest: SAML2|IDP initiated SSO/SLO with diff RelayState set
     * with POST/SOAP binding.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void samlv2IDPInitPostSOAPSSOSLORS()
    throws Exception {
        entering("samlv2IDPInitPostSOAPSSOSLORS", null);
        try {
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 12));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 12));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 12));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 12));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 12));
            configMap.put(TestConstants.KEY_IDP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_IDP_SLO_RESULT + 12));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate12"));
            log(Level.FINEST, "samlv2IDPInitPostSOAPSSOSLORS", "Map:" +
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2idpinitpostssoslors_idplogin", 
                    "samlv2idpinitpostssoslors_ssoinit",
                    "samlv2idpinitpostssoslors_slo", 
                    "samlv2idpinitpostssoslors_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate12"));
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post",
                    false);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");
            configMap.remove("urlparams");
            String termxmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(termxmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPInitPostSOAPSSOSLORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPInitPostSOAPSSOSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPInitPostSOAPSSOSLORS");
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
        try {
            getWebClient();
            
            // delete sp users
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT)
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            } catch (Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        try {
            log(Level.FINEST, "cleanup", "sp users to delete : " + spuserlist);
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), spuserlist,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }
            
            // Create idp users
            log(Level.FINEST, "cleanup", "idp users to delete : " +
                    idpuserlist);
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(idpurl);
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), idpuserlist,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }
        } catch (Exception e) {
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
