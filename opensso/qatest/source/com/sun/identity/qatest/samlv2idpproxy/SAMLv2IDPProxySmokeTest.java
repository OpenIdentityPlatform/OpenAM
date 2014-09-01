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
 * $Id: SAMLv2IDPProxySmokeTest.java,v 1.5 2009/01/27 00:15:33 nithyas Exp $
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
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests IDP Proxy scenario
 */
public class SAMLv2IDPProxySmokeTest extends TestCommon {
    
    public WebClient webClient;
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task1;
    private HtmlPage page1;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private FederationManager fmIDPProxy;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    ArrayList spuserlist = new ArrayList ();
    ArrayList idpuserlist = new ArrayList ();
    ArrayList idpproxyuserlist = new ArrayList ();
    String ssoProfile = "";
    String sloProfile = "";
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2IDPProxySmokeTest() {
        super("SAMLv2IDPProxySmokeTest");
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
        try {
            ssoProfile = strSSOProfile;
            sloProfile = strSLOProfile;
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
            SAMLv2Common.getEntriesFromResourceBundle("samlv2idpproxy" +
                    fileseparator + "samlv2IDPProxySmokeTest", configMap);
    
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
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        try {
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            consoleLogin(webClient, idpproxyurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));

            fmIDPProxy = new FederationManager(idpproxyurl);

            Integer totalUsers = new Integer(
                    (String)configMap.get("totalUsers"));
            for (int i = 1; i < totalUsers + 1; i++) {
                list = new ArrayList();
                list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER + i));
                list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER + i));
                list.add("userpassword=" +
                        configMap.get(TestConstants.KEY_SP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        configMap.get(TestConstants.KEY_SP_USER + i), "User", 
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed while creating users at SP");
                    assert false;
                }
                spuserlist.add(configMap.get(TestConstants.KEY_SP_USER + i));

                // Create idp users
                list.clear();
                list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER + i));
                list.add("userpassword=" +
                        configMap.get(TestConstants.KEY_IDP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                if (FederationManager.getExitCode(fmIDP.createIdentity(webClient,
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                        configMap.get(TestConstants.KEY_IDP_USER + i), "User", 
                        list))
                        != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command " +
                            "failed while creating users at IDP");
                    assert false;
                }
                idpuserlist.add(configMap.get(TestConstants.KEY_IDP_USER + i));

                // Create idp proxy users
                list.clear();
                list.add("sn=" + configMap.get(TestConstants.KEY_IDP_PROXY_USER 
                        + i));
                list.add("cn=" + configMap.get(TestConstants.KEY_IDP_PROXY_USER 
                        + i));
                list.add("userpassword=" +
                        configMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD 
                        + i));
                list.add("inetuserstatus=Active");
                if (FederationManager.getExitCode(fmIDPProxy.createIdentity(
                        webClient, configMap.get(TestConstants.
                        KEY_IDP_PROXY_EXECUTION_REALM), configMap.get(
                        TestConstants.KEY_IDP_PROXY_USER + i), "User", list)) 
                        != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command " +
                            "failed while creating users at IDP Proxy");
                    assert false;
                }
                idpproxyuserlist.add(configMap.get(TestConstants.
                        KEY_IDP_PROXY_USER + i));
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
     * Run saml2 SSO in IDP Proxy scenario.
     * @DocTest: SAML2|Perform SP initiated sso.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void SPInitSSO()
    throws Exception {
        entering("SPInitSSO", null);
        try {
            Reporter.log("Test Description: This test tests SP init SSO with " +
                    ssoProfile + " in samlv2 IDP Proxy scenario");
            configMap.put(TestConstants.KEY_SP_USER,
                    configMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER,
                    configMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER,
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 1));

            log(Level.FINEST, "SPInitSSO", "Running: SPInitSSO");
            getWebClient();
            xmlfile = baseDir + "SAMLv2IDPProxySPInitSSO.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, ssoProfile, false, 
                    true);
            log(Level.FINEST, "SPInitSSO", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPInitSSO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPInitSSO");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform SP initiated slo.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPInitSSO"})
    public void SPInitSLO()
    throws Exception {
        entering("SPInitSLO", null);
        try {
            Reporter.log("Test Description: This test tests SP init SLO with " +
                    sloProfile + "in samlv2 IDP Proxy scenario");
            log(Level.FINEST, "SPInitSLO", "Running: SPInitSLO");
            xmlfile = baseDir + "SAMLv2IDPProxySPInitSLO.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, sloProfile, true);
            log(Level.FINEST, "SPInitSLO", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPInitSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPInitSLO");
    }
        
    /**
     * Run saml2 transient SSO in IDP Proxy scenario.
     * @DocTest: SAML2|Perform SP initiated sso with transient federation.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods={"SPInitSLO"})
    public void SPInitSSOTransient()
    throws Exception {
        entering("SPInitSSOTransient", null);
        try {
            Reporter.log("Test Description: This test tests SP init SSO " +
                    ssoProfile + " with transient federation in samlv2 IDP " +
                    "Proxy Scenario");
            getWebClient();
            configMap.put(TestConstants.KEY_SP_USER,
                    configMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER,
                    configMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER,
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER + 1));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD,
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD + 1));
            configMap.put("urlparams", "NameIDFormat=transient");

            log(Level.FINEST, "SPInitSSOTransient", "Running: SPInitSSOTransient");
            getWebClient();
            xmlfile = baseDir + "SAMLv2IDPProxySPInitSSOTransient.xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, ssoProfile, false, 
                    true);
            log(Level.FINEST, "SPInitSSOTransient", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPInitSSOTransient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPInitSSOTransient");
    }
    
    /**
     * Run saml2 slo
     * @DocTest: SAML2|Perform SP initiated slo.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"SPInitSSOTransient"})
    public void SPInitSLOTransient()
    throws Exception {
        entering("SPInitSLOTransient", null);
        try {
            Reporter.log("Test Description: This test tests SP init SLO with " +
                    sloProfile + " with transient federation in samlv2 IDP " +
                    "Proxy Scenario");
            log(Level.FINEST, "SPInitSLOTransient", "Running: SPInitSLOTransient");
            xmlfile = baseDir + "SAMLv2IDPProxySPInitSLOTransient.xml";
            SAMLv2Common.getxmlSPSLO(xmlfile, configMap, sloProfile, true);
            log(Level.FINEST, "SPInitSLOTransient", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "SPInitSLOTransient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SPInitSLOTransient");
    }

    /**
     * Cleanup methods deletes all the users which were created in setup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            getWebClient();
            // delete sp users
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            log(Level.FINEST, "cleanup", "sp users to delete :" +
                    configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    spuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed at SP");
                assert false;
            }
            
            // Delete idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            log(Level.FINEST, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed at IDP");
                assert false;
            }

            // Delete idp proxy users
            consoleLogin(webClient, idpproxyurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            log(Level.FINEST, "cleanup", "idp proxy users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_PROXY_USER));
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
