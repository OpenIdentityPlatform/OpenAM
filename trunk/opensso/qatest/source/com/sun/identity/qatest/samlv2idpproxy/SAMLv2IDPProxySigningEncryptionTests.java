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
 * $Id: SAMLv2IDPProxySigningEncryptionTests.java,v 1.3 2009/01/27 00:15:33 nithyas Exp $
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
public class SAMLv2IDPProxySigningEncryptionTests extends TestCommon {
    
    String KEY_WANT_AUTHN_REQ_SIGNED_FALSE = 
            "WantAuthnRequestsSigned=\"false\"";
    String KEY_WANT_AUTHN_REQ_SIGNED_TRUE = "WantAuthnRequestsSigned=\"true\"";
    String KEY_AUTHN_REQ_SIGNED_FALSE = "AuthnRequestsSigned=\"false\"";
    String KEY_AUTHN_REQ_SIGNED_TRUE = "AuthnRequestsSigned=\"true\"";
    String KEY_WANT_ASSERTION_SIGNED_FALSE = "WantAssertionsSigned=\"false\"";
    String KEY_WANT_ASSERTION_SIGNED_TRUE = "WantAssertionsSigned=\"true\"";
    String KEY_WANT_NAMEID_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantNameIDEncrypted\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_NAMEID_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantNameIDEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESOLVE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantArtifactResolveSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESOLVE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantArtifactResolveSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantLogoutRequestSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE = "<Attribute name=" +
            "\"wantLogoutRequestSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantLogoutResponseSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantLogoutResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantMNIRequestSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_MNI_REQUEST_SIGNED_TRUE = "<Attribute name=" +
            "\"wantMNIRequestSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantMNIResponseSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_MNI_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantMNIResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ATTRIBUTE_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantAttributeEncrypted\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_ATTRIBUTE_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantAttributeEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ASSERTION_ENCRYPTED_DEFAULT = "<Attribute name=" +
            "\"wantAssertionEncrypted\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_ASSERTION_ENCRYPTED_TRUE = "<Attribute name=" +
            "\"wantAssertionEncrypted\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESPONSE_SIGNED_DEFAULT = "<Attribute name=" +
            "\"wantArtifactResponseSigned\">\n" +
            "            <Value/>" +
            "        </Attribute>";
    String KEY_WANT_ARTIFACT_RESPONSE_SIGNED_TRUE = "<Attribute name=" +
            "\"wantArtifactResponseSigned\">\n" +
            "            <Value>true</Value>" +
            "        </Attribute>";
    String ATTRIB_MAP_DEFAULT = "<Attribute name=\"attributeMap\">\n"
            + "            <Value/>\n"
            + "        </Attribute>";
    String ATTRIB_MAP_VALUE = "<Attribute name=\"attributeMap\">\n"
            + "            <Value>mail=mail</Value>\n"
            + "        </Attribute>";
    private WebClient webClient;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private FederationManager fmIDPProxy;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private Map<String, String> usersMap;
    ArrayList spuserlist = new ArrayList();
    ArrayList idpuserlist = new ArrayList();
    ArrayList idpproxyuserlist = new ArrayList();
    private String baseDir;
    private String spurl;
    private String idpurl;
    private String idpproxyurl;
    private String spMetadata[]= {"",""};
    private String idpMetadata[]= {"",""};
    private String idpProxyMetadata[]= {"",""};
    private HtmlPage page;
    private String ssoProfile;
    private String sloProfile;
    private String strAttr;
    private String strMetadataType;
    
    /**
     * This is constructor for this class.
     */
    public SAMLv2IDPProxySigningEncryptionTests() {
        super("SAMLv2IDPProxySigningEncryptionTests");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    @BeforeMethod(groups={"ldapv3_sec",  "s1ds_sec", "ad_sec",  "amsdk_sec"})
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
    @Parameters({"ssoprofile", "sloprofile", "attribute", "metadata"})
    @BeforeClass(groups={"ldapv3_sec",  "s1ds_sec", "ad_sec",  "amsdk_sec"})
    public void setup(String strSSOProfile, String strSLOProfile, String 
            strAttribute, String strMetadata)
    throws Exception {
        Object[] params = {strSSOProfile, strSLOProfile, strAttribute, 
        strMetadata};
        entering("setup", params);
        List<String> list;
        try {
            ssoProfile = strSSOProfile;
            sloProfile = strSLOProfile;
            strAttr = strAttribute;
            strMetadataType = strMetadata;
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
            idpproxyurl = configMap.get(TestConstants.KEY_IDP_PROXY_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_PROXY_HOST) + 
                    ":" + configMap.get(TestConstants.KEY_IDP_PROXY_PORT) +
                    configMap.get(TestConstants.KEY_IDP_PROXY_DEPLOYMENT_URI);
            
            getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);
            fmIDPProxy = new FederationManager(idpproxyurl);
            
            usersMap = new HashMap<String, String>();
            usersMap = getMapFromResourceBundle("samlv2idpproxy" +
                    fileseparator + "SAMLv2IDPProxySigningEncryptionTests");
            log(Level.FINEST, "setup", "Users map is " + usersMap);
            //create sp user
            list.clear();
            list.add("sn=" + usersMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + usersMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" + usersMap.get(
                    TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            log(Level.FINE, "setup", "SP user to be created is " + list);
            if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                    configMap.get(
                    TestConstants.KEY_SP_EXECUTION_REALM),
                    usersMap.get(TestConstants.KEY_SP_USER), "User", 
                    list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed at SP");
                assert false;
            }
            spuserlist.add(usersMap.get(TestConstants.KEY_SP_USER));
            list.clear();

            //create idp user
            list.clear();
            list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" + usersMap.get(
                    TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            log(Level.FINE, "setup", "IDP user to be created is " + list);
            if (FederationManager.getExitCode(fmIDP.createIdentity(
                    webClient, configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    usersMap.get(TestConstants.KEY_IDP_USER), "User", 
                    list)) != 0) {
            log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed at IDP");
                assert false;
            }
            idpuserlist.add(usersMap.get(TestConstants.KEY_IDP_USER));
            list.clear();
            //create idp proxy user
            list.clear();
            list.add("mail=" + usersMap.get(
                    TestConstants.KEY_IDP_PROXY_USER_MAIL));
            list.add("sn=" + usersMap.get(TestConstants.KEY_IDP_PROXY_USER));
            list.add("cn=" + usersMap.get(TestConstants.KEY_IDP_PROXY_USER));
            list.add("userpassword=" + usersMap.get(
                    TestConstants.KEY_IDP_PROXY_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            log(Level.FINEST, "setup", "IDP Proxy user to be created " +
                    "is " + list);
            if (FederationManager.getExitCode(fmIDPProxy.createIdentity(
                    webClient, configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), usersMap.get(
                    TestConstants.KEY_IDP_PROXY_USER), "User",
                    list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed at IDP Proxy");
                assert false;
            }
            idpproxyuserlist.add(usersMap.get(TestConstants.
                    KEY_IDP_PROXY_USER));
            list.clear();
            Thread.sleep(10000);
            //Change the metadata based on the parameters received. 
            changeMetadata(strAttribute, strMetadata, fmSP, fmIDP, fmIDPProxy);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            cleanup();
            throw e;
        } finally {
            log(Level.FINE, "setup", "Logging out of sp & idp");
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
            consoleLogout(webClient, idpproxyurl + "/UI/Logout");
        }
        exiting("setup");
    }
    
    /**
     * Run SP init SSO, SLO
     * @DocTest: SAML2IDPProxy|Perform SP initiated SSO, SLO 
     */
    @Test(groups={"ldapv3_sec",  "s1ds_sec", "ad_sec",  "amsdk_sec"})
    public void SAMLv2IDPProxySignEncryptSPSSOSLO()
    throws Exception {
        entering("SAMLv2IDPProxySignEncryptSPSSOSLO", null);
        try {
            log(Level.FINE, "SAMLv2IDPProxySignEncryptSPSSOSLO", "\nRunning: " +
                    "SAMLv2IDPProxySignEncryptSPSSOSLO\n");
            Reporter.log("Test Description: This test will enable metadata " +
                    "attribute " +  strAttr + " in " + strMetadataType + 
                    " and run SSO with " + ssoProfile + "and SLO with " +
                    sloProfile);
            configMap.put(TestConstants.KEY_SP_USER, 
                    usersMap.get(TestConstants.KEY_SP_USER));
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            configMap.put(TestConstants.KEY_IDP_USER, 
                    usersMap.get(TestConstants.KEY_IDP_USER));
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER, 
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER));
            configMap.put(TestConstants.KEY_IDP_PROXY_USER_PASSWORD, 
                    usersMap.get(TestConstants.KEY_IDP_PROXY_USER_PASSWORD));
            
            log(Level.FINEST, "SAMLv2IDPProxySignEncryptSPSSOSLO", "Map:" + 
                    configMap);
            
            //Create xml's for each actions.
            String[] arrActions = {"SAMLv2IDPProxySignEncryptSPSSOSLO_ssoinit", 
            "SAMLv2IDPProxySignEncryptSPSSOSLO_slo"};
            String ssoxmlfile = baseDir + arrActions[0] + ".xml";
            SAMLv2Common.getxmlSPInitSSO(ssoxmlfile, configMap, ssoProfile,
                    false, true);
            String sloxmlfile = baseDir + arrActions[1] + ".xml";
            SAMLv2Common.getxmlSPSLO(sloxmlfile, configMap, sloProfile, true);
            
            for (int i = 0; i < arrActions.length; i++) {
                log(Level.FINE, "SAMLv2IDPProxySignEncryptSPSSOSLO",
                        "Executing xml: " + arrActions[i]);
                task = new DefaultTaskHandler(baseDir + arrActions[i]
                        + ".xml");
                page = task.execute(webClient);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "SAMLv2IDPProxySignEncryptSPSSOSLO", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("SAMLv2IDPProxySignEncryptSPSSOSLO");
    }
        
    /**
     * This methods deletes all the users as part of cleanup
     */
    @AfterClass(groups={"ldapv3_sec",  "s1ds_sec", "ad_sec",  "amsdk_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINE, "Cleanup", "Entering Cleanup: ");
            getWebClient();
            
            // delete sp users
            log(Level.FINE, "cleanup", "sp users to delete : " + spuserlist);
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    spuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }
            
            // Delete idp users
            log(Level.FINE, "cleanup", "idp users to delete : " + idpuserlist);
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idpuserlist, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
            }

            // Delete idp proxy users
            log(Level.FINE, "cleanup", "idp proxy users to delete : " + 
                    idpproxyuserlist);
            consoleLogin(webClient, idpproxyurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_PROXY_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_PROXY_AMADMIN_PASSWORD));
            if (FederationManager.getExitCode(fmIDPProxy.deleteIdentities(
                    webClient, configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), idpproxyuserlist, 
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }

            if ((spMetadata[0].equals("")) || (spMetadata[1].equals(""))) {
                log(Level.FINEST, "cleanup", "Default SP metadata empty so " +
                        "dont perform any action");
            } else {
                log(Level.FINE, "cleanup", "Now Load Default SP metadata ");
                loadSPMetadata(spMetadata[0], spMetadata[1], fmSP, 
                        fmIDPProxy, configMap, webClient);
            }
            if ((idpMetadata[0].equals("")) || (idpMetadata[1].equals(""))) {
                log(Level.FINEST, "cleanup", "Default IDP metadata empty so " +
                        "dont perform any action");
            } else {
                log(Level.FINE, "cleanup", "Now Load Default IDP metadata ");
                loadIDPMetadata(idpMetadata[0], idpMetadata[1], 
                        fmIDPProxy, fmIDP, configMap, webClient);
            }
            log(Level.FINE, "cleanup", "Now Load Default IDP Proxy metadata ");
            loadIDPProxyMetadata(idpProxyMetadata[0], idpProxyMetadata[1],
                    fmSP, fmIDP, fmIDPProxy, configMap, webClient);
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
    
    /**
     * This method changes the metadata based on the attribute & metadata type 
     * (sp, idp, all) received. 
     */
    private void changeMetadata(String strAttribute, String strMetadata,
            FederationManager fmsp, FederationManager fmidp, FederationManager 
            fmidpproxy)
    throws Exception {
        String spMetadataMod[] = {"",""};
        String idpMetadataMod[] = {"",""};
        String idpproxyMetadataMod[] = {"",""};
        try {
            log(Level.FINE, "changeMetadata", "Entering changeMetadata ");
            //export metadata
            if (strMetadata.contains("sp") || strMetadata.contains("all")) {
                HtmlPage spExportEntityPage = fmsp.exportEntity(webClient,
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
                if (!(spMetadata[0].equals(null)) & (!spMetadata[0].equals("")) & 
                        !(spMetadata[1].equals(null)) & (!spMetadata[1].equals(""))) {
                    log (Level.FINER, "changeMetadata", "Successfully " +
                            "imported SP metadatada");
                } else {
                    log (Level.SEVERE, "changeMetadata", "Export SP metadata " +
                            "is empty. ");
                    assert false;
                }
            }
            if (strMetadata.contains("idp") || strMetadata.contains("all")) {
                HtmlPage idpExportEntityPage = fmidp.exportEntity(webClient,
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

            HtmlPage idpProxyExportEntityPage = fmidpproxy.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    false, true, true, "saml2");
            if (FederationManager.getExitCode(idpProxyExportEntityPage) != 0) {
               log(Level.SEVERE, "changeMetadata", "exportEntity famadm" +
                       " command failed");
               assert false;
            }
            log (Level.FINEST, "changeMetadata", "Export IDP Proxy metadata" +
                    "is: " + idpProxyExportEntityPage.getWebResponse().
                    getContentAsString());
            idpProxyMetadata[0] = SAMLv2Common.getMetadataFromPage(
                    idpProxyExportEntityPage);
            idpProxyMetadata[1] = SAMLv2Common.getExtMetadataFromPage(
                    idpProxyExportEntityPage);
            log (Level.FINEST, "changeMetadata", "Export IDP Proxy metadata" +
                    "is: " + idpProxyMetadata[0]);
            log (Level.FINEST, "changeMetadata", "Export IDPProxy Ext metadata" +
                    "is: " + idpProxyMetadata[1]);
            if (!(idpProxyMetadata[0].equals(null)) & 
                    (!(idpProxyMetadata[0].equals(""))) & 
                    (!(idpProxyMetadata[1].equals(null))) & 
                    (!(idpProxyMetadata[1].equals("")))) {
                log (Level.FINER, "changeMetadata", "Successfully " +
                        "exported IDP proxy metadatada");
            } else {
                log (Level.SEVERE, "changeMetadata", "Export IDP proxy metadata" +
                        "is empty. ");
                assert false;
            }
            
            //Set the default values to Modified data. 
            spMetadataMod[0] = spMetadata[0];
            spMetadataMod[1] = spMetadata[1];
            idpMetadataMod[0] = idpMetadata[0];
            idpMetadataMod[1] = idpMetadata[1];
            idpproxyMetadataMod[0] = idpProxyMetadata[0];
            idpproxyMetadataMod[1] = idpProxyMetadata[1];
           
            if (strAttribute.equals("WantAuthnRequestsSigned")) {
                idpMetadataMod[0] = idpMetadata[0].replaceAll(
                        KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                        KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                idpproxyMetadataMod[0] = idpProxyMetadata[0].replaceAll(
                        KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                        KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                if (strMetadata.contains("all")) {
                    spMetadataMod[0] = spMetadata[0].replaceAll(
                            KEY_AUTHN_REQ_SIGNED_FALSE,
                            KEY_AUTHN_REQ_SIGNED_TRUE);
                    idpproxyMetadataMod[0] = idpProxyMetadata[0].replaceAll(
                                KEY_AUTHN_REQ_SIGNED_FALSE,
                                KEY_AUTHN_REQ_SIGNED_TRUE);
                }
            }
            if (strAttribute.equals("WantAuthnRequestsSigned")) {
                spMetadataMod[0] = spMetadata[0].replaceAll(
                        KEY_AUTHN_REQ_SIGNED_FALSE,
                        KEY_AUTHN_REQ_SIGNED_TRUE);
                 idpproxyMetadataMod[0] = idpProxyMetadata[0].replaceAll(
                        KEY_AUTHN_REQ_SIGNED_FALSE,
                        KEY_AUTHN_REQ_SIGNED_TRUE);
              if (strMetadata.contains("all")) {
                    idpMetadataMod[0] = idpMetadata[0].replaceAll(
                            KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                            KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                    idpproxyMetadataMod[0] = idpProxyMetadata[0].replaceAll(
                            KEY_WANT_AUTHN_REQ_SIGNED_FALSE,
                            KEY_WANT_AUTHN_REQ_SIGNED_TRUE);
                }
            }
            if (strAttribute.equals("WantAssertionsSigned")) {
                spMetadataMod[0] = spMetadata[0].replaceAll(
                        KEY_WANT_ASSERTION_SIGNED_FALSE,
                        KEY_WANT_ASSERTION_SIGNED_TRUE);
                idpproxyMetadataMod[0] = idpProxyMetadata[0].replaceAll(
                        KEY_WANT_ASSERTION_SIGNED_FALSE,
                        KEY_WANT_ASSERTION_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutRequestSigned") && 
                    strMetadata.contains("all")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_REQUEST_SIGNED_TRUE);
           }
            
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantLogoutResponseSigned") && 
                    strMetadata.contains("all")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_LOGOUT_RESPONSE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIRequestSigned") && 
                    strMetadata.contains("all")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_REQUEST_SIGNED_DEFAULT,
                        KEY_WANT_MNI_REQUEST_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            if (strAttribute.equals("wantMNIResponseSigned") && 
                    strMetadata.contains("all")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_MNI_RESPONSE_SIGNED_DEFAULT,
                        KEY_WANT_MNI_RESPONSE_SIGNED_TRUE);
            }
            
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("sp")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
            }
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("idp")) {
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
            }
            if (strAttribute.equals("wantNameIDEncrypted") && 
                    strMetadata.contains("all")) {
                spMetadataMod[1] = spMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
                idpMetadataMod[1] = idpMetadata[1].replaceAll(
                        KEY_WANT_NAMEID_ENCRYPTED_DEFAULT,
                        KEY_WANT_NAMEID_ENCRYPTED_TRUE);
                idpproxyMetadataMod[1] = idpProxyMetadata[1].replaceAll(
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
                loadSPMetadata(spMetadataMod[0], spMetadataMod[1],
                        fmsp, fmidpproxy, configMap, webClient);
            }
            
            //If idpmetadata has changed then only load at sp & idp side.
            if (!(idpMetadata[0].equals(idpMetadataMod[0]) &&
                    idpMetadata[1].equals(idpMetadataMod[1]))) {
                //delete & load idp metadata
                log (Level.FINEST, "changeMetadata", "IDP metadata has " +
                        "changed" + idpMetadataMod[0]);
                log (Level.FINEST, "changeMetadata", "IDP ext metadata has " +
                        "changed" + idpMetadataMod[1]);
                loadIDPMetadata(idpMetadataMod[0],
                        idpMetadataMod[1], fmidpproxy, fmidp, configMap, webClient);
            }
            loadIDPProxyMetadata(idpproxyMetadataMod[0], idpproxyMetadataMod[1],
                    fmsp, fmidp, fmidpproxy, configMap, webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "changeMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("changeMetadata");
    }
    /**
     * This method loads the IDP metadata on idp & idp proxy
     * @param metadata is the standard metadata of IDP
     * @param metadataext is the extended metadata of IDP
     * @param FederationManager object initiated with IDP Proxy details. 
     * @param FederationManager object initiated with IDP details. 
     * @param MAP containing all the IDP Proxy & IDP details
     * @param WebClient object after admin login is successful.
     */
    public void loadIDPMetadata(String metadata, String metadataext,
            FederationManager fmidpproxy, FederationManager fmidp, Map configMap, 
            WebClient webClient)
    throws Exception {
        try{
            
            if ((metadata.equals(null)) || (metadataext.equals(null)) ||
                    (metadata.equals("")) || (metadataext.equals(""))) {
                log(Level.SEVERE, "loadIDPMetadata", "metadata cannot be " +
                        "empty");
                log(Level.FINEST, "loadIDPMetadata", "metadata is : " +
                        metadata);
                log(Level.FINEST, "loadIDPMetadata", "ext metadata is : " +
                        metadataext);
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    false,  "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted idp entity on " +
                        "IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldnt delete idp " +
                        "entity on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    metadata, metadataext, null, "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Successfully " +
                        "imported IDP metadata on IDP side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import IDP" +
                        " metadata on IDP side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            
            //delete & load idp metadata on IDP Proxy
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_IDP_COT), "");
            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            log(Level.FINER, "loadIDPMetadata", "IDP Ext. Metadata to load " +
                    "on IDP Proxy" + metadataext);
            if (FederationManager.getExitCode(fmidpproxy.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), false, "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Deleted idp entity on " +
                        "IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldnt delete idp " +
                        "entity on IDP Proxy side");
                log(Level.SEVERE, "loadIDPMetadata", "deleteEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidpproxy.importEntity(webClient,
                    (String)configMap.get(TestConstants.
                    KEY_IDP_PROXY_EXECUTION_REALM), metadata, metadataext,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPMetadata", "Successfully " +
                        "imported IDP metadata on IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadIDPMetadata", "Couldn't import IDP  " +
                        "metadata on IDP Proxy side");
                log(Level.SEVERE, "loadIDPMetadata", "importEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadIDPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /*
     * This method loads the SP metadata on sp & idp 
     * @param metadata is the standard metadata of SP
     * @param metadataext is the extended metadata of SP
     * @param FederationManager object initiated with SP details. 
     * @param FederationManager object initiated with IDP details. 
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     */
    public void loadSPMetadata(String metadata, String metadataext, 
            FederationManager fmsp, FederationManager fmidpproxy, Map configMap, 
            WebClient webClient) 
    throws Exception {
        try {
            if ((metadata.equals(null)) || (metadataext.equals(null)) || 
                    (metadata.equals("")) & (metadataext.equals(""))) {
                log(Level.SEVERE, "loadSPMetadata", "metadata cannot be empty");
                log(Level.FINEST, "loadSPMetadata", "metadata is : " + 
                        metadata);
                log(Level.FINEST, "loadSPMetadata", "ext metadata is : " + 
                        metadataext);
                assert false;
            }
            if (FederationManager.getExitCode(fmsp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                    "saml2")) == 0) {
                log(Level.FINEST, "loadSPMetadata", "Deleted sp entity on " +
                        "SP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldnt delete sp " +
                        "entity on SP side");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity (SP)" +
                        " famadm command failed");
                assert false;
            }

            if (FederationManager.getExitCode(fmsp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    metadata, metadataext, null, "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Successfully " +
                        "imported SP metadata on SP side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on SP side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity (SP)" +
                        " famadm command failed");
                assert false;
            }

            //delete & load sp metadata on IDP
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_SP_COT), "");
            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            log(Level.FINER, "loadSPMetadata", "SP Ext. Metadata to load " +
                    "on IDP" + metadataext);
            if (FederationManager.getExitCode(fmidpproxy.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), 
                    false, "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Deleted sp entity on " +
                        "IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldnt delete sp " +
                        "entity on IDP Proxyside");
                log(Level.SEVERE, "loadSPMetadata", "deleteEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidpproxy.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM),
                    metadata, metadataext,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadSPMetadata", "Successfully " +
                        "imported SP metadata on IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadSPMetadata", "Couldn't import SP " +
                        "metadata on IDP Proxy side");
                log(Level.SEVERE, "loadSPMetadata", "importEntity (IDP)" +
                        " famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "loadSPMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    
    /*
     * This method loads the SP metadata on sp & idp 
     * @param metadata is the standard metadata of SP
     * @param metadataext is the extended metadata of SP
     * @param FederationManager object initiated with SP details. 
     * @param FederationManager object initiated with IDP details. 
     * @param MAP containing all the SP & IDP details
     * @param WebClient object after admin login is successful.
     */
    public void loadIDPProxyMetadata(String metadata, String metadataext, 
            FederationManager fmsp, FederationManager fmidp, FederationManager 
            fmidpproxy, Map configMap, WebClient webClient) 
    throws Exception {
        try {
            if ((metadata.equals(null)) || (metadataext.equals(null)) || 
                    (metadata.equals("")) & (metadataext.equals(""))) {
                log(Level.SEVERE, "loadIDPProxyMetadata", "metadata cannot be empty");
                log(Level.FINEST, "loadIDPProxyMetadata", "metadata is : " + 
                        metadata);
                log(Level.FINEST, "loadIDPProxyMetadata", "ext metadata is : " + 
                        metadataext);
                assert false;
            }
            if (FederationManager.getExitCode(fmidpproxy.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM), false,
                    "saml2")) == 0) {
                log(Level.FINEST, "loadIDPProxyMetadata", "Deleted IDP Proxy entity on " +
                        "IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldnt delete IDP Proxy " +
                        "entity on IDP Proxy side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "deleteEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }

            if (FederationManager.getExitCode(fmidpproxy.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_EXECUTION_REALM),
                    metadata, metadataext, null, "saml2")) == 0) {
                log(Level.FINE, "loadIDPProxyMetadata", "Successfully " +
                        "imported IDP Proxy metadata on IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldn't import IDP Proxy " +
                        "metadata on IDP Proxy side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "importEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }

            //delete & load IDP Proxy metadata on IDP
            metadataext = metadataext.replaceAll(
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_COT), "");
            metadataext = metadataext.replaceAll(
                    "hosted=\"true\"", "hosted=\"false\"");
            metadataext = metadataext.replaceAll(
                    "hosted=\"1\"", "hosted=\"0\"");
            log(Level.FINER, "loadIDPProxyMetadata", "IDP Proxy Ext. Metadata to load " +
                    "on IDP" + metadataext);
            if (FederationManager.getExitCode(fmidp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    false, "saml2")) == 0) {
                log(Level.FINE, "loadIDPProxyMetadata", "Deleted IDP Proxy entity on " +
                        "IDP Proxy side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldnt delete IDP Proxy " +
                        "entity on IDP side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "deleteEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmidp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    metadata, metadataext,
                    (String)configMap.get(TestConstants.KEY_IDP_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPProxyMetadata", "Successfully " +
                        "imported IDP Proxy metadata on IDP  side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldn't import IDP Proxy " +
                        "metadata on IDP side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "importEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }
            
            //delete & load IDP Proxy metadata on SP
            if (FederationManager.getExitCode(fmsp.deleteEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_IDP_PROXY_ENTITY_NAME),
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), 
                    false, "saml2")) == 0) {
                log(Level.FINE, "loadIDPProxyMetadata", "Deleted IDP Proxy entity on " +
                        "SP side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldnt delete IDP Proxy " +
                        "entity on SP side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "deleteEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }
            if (FederationManager.getExitCode(fmsp.importEntity(webClient,
                    (String)configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    metadata, metadataext,
                    (String)configMap.get(TestConstants.KEY_SP_COT),
                    "saml2")) == 0) {
                log(Level.FINE, "loadIDPProxyMetadata", "Successfully " +
                        "imported IDP Proxy metadata on SP side");
            } else {
                log(Level.SEVERE, "loadIDPProxyMetadata", "Couldn't import IDP Proxy " +
                        "metadata on SP side");
                log(Level.SEVERE, "loadIDPProxyMetadata", "importEntity (IDP Proxy)" +
                        " famadm command failed");
                assert false;
            }

        } catch (Exception e) {
            log(Level.SEVERE, "loadIDPProxyMetadata", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
}
