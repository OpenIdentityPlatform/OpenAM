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
 * $Id: SAMLv2AssertionQueryTests.java,v 1.3 2009/05/27 23:09:05 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.samlv2;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.MultiProtocolCommon;
import com.sun.identity.qatest.common.SAMLv2Common;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.net.URL;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class to test the new SAMLV2
 * AssertionQuery Profile
 */
public class  SAMLv2AssertionQueryTests extends TestCommon {
    
    private IDMCommon idmc;
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
    private String spurl;
    private String idpurl;
    private String fedSSOURL;
    private String attQueryURL;
    private String ssopage = "Single Sign-on succeeded";
    private String result ="";
    private String configDir;
    private String spmetadata;
    private String idpmetadata;
    private String debugDir;
    private String ATTRIB_ASSERT_CACHE_DEFAULT = "<Attribute name=\""
            + "assertionCacheEnabled\">\n"
            + "            <Value>false</Value>\n"
            + "        </Attribute>";
    private String ATTRIB_ASSERT_CACHE_ENABLE = "<Attribute name=\""
            + "assertionCacheEnabled\">\n"
            + "            <Value>true</Value>\n"
            + "        </Attribute>";
    
    /**
     * Constructor SAMLV2AttributeQueryTests
     */
    public SAMLv2AssertionQueryTests() {
        super("SAMLv2AssertionQueryTests");
        idmc = new IDMCommon();
    }
    
    /**
     * Configures the SP and IDP load meta for the SAMLV2AttributeQueryTests
     * tests to execute
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        HtmlPage page;
        ArrayList list;
        try {
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + SAMLv2Common.fileseparator + "built"
                    + SAMLv2Common.fileseparator + "classes"
                    + SAMLv2Common.fileseparator;
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator
                    + "samlv2TestConfigData", configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator
                    + "SAMLv2AssertionQueryTests", configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            configDir = configMap.get(TestConstants.KEY_SP_CONFIG_DIR);
            debugDir = configDir + fileseparator
                    + configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI)
                    + fileseparator + "debug" + fileseparator + "Federation";
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
            if (FederationManager.getExitCode(fmSP.createIdentity(spWebClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), "User", list))
                    != 0) {
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
                    configMap.get(TestConstants.KEY_IDP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed");
                assert false;
            }
            //Federated SSO URL
            fedSSOURL = spurl + "/saml2/jsp/spSSOInit.jsp?metaAlias=" +
                    configMap.get(TestConstants.KEY_SP_METAALIAS) +
                    "&idpEntityID=" +
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME);
            //Set server to Debug mode
            String debugURL = spurl + "/Debug.jsp?category=" +
                    "Federation&level=3&do=true";
            log(Level.FINEST, "setup", "Debug URL:" + debugURL);
            HtmlPage aipage = (HtmlPage)spWebClient.getPage(debugURL);
            assertionCacheSetup();
        } catch(Exception e) {
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
     * Enable Assertion cache
     */
    public void assertionCacheSetup()
    throws Exception {
        entering("assertionCacheSetup", null);
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
                log(Level.SEVERE, "setup",
                        "exportEntity famadm command failed");
                assert false;
            }
            spmetadata = MultiProtocolCommon.getExtMetadataFromPage(spmetaPage);
            String spmetadataMod = spmetadata.replaceAll(
                    ATTRIB_ASSERT_CACHE_DEFAULT, ATTRIB_ASSERT_CACHE_ENABLE);
            log(Level.FINEST, "assertionCacheSetup", "Modified" +
                    " metadata:" + spmetadataMod);
            if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.FINEST, "assertionCacheSetup", "Deletion of " +
                        "Extended entity failed");
                log(Level.FINEST, "assertionCacheSetup", "deleteEntity" +
                        " famadm command failed");
                assert(false);
            }
            if (FederationManager.getExitCode(spfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "",
                    spmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "assertionCacheSetup", "Failed to" +
                        " import extended metadata");
                log(Level.SEVERE, "assertionCacheSetup", "importEntity" +
                        " famadm command failed");
                assert(false);
            }
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            HtmlPage idpmetaPage = idpfm.exportEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    false, false, true, "saml2");
            if (FederationManager.getExitCode(idpmetaPage) != 0) {
                log(Level.SEVERE, "setup", "exportEntity famadm command" +
                        " failed");
                assert false;
            }
            idpmetadata =
                    MultiProtocolCommon.getExtMetadataFromPage(idpmetaPage);
            String idpmetadataMod = idpmetadata.replaceAll(
                    ATTRIB_ASSERT_CACHE_DEFAULT, ATTRIB_ASSERT_CACHE_ENABLE);
            log(Level.FINEST, "autoFedTransientUserSetup", "Modified IDP" +
                    " metadata:" + idpmetadataMod);
            
            if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    true, "saml2")) != 0) {
                log(Level.SEVERE, "assertionCacheSetup", "Deletion of" +
                        " idp Extended entity failed");
                log(Level.SEVERE, "assertionCacheSetup", "deleteEntity" +
                        " famadm command failed");
                assert(false);
            }
            if (FederationManager.getExitCode(idpfm.importEntity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "",
                    idpmetadataMod, "", "saml2")) != 0) {
                log(Level.SEVERE, "assertionCacheSetup", "Failed to" +
                        " import idp extended metadata");
                log(Level.SEVERE, "assertionCacheSetup", "importEntity" +
                        " famadm command failed");
                assert(false);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "assertionCacheSetup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
        exiting("assertionCacheSetup");
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
     * Execute the AssertionQuery tests
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void assertionQueryTest()
    throws Exception {
        
        Map attrMap = new HashMap();
        Map nidattrMap = new HashMap();
        Iterator attIterator;
        String testName = "assertionQueryTest";
        try {
            //Federate the users
            xmlfile = baseDir + testName + ".xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact",
                    false, false);
            log(Level.FINEST, "attributeQueryTest", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            wpage = task1.execute(webClient);
            if(!wpage.getWebResponse().getContentAsString().contains(ssopage)) {
                log(Level.SEVERE, "assertionQueryTest", "Couldn't " +
                        "federate users");
                assert false;
            }
            String astQuery = spurl + "/aIDRequest.jsp?path=" + debugDir;
            URL aidurl = new URL(astQuery);
            HtmlPage aipage = (HtmlPage)webClient.getPage(aidurl);
            if(!aipage.getWebResponse().getContentAsString().contains("ID=")) {
                log(Level.SEVERE, "assertionQueryTest",
                        "AssertionID Request Failed");
                assert false;
            }
            Thread.sleep(5000); //sleep for response
            String subS = "ID=";
            String subL = "$ID";
            String asserId = null;
            String aidLine = aipage.getWebResponse().getContentAsString();
            int i = aidLine.indexOf(subS) + 3;
            int j = aidLine.indexOf(subL);
            asserId = aidLine.substring(i,j);
            // Query and get the response
            String asstQueryURL = spurl + "/aIDReqTest.jsp?aID=" + asserId +
                    "&spMetaAlias=" +
                    configMap.get(TestConstants.KEY_SP_METAALIAS) +
                    "&idpEntityID=" +
                    configMap.get(TestConstants.KEY_IDP_ENTITY_NAME);
            URL aurl = new URL(asstQueryURL);
            HtmlPage page = (HtmlPage)webClient.getPage(aurl);
            if(!page.getWebResponse().getContentAsString().contains(result)) {
                log(Level.SEVERE, "assertionQueryTest", "Couldn't " +
                        "Terminate");
                assert false;
            }
            String resultPage = page.asXml();
            String noexpect = "No assertion found";
            if (resultPage.indexOf(noexpect) != -1) {
                log(Level.SEVERE, "assertionQueryTest", "The expected " +
                        "result did " + "NOT match with the output");
                log(Level.SEVERE, "assertionQueryTest", "The result is" +
                        resultPage);
                assert false;
            } else if (resultPage.indexOf(asserId) == -1) {
                log(Level.SEVERE, "assertionQueryTest", "The expected " +
                        "result did " + "No AssertionID found");
                assert false;
            }
        } catch (Exception e){
            log(Level.SEVERE, "assertionQueryTest", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Clean up and deleted the created users for this test
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
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
                    list , "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command" +
                        " failed");
                assert false;
            }
            //set the debug back to error
            String debugeURL = spurl + "/Debug.jsp?category=" +
                    "Federation&level=1&do=true";
            HtmlPage aipage = (HtmlPage)webcClient.getPage(debugeURL);
            consoleLogin(webcClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            
            fmIDP = new FederationManager(idpurl);
            list.clear();
            list.add(configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webcClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    list, "User")) != 0) {
                log(Level.SEVERE, "setup", "deleteIdentity famadm command " +
                        "failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl + "/UI/Logout");
            consoleLogout(webClient, idpurl + "/UI/Logout");
        }
    }
}

