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
 * $Id: IDFFAutoFederationTests.java,v 1.8 2009/01/27 00:04:01 nithyas Exp $
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
import java.net.URL;
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
 * 1. First it sets the autofederation attribute to true. 
 * Sets the autofederation attribute & attribute map in the sp & idp extended 
 * metadata. It creates the users with same mail address. 
 * 2. Tests cover sp init SSO with NameIDPolicy set to federated 
 * 3. Tests cover sp init SSO with NameIDPolicy set to onetime 
 */
public class IDFFAutoFederationTests extends IDFFCommon {
    
    public WebClient webClient;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    private String  baseDir;
    private HtmlPage page;
    private URL url;
    private String spmetadata;
    private String idpmetadata;
    private String spurl;
    private String idpurl;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private String AUTO_FED_ENABLED_FALSE = "<Attribute name=\""
            + "enableAutoFederation" + "\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>\n";
    private  String AUTO_FED_ENABLED_TRUE = "<Attribute name=\""
            + "enableAutoFederation" + "\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>\n";
    
    private String AUTO_FED_ATTRIB_DEFAULT = "<Attribute name=\""
            + "autoFederationAttribute\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    private String AUTO_FED_ATTRIB_VALUE = "<Attribute name=\""
            + "autoFederationAttribute\">\n"
            + "            <Value>mail</Value>\n"
            + "        </Attribute>";
    private String IDP_ATTRIB_MAP_DEFAULT = "<Attribute name=\""
            + "idpAttributeMap\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    private String IDP_ATTRIB_MAP_VALUE = "<Attribute name=\""
            + "idpAttributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";
    private String SP_ATTRIB_MAP_DEFAULT = "<Attribute name=\""
            + "spAttributeMap\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    private String SP_ATTRIB_MAP_VALUE = "<Attribute name=\""
            + "spAttributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";
    private String NAME_ID_POLICY_FEDERATED = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>federated</Value>\n" +
            "        </Attribute>";
    private String NAME_ID_POLICY_ONETIME = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>onetime</Value>\n" +
            "        </Attribute>";
    
    /** Creates a new instance of IDFFAutoFederationTests */
    public IDFFAutoFederationTests() {
        super("IDFFAutoFederationTests");
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
                    "idffAutoFederationTests");
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
            HtmlPage idpmetaPage = fmIDP.exportEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false, 
                    false, true, "idff");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm command" +
                        " failed");
                assert false;
            }
            idpmetadata = MultiProtocolCommon.getExtMetadataFromPage(
                    idpmetaPage);
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
     * Change IDFF ext metadata on SP & IDP side to configure autofederation
     * based on mail attribute
     */
    public void autoFedSetup(WebClient webClient, String spmetadata, String 
            idpmetadata, FederationManager fmSP, FederationManager fmIDP, Map 
            configMap, String nameIDPolicy)
    throws Exception {
        try {
            consoleLogin(webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(TestConstants.
                    KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(TestConstants.
                    KEY_IDP_AMADMIN_PASSWORD));
            String spmetadataMod = spmetadata.replaceAll(AUTO_FED_ENABLED_FALSE,
                    AUTO_FED_ENABLED_TRUE);
            spmetadataMod = spmetadataMod.replaceAll(AUTO_FED_ATTRIB_DEFAULT,
                    AUTO_FED_ATTRIB_VALUE);
            spmetadataMod = spmetadataMod.replaceAll(SP_ATTRIB_MAP_DEFAULT,
                    SP_ATTRIB_MAP_VALUE);
            if (nameIDPolicy.equals("onetime")) {
                spmetadataMod = spmetadataMod.replaceAll(
                        NAME_ID_POLICY_FEDERATED,
                        NAME_ID_POLICY_ONETIME);
            } 
            log(Level.FINEST, "autoFedSetup: Modified metadata:",
                    spmetadataMod);

            assert (loadSPMetadata(null, spmetadataMod, fmSP, fmIDP, 
                    configMap, webClient, true));
             
            String idpmetadataMod = idpmetadata.replaceAll(
                    AUTO_FED_ENABLED_FALSE, AUTO_FED_ENABLED_TRUE);
            idpmetadataMod = idpmetadataMod.replaceAll(
                    AUTO_FED_ATTRIB_DEFAULT, AUTO_FED_ATTRIB_VALUE);
            idpmetadataMod = idpmetadataMod.replaceAll(IDP_ATTRIB_MAP_DEFAULT,
                    IDP_ATTRIB_MAP_VALUE);
            log(Level.FINEST, "autoFedSetup: Modified metadata:",
                    idpmetadataMod);

            assert (loadIDPMetadata(null, idpmetadataMod, fmSP, fmIDP, 
                    configMap, webClient, true));
        } catch (Exception e) {
            log(Level.SEVERE, "autoFedSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("autoFedSetup");
    }
    
    /**
     * Run SP initiated auto federation with NameIDPolicy set to federated
     * @DocTest: IDFF | SP initiated Autofederation with NameIDPolicy set to 
     * federated.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void IDFFautoFedSPInitFederated()
    throws Exception {
        entering("IDFFautoFedSPInitFederated", null);
        try {
            //Setup autofederation with default NameIDPolicy
            autoFedSetup(webClient, spmetadata, idpmetadata, fmSP, fmIDP, 
                    configMap, "federated");
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, usersMap.
                    get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));

            //Now perform SSO
            String[] arrActions = {"idffautofedidplogin", 
                    "idffautofedspinit_ssoinit", 
                    "idffautofedspinit_slo", "idffautofedidplogin",
                    "idffautofedspinit_ssoinit",
                    "idffautofedspinit_reg", "idffautofedspinit_term"};
            String idpxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(idpxmlfile, configMap);
            String xmlfile = baseDir + arrActions[1] + ".xml";
            getxmlSPIDFFFederate(xmlfile, configMap, false);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            getxmlSPIDFFLogout(sloxmlfile, configMap);
            String regxmlfile = baseDir + arrActions[5] + ".xml";
            getxmlSPIDFFNameReg(regxmlfile, configMap);
            String termxmlfile = baseDir + arrActions[6] + ".xml";
            getxmlSPIDFFTerminate(termxmlfile, configMap);

            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "IDFFautoFedSPInitFederated",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "IDFFautoFedSPInitFederated", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("IDFFautoFedSPInitFederated");
    }
    
    /**
     * Run SP initiated auto federation with NameIDPolicy set to onetime
     * @DocTest: SAML2| IDP initiated Autofederation with NameIDPolicy set to 
     * onetime.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void IDFFautoFedSPInitOneTime()
    throws Exception {
        entering("IDFFautoFedSPInitOneTime", null);
        try {
            autoFedSetup(webClient, spmetadata, idpmetadata, fmSP, fmIDP, 
                    configMap, "onetime");
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 2));
            //Now perform SSO
            String[] arrActions = {"idffautofedidplogin", 
                    "idffautofedspinitonetime_ssoinit", 
                    "idffautofedspinitonetime_reg",
                    "idffautofedspinitonetime_slo", "idffautofedidplogin", 
                    "idffautofedspinitonetime_ssoinit", 
                    "idffautofedspinitonetime_slo"};
            String xmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(xmlfile, configMap);
            xmlfile = baseDir + arrActions[1] + ".xml";
            getxmlSPIDFFFederate(xmlfile, configMap, false);
            String regxmlfile = baseDir + arrActions[2] + ".xml";
            getxmlSPIDFFNameReg(regxmlfile, configMap);
            String sloxmlfile = baseDir + arrActions[3] + ".xml";
            getxmlSPIDFFLogout(sloxmlfile, configMap);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "IDFFautoFedSPInitOneTime",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "IDFFautoFedSPInitOneTime", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("IDFFautoFedSPInitOneTime");
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
                    configMap.get(
                    TestConstants.KEY_IDP_EXECUTION_REALM),
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
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
