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
 * $Id: IDFFForceAuthNTests.java,v 1.7 2009/01/27 00:04:01 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idff;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This class tests the following:
 * First it sets the forceauthn attribute to true.
 * Then Tests sp init SSO. IDP session should be invalidated & asked for login
 * again
 */
public class IDFFForceAuthNTests extends IDFFCommon {
    
    public WebClient webClient;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    private String  baseDir;
    private HtmlPage page;
    private String spmetadata;
    private String spurl;
    private String idpurl;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private String FORCE_AUTHN_FALSE = "<Attribute name=\""
            + "forceAuthn" + "\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>\n";
    private  String FORCE_AUTHN_TRUE = "<Attribute name=\""
            + "forceAuthn" + "\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>\n";
    
    /** Creates a new instance of IDFFForceAuthNTests */
    public IDFFForceAuthNTests() {
        super("IDFFForceAuthNTests");
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
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestData"));
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
            
            fmSP = new FederationManager(spurl);
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("idff" + fileseparator +
                    "IDFFForceAuthNTests");
            log(Level.FINEST, "setup", "Users Map is " + usersMap);
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalUsers"));
            for (int i = 1; i < totalUsers + 1; i++) {
                //create sp user first
                log(Level.FINEST, "setup", "Value of i is " + i);
                list.clear();
                list.add("mail=" + usersMap.get(TestConstants.KEY_SP_USER_MAIL +
                        i));
                list.add("sn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_SP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINEST, "setup", "SP user to be created is " + list);
                if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                        configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_SP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed");
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
                log(Level.FINE, "setup", "IDP user to be created is " + list);
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
            forceAuthNSetup(webClient, spmetadata, fmSP, fmIDP, configMap);
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
     * Change SP ext metadata on SP & IDP side to set forceAuthn parameter to
     * true
     */
    public void forceAuthNSetup(WebClient webClient, String spmetadata,
            FederationManager fmSP, FederationManager fmIDP, Map configMap)
    throws Exception {
        try {
            String spmetadataMod = spmetadata.replaceAll(FORCE_AUTHN_FALSE,
                    FORCE_AUTHN_TRUE);
            log(Level.FINEST, "forceAuthNSetup", "Modified metadata:" +
                    spmetadataMod);
            
            assert (loadSPMetadata(null, spmetadataMod, fmSP, fmIDP,
                    configMap, webClient, true));
        } catch (Exception e) {
            log(Level.SEVERE, "forceAuthNSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("forceAuthNSetup");
    }
    
    /**
     * Run SP initiated federation with forceauthn set to true
     * @DocTest: IDFF | SP initiated federation with forceauthn set to true
     * TestCase ID: AccessManager_Liberty_50
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void IDFFForceAuthNtrueSPInit()
    throws Exception {
        entering("IDFFForceAuthNtrueSPInit", null);
        try {
            //Setup autofederation with default NameIDPolicy
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            
            //Now perform SSO
            String[] arrActions = {"idffForceAuthn_idplogin",
            "idffForceAuthn_splogin",
            "idffForceAuthn_ssoinit", "idffForceAuthn_reg",
            "idffForceAuthn_term"};
            String idpxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(idpxmlfile, configMap);
            String spxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPLogin(spxmlfile, configMap);
            String xmlfile = baseDir + arrActions[2] + ".xml";
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            String regxmlfile = baseDir + arrActions[3] + ".xml";
            getxmlSPIDFFNameReg(regxmlfile, configMap);
            String termxmlfile = baseDir + arrActions[4] + ".xml";
            getxmlSPIDFFTerminate(termxmlfile, configMap);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "IDFFForceAuthNtrueSPInit",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "IDFFForceAuthNtrueSPInit", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("IDFFForceAuthNtrueSPInit");
    }
    
    /**
     * This methods deletes all the users as part of cleanup
     * It also restores the original metadata.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            //get sp & idp extended metadata
            getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), spuserlist,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            
            assert (loadSPMetadata(null, spmetadata, fmSP, fmIDP,
                    configMap, webClient, true));
            
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
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
