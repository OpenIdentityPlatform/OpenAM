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
 * $Id: SAMLv2IDPProxyRelayStateTests.java,v 1.3 2009/01/27 00:15:33 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2idpproxy;

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
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
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
public class SAMLv2IDPProxyRelayStateTests extends TestCommon {
    
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private FederationManager fmIDPProxy;
    private DefaultTaskHandler task;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    ArrayList idpproxyuserlist = new ArrayList();
    private String  baseDir;
    private HtmlPage page;
    private URL url;
    private String ssoProfile;
    private String sloProfile;
    
    /** Creates a new instance of SAMLv2IDPProxyRelayStateTests */
    public SAMLv2IDPProxyRelayStateTests() {
        super("SAMLv2IDPProxyRelayStateTests");
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
        Object[] params = {strSSOProfile, strSLOProfile};
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
            log(Level.FINEST, "setup", "Map is " + configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            idpproxyurl = configMap.get(TestConstants.KEY_IDP_PROXY_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_PROXY_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PROXY_PORT) +
                    configMap.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
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
            
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            fmIDPProxy = new FederationManager(idpproxyurl);

            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2idpproxy" +
                    fileseparator + "SAMLv2IDPProxyRelayStateTests");
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
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_IDP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
                    assert false;
                }
                idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + i));
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
     * @DocTest: SAML2|SP Init SSO with RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2SPInitSSORS()
    throws Exception {
        entering("samlv2IDPProxySPInitSSORS", null);
        try {
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from URL parameter is loaded after " +
                    "successful SSO. SSO with " + ssoProfile);
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
            configMap.put(TestConstants.KEY_IDP_PROXY_USER,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 1));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate1"));
            log(Level.FINEST, "samlv2IDPProxySPInitSSORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2spIDPProxyinitssors_ssoinit", 
                    "samlv2IDPProxyspinitssors_slo", };
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    false, true);
            configMap.remove("urlparams");
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPProxySPInitSSORS",
                        "Executing xml file: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPProxySPInitSSORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPProxySPInitSSORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SLO with RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2IDPProxySPInitSLORS()
    throws Exception {
        entering("samlv2IDPProxySPInitSLORS", null);
        try {
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from URL parameter is loaded after " +
                    "successful SLO. SLO with " + sloProfile);
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
            configMap.put(TestConstants.KEY_IDP_PROXY_USER,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER + 2));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 2));
            log(Level.FINEST, "samlv2IDPProxySPInitSLORS", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2IDPProxyspinitslors_ssoinit", 
                    "samlv2IDPProxyspinitslors_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    false, true);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate2"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            configMap.remove("urlparams");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPProxySPInitSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPProxySPInitSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPProxySPInitSLORS");
    }
    
    /**
     * @DocTest: SAML2|SP Init SSO/SLO with diff RelayState set
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void samlv2IDPProxySPInitSSOSLORS()
    throws Exception {
        entering("samlv2IDPProxySPInitSSOSLORS", null);
        try {
            Reporter.log("Test Description: This test will make sure that " +
                    "different relay states from URL parameter are loaded " +
                    "after successful SSO & SLO. SSO with " + ssoProfile + 
                    "& SLO with " + sloProfile);
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
            configMap.put(TestConstants.KEY_IDP_PROXY_USER,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER + 3));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT,
                    usersMap.get(TestConstants.KEY_SSO_INIT_RESULT + 3));
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    usersMap.get(TestConstants.KEY_SP_SLO_RESULT + 3));
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate3"));
            log(Level.FINEST, "samlv2IDPProxySPInitSSOSLORS", "Map:" +
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"samlv2IDPProxyspinitssoslors_ssoinit", 
                    "samlv2IDPProxyspinitssoslors_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    false, true);
            configMap.put("urlparams", "RelayState="
                    + usersMap.get("relaystate3"));
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            configMap.remove("urlparams");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINEST, "samlv2IDPProxySPInitSSOSLORS",
                        "Executing xml file:  " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i] + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "samlv2IDPProxySPInitSSOSLORS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("samlv2IDPProxySPInitSSOSLORS");
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
            log(Level.FINEST, "cleanup", "sp users to delete : " + spuserlist);
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    spuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }
            
            // Delete idp users
            log(Level.FINEST, "cleanup", "idp users to delete : " +
                    idpuserlist);
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(idpurl);
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }

            // Delete idp proxy users
            log(Level.FINEST, "cleanup", "idp proxy users to delete : " +
                    idpproxyuserlist);
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
}
