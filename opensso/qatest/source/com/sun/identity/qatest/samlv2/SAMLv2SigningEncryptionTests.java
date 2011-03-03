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
 * $Id: SAMLv2SigningEncryptionTests.java,v 1.9 2009/01/27 00:14:09 nithyas Exp $
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests various samlv2 scenarios along with signing & encryption
 * turned on. Metadata is changed before the tests are run & it is reverted back
 * to original after the tests are run.  
 * 1. SP Initiated SSO, SLO, Termination
 * 2. SP Initiated SSO, SLO, Termination with POST/SOAP profile
 * 3. IDP Initiated SSO, SLO, Termination
 * 4. IDP Initiated SSO, SLO, Termination with POST/SOAP profile
 * This class covers Testcases from SAMLv2_usecase_10_1 till SAMLv2_usecase_26_4
 */
public class SAMLv2SigningEncryptionTests extends TestCommon {
    
    String KEY_WANT_AUTHN_REQ_SIGNED_FALSE = 
            "WantAuthnRequestsSigned=\"false\"";
    String KEY_WANT_AUTHN_REQ_SIGNED_TRUE = "WantAuthnRequestsSigned=\"true\"";
    String KEY_AUTHN_REQ_SIGNED_FALSE = "AuthnRequestsSigned=\"false\"";
    String KEY_AUTHN_REQ_SIGNED_TRUE = "AuthnRequestsSigned=\"true\"";
    String KEY_WANT_ASSERTION_SIGNED_FALSE = "WantAssertionsSigned=\"false\"";
    String KEY_WANT_ASSERTION_SIGNED_TRUE = "WantAssertionsSigned=\"true\"";
    String KEY_WANT_NAMEID_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantNameIDEncrypted\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_NAMEID_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantNameIDEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESOLVE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantArtifactResolveSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESOLVE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantArtifactResolveSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantLogoutRequestSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE = "<Attribute name=" +
            "\"wantLogoutRequestSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantLogoutResponseSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantLogoutResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantMNIRequestSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_REQUEST_SIGNED_TRUE = "<Attribute name=" +
            "\"wantMNIRequestSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantMNIResponseSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantMNIResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ATTRIBUTE_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantAttributeEncrypted\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_ATTRIBUTE_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantAttributeEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ASSERTION_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantAssertionEncrypted\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_ASSERTION_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantAssertionEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantArtifactResponseSigned\">\n" +
            "            <Value></Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantArtifactResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String ATTRIB_MAP_DEFAULT = "<Attribute name=\"attributeMap\"/>\n";
    String ATTRIB_MAP_VALUE = "<Attribute name=\"attributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    private String baseDir;
    private String spurl;
    private String idpurl;
    private String spMetadata[]= {"",""};
    private String idpMetadata[]= {"",""};
    private HtmlPage page;
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2SigningEncryptionTests() {
        super("SAMLv2SigningEncryptionTests");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    @BeforeMethod(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
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
     * It also changes the metadata attribute based on the passed parameter
     */
    @Parameters({"attribute", "metadata"})
    @BeforeClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void setup(String strAttribute, String strMetadata)
    throws Exception {
        Object[] params = {strAttribute, strMetadata};
        entering("setup", params);
        Reporter.log("setup parameters: " + params);
        List<String> list;
        try {
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData");
            configMap.putAll(getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData"));
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2" + fileseparator +
                    "samlv2SigningEncryptionTests");
            log(Level.FINEST, "setup", "Users map is " + usersMap);
            Integer totalUsers = new Integer(
                    (String)usersMap.get("totalUsers"));
            for (int i = 1; i < totalUsers + 1; i++) {
                //create sp user
                list.clear();
                list.add("sn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("cn=" + usersMap.get(TestConstants.KEY_SP_USER + i));
                list.add("userpassword=" + usersMap.get(
                        TestConstants.KEY_SP_USER_PASSWORD + i));
                list.add("inetuserstatus=Active");
                log(Level.FINE, "setup", "SP user to be created is " + list);
                if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                        configMap.get(
                        TestConstants.KEY_SP_EXECUTION_REALM),
                        usersMap.get(TestConstants.KEY_SP_USER + i), "User", 
                        list)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm command" +
                            " failed at SP");
//                    assert false;
                }
                spuserlist.add(usersMap.get(TestConstants.KEY_SP_USER + i));
                list.clear();

                //create idp user
                list.clear();
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
                            " failed at IDP");
//                    assert false;
                }
                idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER + i));
                list.clear();
            }
            Thread.sleep(10000);
            //Change the metadata based on the parameters received. 
            changeMetadata(strAttribute, strMetadata, fmSP, fmIDP);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } finally {
            log(Level.FINE, "setup", "Logging out of sp & idp");
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * Run SP init SSO, SLO, Termination
     * @DocTest: SAML2|Perform SP initiated SSO, SLO & Termination.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SignEncryptSPSSOSLOTermArt()
    throws Exception {
        entering("SignEncryptSPSSOSLOTermArt", null);
        try {
            log(Level.FINE, "SignEncryptSPSSOSLOTermArt", "\nRunning: " +
                    "SignEncryptSPSSOSLOTermArt\n");
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 1));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 1));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 1));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 1));
            
            log(Level.FINEST, "SignEncryptSPSSOSLOTermArt", "Map:" + configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"SignEncryptSPSSOSLOTermArt_ssoinit", 
            "SignEncryptSPSSOSLOTermArt_slo",
            "SignEncryptSPSSOSLOTermArt_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "artifact",
                    false, false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "http", false);
            String terminatexmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(terminatexmlfile, configMap, "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SignEncryptSPSSOSLOTermArt",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "SignEncryptSPSSOSLOTermArt", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SignEncryptSPSSOSLOTermArt");
    }
    
    /**
     *
     * Run IDP initiated SSO, SLO & Termination
     * @DocTest: SAML2|Perform IDP initiated SSO, SLO & Termination.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SignEncryptIDPSSOSLOTermArt()
    throws Exception {
        entering("SignEncryptIDPSSOSLOTermArt", null);
        try {
            log(Level.FINE, "SignEncryptIDPSSOSLOTermArt", "\nRunning: " +
                    "SignEncryptIDPSSOSLOTermArt\n");
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 2));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 2));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 2));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 2));
            
            log(Level.FINEST, "SignEncryptIDPSSOSLOTermArt", "Map:" +
                    configMap);
            
            String[] arrActions = {"SignEncryptIDPSSOSLOTermArt_idplogin", 
            "SignEncryptIDPSSOSLOTermArt_idpsamlv2ssoinit",
            "SignEncryptIDPSSOSLOTermArt_slo", 
            "SignEncryptIDPSSOSLOTermArt_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "artifact",
                    false);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "http");
            String terminatexmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(terminatexmlfile, configMap,
                    "http");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SignEncryptIDPSSOSLOTermArt",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "SignEncryptIDPSSOSLOTermArt", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SignEncryptIDPSSOSLOTermArt");
    }
    
    /**
     * Run Perform SP init SSO, SLO, Term with post/soap binding
     * @DocTest: SAML2|Perform SP init SSO, SLO, Term with post/soap binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SignEncryptSPSSOSLOTermPost()
    throws Exception {
        entering("SignEncryptSPSSOSLOTermPost", null);
        try {
            log(Level.FINE, "SignEncryptSPSSOSLOTermPost", 
                    "\nRunning: SignEncryptSPSSOSLOTermPost\n");
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 3));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 3));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 3));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 3));
            log(Level.FINEST, "SignEncryptSPSSOSLOTermPost", "Map:" + 
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"SignEncryptSPSSOSLOTermPost_ssoinit", 
            "SignEncryptSPSSOSLOTermPost_slo",
            "SignEncryptSPSSOSLOTermPost_terminate"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, "post", false,
                    false);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, "soap", false);
            String terminatexmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlSPTerminate(terminatexmlfile, configMap, "soap");
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SignEncryptSPSSOSLOTermPost",
                        "Inside for loop. value of i is " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "SignEncryptSPSSOSLOTermPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SignEncryptSPSSOSLOTermPost");
    }
    
    /**
     * Run IDP Init SSO, SLO, Term with Post/SOAP binding
     * @DocTest: SAML2|IDP Init SSO, SLO, Term with Post/SOAP binding.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void SignEncryptIDPSSOSLOTermPost()
    throws Exception {
        entering("SignEncryptIDPSSOSLOTermPost", null);
        try {
            log(Level.FINE, "SignEncryptIDPSSOSLOTermPost", "\nRunning: " +
                    "SignEncryptIDPSSOSLOTermPost\n");
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER + 4));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD + 4));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER + 4));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD + 4));
            
            log(Level.FINEST, "SignEncryptIDPSSOSLOTermPost", "Map:" + 
                    configMap);
            
            String[] arrActions = {"SignEncryptIDPSSOSLOTermPost_idplogin", 
            "sSignEncryptIDPSSOSLOTermPost_ssoinit",
            "SignEncryptIDPSSOSLOTermPost_slo", 
            "SignEncryptIDPSSOSLOTermPost_terminate"};
            String loginxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlIDPLogin(loginxmlfile, configMap);
            String ssoxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlIDPInitSSO(ssoxmlfile, configMap, "post", false);
            String sloxmlfile = baseDir + arrActions[2] + ".xml";
            SAMLv2Common.getxmlIDPSLO(sloxmlfile, configMap, "soap");
            String terminatexmlfile = baseDir + arrActions[3] + ".xml";
            SAMLv2Common.getxmlIDPTerminate(terminatexmlfile, configMap,
                    "soap");
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SignEncryptIDPSSOSLOTermPost",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "SignEncryptIDPSSOSLOTermPost", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SignEncryptIDPSSOSLOTermPost");
    }
    
    /**
     * This methods deletes all the users as part of cleanup
     */
    @AfterClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        SAMLv2Common samlv2Common;
        try {
            log(Level.FINE, "Cleanup", "Entering Cleanup: ");
            getWebClient();
            
            // delete sp users
            log(Level.FINE, "cleanup", "sp users to delete : " + spuserlist);
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), spuserlist,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }
            
            // Create idp users
            log(Level.FINE, "cleanup", "idp users to delete : " + idpuserlist);
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), idpuserlist,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }

            if ((spMetadata[0].equals("")) || (spMetadata[1].equals(""))) {
                log(Level.FINEST, "cleanup", "Default SP metadata empty so " +
                        "dont perform any action");
            } else {
                log(Level.FINE, "cleanup", "Now Load Default SP metadata ");
                samlv2Common = new SAMLv2Common();
                samlv2Common.loadSPMetadata(spMetadata[0], spMetadata[1], fmSP, 
                        fmIDP, configMap, webClient);
            }
            if ((idpMetadata[0].equals("")) || (idpMetadata[1].equals(""))) {
                log(Level.FINEST, "cleanup", "Default IDP metadata empty so " +
                        "dont perform any action");
            } else {
                log(Level.FINE, "cleanup", "Now Load Default IDP metadata ");
                samlv2Common = new SAMLv2Common();
                samlv2Common.loadIDPMetadata(idpMetadata[0], idpMetadata[1], 
                        fmSP, fmIDP, configMap, webClient);
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
    
    /**
     * This method changes the metadata based on the attribute & metadata type 
     * (sp, idp, both) received. 
     */
    private void changeMetadata(String strAttribute, String strMetadata,
            FederationManager fmsp, FederationManager fmidp)
    throws Exception {
        String spMetadataMod[] = {"",""};
        String idpMetadataMod[] = {"",""};
        SAMLv2Common samlv2Common;
        try {
            log(Level.FINE, "changeMetadata", "Entering changeMetadata ");
            //export metadata
            if (strMetadata.contains("sp") || strMetadata.contains("both")) {
                HtmlPage spExportEntityPage = fmSP.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                        false, true, true, "saml2");
                if (FederationManager.getExitCode(spExportEntityPage) != 0) {
                   log(Level.SEVERE, "changeMetadata", "exportEntity famadm" +
                           " command failed");
                   assert false;
                }
                log (Level.FINEST, "changeMetadata", "Export SP metadata " +
                        "page is: " + spExportEntityPage.getWebResponse().
                        getContentAsString());
                spMetadata[0] = SAMLv2Common.getMetadataFromPage(
                        spExportEntityPage);
                spMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                        spExportEntityPage);
                log (Level.FINEST, "changeMetadata", "Export SP metadata " +
                        "is: " + spMetadata[0]);
                log (Level.FINEST, "changeMetadata", "Export SP Ext metadata " +
                        "is: " + spMetadata[1]);
                if (!(spMetadata[0].equals(null)) & (spMetadata[0]!="") & 
                        !(spMetadata[1].equals(null)) & (spMetadata[1]!="")) {
                    log (Level.FINER, "changeMetadata", "Successfully " +
                            "imported SP metadatada");
                } else {
                    log (Level.SEVERE, "changeMetadata", "Export SP metadata " +
                            "is empty. ");
                    assert false;
                }
            }
            if (strMetadata.contains("idp") || strMetadata.contains("both")) {
                HtmlPage idpExportEntityPage = fmIDP.exportEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                        false, true, true, "saml2");
                if (FederationManager.getExitCode(idpExportEntityPage) != 0) {
                   log(Level.SEVERE, "changeMetadata", "exportEntity famadm" +
                           " command failed");
                   assert false;
                }
                log (Level.FINEST, "changeMetadata", "Export IDP metadata" +
                        "is: " + idpExportEntityPage.getWebResponse().
                        getContentAsString());
                idpMetadata[0] = SAMLv2Common.getMetadataFromPage(
                        idpExportEntityPage);
                idpMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                        idpExportEntityPage);
                log (Level.FINEST, "changeMetadata", "Export IDP metadata" +
                        "is: " + idpMetadata[0]);
                log (Level.FINEST, "changeMetadata", "Export IDP Ext metadata" +
                        "is: " + idpMetadata[1]);
                if (!(idpMetadata[0].equals(null)) & (idpMetadata[0]!="") & 
                        !(idpMetadata[1].equals(null)) & (idpMetadata[1]!="")) {
                    log (Level.FINER, "changeMetadata", "Successfully " +
                            "imported IDP metadatada");
                } else {
                    log (Level.SEVERE, "changeMetadata", "Export IDP metadata" +
                            "is empty. ");
                    assert false;
                }
            }
            
            //Set the default values to Modified data. 
            spMetadataMod[0] = spMetadata[0];
            spMetadataMod[1] = spMetadata[1];
            idpMetadataMod[0] = idpMetadata[0];
            idpMetadataMod[1] = idpMetadata[1];
            
            if (strAttribute.equals("WantAuthnRequestsSigned")) {
                idpMetadataMod[0] = idpMetadata[0].replaceAll(
                        KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                        KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                if (strMetadata.contains("both")) {
                    spMetadataMod[0] = spMetadata[0].replaceAll(
                            KEY_AUTHN_REQ_SIGNED_FALSE,
                            KEY_AUTHN_REQ_SIGNED_TRUE);
                }
            }
            if (strAttribute.equals("WantAuthnRequestsSigned")) {
                spMetadataMod[0] = spMetadata[0].replaceAll(
                        KEY_AUTHN_REQ_SIGNED_FALSE,
                        KEY_AUTHN_REQ_SIGNED_TRUE);
                if (strMetadata.contains("both")) {
                    idpMetadataMod[0] = idpMetadata[0].replaceAll(
                            KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                            KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                }
            }
            if (strAttribute.equals("WantAssertionsSigned")) {
                spMetadataMod[0] = spMetadata[0].replaceAll(
                        KEY_WANT_ASSERTION_SIGNED_FALSE,
                        KEY_WANT_ASSERTION_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("both")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("both")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("both")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("both")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
            }
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
            }
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("both")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
            }
            
            if (strAttribute.equals("wantArtifactResolveSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_ARTIFACT_RESOLVE_SIGNED_DEFAULT,
                        KEY_WANT_ARTIFACT_RESOLVE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantArtifactResponseSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_ARTIFACT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_ARTIFACT_RESPONSE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantAssertionEncrypted") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_ASSERTION_ENCRYPTED_DEFAULT,
                        KEY_WANT_ASSERTION_ENCRYPTED_TRUE);
            }
            
            if (strAttribute.equals("wantAttributeEncrypted") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_ATTRIBUTE_ENCRYPTED_DEFAULT,
                        KEY_WANT_ATTRIBUTE_ENCRYPTED_TRUE);
                spMetadataMod[1]= spMetadata[1].replaceAll(
                        ATTRIB_MAP_DEFAULT,
                        ATTRIB_MAP_VALUE);
            }
            
            //If spmetadata has changed then only load at sp & idp side.
            if (!(spMetadata[0].equals(spMetadataMod[0]) &&
                    spMetadata[1].equals(spMetadataMod[1]))) {
                //delete & load sp metadata
                log (Level.FINEST, "changeMetadata", "SP metadata has changed" +
                        spMetadataMod[0]);
                log (Level.FINEST, "changeMetadata", "SP ext metadata has " +
                        "changed" + spMetadataMod[1]);
                samlv2Common = new SAMLv2Common();
                samlv2Common.loadSPMetadata(spMetadataMod[0], spMetadataMod[1],
                        fmsp, fmidp, configMap, webClient);
            }
            
            //If idpmetadata has changed then only load at sp & idp side.
            if (!(idpMetadata[0].equals(idpMetadataMod[0]) &&
                    idpMetadata[1].equals(idpMetadataMod[1]))) {
                //delete & load idp metadata
                log (Level.FINEST, "changeMetadata", "IDP metadata has " +
                        "changed" + idpMetadataMod[0]);
                log (Level.FINEST, "changeMetadata", "IDP ext metadata has " +
                        "changed" + idpMetadataMod[1]);
                samlv2Common = new SAMLv2Common();
                samlv2Common.loadIDPMetadata(idpMetadataMod[0],
                        idpMetadataMod[1], fmsp, fmidp, configMap, webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "changeMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("changeMetadata");
    }
}
