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
 * $Id: IDFFDefaultURLsTests.java,v 1.6 2009/01/27 00:04:01 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idff;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class tests IDFF Federation, SLO, SSO, Name registration & Termination 
 * 1. SP Initiated Federation
 * 2. SP Initiated SLO
 * 3. SP Initiated SSO 
 * 4. SP Initiated Name Registration
 * 5. SP Initiated Termination
 * 6. IDP Initiated SLO. As IDP init federation is not supported, 
 * SP init federation is performed first to follow IDP init SLO. 
 * 7. IDP Initiated Name registration. 
 * 8. IDP Initiated Termination
 */
public class IDFFDefaultURLsTests extends IDFFCommon {
    
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private DefaultTaskHandler task;
    private HtmlPage page;
    private Map<String, String> configMap;
    private String  baseDir;
    private String xmlfile;
    private String spurl;
    private String idpurl;
    private String spmetadata;
    private String idpmetadata;
    private String REGISTRATION_DONE_DEFAULT = "        <Attribute name" +
            "=\"registrationDoneURL\">\n" +
            "            <Value/>\n" + 
            "        </Attribute>";
    private String TERMINATION_DONE_DEFAULT = "        <Attribute name" +
            "=\"terminationDoneURL\">\n" +
            "            <Value/>\n" + 
            "        </Attribute>";
    private String LOGOUT_DONE_DEFAULT = "        <Attribute name" +
            "=\"logoutDoneURL\">\n" +
            "            <Value/>\n" + 
            "        </Attribute>";
    private String FEDERATION_DONE_DEFAULT = "        <Attribute name" +
            "=\"federationDoneURL\">\n" +
            "            <Value/>\n" + 
            "        </Attribute>";

    /**
     * Creates a new instance of IDFFDefaultURLsTests
     */
    public IDFFDefaultURLsTests() {
        super("IDFFDefaultURLsTests");
    }
    
    /**
     * Create the webClient 
     */
    private void getWebClient()
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage(), null);
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
        entering("setup", null);
        List<String> list;
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestData"));
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "IDFFDefaultURLsTests"));
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
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            
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
            log(Level.FINE, "setup", "SP user created is " + list);
            
            // Create idp users
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
            log(Level.FINE, "setup", "IDP user created is " + list);
            
            //get sp & idp extended metadata
            HtmlPage spmetaPage = fmSP.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "idff");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm command" +
                        " failed");
                assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            HtmlPage idpmetaPage = fmIDP.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    false, false, true, "idff");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm command" +
                        " failed");
                assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(idpmetaPage);
            defaultURLsSetup(webClient, spmetadata, idpmetadata, fmSP, fmIDP, 
                    configMap);
        } catch (Exception e) {
            cleanup();
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
     * Change SP & IDP ext metadata on SP & IDP side to set different default
     * URL's
     */
    public void defaultURLsSetup(WebClient webClient, String spmetadata, String 
            idpmetadata, FederationManager fmSP, FederationManager fmIDP, 
            Map configMap)
    throws Exception {
        try {
            String SP_FEDERATION_DONE_VALUE =  "        <Attribute name" +
                    "=\"federationDoneURL\">\n" +
                    "            <Value>" + configMap.get
                    ("sp_federationDoneURL") + "</Value>\n" + 
                    "        </Attribute>";
            String SP_LOGOUT_DONE_VALUE =  "        <Attribute name=" +
                    "\"logoutDoneURL\">\n" +
                    "            <Value>" + configMap.get("sp_logoutDoneURL") + 
                    "</Value>\n" + 
                    "        </Attribute>";
            String SP_REGISTRATION_DONE_VALUE =  "        <Attribute name=" +
                    "\"registrationDoneURL\">\n" +
                    "            <Value>" + configMap.get
                    ("sp_registrationDoneURL") + "</Value>\n" + 
                    "        </Attribute>";
            String SP_TERMINATION_DONE_VALUE =  "        <Attribute name=" +
                    "\"terminationDoneURL\">\n" +
                    "            <Value>" + configMap.get
                    ("sp_terminationDoneURL") + "</Value>\n" + 
                    "        </Attribute>";
            String IDP_LOGOUT_DONE_VALUE =  "        <Attribute name=" +
                    "\"logoutDoneURL\">\n" +
                    "            <Value>" + configMap.get("idp_logoutDoneURL") + 
                    "</Value>\n" + 
                    "        </Attribute>";
            String IDP_REGISTRATION_DONE_VALUE =  "        <Attribute name=" +
                    "\"registrationDoneURL\">\n" +
                    "            <Value>" + configMap.get
                    ("idp_registrationDoneURL") + "</Value>\n" + 
                    "        </Attribute>";
            String IDP_TERMINATION_DONE_VALUE =  "        <Attribute name=" +
                    "\"terminationDoneURL\">\n" +
                    "            <Value>" + configMap.get
                    ("idp_terminationDoneURL") + "</Value>\n" + 
                    "        </Attribute>";
            String spmetadataMod = 
                    spmetadata.replaceAll(FEDERATION_DONE_DEFAULT,
                    SP_FEDERATION_DONE_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(LOGOUT_DONE_DEFAULT,
                    SP_LOGOUT_DONE_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(REGISTRATION_DONE_DEFAULT,
                    SP_REGISTRATION_DONE_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(TERMINATION_DONE_DEFAULT,
                    SP_TERMINATION_DONE_VALUE);
            log(Level.FINEST, "defaultURLsSetup", "Modified SP metadata:" +
                    spmetadataMod);
            String idpmetadataMod = idpmetadata.replaceAll(LOGOUT_DONE_DEFAULT,
                    IDP_LOGOUT_DONE_VALUE);
            idpmetadataMod =
                    idpmetadataMod.replaceAll(REGISTRATION_DONE_DEFAULT,
                    IDP_REGISTRATION_DONE_VALUE);
            idpmetadataMod = idpmetadataMod.replaceAll(TERMINATION_DONE_DEFAULT,
                    IDP_TERMINATION_DONE_VALUE);
            log(Level.FINEST, "defaultURLsSetup", "Modified IDP metadata:" +
                    idpmetadataMod);
            
            assert (loadSPMetadata(null, spmetadataMod, fmSP, fmIDP,
                    configMap, webClient, true));
            assert (loadIDPMetadata(null, idpmetadataMod, fmSP, fmIDP,
                    configMap, webClient, true));
        } catch (Exception e) {
            log(Level.SEVERE, "defaultURLsSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("defaultURLsSetup");
    }    
    
    /**
     * @DocTest: IDFF|Perform SP initiated federation.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void DefaultURLSPInitFederation()
    throws Exception {
        entering("DefaultURLSPInitFederation", null);
        try {
            log(Level.FINE, "DefaultURLSPInitFederation", 
                    "Running: DefaultURLSPInitFederation");
            getWebClient();
            log(Level.FINE, "DefaultURLSPInitFederation", "Login to SP with " + 
                    configMap.get(TestConstants.KEY_SP_USER));
            consoleLogin(webClient, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_SP_USER),
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            xmlfile = baseDir + "DefaultURLSPInitFederation.xml";
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            log(Level.FINE, "DefaultURLSPInitFederation", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLSPInitFederation", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLSPInitFederation");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated SLO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLSPInitFederation"})
    public void DefaultURLSPInitSLO()
    throws Exception {
        entering("DefaultURLSPInitSLO", null);
        try {
            log(Level.FINE, "DefaultURLSPInitSLO",
                    "Running: DefaultURLSPInitSLO");
            xmlfile = baseDir + "DefaultURLSPInitSLO.xml";
            getxmlSPIDFFLogout(xmlfile, configMap);
            log(Level.FINE, "DefaultURLSPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLSPInitSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLSPInitSLO");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated SSO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLSPInitSLO"})
    public void DefaultURLSPInitSSO()
    throws Exception {
        entering("DefaultURLSPInitSSO", null);
        try {
            log(Level.FINE, "DefaultURLSPInitSSO", "Running: " +
                    "DefaultURLSPInitSSO");
            log(Level.FINE, "DefaultURLSPInitSSO", "Login to IDP with " + 
                    TestConstants.KEY_IDP_USER);
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_USER),
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            xmlfile = baseDir + "DefaultURLSPInitSSO.xml";
            getxmlSPIDFFSSO(xmlfile, configMap);
            log(Level.FINE, "DefaultURLSPInitSSO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLSPInitSSO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLSPInitSSO");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated Name registration.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLSPInitSSO"})
    public void DefaultURLSPInitNameReg()
    throws Exception {
        entering("DefaultURLSPInitNameReg", null);
        try {
            log(Level.FINE, "DefaultURLSPInitNameReg", "Running: " +
                    "DefaultURLSPInitNameReg");
            xmlfile = baseDir + "DefaultURLSPInitNameReg.xml";
            getxmlSPIDFFNameReg(xmlfile, configMap);
            log(Level.FINE, "DefaultURLSPInitNameReg", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLSPInitNameReg", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLSPInitNameReg");
    }
   
    /**
     * @DocTest: IDFF|Perform SP initiated Termination.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLSPInitNameReg"})
    public void DefaultURLSPInitTerminate()
    throws Exception {
        entering("DefaultURLSPInitTerminate", null);
        try {
            log(Level.FINE, "DefaultURLSPInitTerminate", 
                    "Running: DefaultURLSPInitTerminate");
            xmlfile = baseDir + "DefaultURLSPInitTerminate.xml";
            getxmlSPIDFFTerminate(xmlfile, configMap);
            log(Level.FINE, "DefaultURLSPInitTerminate", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLSPInitTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("DefaultURLSPInitTerminate");
    }

    /**
     * @DocTest: IDFF|Perform IDP initiated SLO.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLSPInitTerminate"})
    public void DefaultURLIDPInitSLO()
    throws Exception {
        entering("DefaultURLIDPInitSLO", null);
        try {
            log(Level.FINE, "DefaultURLIDPInitSLO", "Running: " +
                    "DefaultURLIDPInitSLO");
            xmlfile = baseDir + "DefaultURLIDPInitSLO_SSO.xml";
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_SP_USER),
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            log(Level.FINE, "DefaultURLIDPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
            xmlfile = baseDir + "DefaultURLIDPInitSLO.xml";
            getxmlIDPIDFFLogout(xmlfile, configMap);
            log(Level.FINE, "DefaultURLIDPInitSLO", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLIDPInitSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLIDPInitSLO");
    }
   
    /**
     * @DocTest: IDFF|Perform IDP initiated Name registration.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLIDPInitSLO"})
    public void DefaultURLIDPInitNameReg()
    throws Exception {
        entering("DefaultURLIDPInitNameReg", null);
        try {
            log(Level.FINE, "DefaultURLIDPInitNameReg", "Running: " +
                    "DefaultURLIDPInitNameReg");
            consoleLogin(webClient, idpurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_IDP_USER),
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            xmlfile = baseDir + "DefaultURLIDPInitNameReg.xml";
            getxmlIDPIDFFNameReg(xmlfile, configMap);
            log(Level.FINE, "DefaultURLIDPInitNameReg", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLIDPInitNameReg", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("DefaultURLIDPInitNameReg");
    }
   
    /**
     * @DocTest: IDFF|Perform IDP initiated Termination.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"DefaultURLIDPInitNameReg"})
    public void DefaultURLIDPInitTerminate()
    throws Exception {
        entering("DefaultURLIDPInitTerminate", null);
        try {
            log(Level.FINE, "DefaultURLIDPInitTerminate", "Running: " +
                    "DefaultURLIDPInitTerminate");
            xmlfile = baseDir + "DefaultURLIDPInitTerminate.xml";
            getxmlIDPIDFFTerminate(xmlfile, configMap);
            log(Level.FINE, "DefaultURLIDPInitTerminate", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "DefaultURLIDPInitTerminate", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("DefaultURLIDPInitTerminate");
    }

    /**
     * This methods deletes all the users & loads original SP & IDP metadata 
     * as part of cleanup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        try {
            log(Level.FINE, "cleanup", "Entering Cleanup");
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
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
            fmIDP = new FederationManager(idpurl);
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

            //Load default metadata back. 
            assert (loadSPMetadata(null, spmetadata, fmSP, fmIDP,
                    configMap, webClient, true));
            //Load default metadata back. 
            assert (loadIDPMetadata(null, idpmetadata, fmSP, fmIDP,
                    configMap, webClient, true));
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
