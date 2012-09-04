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
 * $Id: SAMLv2IDPProxyDefaultRelayStateSPTests.java,v 1.3 2009/01/27 00:15:32 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2idpproxy;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class contains tests for defaultRelayState attribute.
 * The defaultRelayState attribute is set in sp ext metadata & following tests
 * are run to make sure defaultRelayState attribute is used.
 * RelayState parameter overides defaultRelayState attribute. Following tests
 * cover that aspect too.
 * 1. SP Initiated SSO, SLO, Termination
 * 2. SP Initiated SSO, SLO, Termination with POST/SOAP profile
 * 3. SP Initiated SSO, SLO, Termination with RelayState parameter
 * 4. SP Initiated SSO, SLO, Termination with POST/SOAP profile with RelayState 
 * parameter
 */
public class SAMLv2IDPProxyDefaultRelayStateSPTests extends TestCommon {
    
    private String DEFAULT_RELAY_STATE = "       <Attribute name" +
            "=\"defaultRelayState\">\n" +
            "            <Value/>\n" +
            "        </Attribute>\n";
    public WebClient webClient;
    private String DEFAULT_RELAY_STATE_MOD;
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task;
    private HtmlPage page;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private FederationManager fmIDPProxy;
    private String spmetadata;
    private String idpproxymetadata;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    private String ssoProfile;
    private String sloProfile;
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2IDPProxyDefaultRelayStateSPTests() {
        super("SAMLv2IDPProxyDefaultRelayStateSPTests");
    }
    
    /**
     * This setup method creates required users.
     */
    @Parameters({"ssoprofile", "sloprofile"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String strSSOProfile, String strSLOProfile)
    throws Exception {
        ArrayList list;
        Object[] params = {strSSOProfile, strSLOProfile};
        try {
            ssoProfile = strSSOProfile;
            sloProfile = strSLOProfile;
            log(Level.FINEST, "setup", "Entering");
            //Upload global properties file in configMap
            baseDir = getTestBase();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.putAll(getMapFromResourceBundle("samlv2idpproxy" +
                    fileseparator + "SAMLv2IDPProxyDefaultRelayStateSPTests"));
            log(Level.FINEST, "setup", "ConfigMap is : " + configMap );
            
            // Create sp users
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
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            list = new ArrayList();
            list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            
            // Create idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            
            // Create idp proxy users
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            
            fmIDPProxy = new FederationManager(idpproxyurl);
            list.clear();
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_PROXY_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_PROXY_USER));
            list.add("userpassword=" + configMap.get(
                    TestConstants.KEY_IDP_PROXY_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDPProxy.createIdentity(
                    webClient, configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), configMap.get(
                    TestConstants.KEY_IDP_PROXY_USER), "User",
                    list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed at IDP Proxy");
                assert false;
            }
            DefaultRelayStateSPSetup(webClient, fmSP, fmIDPProxy, configMap);
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
     * This is setup method. It sets the defaultRelayState attribute in
     * sp extended metadata
     */
    public void DefaultRelayStateSPSetup(WebClient webClient, FederationManager 
            fmsp, FederationManager fmidpproxy, Map configMap)
    throws Exception {
        entering("DefaultRelayStateSPSetup", null);
        try {
            HtmlPage spmetaPage = fmsp.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               log(Level.SEVERE, "DefaultRelayStateSPSetup", "exportEntity" +
                       " famadm command failed");
               assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            DEFAULT_RELAY_STATE_MOD = "       <Attribute " +
                    "name=\"defaultRelayState\">\n" +
                    "            <Value>" + configMap.get("defaultRelayState") +
                    "</Value>\n" +
                    "        </Attribute>\n";
            String spmetadataMod = spmetadata.replaceAll(DEFAULT_RELAY_STATE,
                    DEFAULT_RELAY_STATE_MOD);
            log(Level.FINEST, "DefaultRelayStateSPSetup", "Modified metadata:" +
                    spmetadataMod);
            if (FederationManager.getExitCode(fmsp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Deletion of " +
                        "Extended entity failed");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "deleteEntity" +
                        " famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Deleted SP Ext " +
                        "entity");
            }
            
            if (FederationManager.getExitCode(fmsp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    "", spmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Failed to " +
                        "import extended metadata");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "importEntity" +
                        " famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Imported SP " +
                        "extended metadata");
            }

            //Import SP ext metadata on IDP side 
            HtmlPage idpproxymetaPage = fmidpproxy.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), false, false, true, 
                    "saml2");
            if (FederationManager.getExitCode(idpproxymetaPage) != 0) {
               log(Level.SEVERE, "DefaultRelayStateSPSetup", "exportEntity" +
                       " famadm command failed");
               assert false;
            }
            idpproxymetadata = MultiProtocolCommon.getExtMetadataFromPage(
                    idpproxymetaPage);
            String idpproxymetadataMod = idpproxymetadata.replaceAll(
                    DEFAULT_RELAY_STATE, DEFAULT_RELAY_STATE_MOD);
            log(Level.FINEST, "DefaultRelayStateSPSetup", "Modified metadata:" +
                    idpproxymetadataMod);
            if (FederationManager.getExitCode(fmidpproxy.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), true, "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Deletion of " +
                        "Extended entity failed on IDP Proxy side");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "deleteEntity " +
                        "famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Deleted SP Ext " +
                        "entity on IDP Proxy side");
            }
            
           if (FederationManager.getExitCode(fmidpproxy.importEntity(webClient,
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), "", idpproxymetadataMod, 
                    "", "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Failed to " +
                        "import SP extended metadata on IDP Proxy side");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "importEntity" +
                        " famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Imported SP " +
                        "extended metadata on IDP Proxy side");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultRelayStateSPSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("DefaultRelayStateSPSetup");
    }
    
    /**
     * Create the webClient which will be used for the rest of the tests.
     */
    public void getWebClient()
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
     * Run saml2 SP initiated sso with defaultRelayState set in metadata
     * @DocTest: SAML2|Perform SP initiated sso with defaultRelayState.
     * Testcase ID: SAMLv2_usecase_30_1
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SPSSOInitdefaultRS()
    throws Exception {
        entering("SPSSOInitdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitdefaultRS", "Running: " +
                    "SPSSOInitdefaultRS");
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from ext metadata is loaded after " +
                    "successful SSO. SSO with " + ssoProfile);
            getWebClient();
            xmlfile = baseDir + "spssoinitdefaultrs.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, ssoProfile, false, 
                    true);
            log(Level.FINE, "SPSSOInitdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitdefaultRS");
    }
    
    /**
     * Run saml2 SP initiated SLO with defaultRelayState set in metadata
     * @DocTest: SAML2|Perform SP initiated SLO with defaultRelayState.
     * Testcase ID: SAMLv2_usecase_30_9
     */
   @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSSOInitdefaultRS"})
    public void SPSLOdefaultRS()
    throws Exception {
        entering("SPSLOdefaultRS", null);
        try {
            log(Level.FINE, "SPSLOdefaultRS", "Running:" +
                    " SPSLOdefaultRS");
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from ext metadata is loaded after " +
                    "successful SLO. SLO with " + sloProfile);
            xmlfile = baseDir + "SPSLOdefaultRS.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, sloProfile, true);
            log(Level.FINE, "SPSLOdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLOdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLOdefaultRS");
    }
    
    /**
     * SP Init SSO with RelayState specified in URL
     * @DocTest: SAML2|Perform SP initiated sso with RelayState specified in URL
     * Testcase ID: SAMLv2_usecase_30_5
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSLOdefaultRS"})
    public void SPSSOInitRSdefaultRS()
    throws Exception {
        entering("SPSSOInitRSdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitRSdefaultRS", "Running: " +
                    "SPSSOInitRSdefaultRS");
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from URL parameter is loaded instead of the " +
                    "relay state from the extended metadata after successful" +
                    " SSO. SSO with " + ssoProfile);
            getWebClient();
            xmlfile = baseDir + "SPSSOInitRSdefaultRS.xml";
            configMap.put("urlparams", "RelayState=" + configMap.get(
                    "RelayState"));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT, 
                    (String)configMap.get("ssoinitresultRS"));
            log(Level.FINEST, "SPSSOInitRSdefaultRS", "ConfigMap " + 
                    configMap);
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, ssoProfile, true,
                    true);
            log(Level.FINE, "SPSSOInitRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitRSdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitRSdefaultRS");
    }
    
    /**
     * SP Init SLO with RelayState  
     * @DocTest: SAML2|Perform SP initiated slo.
     * Testcase ID: SAMLv2_usecase_30_13
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSSOInitRSdefaultRS"})
    public void SPSLORSdefaultRS()
    throws Exception {
        entering("SPSLORSdefaultRS", null);
        try {
            log(Level.FINE, "SPSLORSdefaultRS", "Running: " +
                    "SPSLORSdefaultRS");
            Reporter.log("Test Description: This test will make sure the " +
                    "relay state from URL parameter is loaded instead of the " +
                    "relay state from the extended metadata after " +
                    "successful SLO. SLO with " + sloProfile);
            xmlfile = baseDir + "SPSLORSdefaultRS.xml";
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    (String)configMap.get(TestConstants.KEY_SP_SLO_RESULT + 
                    "RS"));
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, sloProfile, true);
            log(Level.FINE, "SPSLORSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLORSdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLORSdefaultRS");
    }
    
    /**
     * Cleanup methods deletes all the users which were created in setup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        try {
            getWebClient();
            // delete sp users
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_SP_USER));
            log(Level.FINE, "cleanup", "sp users to delete :" +
                    configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed at SP");
                assert false;
            }
            
            // Delete idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_IDP_USER));
            log(Level.FINE, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed at IDP");
                assert false;
            }

             consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_IDP_PROXY_USER));
            log(Level.FINE, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER));
            if (FederationManager.getExitCode(fmIDPProxy.deleteIdentities(
                    webClient, configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed at IDP Proxy");
                assert false;
            }
            
           if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of " +
                        "Extended entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed at SP");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Deleted SP Ext entity");
            }
            
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to import extended " +
                        "metadata");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Imported SP extended metadata");
            }

            //Import SP ext metadata on IDP Proxy side 
            if (FederationManager.getExitCode(fmIDPProxy.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of " +
                        "Extended entity failed on IDP Proxy side");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Deleted SP Ext entity on IDP " +
                        "Proxy side");
            }
            
           if (FederationManager.getExitCode(fmIDPProxy.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    "", idpproxymetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to " +
                        "import SP extended metadata on IDP Proxy side");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Imported SP " +
                        "extended metadata on IDP Proxy side");
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
