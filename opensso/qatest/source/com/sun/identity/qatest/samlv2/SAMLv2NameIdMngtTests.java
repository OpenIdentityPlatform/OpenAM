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
 * $Id: SAMLv2NameIdMngtTests.java,v 1.6 2009/03/25 19:42:48 mrudulahg Exp $
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
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class to test the new SAMLV2
 * NameIdManagementProfile
 */
public class SAMLv2NameIdMngtTests extends TestCommon {
    
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
    private String testName;
    private String termInit;
    private String termBind;
    private String newIdInit;
    private String newIdBind;
    private String spuser;
    private String spuserpwd;
    private String idpuser;
    private String idpuserpwd;
    private String ssopage;
    private String mnipage;
    
    /**
     * Creates new Instance of SAMLV2NameIdMngtTests
     */
    public  SAMLv2NameIdMngtTests() {
        super("SAMLv2NameIdMngtTests");
        idmc = new IDMCommon();
    }
    
    /**
     * Configures the SP and IDP load meta for the nameIdManagement tests to
     * execute
     * @param Group name
     * @param profiletest name
     * @param termination initiation
     * @param termination binding
     * @param new Id initiator
     * @param new Id binding
     */
    @Parameters({"groupName", "ptestName", "tinitor", "tbind", "nidInitor", "nidBind"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String strGroupName, String ptestName, String tinitor,
            String tbind, String nidInitor, String nidBind )
            throws Exception {
        HtmlPage page;
        ArrayList list;
        try {
            testName = ptestName;
            termInit = tinitor;
            termBind = tbind;
            newIdInit = nidInitor;
            newIdBind = nidBind;
            ResourceBundle rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            baseDir = getBaseDir() + SAMLv2Common.fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + SAMLv2Common.fileseparator + "built"
                    + SAMLv2Common.fileseparator + "classes"
                    + SAMLv2Common.fileseparator;
            configMap = new HashMap<String, String>();
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestData",
                    configMap);
            SAMLv2Common.getEntriesFromResourceBundle("samlv2" + fileseparator +
                    "samlv2TestConfigData",
                    configMap);
            // assign SP and IDP user to be unique for each test for federation
            spuser = "SP" + testName;
            spuserpwd = "SP" + testName;
            idpuser = "IDP" + testName;
            idpuserpwd = "IDP" + testName;
            configMap.put(TestConstants.KEY_SP_USER, spuser);
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, spuserpwd);
            configMap.put(TestConstants.KEY_IDP_USER, idpuser);
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, idpuserpwd);
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
     * Execute the nameIdmanagement tests for the given scenarios of
     * termination request from SP or IDP and newID request from SP or IDP
     * for SOAP and HTTP
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void nameIdMgntProfileTest()
    throws Exception {
        
        Map attrMap = new HashMap();
        Map nidattrMap = new HashMap();
        Iterator attIterator;
        String fednameidkey = null;
        String fednameinfo = null;
        String fednameidkeyNew = null;
        String fednameinfoNew = null;
        String nidnamekey = null;
        String nidnameinfo = null;
        try {
            log(Level.FINE, "nameIdMgntProfileTest", "Running: " + testName );
            Reporter.log("Test Description: This test will run to make sure " +
                    " Termination with: " + termInit +
                    " Termination binding: "  + termBind + " and " +
                    " NewID Requestor: " + newIdInit +
                    " NewID binding:  " + newIdBind + " will work fine");
            ssopage = configMap.get(TestConstants.KEY_SSO_RESULT);
            mnipage = configMap.get(TestConstants.KEY_TERMINATE_RESULT);
            //Federate the users
            xmlfile = baseDir + testName + ".xml";
            SAMLv2Common.getxmlSPInitSSO(xmlfile, configMap, "artifact",
                    false, false);
            log(Level.FINEST, "nameIdMgntProfileTest", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            wpage = task1.execute(webClient);
            if(!wpage.getWebResponse().getContentAsString().contains(ssopage)) {
                log(Level.SEVERE, "nameIdMgntProfileTest", "Couldn't " +
                        "federate users");
                assert false;
            }
            Thread.sleep(10000);
            attrMap = idmc.getIdentityAttributes(spuser, realm);
            Set kattrSet = (Set) attrMap.get("sun-fm-saml2-nameid-infokey");
            for (Iterator itr = kattrSet.iterator(); itr.hasNext();) {
                fednameidkey = (String) itr.next();
            }
            Set iattrSet = (Set) attrMap.get("sun-fm-saml2-nameid-info");
            for (Iterator itr = iattrSet.iterator(); itr.hasNext();) {
                fednameinfo = (String) itr.next();
            }
            log(Level.FINEST, "nameIdMgntProfileTest", "FIRSTSSO " + fednameidkey);
            log(Level.FINEST, "nameIdMgntProfileTest", "FIRSTSSO" + fednameinfo);
            String sphttp = SAMLv2Common.getTerminateURL(termInit, termBind,
                    configMap);
            Thread.sleep(10000);
            // Terminate
            URL turl = new URL(sphttp);
            HtmlPage page = (HtmlPage)webClient.getPage(turl);
            if(!page.getWebResponse().getContentAsString().contains(mnipage)) {
                log(Level.SEVERE, "nameIdMgntProfileTest", "Couldn't " +
                        "Terminate");
                assert false;
            }
            Thread.sleep(35000);
            attrMap = idmc.getIdentityAttributes(spuser, realm);
            if (attrMap.containsKey("sun-fm-saml2-nameid-infokey")) {
                Set kattrSetSecond = (Set) attrMap.get("sun-fm-saml2-nameid-infokey");
                for (Iterator itr = kattrSetSecond.iterator(); itr.hasNext();) {
                    fednameidkeyNew = (String) itr.next();
                }
            }
            if (attrMap.containsKey("sun-fm-saml2-nameid-info")) {
            Set iattrSetNew = (Set) attrMap.get("sun-fm-saml2-nameid-info");
                for (Iterator itr = iattrSetNew.iterator(); itr.hasNext();) {
                    fednameinfoNew = (String) itr.next();
                }
            }
            log(Level.FINEST, "nameIdMgntProfileTest", "After termintation " +
                    "fednameidkey: " + fednameidkeyNew);
            log(Level.FINEST, "nameIdMgntProfileTest", "After termintation " +
                    "fednameinfoNew: " + fednameinfoNew);

            // Single Signon
            log(Level.FINEST, "nameIdMgntProfileTest", "Before SSO SP page : " +
                    webClient.getPage(spurl).getWebResponse().
                    getContentAsString());
            log(Level.FINEST, "nameIdMgntProfileTest", "Before SSO IDP page: " +
                    webClient.getPage(idpurl).getWebResponse().
                    getContentAsString());
            URL surl = new URL(fedSSOURL);
            HtmlPage spage = (HtmlPage)webClient.getPage(surl);
            if(!spage.getWebResponse().getContentAsString().contains(ssopage)) {
                log(Level.SEVERE, "nameIdMgntProfileTest", "Failed" +
                        "SSO after termination");
                log(Level.FINE, "nameIdMgntProfileTest", "Page received: " +
                        spage.getWebResponse().getContentAsString());
                assert false;
            }
            Thread.sleep(10000);
            //NewID Request
            String newIdURL = SAMLv2Common.getNewIDRequestURL(newIdInit,
                    newIdBind, configMap);
            URL newidurl = new URL(newIdURL);
            HtmlPage npage = (HtmlPage)webClient.getPage(newidurl);
            if(!page.getWebResponse().getContentAsString().contains(mnipage)) {
                log(Level.SEVERE, "nameIdMgntProfileTest", "Failed" +
                        "getting the newId request");
                assert false;
            }
            Thread.sleep(15000);
            nidattrMap = idmc.getIdentityAttributes(spuser, realm);
            Set anitattrSet = (Set) nidattrMap.get("sun-fm-saml2-nameid-infokey");
            for (Iterator itr = anitattrSet.iterator(); itr.hasNext();) {
                nidnamekey = (String) itr.next();
            }
            Set aniptiattrSet = (Set) nidattrMap.get("sun-fm-saml2-nameid-info");
            for (Iterator itr = aniptiattrSet.iterator(); itr.hasNext();) {
                nidnameinfo = (String) itr.next();
            }
            log(Level.FINEST, "nameIdMgntProfileTest", "FINALSSO " + nidnamekey);
            log(Level.FINEST, "nameIdMgntProfileTest", "FINALSSO" + nidnameinfo);
            //Compare and validate
            if ((fednameidkey.equals(nidnamekey)) ||
                    (fednameinfo.equals(nidnameinfo))) {
                assert false;
            }
        } catch (Exception e){
            log(Level.SEVERE, "nameIdMgntProfileTest", e.getMessage());
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
            consoleLogout(webcClient, spurl + "/UI/Logout");
            consoleLogout(webcClient, idpurl + "/UI/Logout");
        }
    }
}
