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
 * $Id: FedletAttributeQueryTests.java,v 1.1 2009/08/20 17:05:20 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.fedlet;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.FedletCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.owasp.esapi.codecs.HTMLEntityCodec;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the Fedlet Attribute Query testcases by going through
 * the following links:-
 * Run Identity Provider initiated Single Sign-On using HTTP Artifact binding
 * Run Identity Provider initiated Single Sign-On using HTTP POST binding
 * Run Fedlet (SP) initiated Single Sign-On using HTTP Artifact binding
 * Run Fedlet (SP) initiated Single Sign-On using HTTP POST binding
 */
public class FedletAttributeQueryTests extends TestCommon {

    public WebClient webClient;    
    private Map<String, String> configMap;
    private String baseDir ;
    private String xmlfile;
    private DefaultTaskHandler task1;
    private FederationManager fmfedletIDP;
    private String fedletURL;
    private HtmlPage page1;
    private HtmlPage fedletPage;
    private String testAttribute;
    private ArrayList idpattrmultiVal;
    private ResourceBundle rbsa;
    private ArrayList idproles;
    private ArrayList idpuserSessionAttr;
    private HTMLEntityCodec decoder;
    private String fedletidpurl;
    
    /**
     * This is constructor for this class.
     */
    public FedletAttributeQueryTests() {
        super("FedletAttributeQueryTests");
    }

    /**
     * This setup method creates the required users and roles.
     */
    @Parameters({"ptestName", "pAttribute"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String ptestName, String pAttribute)
    throws Exception {
        ArrayList list;
        try {
            log(Level.FINEST, "setup", "Entering");
            idproles = new ArrayList();
            idpuserSessionAttr = new ArrayList();
            configMap = new HashMap<String, String>();
            testAttribute = pAttribute;
            decoder = new HTMLEntityCodec();

            // Upload global properties file in configMap
            FedletCommon.getEntriesFromResourceBundle("AMConfig", configMap);
            FedletCommon.getEntriesFromResourceBundle("fedlet" + fileseparator +
                    "FedletTests", configMap);
            rbsa = ResourceBundle.getBundle("fedlet" + fileseparator +
                    "FedletAttributeQueryTests");
            log(Level.FINEST, "setup", "ConfigMap is : " + configMap);

            baseDir = getBaseDir() + fileseparator
                    + configMap.get(TestConstants.KEY_ATT_SERVER_NAME)
                    + fileseparator + "built" + fileseparator + "classes"
                    + fileseparator;
            fedletURL = configMap.get(TestConstants.KEY_FEDLET_WAR_LOCATION);
            log(Level.FINEST, "setup", "Fedlet URL is : " + fedletURL);
            fedletPage = (HtmlPage) webClient.getPage(fedletURL);
            log(Level.FINEST, "setup", "Fedlet Index Page: " +
                    fedletPage.getWebResponse().getContentAsString());

            // Create idp user
            fedletidpurl = configMap.get(TestConstants.KEY_AMC_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_AMC_HOST) + ":"
                    + configMap.get(TestConstants.KEY_AMC_PORT)
                    + configMap.get(TestConstants.KEY_AMC_URI);
            consoleLogin(webClient, fedletidpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD));

            fmfedletIDP = new FederationManager(fedletidpurl);
            idpattrmultiVal = new ArrayList();
            list = new ArrayList();
            String idpuserMultiAttr = configMap.get(
                    TestConstants.KEY_IDP_USER_MULTIATTRIBUTES);
            String idpuserAttrQ = rbsa.getString("idpuser" + "." +
                    "multivalueattributes");
            idpuserMultiAttr = idpuserMultiAttr + "," + idpuserAttrQ;
            list = (ArrayList) parseStringToList(idpuserMultiAttr, ",", "&");
            list.add("sn=" + configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");

            // grabbing idp attribute multivalues list
            Iterator iter = list.iterator();
            while(iter.hasNext()) {
                String str = (String) iter.next();
                int index = str.indexOf("=");
                if (index != -1) {
                    String attrName = str.substring(0, index).trim();
                    String strVal = str.substring(index + 1).trim();

                    // checking nsrole attribute
                    if (attrName.equalsIgnoreCase("nsrole")) {
                        int id = strVal.indexOf("=");
                        if (id != -1) {
                            String rVal = strVal.substring(id + 1).trim();
                            log(Level.FINEST, "setup", "idp roles:" + rVal);
                            idproles.add(rVal);
                        }
                    }

                    // adding multivalues to list
                    if (attrName.equalsIgnoreCase(testAttribute)) {
                        idpattrmultiVal.add(strVal);
                    }
                }
            }
            if (FederationManager.getExitCode(
                    fmfedletIDP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER),
                    "User", list)) != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command " +
                        "failed");
                assert false;
            }

            // create roles if nsrole attribute is found 
            // and assign the role to user
            Iterator iterr = idproles.iterator();
            while (iterr.hasNext()) {
                String strr = (String) iterr.next();
                List listr = new ArrayList();
                listr.add("cn=" + strr);
                listr.add("userpassword=" + "secret12");
                if (FederationManager.getExitCode(fmfedletIDP.createIdentity(
                        webClient, configMap.get(
                        TestConstants.KEY_ATT_EXECUTION_REALM), strr, "role",
                        listr)) != 0) {
                    log(Level.SEVERE, "setup", "createIdentity famadm " +
                            "command" + " failed");
                    assert false;
                }
                fmfedletIDP.addMember(webClient, configMap.get(
                        TestConstants.KEY_ATT_EXECUTION_REALM),
                        configMap.get(TestConstants.KEY_FEDLETIDP_USER),
                        "user", strr, "role");
            }

            // adding session service to the user
            idpuserSessionAttr.add("iplanet-am-session-quota-limit=4");
            idpattrmultiVal.add("4");
            fmfedletIDP.addSvcIdentity(webClient, 
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER),
                    "user", "iPlanetAMSessionService", idpuserSessionAttr);                       

            // grabbing entrydn value of the fedlet idp user
            List listedn = new ArrayList();
            listedn.add("entrydn");
            page1 = fmfedletIDP.getIdentity(webClient,
                    configMap.get(TestConstants.KEY_ATT_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER),
                    "User", listedn);
            log(Level.FINEST, "setup", "Fedlet IDP User EntryDN Page: " +
                    page1.getWebResponse().getContentAsString());
            String str = page1.getWebResponse().getContentAsString();
            str = decoder.decode(str);
            String val = "";
            int index = str.indexOf("entrydn");
            if (index != -1) {
                String attrVal = str.substring(index + 8).trim();
                int subin = attrVal.indexOf("\n");
                if (subin != -1) {
                    val = attrVal.substring(0, subin).trim();
                }
                log(Level.FINEST, "setup", "Fedlet IDP User EntryDN " +
                        "Attribute Value: " + val);
                idpattrmultiVal.add(val);
            }            

            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }

    /**
     * Creates the webClient which will be used for rest of the tests.
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
     * Run Attribute Query when it is Identity Provider initiated Single
     * Sign-On using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_FedletAttrQuery()
    throws Exception {
        entering("testIDPSSOHTTPPost_FedletAttrQuery", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery",
                    "Running: testIDPSSOHTTPPost_FedletAttrQuery");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery",
                    "Attribute: " + testAttribute);            
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Fedlet Attribute Query
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery",
                    "Running: testIDPSSOHTTPPost_FedletAttrQuery");
            String strattrq = "Fedlet Attribute Query";
            Reporter.log("Test Description: This testcase tests the " +
                    "Fedlet Attribute Query when it is Identity Provider " +
                    "initiated Single Sign-On using HTTP POST binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletAttrQuery", strattrq);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strattrq);
            page1 = (HtmlPage) anchor.click();

            page1 = attributeQuery(page1, "EntryDN", "SessionQuota", "nsrole");

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPPost_FedletAttrQuery",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_FedletAttrQuery",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_FedletAttrQuery");
    }

    /**
     * Run Attribute Query when it is Identity Provider initiated Single
     * Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_FedletAttrQuery()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_FedletAttrQuery", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    "Running: testIDPSSOHTTPArtifact_FedletAttrQuery");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    "Attribute: " + testAttribute);            
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Fedlet Attribute Query
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    "Running: testIDPSSOHTTPArtifact_FedletAttrQuery");
            String strattrq = "Fedlet Attribute Query";
            Reporter.log("Test Description: This testcase tests the " +
                    "Fedlet Attribute Query when it is Identity Provider " +
                    "initiated Single Sign-On using HTTP Artifact binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    strattrq);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strattrq);
            page1 = (HtmlPage) anchor.click();

            page1 = attributeQuery(page1, "EntryDN", "SessionQuota", "nsrole");

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_FedletAttrQuery",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_FedletAttrQuery",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_FedletAttrQuery");
    }

    /**
     * Run Attribute Query when it is Fedlet(SP) initiated Single Sign-On
     * using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_FedletAttrQuery()
    throws Exception {
        entering("testFedletSSOHTTPPost_FedletAttrQuery", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    "Running: testFedletSSOHTTPPost_FedletAttrQuery");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    "Attribute: " + testAttribute);            
            xmlfile = baseDir + "testfedletssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Fedlet Attribute Query
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    "Running: testFedletSSOHTTPPost_FedletAttrQuery");
            String strattrq = "Fedlet Attribute Query";
            Reporter.log("Test Description: This testcase tests the " +
                    "Fedlet Attribute Query when it is Fedlet (SP) " +
                    "initiated Single Sign-On using HTTP POST binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletAttrQuery",
                    strattrq);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strattrq);
            page1 = (HtmlPage) anchor.click();

            page1 = attributeQuery(page1, "EntryDN", "SessionQuota", "nsrole");

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPPost_FedletAttrQuery",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_FedletAttrQuery",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_FedletAttrQuery");
    }

    /**
     * Run Attribute Query when it is Fedlet(SP) initiated Single Sign-On
     * using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_FedletAttrQuery()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_FedletAttrQuery", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    "Running: testFedletSSOHTTPArtifact_FedletAttrQuery");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery", str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    "Attribute: " + testAttribute);            
            xmlfile = baseDir + "testfedletssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Fedlet Attribute Query
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    "Running: testFedletSSOHTTPArtifact_FedletAttrQuery");
            String strattrq = "Fedlet Attribute Query";
            Reporter.log("Test Description: This testcase tests the " +
                    "Fedlet Attribute Query when it is Fedlet (SP) " +
                    "initiated Single Sign-On using HTTP Artifact binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    strattrq);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strattrq);
            page1 = (HtmlPage) anchor.click();

            page1 = attributeQuery(page1, "EntryDN", "SessionQuota", "nsrole");

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_FedletAttrQuery",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_FedletAttrQuery",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_FedletAttrQuery");
    }

    /**
     * Cleanup method deletes all the users and roles which were
     * created in setup
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        try {
            getWebClient();

            // delete idp users
            fedletidpurl = configMap.get(TestConstants.KEY_AMC_PROTOCOL)
            + "://" + configMap.get(TestConstants.KEY_AMC_HOST) + ":"
                    + configMap.get(TestConstants.KEY_AMC_PORT)
                    + configMap.get(TestConstants.KEY_AMC_URI);
            consoleLogin(webClient, fedletidpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD));

            fmfedletIDP = new FederationManager(fedletidpurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            log(Level.FINEST, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_FEDLETIDP_USER));
            if (FederationManager.getExitCode(fmfedletIDP.deleteIdentities(
                    webClient, configMap.get(
                    TestConstants.KEY_ATT_EXECUTION_REALM),
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities " +
                        "famadm command failed");                
            }
            // delete roles if nsrole attribute is found
            Iterator iterr = idproles.iterator();
            while (iterr.hasNext()) {
                String strr = (String) iterr.next();
                List listr = new ArrayList();
                listr.add(strr);
                if (FederationManager.getExitCode(fmfedletIDP.deleteIdentities
                        (webClient, configMap.get(TestConstants.
                        KEY_ATT_EXECUTION_REALM), listr, "role")) != 0) {
                    log(Level.SEVERE, "cleanup", "deleteIdentity " +
                            "famadm command failed");                    
                }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }

    /**
     * @param page is the Attribute Query Page
     * @param attr1 is the first attribute parameter in the attribute query
     * @param attr2 is the second attribute parameter in the attribute query
     * @param attr3 is the third attribute parameter in the attribute query
     */
    public HtmlPage attributeQuery(HtmlPage page, String attr1, String attr2,
            String attr3) throws Exception {
        try {
            getWebClient();
            log(Level.FINEST, "attributeQuery", "Attribute Query Page: " +
                    page.getWebResponse().getContentAsString());
            HtmlForm form = (HtmlForm) page.getForms().get(0);
            HtmlInput attribute1 = form.getInputByName("attr1");
            attribute1.setValueAttribute(attr1);
            HtmlInput attribute2 = form.getInputByName("attr2");
            attribute2.setValueAttribute(attr2);
            HtmlInput attribute3 = form.getInputByName("attr3");
            attribute3.setValueAttribute(attr3);
            page = (HtmlPage) form.getInputByName("Submit").click();
            log(Level.FINEST, "attributeQuery",
                    "Fedlet Attribute Query Response Page: " +
                    page.getWebResponse().getContentAsString());

            return page;
        } catch (Exception e) {
            log(Level.FINEST, "attributeQuery", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

