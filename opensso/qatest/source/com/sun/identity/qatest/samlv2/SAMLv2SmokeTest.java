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
 * $Id: SAMLv2SmokeTest.java,v 1.12 2009/06/24 23:03:43 mrudulahg Exp $
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
import com.gargoylesoftware.htmlunit.ScriptException;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class tests Atrifact, HTTP-Redirect, POST & SOAP profiles with
 * SP & IDP initiated
 */
public class SAMLv2SmokeTest extends TestCommon {
    
    public WebClient webClient;
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task1;
    private HtmlPage page1;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2SmokeTest() {
        super("SAMLv2SmokeTest");
    }
    
    /**
     * This setup method creates required users.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup() 
    throws Exception {
        URL url;
        HtmlPage page;
        ArrayList list;
        try {
            log(Level.FINEST, "setup", "Entering");
            //Upload global properties file in configMap
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + SAMLv2Common.fileseparator + "built"
                    + SAMLv2Common.fileseparator + "classes"
                    + SAMLv2Common.fileseparator;
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2SmokeTest", configMap);
            log(Level.FINEST, "setup", "ConfigMap is : " + configMap );
            
            // Create sp users
            String spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
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
            consoleLogout(webClient, spurl + "/UI/Logout");
            
            // Create idp users
            String idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
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
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed");
                assert false;
            }
            consoleLogout(webClient, idpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Create the webClient which will be used for the rest of the tests.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
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
     * Run saml2 profile testcase 1.
     * @DocTest: SAML2|Perform SP initiated sso.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSPSSOInit()
    throws Exception {
        entering("testSPSSOInit", null);
        try {
            log(Level.FINEST, "testSPSSOInit", "Running: testSPSSOInit");
            getWebClient();
            xmlfile = baseDir + "test1spssoinit.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact", false, 
                    false);
            log(Level.FINEST, "testSPSSOInit", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSSOInit", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSSOInit");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform SP initiated slo.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPSSOInit"})
    public void testSPSLO()
    throws Exception {
        entering("testSPSLO", null);
        try {
            log(Level.FINEST, "testSPSLO", "Running: testSPSLO");
            xmlfile = baseDir + "test2spslo.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "http", false);
            log(Level.FINEST, "testSPSSOInit", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSLO");
    }
    
    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform SP initiated termination
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPSLO"})
    public void testSPTerminate()
    throws Exception {
        entering("testSPTerminate", null);
        try {
            log(Level.FINEST, "testSPTerminate", "Running: testSPTerminate");
            xmlfile = baseDir + "test3spterminate.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "http");
            log(Level.FINEST, "testSPTerminate", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPTerminate");
    }
    
    /**
     * Run saml2 profile .
     * @DocTest: SAML2|Perform idp initiated sso.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testSPTerminate"})
     public void testIDPSSO()
    throws Exception {
        entering("testIDPSSO", null);
        try {
            log(Level.FINEST, "testIDPSSO", "\nRunning: testIDPSSO\n");
            getWebClient();
            xmlfile = baseDir + "test7idplogin.xml";
            SAMLv2Common.getxmlIDPLogin(xmlfile, configMap);
            log(Level.FINEST, "testIDPSSO", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            xmlfile = baseDir + "test7idpssoinit.xml";
            SAMLv2Common.getxmlIDPInitSSO(xmlfile, configMap, "artifact",
                    false);
            log(Level.FINEST, "testIDPSSO", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSO");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform idp initiated slo.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testIDPSSO"})
    public void testIDPSLO()
    throws Exception {
        entering("testIDPSLO", null);
        try {
            log(Level.FINEST, "testIDPSLO", "Running: testIDPSLO");
            xmlfile = baseDir + "test8idpslo.xml";
            SAMLv2Common.getxmlIDPSLO(xmlfile, configMap, "http");
            log(Level.FINEST, "testIDPSLO", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSLO");
    }
    
    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform  idp initiated termination
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"testIDPSLO"})
    public void testIDPTerminate()
    throws Exception {
        entering("testIDPTerminate", null);
        try {
            log(Level.FINEST, "testIDPTerminate", "Running: testIDPTerminate");
            xmlfile = baseDir + "test9idpterminate.xml";
            SAMLv2Common.getxmlIDPTerminate(xmlfile, configMap, "http");
            log(Level.FINEST, "testIDPTerminate", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPTerminate");
    }
    
    /**
     * Run saml2 profile testcase 4.
     * @DocTest: SAML2|Perform SP initiated sso with post profile.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testIDPTerminate"})
    public void testSPSSOInitPost()
    throws Exception {
        entering("testSPSSOInitPost", null);
        try {
            log(Level.FINEST, "testSPSSOInitPost", "Running: testSPSSOInitPost");
            getWebClient();
            xmlfile = baseDir + "test4spssoinit.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "post", false, 
                    false);
            log(Level.FINEST, "testSPSSOInitPost", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSSOInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSSOInitPost");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform SP initiated slo with soap profile.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testSPSSOInitPost"})
    public void testSPSLOSOAP()
    throws Exception {
        entering("testSPSLOSOAP", null);
        try {
            log(Level.FINEST, "testSPSLOSOAP", "Running: testSPSLOSOAP");
            xmlfile = baseDir + "test5spslo.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "soap", false);
            log(Level.FINEST, "testSPSLOSOAP", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSLOSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSLOSOAP");
    }
    
    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform SP initiated termination with soap profile
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testSPSLOSOAP"})
    public void testSPTerminateSOAP()
    throws Exception {
        entering("testSPTerminateSOAP", null);
        try {
            log(Level.FINEST, "testSPTerminateSOAP", "Running: " +
                    "testSPTerminateSOAP");
            xmlfile = baseDir + "test6spterminate.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "soap");
            log(Level.FINEST, "testSPTerminateSOAP", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPTerminateSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPTerminateSOAP");
    }
    
    
    /**
     * Run saml2 profile testcase 1.
     * @DocTest: SAML2|Perform  idp initiated sso with post profile.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testSPTerminateSOAP"})
    public void testIDPSSOInitPost()
    throws Exception {
        entering("testIDPSSOInitPost", null);
        try {
            log(Level.FINEST, "testIDPSSOInitPost", "Running: testIDPSSOInitPost");
            getWebClient();
            xmlfile = baseDir + "test10idplogin.xml";
            SAMLv2Common.getxmlIDPLogin(xmlfile, configMap);
            log(Level.FINEST, "testIDPSSOInitPost", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            
            xmlfile = baseDir + "test10idpssoinit.xml";
            SAMLv2Common.getxmlIDPInitSSO(xmlfile, configMap, "post", false);
            log(Level.FINEST, "testIDPSSOInitPost", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOInitPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOInitPost");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform  idp initiated slo with soap profile .
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testIDPSSOInitPost"})
    public void testIDPSLOSOAP()
    throws Exception {
        entering("testIDPSLOSOAP", null);
        try {
            log(Level.FINEST, "testIDPSLOSOAP", "Running: testIDPSLOSOAP");
            
            xmlfile = baseDir + "test11idpslo.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "soap", false);
            log(Level.FINEST, "testIDPSLOSOAP", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSLOSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSLOSOAP");
    }
    
    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform  idp initiated termination with soap profile
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testIDPSLOSOAP"})
    public void testIDPTerminateSOAP()
    throws Exception {
        entering("testIDPTerminateSOAP", null);
        try {
            log(Level.FINEST, "testIDPTerminateSOAP",
                    "Running: testIDPTerminateSOAP");
            xmlfile = baseDir + "test12idpterminate.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "soap");
            log(Level.FINEST, "testIDPTerminateSOAP", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPTerminateSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPTerminateSOAP");
    }

        /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform SP initiated slo with POST profile.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testIDPTerminateSOAP"})
    public void testSPSLOPOST()
    throws Exception {
        entering("testSPSLOPOST", null);
        try {
            log(Level.FINEST, "testSPSLOPOST", "First try SP init SSO Post");
            testSPSSOInitPost();
            log(Level.FINEST, "testSPSLOPOST", "Now get xml for SP SLO POST");
            xmlfile = baseDir + "test13spslo.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "post", false);
            log(Level.FINEST, "testSPSLOPOST", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPSLOPOST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPSLOPOST");
    }

    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform SP initiated termination with POST profile
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testSPSLOPOST"})
    public void testSPTerminatePOST()
    throws Exception {
        entering("testSPTerminatePOST", null);
        try {
            log(Level.FINEST, "testSPTerminatePOST", "Running: " +
                    "testSPTerminatePOST");
            xmlfile = baseDir + "test14spterminate.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "post");
            log(Level.FINEST, "testSPTerminatePOST", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testSPTerminatePOST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testSPTerminatePOST");
    }

    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform  idp initiated slo with POST profile.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testSPTerminatePOST"})
    public void testIDPSLOPOST()
    throws Exception {
        entering("testIDPSLOPOST", null);
        try {
            log(Level.FINEST, "testIDPSLOPOST", "First try SP init SSO Post");
            testSPSSOInitPost();
            log(Level.FINEST, "testIDPSLOPOST", "Now get xml for IDP SLO POST");
            xmlfile = baseDir + "test15idpslo.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, "post", false);
            log(Level.FINEST, "testIDPSLOPOST", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSLOPOST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSLOPOST");
    }

    /**
     * Run saml2 termination
     * @DocTest: SAML2|Perform  idp initiated termination with post profile
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"},
    dependsOnMethods={"testIDPSLOPOST"})
    public void testIDPTerminatePOST()
    throws Exception {
        entering("testIDPTerminatePOST", null);
        try {
            log(Level.FINEST, "testIDPTerminatePOST",
                    "Running: testIDPTerminatePOST");
            xmlfile = baseDir + "test16idpterminate.xml";
            SAMLv2Common.getxmlSPTerminate(xmlfile, configMap, "post");
            log(Level.FINEST, "testIDPTerminatePOST", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPTerminatePOST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPTerminatePOST");
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
            String spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_SP_USER));
            log(Level.FINEST, "cleanup", "sp users to delete :" +
                    configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            consoleLogout(webClient, spurl + "/UI/Logout");
            
            // Create idp users
            String idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_IDP_USER));
            log(Level.FINEST, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            consoleLogout(webClient, idpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }    
}
