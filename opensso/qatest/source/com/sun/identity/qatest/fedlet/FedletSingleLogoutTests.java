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
 * $Id: FedletSingleLogoutTests.java,v 1.1 2009/08/20 17:06:15 vimal_67 Exp $
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
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.FedletCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests Http-Atrifact and HTTP-POST profiles. It tests the following
 * testcases:-
 * Run Identity Provider initiated Single Logout using SOAP binding
 * Run Identity Provider initiated Single Logout using HTTP-Redirect binding
 * Run Identity Provider initiated Single Logout using HTTP-POST binding
 * Run Fedlet initiated Single Logout using SOAP binding
 * Run Fedlet initiated Single Logout using HTTP-Redirect binding
 * Run Fedlet initiated Single Logout using HTTP-POST binding
 */
public class FedletSingleLogoutTests extends TestCommon {
    
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
    private String fedletidpurl;

    /**
     * This is constructor for this class.
     */
    public FedletSingleLogoutTests() {
        super("FedletSingleLogoutTests");
    }

    /**
     * This setup method creates the required users.
     */
    @Parameters({"ptestName", "pAttribute"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String ptestName, String pAttribute)
    throws Exception {
        ArrayList list;
        try {
            log(Level.FINEST, "setup", "Entering");
            configMap = new HashMap<String, String>();
            testAttribute = pAttribute;

            //Upload global properties file in configMap
            FedletCommon.getEntriesFromResourceBundle("AMConfig", configMap);
            FedletCommon.getEntriesFromResourceBundle("fedlet" + fileseparator +
                    "FedletTests", configMap);
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
     * Run Identity Provider initiated Single Logout using SOAP binding when
     * it is Identity Provider initiated Single Sign-On using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_IDPSLOSOAP()
    throws Exception {
        entering("testIDPSSOHTTPPost_IDPSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP",
                    "Running: testIDPSSOHTTPPost_IDPSLOSOAP");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", "Attribute: " +
                    testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP",
                    "Running: testIDPSSOHTTPPost_IDPSLOSOAP");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using SOAP binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOSOAP", " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPPost_IDPSLOSOAP",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_IDPSLOSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_IDPSLOSOAP");
    }

    /**
     * Run Identity Provider initiated Single Logout using Http-Redirect
     * binding when it is Identity Provider initiated Single Sign-On
     * using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_IDPSLOHTTPRedirect()
    throws Exception {
        entering("testIDPSSOHTTPPost_IDPSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPPost_IDPSLOHTTPRedirect");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPPost_IDPSLOHTTPRedirect");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider initiated " +
                    "Single Single Logout using HTTP-Redirect binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, 
                                "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_IDPSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_IDPSLOHTTPRedirect");
    }

    /**
     * Run Identity Provider initiated Single Logout using HTTP-POST binding
     * when it is Identity Provider initiated Single Sign-On using
     * HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_IDPSLOHTTPPost()
    throws Exception {
        entering("testIDPSSOHTTPPost_IDPSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    "Running: testIDPSSOHTTPPost_IDPSLOHTTPPost");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    "Running: testIDPSSOHTTPPost_IDPSLOHTTPPost");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using HTTP-POST binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_IDPSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_IDPSLOHTTPPost");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using SOAP binding when it is
     * Identity Provider initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_FedletSLOSOAP()
    throws Exception {
        entering("testIDPSSOHTTPPost_FedletSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP",
                    "Running: testIDPSSOHTTPPost_FedletSLOSOAP");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP", "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP",
                    "Running: testIDPSSOHTTPPost_FedletSLOSOAP");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using SOAP binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPPost_FedletSLOSOAP",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_FedletSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_FedletSLOSOAP");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-Redirect binding when
     * it is Identity Provider initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_FedletSLOHTTPRedirect()
    throws Exception {
        entering("testIDPSSOHTTPPost_FedletSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPPost_FedletSLOHTTPRedirect");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPPost_FedletSLOHTTPRedirect");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-Redirect binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, 
                                "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_FedletSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_FedletSLOHTTPRedirect");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-POST binding when
     * it is Identity Provider initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPPost_FedletSLOHTTPPost()
    throws Exception {
        entering("testIDPSSOHTTPPost_FedletSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    "Running: testIDPSSOHTTPPost_FedletSLOHTTPPost");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost", str);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    "Running: testIDPSSOHTTPPost_FedletSLOHTTPPost");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-POST binding");
            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPPost_FedletSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPPost_FedletSLOHTTPPost");
    }

    /**
     * Run Identity Provider initiated Single Logout using SOAP binding when
     * it is Identity Provider initiated Single Sign-On 
     * using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_IDPSLOSOAP()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_IDPSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOSOAP");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOSOAP");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using SOAP binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_IDPSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_IDPSLOSOAP");
    }

    /**
     * Run Identity Provider initiated Single Logout using Http-Redirect
     * binding when it is Identity Provider initiated Single Sign-On
     * using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider initiated " +
                    "Single Single Logout using HTTP-Redirect binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_IDPSLOHTTPRedirect");
    }

    /**
     * Run Identity Provider initiated Single Logout using HTTP-POST binding
     * when it is Identity Provider initiated Single Sign-On using
     * HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_IDPSLOHTTPPost()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_IDPSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOHTTPPost");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Running: testIDPSSOHTTPArtifact_IDPSLOHTTPPost");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using HTTP-POST binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_IDPSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_IDPSLOHTTPPost");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using SOAP binding when it is
     * Identity Provider initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_FedletSLOSOAP()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_FedletSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOSOAP");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOSOAP");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using SOAP binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_FedletSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_FedletSLOSOAP");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-Redirect binding when
     * it is Identity Provider initiated Single Sign-On using
     * HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-Redirect binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_FedletSLOHTTPRedirect");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-POST binding when it is
     * Identity Provider initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testIDPSSOHTTPArtifact_FedletSLOHTTPPost()
    throws Exception {
        entering("testIDPSSOHTTPArtifact_FedletSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOHTTPPost");
            String str = "Run Identity Provider initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost", str);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Running: testIDPSSOHTTPArtifact_FedletSLOHTTPPost");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-POST binding");
            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testIDPSSOHTTPArtifact_FedletSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testIDPSSOHTTPArtifact_FedletSLOHTTPPost");
    }


    /**
     * Run Identity Provider initiated Single Logout using SOAP binding when
     * it is Fedlet initiated Single Sign-On using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_IDPSLOSOAP()
    throws Exception {
        entering("testFedletSSOHTTPPost_IDPSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP",
                    "Running: testFedletSSOHTTPPost_IDPSLOSOAP");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP", "SSO Run " +
                    xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP", "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP",
                    "Running: testFedletSSOHTTPPost_IDPSLOSOAP");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using SOAP binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testFedletSSOHTTPPost_IDPSLOSOAP",
                            "Couldn't find attribute value " +
                            multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_IDPSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_IDPSLOSOAP");
    }

    /**
     * Run Identity Provider initiated Single Logout using Http-Redirect
     * binding when it is Fedlet initiated Single Sign-On
     * using HTTP POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_IDPSLOHTTPRedirect()
    throws Exception {
        entering("testFedletSSOHTTPPost_IDPSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPPost_IDPSLOHTTPRedirect");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPPost_IDPSLOHTTPRedirect");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider initiated " +
                    "Single Single Logout using HTTP-Redirect binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_IDPSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_IDPSLOHTTPRedirect");
    }

    /**
     * Run Identity Provider initiated Single Logout using HTTP-POST binding
     * when it is Fedlet initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_IDPSLOHTTPPost()
    throws Exception {
        entering("testFedletSSOHTTPPost_IDPSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    "Running: testFedletSSOHTTPPost_IDPSLOHTTPPost");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    "Running: testFedletSSOHTTPPost_IDPSLOHTTPPost");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using HTTP-POST binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, 
                                "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_IDPSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_IDPSLOHTTPPost");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using SOAP binding when it is
     * Fedlet(SP) initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_FedletSLOSOAP()
    throws Exception {
        entering("testFedletSSOHTTPPost_FedletSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    "Running: testFedletSSOHTTPPost_FedletSLOSOAP");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    "Running: testFedletSSOHTTPPost_FedletSLOSOAP");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using SOAP binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, "testFedletSSOHTTPPost_FedletSLOSOAP",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_FedletSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_FedletSLOSOAP");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-Redirect binding when
     * it is Fedlet(SP) initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_FedletSLOHTTPRedirect()
    throws Exception {
        entering("testFedletSSOHTTPPost_FedletSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPPost_FedletSLOHTTPRedirect");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    str);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPPost_FedletSLOHTTPRedirect");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-Redirect binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_FedletSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_FedletSLOHTTPRedirect");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-POST binding when
     * it is Fedlet(SP) initiated Single Sign-On using HTTP-POST binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPPost_FedletSLOHTTPPost()
    throws Exception {
        entering("testFedletSSOHTTPPost_FedletSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    "Running: testFedletSSOHTTPPost_FedletSLOHTTPPost");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP POST binding";
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost", str);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttppost.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    "Running: testFedletSSOHTTPPost_FedletSLOHTTPPost");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-POST binding");
            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPPost_FedletSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPPost_FedletSLOHTTPPost");
    }


    /**
     * Run Identity Provider initiated Single Logout using SOAP binding when
     * it is Fedlet(SP) initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_IDPSLOSOAP()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_IDPSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOSOAP");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP", str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOSOAP");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using SOAP binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP", strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE, 
                                "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                            "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_IDPSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_IDPSLOSOAP");
    }

    /**
     * Run Identity Provider initiated Single Logout using Http-Redirect
     * binding when it is Fedlet(SP) initiated Single Sign-On
     * using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider initiated " +
                    "Single Single Logout using HTTP-Redirect binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_IDPSLOHTTPRedirect");
    }

    /**
     * Run Identity Provider initiated Single Logout using HTTP-POST binding
     * when it is Fedlet(SP) initiated Single Sign-On using
     * HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_IDPSLOHTTPPost()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_IDPSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOHTTPPost");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost", str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    "Running: testFedletSSOHTTPArtifact_IDPSLOHTTPPost");
            String strlog = "Run Identity Provider initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Identity Provider " +
                    "initiated Single Single Logout using HTTP-POST binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_IDPSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_IDPSLOHTTPPost");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using SOAP binding when it is
     * Fedlet(SP) initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_FedletSLOSOAP()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_FedletSLOSOAP", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOSOAP");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP", str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    "SSO Page: " +
                    page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOSOAP");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using SOAP binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using SOAP binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_FedletSLOSOAP",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_FedletSLOSOAP");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-Redirect binding when
     * it is Fedlet(SP) initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-Redirect binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-Redirect binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_" +
                                "FedletSLOHTTPRedirect",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_FedletSLOHTTPRedirect");
    }

    /**
     * Run Fedlet(SP) initiated Single Logout using HTTP-POST binding when it is
     * Fedlet(SP) initiated Single Sign-On using HTTP Artifact binding
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void testFedletSSOHTTPArtifact_FedletSLOHTTPPost()
    throws Exception {
        entering("testFedletSSOHTTPArtifact_FedletSLOHTTPPost", null);
        try {

            // Single Sign On
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOHTTPPost");
            String str = "Run Fedlet (SP) initiated Single Sign-On " +
                    "using HTTP Artifact binding";
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    str);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Attribute: " + testAttribute);
            getWebClient();
            xmlfile = baseDir + "testidpssohttpartifact.xml";
            String urlStr = FedletCommon.getAnchors(fedletPage, str);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, urlStr);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    "SSO Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    "SSO Page: " + page1.getWebResponse().getContentAsString());

            // Single LogOut
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    "Running: testFedletSSOHTTPArtifact_FedletSLOHTTPPost");
            String strlog = "Run Fedlet initiated Single Logout " +
                    "using HTTP-POST binding";
            Reporter.log("Test Description: This testcase tests the " +
                    "single logout in Fedlet(SP) initiated Single Logout " +
                    "using HTTP-POST binding");
            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    strlog);

            HtmlAnchor anchor = page1.getFirstAnchorByText(strlog);
            page1 = (HtmlPage) anchor.click();

            log(Level.FINEST, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    " SLO Page: " +
                    page1.getWebResponse().getContentAsString());
            page1 = singleLogout(fedletPage, str);

            // checking multivalues of a attribute
            Iterator iter = idpattrmultiVal.iterator();
            while (iter.hasNext()) {
                String multiVal = (String) iter.next();
                    if (!page1.getWebResponse().getContentAsString().
                            contains(multiVal)) {
                        log(Level.SEVERE,
                                "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                                "Couldn't find attribute value " + multiVal);
                        assert false;
                    }
            }
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "testFedletSSOHTTPArtifact_FedletSLOHTTPPost",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("testFedletSSOHTTPArtifact_FedletSLOHTTPPost");
    }

    /**
     * Cleanup method deletes all the users which were created in setup
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
            consoleLogout(webClient, fedletidpurl + "/UI/Logout");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }

    /**
     * @param string is the string passed whether it is Fedlet(SP) or IDP
     * initated SOAP, HTTP-Redirect or HTTP-POST profile
     */
    public HtmlPage singleLogout(HtmlPage page, String urlStr)
            throws Exception {
        try {
            getWebClient();
            String str = "";
            log(Level.FINEST, "singleLogout", "Running: singleLogout");
            log(Level.FINEST, "singleLogout", "Page: " +
                    page.getWebResponse().getContentAsString());

            log(Level.FINEST, "singleLogout", "Single Sign On Link: " + urlStr);
            getWebClient();
            xmlfile = baseDir + "singlesignon.xml";
            String st = FedletCommon.getAnchors(page, urlStr);
            FedletCommon.getxmlFedletSSO(xmlfile, configMap, st);
            log(Level.FINEST, "singleLogout", "Run " + xmlfile);
            task1 = new DefaultTaskHandler(xmlfile);
            page1 = task1.execute(webClient);
            log(Level.FINEST, "singleLogout", "Page: " +
                    page1.getWebResponse().getContentAsString());

            return page1;
        } catch (Exception e) {
            log(Level.FINEST, "singleLogout", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }   
    

}
