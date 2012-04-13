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
 * $Id: SAMLv2DefaultRelayStateSPTests.java,v 1.8 2009/01/27 00:14:08 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
public class SAMLv2DefaultRelayStateSPTests extends TestCommon {
    
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
    private String spmetadata;
    private String idpmetadata;
    private String spurl;
    private String idpurl;
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2DefaultRelayStateSPTests() {
        super("SAMLv2DefaultRelayStateSPTests");
    }
    
    /**
     * This setup method creates required users.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        ArrayList list;
        try {
            log(Level.FINEST, "setup", "Entering");
            //Upload global properties file in configMap
            baseDir = getTestBase();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator + 
                    "SAMLv2DefaultRelayStateSPTests"));
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
            DefaultRelayStateSPSetup(webClient, fmSP, fmIDP, configMap);
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
     * This is setup method. It sets the defaultRelayState attribute in
     * sp extended metadata
     */
    public void DefaultRelayStateSPSetup(WebClient webClient, FederationManager 
            fmSP, FederationManager fmIDP, Map configMap)
    throws Exception {
        entering("DefaultRelayStateSPSetup", null);
        try {
            HtmlPage spmetaPage = fmSP.exportEntity(webClient,
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
            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
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
            
            if (FederationManager.getExitCode(fmSP.importEntity(webClient,
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
            spmetaPage = fmIDP.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
               log(Level.SEVERE, "DefaultRelayStateSPSetup", "exportEntity" +
                       " famadm command failed");
               assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            String idpmetadataMod = idpmetadata.replaceAll(DEFAULT_RELAY_STATE,
                    DEFAULT_RELAY_STATE_MOD);
            log(Level.FINEST, "DefaultRelayStateSPSetup", "Modified SP " +
                    "metadata from IDP side: " + idpmetadataMod);
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Deletion of " +
                        "Extended entity failed on IDP side");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "deleteEntity " +
                        "famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Deleted SP Ext " +
                        "entity on IDP side");
            }
            
           if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    "", idpmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "Failed to " +
                        "import extended metadata on IDP side");
                log(Level.SEVERE, "DefaultRelayStateSPSetup", "importEntity" +
                        " famadm command failed");
                assert(false);
            } else {
                log(Level.FINE, "DefaultRelayStateSPSetup", "Imported SP " +
                        "extended metadata on IDP side");
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
    public void SPSSOInitArtdefaultRS()
    throws Exception {
        entering("SPSSOInitArtdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitArtdefaultRS", "Running: " +
                    "SPSSOInitArtdefaultRS");
            getWebClient();
            xmlfile = baseDir + "spssoinitdefaultrs.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact", false, 
                    false);
            log(Level.FINE, "SPSSOInitArtdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitArtdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitArtdefaultRS");
    }
    
    /**
     * Run saml2 SP initiated SLO with defaultRelayState set in metadata
     * @DocTest: SAML2|Perform SP initiated SLO with defaultRelayState.
     * Testcase ID: SAMLv2_usecase_30_9
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSSOInitArtdefaultRS"})
    public void SPSLOHTTPdefaultRS()
    throws Exception {
        entering("SPSLOHTTPdefaultRS", null);
        try {
            log(Level.FINE, "SPSLOHTTPdefaultRS", "Running:" +
                    " SPSLOHTTPdefaultRS");
            xmlfile = baseDir + "SPSLOHTTPdefaultRS.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "http", false);
            log(Level.FINE, "SPSLOHTTPdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLOHTTPdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLOHTTPdefaultRS");
    }
    
    /**
     * SP Init termination
     * @DocTest: SAML2|Perform SP initiated termination
     * Testcase ID: SAMLv2_usecase_30_17
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSLOHTTPdefaultRS"})
    public void SPTerminateHTTPdefaultRS()
    throws Exception {
        entering("SPTerminateHTTPdefaultRS", null);
        try {
            log(Level.FINE, "SPTerminateHTTPdefaultRS", "Running: " +
                    "SPTerminateHTTPdefaultRS");
            xmlfile = baseDir + "SPTerminateHTTPdefaultRS.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "http");
            log(Level.FINE, "SPTerminateHTTPdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPTerminateHTTPdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("SPTerminateHTTPdefaultRS");
    }
    
    /**
     * SP Init SSO with RelayState specified in URL
     * @DocTest: SAML2|Perform SP initiated sso with RelayState specified in URL
     * Testcase ID: SAMLv2_usecase_30_5
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPTerminateHTTPdefaultRS"})
    public void SPSSOInitArtRSdefaultRS()
    throws Exception {
        entering("SPSSOInitArtRSdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitArtRSdefaultRS", "Running: " +
                    "SPSSOInitArtRSdefaultRS");
            getWebClient();
            xmlfile = baseDir + "SPSSOInitArtRSdefaultRS.xml";
            configMap.put("urlparams", "RelayState=" + configMap.get(
                    "RelayState"));
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT, 
                    (String)configMap.get("ssoinitresultRS"));
            log(Level.FINEST, "SPSSOInitArtRSdefaultRS", "ConfigMap " + 
                    configMap);
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact", false,
                    false);
            log(Level.FINE, "SPSSOInitArtRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitArtRSdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitArtRSdefaultRS");
    }
    
    /**
     * SP Init SLO with RelayState  
     * @DocTest: SAML2|Perform SP initiated slo.
     * Testcase ID: SAMLv2_usecase_30_13
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSSOInitArtRSdefaultRS"})
    public void SPSLOHTTPRSdefaultRS()
    throws Exception {
        entering("SPSLOHTTPRSdefaultRS", null);
        try {
            log(Level.FINE, "SPSLOHTTPRSdefaultRS", "Running: " +
                    "SPSLOHTTPRSdefaultRS");
            xmlfile = baseDir + "SPSLOHTTPRSdefaultRS.xml";
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    (String)configMap.get(TestConstants.KEY_SP_SLO_RESULT + 
                    "RS"));
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "http", false);
            log(Level.FINE, "SPSLOHTTPRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLOHTTPRSdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLOHTTPRSdefaultRS");
    }
    
    /**
     * SP Init Termination with RelayState  
     * @DocTest: SAML2|Perform SP initiated termination
     * Testcase ID: SAMLv2_usecase_30_21
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPSLOHTTPRSdefaultRS"})
    public void SPTerminateHTTPRSdefaultRS()
    throws Exception {
        entering("SPTerminateHTTPRSdefaultRS", null);
        try {
            log(Level.FINE, "SPTerminateHTTPRSdefaultRS", "Running: " +
                    "SPTerminateHTTPRSdefaultRS");
            xmlfile = baseDir + "SPTerminateHTTPRSdefaultRS.xml";
            configMap.put(TestConstants.KEY_TERMINATE_RESULT,
                    (String)configMap.get(TestConstants.KEY_TERMINATE_RESULT + 
                    "RS"));
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "http");
            log(Level.FINE, "SPTerminateHTTPRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPTerminateHTTPRSdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("SPTerminateHTTPRSdefaultRS");
    }
        
    /**
     * SP Init SSO with post profile
     * @DocTest: SAML2|Perform SP initiated sso with post profile.
     * Testcase ID: SAMLv2_usecase_30_3
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPTerminateHTTPRSdefaultRS"})
    public void SPSSOInitPostdefaultRS()
    throws Exception {
        entering("SPSSOInitPostdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitPostdefaultRS", "Running: " +
                    "SPSSOInitPostdefaultRS");
            getWebClient();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator + 
                    "SAMLv2DefaultRelayStateSPTests"));
            xmlfile = baseDir + "SPSSOInitPostdefaultRS.xml";
            log(Level.FINEST, "SPSSOInitPostdefaultRS", "ConfigMap " + 
                    configMap);
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "post", false, 
                    false);
            log(Level.FINE, "SPSSOInitPostdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitPostdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitPostdefaultRS");
    }
    
    /**
     * SP Init SLO with soap profile
     * @DocTest: SAML2|Perform SP initiated slo with soap profile.
     * Testcase ID: SAMLv2_usecase_30_11
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPSSOInitPostdefaultRS"})
    public void SPSLOSOAPdefaultRS()
    throws Exception {
        entering("SPSLOSOAPdefaultRS", null);
        try {
            log(Level.FINE, "SPSLOSOAPdefaultRS", "Running: " +
                    "SPSLOSOAPdefaultRS");
            xmlfile = baseDir + "SPSLOSOAPdefaultRS.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "soap", false);
            log(Level.FINE, "SPSLOSOAPdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLOSOAPdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLOSOAPdefaultRS");
    }
    
    /**
     * SP Init Termination with soap profile
     * @DocTest: SAML2|Perform SP initiated termination with soap profile
     * Testcase ID: SAMLv2_usecase_30_19
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPSLOSOAPdefaultRS"})
    public void SPTerminateSOAPdefaultRS()
    throws Exception {
        entering("SPTerminateSOAPdefaultRS", null);
        try {
            log(Level.FINE, "SPTerminateSOAPdefaultRS", "Running: " +
                    "SPTerminateSOAPdefaultRS");
            xmlfile = baseDir + "SPTerminateSOAPdefaultRS.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "soap");
            log(Level.FINE, "SPTerminateSOAPdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPTerminateSOAPdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("SPTerminateSOAPdefaultRS");
    }
    
    /**
     * SP Init SSO with RelayState using post profile 
     * @DocTest: SAML2|Perform SP initiated sso with post profile.
     * Testcase ID: SAMLv2_usecase_30_7
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPTerminateSOAPdefaultRS"})
    public void SPSSOInitPostRSdefaultRS()
    throws Exception {
        entering("SPSSOInitPostRSdefaultRS", null);
        try {
            log(Level.FINE, "SPSSOInitPostRSdefaultRS", "Running: " +
                    "SPSSOInitPostRSdefaultRS");
            getWebClient();
            xmlfile = baseDir + "SPSSOInitPostRSdefaultRS.xml";
            configMap.put(TestConstants.KEY_SSO_INIT_RESULT, 
                    (String)configMap.get(TestConstants.KEY_SSO_INIT_RESULT + 
                    "RS"));
           configMap.put("urlparams", "RelayState=" + configMap.get(
                   "RelayState"));
           log(Level.FINEST, "SPSSOInitPostRSdefaultRS", "ConfigMap " +
                   configMap); 
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "post", false,
                    false);
            log(Level.FINE, "SPSSOInitPostRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSSOInitPostRSdefaultRS", e.getMessage());
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, spurl + "/UI/Logout");
            e.printStackTrace();
            throw e;
        }
        exiting("SPSSOInitPostRSdefaultRS");
    }
    
    /**
     * SP Init SLO with RelayState using soap profile 
     * @DocTest: SAML2|Perform SP initiated slo with soap profile.
     * Testcase ID: SAMLv2_usecase_30_15
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPSSOInitPostRSdefaultRS"})
    public void SPSLOSOAPRSdefaultRS()
    throws Exception {
        entering("SPSLOSOAPRSdefaultRS", null);
        try {
            log(Level.FINE, "SPSLOSOAPRSdefaultRS", "Running: " +
                    "SPSLOSOAPRSdefaultRS");
            xmlfile = baseDir + "SPSLOSOAPRSdefaultRS.xml";
            configMap.put(TestConstants.KEY_SP_SLO_RESULT,
                    (String)configMap.get(TestConstants.KEY_SP_SLO_RESULT + 
                    "RS"));
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "soap", false);
            log(Level.FINE, "SPSLOSOAPRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPSLOSOAPRSdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPSLOSOAPRSdefaultRS");
    }
    
    /**
     * SP Init termination with RelayState using soap profile 
     * @DocTest: SAML2|Perform SP initiated termination with soap profile
     * Testcase ID: SAMLv2_usecase_30_23
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"SPSLOSOAPRSdefaultRS"})
    public void SPTerminateSOAPRSdefaultRS()
    throws Exception {
        entering("SPTerminateSOAPRSdefaultRS", null);
        try {
            log(Level.FINE, "SPTerminateSOAPRSdefaultRS", "Running: " +
                    "SPTerminateSOAPRSdefaultRS");
            xmlfile = baseDir + "SPTerminateSOAPRSdefaultRS.xml";
            configMap.put(TestConstants.KEY_TERMINATE_RESULT,
                    (String)configMap.get(TestConstants.KEY_TERMINATE_RESULT + 
                    "RS"));
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "soap");
            log(Level.FINE, "SPTerminateSOAPRSdefaultRS", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPTerminateSOAPRSdefaultRS", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("SPTerminateSOAPRSdefaultRS");
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
                        " failed");
                assert false;
            }
            
            // Create idp users
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
                        " failed");
                assert false;
            }

            if (FederationManager.getExitCode(fmSP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of " +
                        "Extended entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
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

            //Import SP ext metadata on IDP side 
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of " +
                        "Extended entity failed on IDP side");
                log(Level.SEVERE, "cleanup", "deleteEntity famadm command" +
                        " failed");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Deleted SP Ext entity on IDP side");
            }
            
           if (FederationManager.getExitCode(fmIDP.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                    idpmetadata, "", "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Failed to " +
                        "import extended metadata on IDP side");
                log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                        " failed");
                assert(false);
            } else {
                log(Level.FINE, "cleanup", "Imported SP " +
                        "extended metadata on IDP side");
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
