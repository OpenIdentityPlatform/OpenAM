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
 * $Id: MultiProtocolSmokeTest.java,v 1.8 2009/01/27 00:07:56 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.multiprotocol;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.WSFedCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class tests Multiprotocol related SP SSO, SLO & IDP initiated SLO across
 * samlv2, WSFed & IDFF protocols.
 */
public class MultiProtocolSmokeTest extends TestCommon {
    
    public WebClient webClient;
    private Map<String, String> idffConfigMap;
    private Map<String, String> samlv2ConfigMap;
    private Map<String, String> multiprotocolConfigMap;
    private Map<String, String> wsfedConfigMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task;
    private HtmlPage page;
    private FederationManager fmIDFFSP;
    private FederationManager fmSAMLv2SP;
    private FederationManager fmWSFedSP;
    private FederationManager fmIDP;
    private String fileseparator;
    private String idffspurl;
    private String samlv2spurl;
    private String wsfedspurl;
    private String idpurl;
    private URL url;
    
    /**
     * This is constructor for this class.
     */
    public MultiProtocolSmokeTest() {
        super("MultiProtocolSmokeTest");
    }
    
    /**
     * This setup method creates required users.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        ArrayList list;
        entering("setup", null);
        try {
            fileseparator = System.getProperty("file.separator");
            //Upload global properties file in configMap
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + fileseparator + "built" + fileseparator + "classes"
                    + fileseparator;
            idffConfigMap = new HashMap<String, String>();
            samlv2ConfigMap = new HashMap<String, String>();
            wsfedConfigMap = new HashMap<String, String>();
            idffConfigMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            idffConfigMap.putAll(getMapFromResourceBundle("idff" +
                    fileseparator + "idffTestData"));
            samlv2ConfigMap = getMapFromResourceBundle("samlv2" +
                    fileseparator + "samlv2TestConfigData");
            samlv2ConfigMap.putAll(getMapFromResourceBundle("samlv2" +
                    fileseparator + "samlv2TestData"));
            wsfedConfigMap = getMapFromResourceBundle("wsfed" + fileseparator +
                    "WSFedTestConfigData");
            wsfedConfigMap.putAll(getMapFromResourceBundle("wsfed" +
                    fileseparator + "WSFedTestData"));
            multiprotocolConfigMap =
                    getMapFromResourceBundle("multiprotocol" + fileseparator +
                    "MultiProtocolSmokeTest");
            log(Level.FINEST, "setup", "IDFF ConfigMap is : " + idffConfigMap );
            log(Level.FINEST, "setup", "SAMLv2 ConfigMap is : " +
                    samlv2ConfigMap );
            log(Level.FINEST, "setup", "WSFed ConfigMap is : " +
                    wsfedConfigMap );
            log(Level.FINEST, "setup", "Multiprotocol ConfigMap is : " +
                    multiprotocolConfigMap );
            
            // Create sp users
            idffspurl = idffConfigMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + idffConfigMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    idffConfigMap.get(TestConstants.KEY_SP_PORT) +
                    idffConfigMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            samlv2spurl = samlv2ConfigMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + samlv2ConfigMap.get(TestConstants.KEY_SP_HOST) +
                    ":" +  samlv2ConfigMap.get(TestConstants.KEY_SP_PORT) +
                    samlv2ConfigMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            wsfedspurl = wsfedConfigMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + wsfedConfigMap.get(TestConstants.KEY_SP_HOST) +
                    ":" + wsfedConfigMap.get(TestConstants.KEY_SP_PORT) +
                    wsfedConfigMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = idffConfigMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + idffConfigMap.get(TestConstants.KEY_IDP_HOST) +
                    ":" + idffConfigMap.get(TestConstants.KEY_IDP_PORT) +
                    idffConfigMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            getWebClient();
            
            //Create user on IDFF SP.
            consoleLogin(webClient, idffspurl + "/UI/Login",
                    idffConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    idffConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmIDFFSP = new FederationManager(idffspurl);
            list = new ArrayList();
            list.add("sn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_IDFF_SP_USER));
            list.add("cn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_IDFF_SP_USER));
            list.add("userpassword=" +
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDFF_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            idffConfigMap.put(TestConstants.KEY_SP_USER,
                    multiprotocolConfigMap.get(TestConstants.KEY_IDFF_SP_USER));
            idffConfigMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDFF_SP_USER_PASSWORD));
            idffConfigMap.put(TestConstants.KEY_IDP_USER,
                    multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER));
            idffConfigMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER_PASSWORD));
            if (FederationManager.getExitCode(fmIDFFSP.createIdentity(webClient,
                    idffConfigMap.get(TestConstants.KEY_SP_REALM),
                    multiprotocolConfigMap.get(TestConstants.KEY_IDFF_SP_USER),
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "IDFF User is not created " +
                        "successfully.");
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            } else {
                log(Level.FINE, "setup", "IDFF User is created successfully.");
            }
            
            //Create user on SAMLv2 SP.
            consoleLogin(webClient, samlv2spurl + "/UI/Login",
                    samlv2ConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    samlv2ConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSAMLv2SP = new FederationManager(samlv2spurl);
            list = new ArrayList();
            list.add("sn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER));
            list.add("cn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER));
            list.add("userpassword=" +
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            samlv2ConfigMap.put(TestConstants.KEY_SP_USER,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER));
            samlv2ConfigMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER_PASSWORD));
            samlv2ConfigMap.put(TestConstants.KEY_IDP_USER,
                    multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER));
            samlv2ConfigMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER_PASSWORD));
            if (FederationManager.getExitCode(fmSAMLv2SP.createIdentity(
                    webClient, samlv2ConfigMap.get(TestConstants.KEY_SP_REALM),
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_SAMLv2_SP_USER), "User", list)) != 0) {
                log(Level.SEVERE, "setup", "SAMLv2 User is not created " +
                        "successfully.");
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            } else {
                log(Level.FINE, "setup", "SAMLv2 User is created successfully");
            }
            
            //Create user on WSfed SP.
            consoleLogin(webClient, wsfedspurl + "/UI/Login",
                    wsfedConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    wsfedConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmWSFedSP = new FederationManager(wsfedspurl);
            list = new ArrayList();
            list.add("sn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_WSFed_SP_USER));
            list.add("cn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_WSFed_SP_USER));
            list.add("userpassword=" +
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_WSFed_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            wsfedConfigMap.put(TestConstants.KEY_SP_USER,
                    multiprotocolConfigMap.get(
                    TestConstants.KEY_WSFed_SP_USER));
            wsfedConfigMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_WSFed_SP_USER_PASSWORD));
            wsfedConfigMap.put(TestConstants.KEY_IDP_USER,
                    multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER));
            wsfedConfigMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER_PASSWORD));
            if (FederationManager.getExitCode(fmWSFedSP.createIdentity(
                    webClient, wsfedConfigMap.get(TestConstants.KEY_SP_REALM),
                    multiprotocolConfigMap.get(TestConstants.KEY_WSFed_SP_USER),
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "WSFed User is not created " +
                        "successfully.");
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            } else {
                log(Level.FINE, "setup", "WSFed User is created successfully.");
            }
            
            // Create idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    idffConfigMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    idffConfigMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add("sn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER));
            list.add("cn=" + multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER));
            list.add("userpassword=" +
                    multiprotocolConfigMap.get(TestConstants.
                    KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDP.createIdentity(webClient,
                    idffConfigMap.get(TestConstants.KEY_IDP_REALM),
                    multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER),
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, idffspurl + "/UI/Logout");
            consoleLogout(webClient, samlv2spurl + "/UI/Logout");
            consoleLogout(webClient, wsfedspurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * Federate SAMLv2 users for multiprotocol testing.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, dependsOnMethods={"setup"})
    public void federateSAMLv2Users()
    throws Exception {
        entering("federateSAMLv2Users", null);
        try {
            log(Level.FINE, "federateSAMLv2Users",
                    "Running: federateSAMLv2Users");
            getWebClient();
            log(Level.FINE, "federateUsers", "Federate SAMLv2 Users");
            xmlfile = baseDir +
                    "MultiProtocolfederateUsers_SAMLv2SPSSOInit.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, samlv2ConfigMap, "artifact",
                    false, false);
            log(Level.FINE, "federateSAMLv2Users", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "federateSAMLv2Users", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } finally {
            consoleLogout(webClient, samlv2spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("federateSAMLv2Users");
    }
    
    /**
     * Federate IDFF users.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"federateSAMLv2Users"})
    public void federateIDFFUsers()
    throws Exception {
        entering("federateIDFFUsers", null);
        try {
            getWebClient();
            log(Level.FINE, "federateIDFFUsers", "Federate IDFF users");
            xmlfile = baseDir + "MultiProtocolfederateUsers_IDFFSPSSOInit.xml";
            consoleLogin(webClient, idffspurl + "/UI/Login",
                    idffConfigMap.get(TestConstants.KEY_SP_USER),
                    idffConfigMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            xmlfile = baseDir + "testspinitfederation.xml";
            IDFFCommon.getxmlSPIDFFFederate(xmlfile, idffConfigMap, true);
            log(Level.FINE, "federateIDFFUsers", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "federateIDFFUsers", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } finally {
            consoleLogout(webClient, idffspurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("federateIDFFUsers");
    }
    
    /**
     * Create the webClient which will be used for the rest of the tests.
     */
    public void getWebClient()
    throws Exception {
        try {
            //webClient = new WebClient(BrowserVersion.MOZILLA_1_0);
            webClient = new WebClient();
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Multiprotocol SSO : First SAMLv2, SP initiated SSO,
     * then check for IDFF & WSFed SSO without SP or IDP login
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void MultiProtocolSPSSOSAMLv2Init()
    throws Exception {
        entering("MultiProtocolSPSSOSAMLv2Init", null);
        try {
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init",
                    "Running: MultiProtocolSPSSOSAMLv2Init");
            Reporter.log("Test Description: MultiProtocolSPSSOSAMLv2Init test" +
                    " executes SAMLv2, SP initiated SSO,  then check for " +
                    "IDFF & WSFed SSO without SP or IDP login");
            getWebClient();
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "Run SAMLv2 SSO " +
                    "Init first");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOSAMLv2Init_SAMLv2SPSSOInit.xml";
            SAMLv2Common.getxmlSPSSO(xmlfile, samlv2ConfigMap, "artifact");
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "After SAMLv2 " +
                    "SSO now run IDFF SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOSAMLv2Init_IDFFSPSSOInit.xml";
            MultiProtocolCommon.getxmlIDFFSPInitSSO(xmlfile, idffConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "After SAMLv2, " +
                    "IDFF SSO now run WSfed SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOSAMLv2Init_WSFedSPSSOInit.xml";
            MultiProtocolCommon.getxmlWSFedSPInitSSO(xmlfile, wsfedConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSSOSAMLv2Init", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSSOSAMLv2Init");
    }
    
    /**
     * Multiprotocol SLO : First SAMLv2, SP initiated SLO,
     * then check to see SAMLv2, IDFF & WSfed SP sessions are terminated or not.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"MultiProtocolSPSSOSAMLv2Init"})
    public void MultiProtocolSPSLOSAMLv2Init()
    throws Exception {
        entering("MultiProtocolSPSSOSAMLv2Init", null);
        try {
            log(Level.FINE, "MultiProtocolSPSLOSAMLv2Init", "Run SAMLv2 SLO " +
                    "first");
            Reporter.log("Test Description: MultiProtocolSPSLOSAMLv2Init test" +
                    " executes SAMLv2, SP initiated SLO, then check to see " +
                    "SAMLv2, IDFF & WSfed SP sessions are terminated or not.");
            String samlv2SLOurl = samlv2ConfigMap.get(TestConstants.
                    KEY_SP_PROTOCOL) +"://" + samlv2ConfigMap.get(TestConstants.
                    KEY_SP_HOST) + ":" + samlv2ConfigMap.get(TestConstants.
                    KEY_SP_PORT) + samlv2ConfigMap.get(TestConstants.
                    KEY_SP_DEPLOYMENT_URI) +
                    "/saml2/jsp/spSingleLogoutInit.jsp" + "?metaAlias=" +
                    samlv2ConfigMap.get(TestConstants.
                    KEY_SP_METAALIAS) + "&idpEntityID=" + samlv2ConfigMap.
                    get(TestConstants.KEY_IDP_ENTITY_NAME);
            log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "SAMLv2 SLO " +
                    "URL is " + samlv2SLOurl);            
            page = (HtmlPage)webClient.getPage(samlv2SLOurl);
            log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "SAMLv2 SLO " +
                    "page is " + page.getWebResponse().getContentAsString());            

            Thread.sleep(10000);

            HtmlPage idffpage = (HtmlPage)webClient.getPage(idffspurl + "/UI/Login");
            if (idffpage.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "IDFF " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOSAMLv2Init", "IDFF " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init",
                        idffpage.getWebResponse().getContentAsString());
                assert false;
            }
            
            //Thread.sleep(5000);
            HtmlPage wsfedpage = (HtmlPage)webClient.getPage(wsfedspurl+ "/UI/Login");
            if (wsfedpage.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "WSFed " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOSAMLv2Init", "WSFed " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init",
                        wsfedpage.getWebResponse().getContentAsString());
                assert false;
            }

            log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "SAMLv2 " +
                        "SP url is " + samlv2spurl);
            page = (HtmlPage)webClient.getPage(samlv2spurl + "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "SAMLv2 " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOSAMLv2Init", "SAMLv2 " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
            
            HtmlPage idpPage = (HtmlPage)webClient.getPage(idpurl);
            if (idpPage.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init", "IDP " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOSAMLv2Init", "IDP " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOSAMLv2Init",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSLOSAMLv2Init", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSLOSAMLv2Init");
    }
    
       /**
     * Multiprotocol SSO : First IDFF, SP initiated SSO,
     * then check for SAMLv2 & WSFed SSO without SP or IDP login
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods={"MultiProtocolSPSLOSAMLv2Init"})
    public void MultiProtocolSPSSOIDFFInit()
    throws Exception {
        entering("MultiProtocolSPSSOIDFFInit", null);
        try {
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit",
                    "Running: MultiProtocolSPSSOIDFFInit");
            
            getWebClient();
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "Run IDFF SSO " +
                    "Init first");
            Reporter.log("Test Description: MultiProtocolSPSSOIDFFInit test" +
                    " executes IDFF, SP initiated SSO,  then check for " +
                    "SAMLv2 & WSFed SSO without SP or IDP login");
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    idffConfigMap.get(TestConstants.KEY_IDP_USER),
                    idffConfigMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            xmlfile = baseDir +
                    "MultiProtocolSPSSOIDFFInit_IDFFSPSSOInit.xml";
            IDFFCommon.getxmlSPIDFFSSO(xmlfile, idffConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "After IDFF " +
                    "SSO now run SAMLv2 SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOIDFFInit_SAMLv2SPSSOInit.xml";
            MultiProtocolCommon.getxmlSAMLv2SPInitSSO(xmlfile, samlv2ConfigMap, 
                    "artifact");
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "After SAMLv2, " +
                    "IDFF SSO now run WSfed SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOIDFFInit_WSFedSPSSOInit.xml";
            MultiProtocolCommon.getxmlWSFedSPInitSSO(xmlfile, wsfedConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOIDFFInit", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSSOIDFFInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSSOIDFFInit");
    }
    
    /**
     * Multiprotocol SLO : First IDFF, SP initiated SLO,
     * then check to see SAMLv2, IDFF & WSfed SP sessions are terminated or not.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"MultiProtocolSPSSOIDFFInit"})
    public void MultiProtocolSPSLOIDFFInit()
    throws Exception {
        entering("MultiProtocolSPSLOIDFFInit", null);
        try {
            log(Level.FINE, "MultiProtocolSPSLOIDFFInit", "Run IDFF SLO " +
                    "first");
            Reporter.log("Test Description: MultiProtocolSPSLOIDFFInit test" +
                    " executes IDFF, SP initiated SLO, then check to see " +
                    "SAMLv2, IDFF & WSfed SP sessions are terminated or not.");
            
            String idffSLOurl = idffConfigMap.get(TestConstants.
                    KEY_SP_PROTOCOL) +"://" + idffConfigMap.get(TestConstants.
                    KEY_SP_HOST) + ":" + idffConfigMap.get(TestConstants.
                    KEY_SP_PORT) + idffConfigMap.get(TestConstants.
                    KEY_SP_DEPLOYMENT_URI) + "/liberty-logout?metaAlias=" + 
                    idffConfigMap.get(TestConstants.KEY_SP_METAALIAS);
            log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "IDFF SLO url " +
                    idffSLOurl);

            page = (HtmlPage)webClient.getPage(idffSLOurl);
            log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "IDFF SLO output" +
                    page.getWebResponse().getContentAsString());
            
            Thread.sleep(10000);
            page = (HtmlPage)webClient.getPage(samlv2spurl + "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "SAMLv2 " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOIDFFInit", "SAMLv2 " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
            
            page = (HtmlPage)webClient.getPage(wsfedspurl+ "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "WSFed " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOIDFFInit", "WSFed " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }

            Thread.sleep(5000);
            page = (HtmlPage)webClient.getPage(idffspurl + "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "IDFF " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOIDFFInit", "IDFF " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
            
            HtmlPage idpPage = (HtmlPage)webClient.getPage(idpurl);
            if (idpPage.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit", "IDP " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOIDFFInit", "IDP " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOIDFFInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSLOIDFFInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSLOIDFFInit");
    }

    /**
     * Multiprotocol SSO : First WSFed, SP initiated SSO,
     * then check for IDFF & SAMLv2 SSO without SP or IDP login
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods={"MultiProtocolSPSLOIDFFInit"})
    public void MultiProtocolSPSSOWSFedInit()
    throws Exception {
        entering("MultiProtocolSPSSOWSFedInit", null);
        try {
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit",
                    "Running: MultiProtocolSPSSOWSFedInit");
            Reporter.log("Test Description: MultiProtocolSPSSOWSFedInit test" +
                    " executes WSFed, SP initiated SSO,  then check for " +
                    "SAMLv2 & IDFF SSO without SP or IDP login");
            
            getWebClient();
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit", "Run WSFed SSO " +
                    "Init first");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOWSFedInit_WSFedSPSSOInit.xml";
            WSFedCommon.getxmlSPInitSSO(xmlfile, wsfedConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit", "After WSFed " +
                    "SSO now run SAMLv2 SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOWSFedInit_SAMLv2SPSSOInit.xml";
            MultiProtocolCommon.getxmlSAMLv2SPInitSSO(xmlfile, samlv2ConfigMap, 
                    "artifact");
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            
            log(Level.FINE, "MultiProtocolSPSSOWSFedInit", "After SAMLv2, " +
                    "IDFF SSO now run IDFF SSO");
            xmlfile = baseDir +
                    "MultiProtocolSPSSOSAMLv2Init_IDFFSPSSOInit.xml";
            MultiProtocolCommon.getxmlIDFFSPInitSSO(xmlfile, idffConfigMap);
            log(Level.FINE, "MultiProtocolSPSSOSAMLv2Init", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSSOWSFedInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSSOWSFedInit");
    }
    
    /**
     * Multiprotocol SLO : First WSFed, SP initiated SLO,
     * then check to see SAMLv2, IDFF & WSfed SP sessions are terminated or not.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"MultiProtocolSPSSOWSFedInit"})
    public void MultiProtocolSPSLOWSFedInit()
    throws Exception {
        entering("MultiProtocolSPSLOWSFedInit", null);
        try {
            log(Level.FINE, "MultiProtocolSPSLOWSFedInit", "Run WSFed SLO " +
                    "first");
            Reporter.log("Test Description: MultiProtocolSPSLOWSFedInit test" +
                    " executes WSFed, SP initiated SLO, then check to see " +
                    "SAMLv2, IDFF & WSfed SP sessions are terminated or not.");
            String wsfedSLOurl = wsfedConfigMap.get(TestConstants.
                    KEY_SP_PROTOCOL) +"://" + wsfedConfigMap.get(TestConstants.
                    KEY_SP_HOST) + ":" + wsfedConfigMap.get(TestConstants.
                    KEY_SP_PORT) + wsfedConfigMap.get(TestConstants.
                    KEY_SP_DEPLOYMENT_URI) + "/WSFederationServlet/metaAlias" + 
                    wsfedConfigMap.get(TestConstants.KEY_SP_METAALIAS) + 
                    "?wa=wsignout1.0";
            log(Level.FINE, "MultiProtocolSPSLOWSFedInit", "WSFed SLO url " +
                    wsfedSLOurl);

            page = (HtmlPage)webClient.getPage(wsfedSLOurl);
            log(Level.FINEST, "MultiProtocolSPSLOWSFedInit", "WSFed SLO output" +
                    page.getWebResponse().getContentAsString());
           
            Thread.sleep(10000); 
            page = (HtmlPage)webClient.getPage(idffspurl + "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit", "IDFF " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOWSFedInit", "IDFF " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
            
            page = (HtmlPage)webClient.getPage(samlv2spurl + "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit", "SAMLv2 " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOWSFedInit", "SAMLv2 " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
            
            page = (HtmlPage)webClient.getPage(wsfedspurl+ "/UI/Login");
            if (page.getTitleText().contains("(Login)")) {
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit", "WSFed " +
                        "session is destroyed. Login page is returned.");
            } else {
                log(Level.SEVERE, "MultiProtocolSPSLOWSFedInit", "WSFed " +
                        "session is NOT destroyed.");
                log(Level.FINEST, "MultiProtocolSPSLOWSFedInit",
                        page.getWebResponse().getContentAsString());
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "MultiProtocolSPSLOWSFedInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("MultiProtocolSPSLOWSFedInit");
    }

    /**
     * This methods deletes all the users as part of cleanup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        try {
            log(Level.FINE, "cleanup", "Entering Cleanup");
            getWebClient();
            //Delete IDFF SP user
            consoleLogin(webClient, idffspurl + "/UI/Login",
                    idffConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    idffConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(multiprotocolConfigMap.get(
                    TestConstants.KEY_IDFF_SP_USER));
            log(Level.FINE, "cleanup", "IDFF SP users to delete :" +
                    multiprotocolConfigMap.get(TestConstants.KEY_IDFF_SP_USER));
            if (FederationManager.getExitCode(fmIDFFSP.deleteIdentities(
                    webClient, idffConfigMap.get(TestConstants.KEY_SP_REALM),
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
            }
            
            //Delete SAMLv2 SP User
            consoleLogin(webClient, samlv2spurl + "/UI/Login",
                    samlv2ConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    samlv2ConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(multiprotocolConfigMap.get(
                    TestConstants.KEY_SAMLv2_SP_USER));
            log(Level.FINE, "cleanup", "SAMLv2 SP users to delete :" +
                    multiprotocolConfigMap.get(
                    TestConstants.KEY_SAMLv2_SP_USER));
            if (FederationManager.getExitCode(fmSAMLv2SP.deleteIdentities(
                    webClient, samlv2ConfigMap.get(TestConstants.KEY_SP_REALM),
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }
            
            //Delete WSFed SP User
            consoleLogin(webClient, wsfedspurl + "/UI/Login",
                    wsfedConfigMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    wsfedConfigMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            idList = new ArrayList();
            idList.add(multiprotocolConfigMap.get(
                    TestConstants.KEY_WSFed_SP_USER));
            log(Level.FINE, "cleanup", "WSFed SP users to delete :" +
                    multiprotocolConfigMap.get(
                    TestConstants.KEY_WSFed_SP_USER));
            if (FederationManager.getExitCode(fmWSFedSP.deleteIdentities(
                    webClient, wsfedConfigMap.get(TestConstants.KEY_SP_REALM),
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
            }
            
            // Create idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    idffConfigMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    idffConfigMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            idList = new ArrayList();
            idList.add(multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER));
            log(Level.FINE, "cleanup", "idp users to delete :" +
                    multiprotocolConfigMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    idffConfigMap.get(TestConstants.KEY_IDP_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, idffspurl + "/UI/Logout");
            consoleLogout(webClient, samlv2spurl + "/UI/Logout");
            consoleLogout(webClient, wsfedspurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("cleanup");
    }
}
