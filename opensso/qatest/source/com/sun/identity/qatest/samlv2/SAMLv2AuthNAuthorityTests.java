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
 * $Id: SAMLv2AuthNAuthorityTests.java,v 1.6 2009/06/08 23:28:39 mrudulahg Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;


import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.ResourceBundle;
import java.net.URL;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class to test the new SAMLv2 Authentication Authority Tests
 * SP communication to the IDP for the SAML authentication Request
 * and IDP provides assertion of the user based on different levels
 * of ACComparision such as "exact","minimum","maximum","better"
 */
public class SAMLv2AuthNAuthorityTests extends TestCommon {
    
    private WebClient spWebClient;
    private WebClient idpWebClient;
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task1;
    private HtmlPage wpage;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    public  WebClient webClient;
    private ResourceBundle modData;
    private String spurl;
    private String idpurl;
    private String fedSSOURL;
    private String spmetadata;
    private String idpmetadata;
    private String testName;
    private String testType;
    private String testAttribute;
    private String testModulename;
    private String testClassType;
    private String testCompareType;
    private String metaAliasname;
    private String idptestRealm;
    private String firstModInstance;
    private String secondModInstance;
    private String moduleServiceName;
    private String moduleSubConfigId;
    private String ATTRIB_ASSERT_CACHE_DEFAULT = "<Attribute name=\""
            + "assertionCacheEnabled\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>";
    private String ATTRIB_ASSERT_CACHE_ENABLE = "<Attribute name=\""
            + "assertionCacheEnabled\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>";
    private String ATTRIB_AuthNCONTEXTCLASSREF_DEFAULT = "<Attribute name=\""
            + "idpAuthncontextClassrefMapping\">\n"
            + "            <Value>urn:oasis:names:tc:SAML:2.0:ac:classes:"
            + "PasswordProtectedTransport|0||default</Value>\n"
            + "        </Attribute>";
    private String ATTRIB_AuthNCONTEXTCLASSREF_IDP_ENABLE = "<Attribute name=\""
            + "idpAuthncontextClassrefMapping\">\n"
            + "            <Value>urn:oasis:names:tc:SAML:2.0:ac:classes:"
            + "Password|6|module=ldap-samlv2-2|</Value>\n"
            + "            <Value>urn:oasis:names:tc:SAML:2.0:ac:classes:"
            + "PasswordProtectedTransport|10|module=ldap-samlv2-1|default</Value>\n"
            + "        </Attribute>";
    /**
     * Constructor SAMLv2AuthNAuthorityTests
     */
    public SAMLv2AuthNAuthorityTests() {
        super("SAMLv2AuthNAuthorityTests");
    }

    /**
     * Configures the SP and IDP load meta for the SAMLv2AuthNAuthorityTests
     * tests to execute
     */
    @Parameters({"ptestName", "ptestType", "pAttribute", "ptestModule",
        "pACClassType", "pACCompareType"})
    @BeforeClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void setup(String ptestName, String ptestType, String pAttribute,
            String ptestModule, String pACClassType, String pACCompareType)
            throws Exception {
        ArrayList list;
        try {
            testName = ptestName;
            testType = ptestType;
            testAttribute = pAttribute;
            testModulename = ptestModule;
            testClassType = pACClassType;
            testCompareType = pACCompareType;
            firstModInstance = testModulename + "-samlv2" + "-1";
            secondModInstance = testModulename + "-samlv2" + "-2";
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator 
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) 
                    + SAMLv2Common.fileseparator + "built" 
                    + SAMLv2Common.fileseparator + "classes" 
                    + SAMLv2Common.fileseparator;
            modData = ResourceBundle.getBundle("config" + fileseparator +
                    "AuthenticationConfig-Generated");
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator 
                    + "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator 
                    + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("config" +
                    fileseparator + "AuthenticationConfig-Generated",
                    configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator 
                    + "SAMLv2AuthNAuthorityTests", configMap);
            configMap.put(TestConstants.KEY_SP_USER, "sp" + testName);
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, "sp" + testName);
            configMap.put(TestConstants.KEY_IDP_USER, "idp" + testName);
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, "idp" +
                    testName);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            getWebClient();
            // Create sp users
            consoleLogin(spWebClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            list = new ArrayList();
            list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmSP.createIdentity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            // Create idp users
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);

            consoleLogin(idpWebClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDP.createIdentity(idpWebClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), 
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed");
                assert false;
            }
            idptestRealm = configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM);
            //Create Authentication modules on IDP
            authNAuthorityModuleSetup(testModulename, firstModInstance);
            authNAuthorityModuleSetup(testModulename, secondModInstance);

            authNAuthorityMapSetup();
            //Federated SSO URL
            fedSSOURL = spurl + "/saml2/jsp/spSSOInit.jsp?metaAlias=" +
                    configMap.get(TestConstants.KEY_SP_METAALIAS) +
                    "&idpEntityID=" +
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(spWebClient, spurl + "/UI/Logout");
            consoleLogout(idpWebClient, idpurl + "/UI/Logout");
        }
        exiting("setup");
    }

    /**
     * Enable AuthNAuthority setup
     */
    public void authNAuthorityMapSetup()
            throws Exception {
        entering("authNAuthorityMapSetup", null);
        try {
            //get sp & idp extended metadata
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            consoleLogin(webClient, spurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            HtmlPage spmetaPage = spfm.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(spmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm " +
                        "command failed");
                assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            String spmetadataMod =
                    spmetadata.replaceAll(ATTRIB_ASSERT_CACHE_DEFAULT,
                    ATTRIB_ASSERT_CACHE_ENABLE);
            log(Level.FINEST, "authNAuthorityMapSetup", "Modified" +
                    " metadata:" + spmetadataMod);
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.FINEST, "authNAuthorityMapSetup", "Deletion of " +
                        "Extended entity failed");
                log(Level.FINEST, "authNAuthorityMapSetup", "deleteEntity" +
                        " famadm command failed");
                assert (false);
            }
            if (FederationManager.getExitCode(spfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "authNAuthorityMapSetup", "Failed to" +
                        " import extended metadata");
                log(Level.SEVERE, "authNAuthorityMapSetup", "importEntity" +
                        " famadm command failed");
                assert (false);
            }
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            HtmlPage idpmetaPage = idpfm.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    false, false,
                    true, "saml2");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm " +
                        "command failed");
                assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(idpmetaPage);
            String idpmetadataMod =
                    idpmetadata.replaceAll(ATTRIB_ASSERT_CACHE_DEFAULT,
                    ATTRIB_ASSERT_CACHE_ENABLE);
            log(Level.FINEST, "authNAuthorityMapSetup", "Before Modified IDP" +
                    " metadata:\n" + idpmetadataMod);
            idpmetadataMod =
                    idpmetadataMod.replace(ATTRIB_AuthNCONTEXTCLASSREF_DEFAULT,
                    ATTRIB_AuthNCONTEXTCLASSREF_IDP_ENABLE);
            log(Level.FINEST, "authNAuthorityMapSetup", "Modified IDP" +
                    " metadata:\n" + idpmetadataMod);
            if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "authNAuthorityMapSetup", "Deletion of" +
                        " idp Extended entity failed");
                log(Level.SEVERE, "authNAuthorityMapSetup", "deleteEntity" +
                        " famadm command failed");
                assert (false);
            }
            if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                    idpmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "authNAuthorityMapSetup", "Failed to" +
                        " import idp extended metadata");
                log(Level.SEVERE, "authNAuthorityMapSetup", "importEntity" +
                        " famadm command failed");
                assert (false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "authNAuthorityMapSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("authNAuthorityMapSetup");
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
            spWebClient = new WebClient(BrowserVersion.FIREFOX_3);
            idpWebClient = new WebClient(BrowserVersion.FIREFOX_3);
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Execute the authNAuthorityQuery tests
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void authNAuthorityQueryTest()
            throws Exception {
        String authnQueryURL = null;
        String ssopage;
        HtmlPage resultPage;
        String result;
        String strCompare;
        String expresult;
        try {
            Reporter.log("Test Description: This test  " 
                    + testName + " will run to make sure " 
                    + " Authentication Query with test Type : " 
                    + testType + " for the binding : " 
                    + testAttribute + " will work fine");
            ssopage = configMap.get(TestConstants.KEY_SSO_RESULT);
            //Federate the users
            xmlfile = baseDir + testName + ".xml";
            //getxmlIDPInitSSO , getxmlSPInitSSO
            if (testAttribute.equalsIgnoreCase("artifact")) {
                SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact",
                        false, false);
            } else if (testAttribute.equalsIgnoreCase("post")) {
                SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "post",
                        false, false);
            }
            log(Level.FINEST, "authNAuthorityQueryTest", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            wpage = task1.execute(webClient);
            if (!wpage.getWebResponse().getContentAsString().contains(ssopage)){
                log(Level.SEVERE, "authNAuthorityQueryTest", "Couldn't " +
                        "federate users");
                assert false;
            }
            Thread.sleep(5000);
            // Single Signon
            log(Level.FINEST, "authNAuthorityQueryTest", "FederatedURL" 
                    + fedSSOURL);
            URL surl = new URL(fedSSOURL);
            HtmlPage spage = (HtmlPage) webClient.getPage(surl);
            if (!spage.getWebResponse().getContentAsString().contains(ssopage)){
                log(Level.SEVERE, "authNAuthorityQueryTest", "Failed" + "SSO");
                assert false;
            }
            metaAliasname = configMap.get(TestConstants.KEY_SP_METAALIAS);
            authnQueryURL = spurl + "/authNQuery.jsp?spMetaAlias=" +
                    metaAliasname + "&authnAuthorityEntityID=" +
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME) 
                    + "&acComparisionClass=" + testClassType 
                    + "&acComparisionReq=" + testCompareType;
            log(Level.FINEST, "authNAuthorityQueryTest", "NoAttr case :" 
                    + authnQueryURL);
            log(Level.FINEST, "authNAuthorityQueryTest", "AttributeQueryURL" +
                    authnQueryURL);
            URL aqurl = new URL(authnQueryURL);
            resultPage = (HtmlPage) webClient.getPage(aqurl);
            result = resultPage.getWebResponse().getContentAsString();
            strCompare = "Comparison=" + testCompareType;
            log(Level.FINEST, "authNAuthorityQueryTest", "Expected Result" +
                    strCompare);
            if (!resultPage.getWebResponse().getContentAsString().
                    contains(strCompare)) {
                expresult = getExpectedResult(testCompareType, testClassType);
                if (!resultPage.getWebResponse().getContentAsString().
                        contains(expresult)) {
                    log(Level.SEVERE, "authNAuthorityQueryTest", "Couldn't " +
                            "Get results");
                    assert false;
                }
            }
            log(Level.FINEST, "authNAuthorityQueryTest", "Result Page" +
                    resultPage.asXml());
        } catch (Exception e) {
            log(Level.SEVERE, "authNAuthorityQueryTest", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Clean up and deleted the created users and module instances for this test
     */
    @AfterClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        ArrayList list;
        WebClient webcClient = new WebClient();
        try {
            consoleLogin(webcClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            list = new ArrayList();
            list.add(configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webcClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    list, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentity SP famadm command" 
                        + " failed");
            }
            if (FederationManager.getExitCode(fmSP.deleteEntity(webcClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.FINEST, "cleanup", "Deletion of " +
                        "Extended entity failed");
                log(Level.FINEST, "cleanup", "deleteEntity" +
                        " famadm command failed");
                assert (false);
            }
            if (FederationManager.getExitCode(fmSP.importEntity(webcClient,
                   configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "", 
                   spmetadata, "", "saml2")) != 0) {
               log(Level.SEVERE, "cleanup", "Failed to import extended " +
                       "metadata");
               log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                       " failed");
               assert(false);
            } 
            
            consoleLogin(webcClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add(configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webcClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    list, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentity IDP famadm command" 
                        + "failed");
            }
            list.clear();
            list.add(firstModInstance);
            list.add(secondModInstance);
            Thread.sleep(5000);
            if (FederationManager.getExitCode(fmIDP.deleteAuthInstances(
                    webcClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    list)) != 0) {
                log(Level.SEVERE, "cleanup",
                        "deleteAuthInstances ssoadm command failed");
            }
            if (FederationManager.getExitCode(fmIDP.deleteEntity(webcClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "cleanup", "Deletion of" +
                        " idp Extended entity failed");
                log(Level.SEVERE, "cleanup", "deleteEntity" +
                        " famadm command failed");
                assert (false);
            }
            if (FederationManager.getExitCode(fmIDP.importEntity(webcClient,
                   configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                   idpmetadata, "", "saml2")) != 0) {
               log(Level.SEVERE, "cleanup", "Failed to import idp " +
                       "extended metadata");
               log(Level.SEVERE, "cleanup", "importEntity famadm command" +
                       " failed");
               assert(false);
            } 
            
            Thread.sleep(5000);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webcClient, spurl + "/UI/Logout");
            consoleLogout(webcClient, idpurl + "/UI/Logout");
        }
    }

    /**
     * Creates the module instances for the authNAuthorityTests
     * @param strModname Module name
     * @param strInsname instance name
     * @throws java.lang.Exception 
     */
    private void authNAuthorityModuleSetup(String strModname,
            String strInsname)
            throws Exception {
        try {
            moduleServiceName = configMap.get(strModname +
                    ".module-service-name");
            moduleSubConfigId = "serverconfig";
            List mapModDataList = new ArrayList();

            String serverName =
                    configMap.get(TestConstants.KEY_IDP_SERVER_ALIAS);
            ResourceBundle cfgData =
                    ResourceBundle.getBundle("Configurator-" + serverName +
                    "-Generated");

            String umdatastore = cfgData.getString("umdatastore");
            boolean bEmb = false;

            Map mCfgData = null;
            if (umdatastore.equals("embedded")) {
                bEmb = true;
                mCfgData = getSvrcfgDetails(fmIDP, idpWebClient, idpurl);
            }

            Enumeration bundleKeys = modData.getKeys();
            String value = null;
            while (bundleKeys.hasMoreElements()) {
                String key = (String) bundleKeys.nextElement();
                if (key.startsWith(strModname) && !(key.contains("module-"))) {
                    String actualKey = key.substring(key.indexOf(".") + 1,
                            key.length());

                    if (bEmb) {
                        if (actualKey.equals("iplanet-am-auth-ldap-server")) {
                            value = (String) mCfgData.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_SERVER) +
                                    ":" + (String) mCfgData.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_LDAP_PORT);
                        } else if (actualKey.
                                equals("iplanet-am-auth-ldap-base-dn")) {
                            value = (String) mCfgData.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." +
                                    SMSConstants.UM_LDAPv3_ORGANIZATION_NAME);
                        } else if (actualKey.
                                equals("iplanet-am-auth-ldap-bind-dn")) {
                            value = (String) mCfgData.get(
                                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                                    "." + SMSConstants.UM_LDAPv3_AUTHID);
                        } else if (actualKey.
                                equals("iplanet-am-auth-ldap-bind-passwd")) {
                            value = configMap.get(
                                    TestConstants.KEY_IDP_AMADMIN_PASSWORD);
                        } else {
                            value = modData.getString(key);
                        }
                    } else {
                        value = modData.getString(key);
                    }
                    if (!actualKey.equalsIgnoreCase("realm.1")) {
                        mapModDataList.add(actualKey + "=" + value);
                    } 
                }
            }

            log(Level.FINEST, "authNAuthorityModuleSetup", "mapModDataList : "
                    + mapModDataList);
            if (FederationManager.getExitCode(
                    fmIDP.createSubCfg(idpWebClient, moduleServiceName,
                    strInsname, mapModDataList, idptestRealm,
                    moduleSubConfigId, "0")) != 0) {
                log(Level.SEVERE, "authNAuthorityModuleSetup",
                        "createSubCfg (Module) famadm command failed");
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "authNAuthorityModuleSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * @param attrCompare , AC comparison type
     * @param attrClass, AC comparision class
     * @return returns the expected results
     */
    private String getExpectedResult(String attrCompare, String attrClass) {
        String expected = "";
        if (attrCompare.equalsIgnoreCase("exact") &&
                attrClass.equalsIgnoreCase("Password")) {
            expected = "No assertion found";
        } else if (attrCompare.equalsIgnoreCase("maximum") &&
                attrClass.equalsIgnoreCase("Password")) {
            expected = "No assertion found";
        } else if (attrCompare.equalsIgnoreCase("better") &&
                attrClass.equalsIgnoreCase("PasswordProtectedTransport")) {
            expected = "No assertion found";
        } else {
            expected = "PasswordProtectedTransport";
        }
        return expected;
    }
}
