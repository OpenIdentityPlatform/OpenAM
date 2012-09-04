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
 * $Id: IDFFNameIDPolicyTests.java,v 1.7 2009/01/27 00:04:01 nithyas Exp $
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
 * 1. It sets the nameIDPolicy attribute to none, onetime. 
 * 2. Tests cover sp init SSO with NameIDPolicy set to none 
 * 3. Tests cover sp init SSO with NameIDPolicy set to onetime 
 */
public class IDFFNameIDPolicyTests extends IDFFCommon {
    
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
    private String NAME_ID_POLICY_FEDERATED = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>federated</Value>\n" +
            "        </Attribute>";
    private String NAME_ID_POLICY_ONETIME = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>onetime</Value>\n" +
            "        </Attribute>";
    private String NAME_ID_POLICY_ANY = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>any</Value>\n" +
            "        </Attribute>";
    private String NAME_ID_POLICY_NONE = "        <Attribute name=" +
            "\"nameIDPolicy\">\n" +
            "            <Value>none</Value>\n" +
            "        </Attribute>";
    
    /** Creates a new instance of IDFFNameIDPolicyTests */
    public IDFFNameIDPolicyTests() {
        super("IDFFNameIDPolicyTests");
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
                    "IDFFNameIDPolicyTests");
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
                if (FederationManager.getExitCode(fmSP.createIdentity(
                        webClient, configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_SP_USER + i), "User",
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "exportEntity famadm command" +
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
                    log(Level.SEVERE, "setup", "exportEntity famadm command" +
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
     * Change IDFF ext metadata on SP & IDP side to configure nameIDPolicy
     */
    public void nameIDPolicySetup(WebClient webClient, String spmetadata, 
            FederationManager fmSP, FederationManager fmIDP, Map configMap, 
            String nameIDPolicy)
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
            String spmetadataMod = spmetadata;
            if (nameIDPolicy.equals("onetime")) {
                spmetadataMod = spmetadata.replaceAll(
                        NAME_ID_POLICY_FEDERATED,
                        NAME_ID_POLICY_ONETIME);
            } else if (nameIDPolicy.equals("none")) {
                spmetadataMod = spmetadata.replaceAll(
                        NAME_ID_POLICY_FEDERATED,
                        NAME_ID_POLICY_NONE);
            } else if (nameIDPolicy.equals("any")) {
                spmetadataMod = spmetadata.replaceAll(
                        NAME_ID_POLICY_FEDERATED,
                        NAME_ID_POLICY_ANY);
            }
            log(Level.FINEST, "nameIDPolicySetup: Modified metadata:",
                    spmetadataMod);

            assert (loadSPMetadata(null, spmetadataMod, fmSP, fmIDP, 
                    configMap, webClient, true));
        } catch (Exception e) {
            log(Level.SEVERE, "nameIDPolicySetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("nameIDPolicySetup");
    }
    
    /**
     * Run SP initiated SSO with NameIDPolicy set to none
     * @DocTest: IDFF| SP initiated SSO with NameIDPolicy set to none.
     * Testcase ID: AccessManager_Liberty_47
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void nameIDPolicyNone()
    throws Exception {
        entering("nameIDPolicyNone", null);
        try {
            configMap.put(TestConstants.KEY_SP_USER,
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER,
                    usersMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD,
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            //Now perform SSO
            String[] arrActions = {"nameIDPolicyNone_splogin", 
                    "nameIDPolicyNone_ssoinit", 
                    "nameIDPolicyNone_slo"};
            String spxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPLogin(spxmlfile, configMap);
            String fedxmlfile = baseDir + arrActions[1] + ".xml";
            getxmlSPIDFFFederate(fedxmlfile, configMap, true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            getxmlSPIDFFLogout(sloxmlfile, configMap);
            log(Level.FINE, "nameIDPolicyNone", "First establish the " +
                    "federation & then set the NameIDPolicy to none");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "nameIDPolicyNone",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
            
            log(Level.FINE, "nameIDPolicyNone", "Set the NameIDPolicy to none");
            nameIDPolicySetup(webClient, spmetadata, fmSP, fmIDP, 
                    configMap, "none");
            String[] arrActionsNone = {"nameIDPolicyNone_idplogin", 
                    "nameIDPolicyNone_sso", "nameIDPolicyNone_reg", 
                    "nameIDPolicyNone_slo", "nameIDPolicyNone_idplogin",
                    "nameIDPolicyNone_sso", 
                    "nameIDPolicyNone_term"};
            String idpxmlfile = baseDir + arrActionsNone[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(idpxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActionsNone[1] + ".xml";
            getxmlSPIDFFSSO(ssoxmlfile, configMap);
            String regxmlfile = baseDir + arrActionsNone[2] + ".xml";
            getxmlSPIDFFNameReg(regxmlfile, configMap);
            String termxmlfile = baseDir + arrActionsNone[6] + ".xml";
            getxmlSPIDFFTerminate(termxmlfile, configMap);
            for (int i = 0; i < arrActionsNone.length; i++) {
                log(Level.FINE, "nameIDPolicyNone",
                        "Executing xml: " + arrActionsNone[i]);
                task = new DefaultTaskHandler(baseDir + arrActionsNone[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "nameIDPolicyNone", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("nameIDPolicyNone");
    }

    /**
     * Run SP initiated federation with NameIDPolicy set to onetime
     * @DocTest: IDFF| SP initiated federation with NameIDPolicy set to 
     * onetime.
     * Testcase ID: AccessManager_Liberty_46
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"}, 
    dependsOnMethods={"nameIDPolicyNone"})
    public void nameIDPolicyOneTime()
    throws Exception {
        entering("nameIDPolicyOneTime", null);
        try {
            nameIDPolicySetup(webClient, spmetadata, fmSP, fmIDP, 
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
            String[] arrActions = {"nameIDPolicyOneTime_splogin", 
                    "nameIDPolicyOneTime_ssoinit", 
                    "nameIDPolicyOneTime_slo", "nameIDPolicyOneTime_splogin", 
                    "nameIDPolicyOneTime_ssoinit", 
                    "nameIDPolicyOneTime_slo"};
            String xmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPLogin(xmlfile, configMap);
            xmlfile = baseDir + arrActions[1] + ".xml";
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            getxmlSPIDFFLogout(sloxmlfile, configMap);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "nameIDPolicyOneTime",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "nameIDPolicyOneTime", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("nameIDPolicyOneTime");
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
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm" +
                        " command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            log(Level.FINEST, "cleanup", "Logging out of SP & IDP");
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("cleanup");
    }
}
