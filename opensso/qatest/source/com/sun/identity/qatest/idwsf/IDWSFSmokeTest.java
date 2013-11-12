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
 * $Id: IDWSFSmokeTest.java,v 1.2 2009/02/14 00:58:08 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.idwsf;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDFFCommon;
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
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests IDWSF PP Modify & query.
 */
public class IDWSFSmokeTest extends IDFFCommon {
    
    private WebClient webClientTest;
    private FederationManager fmSP;
    private FederationManager fmIDP;
    private DefaultTaskHandler task;
    private Map<String, String> configMap;
    private String  baseDir;
    private String wscURL;
    private String xmlfile;
    private String spurl;
    private String idpurl;
    private String strAttribute;
    private String strAttrContainer;
    private String strAttrValue;
    private String testIndex;
    private String uuidIDPuser;

    /** Creates a new instance of IDWSFSmokeTest */
    public IDWSFSmokeTest() {
        super("IDWSFSmokeTest");
    }
    
    /**
     * Create the webClient 
     */
    private WebClient getWebClient()
    throws Exception {
        try {
            WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3);
            return webClient;
        } catch (Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * This is setup method. It creates required users for test
     * @param index
     * @throws Exception
     */
    @Parameters({"testIndex"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String index)
    throws Exception {
        Object[] params = {index};
        entering("setup", params);
        List<String> list;
        try {
            testIndex = index;
            baseDir = getTestBase();
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            configMap = getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestConfigData");
            configMap.putAll(getMapFromResourceBundle("idff" + fileseparator +
                    "idffTestData"));
            configMap.putAll(getMapFromResourceBundle("idwsf" + fileseparator +
                    "IDWSFSmokeTest"));
            log(Level.FINEST, "setup", "Map is " + configMap);
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_SP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_SP_PORT) +
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) +
                    "://" + configMap.get(TestConstants.KEY_IDP_HOST) + ":" +
                    configMap.get(TestConstants.KEY_IDP_PORT) +
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            wscURL = clientURL + "/wsc/index.jsp";
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        WebClient webClient = null;
        try {
            webClient = getWebClient();
            list = new ArrayList();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login", configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            fmIDP = new FederationManager(idpurl);

            configMap.put(TestConstants.KEY_SP_USER, configMap.get(
                    TestConstants.KEY_SP_USER) + testIndex);
            configMap.put(TestConstants.KEY_SP_USER_PASSWORD, configMap.get(
                    TestConstants.KEY_SP_USER_PASSWORD) + testIndex);
            list = new ArrayList();
            list.add("sn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_SP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmSP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_SP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            log(Level.FINE, "setup", "SP user created is " + list);
            
            configMap.put(TestConstants.KEY_IDP_USER, configMap.get(
                    TestConstants.KEY_IDP_USER) + testIndex);
            configMap.put(TestConstants.KEY_IDP_USER_PASSWORD, configMap.get(
                    TestConstants.KEY_IDP_USER_PASSWORD) + testIndex);
            // Create idp users
            list.clear();
            list.add("sn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("cn=" + configMap.get(TestConstants.KEY_IDP_USER));
            list.add("userpassword=" +
                    configMap.get(TestConstants.KEY_IDP_USER_PASSWORD));
            list.add("inetuserstatus=Active");
            if (FederationManager.getExitCode(fmIDP.createIdentity(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM),
                    configMap.get(TestConstants.KEY_IDP_USER), "User", list))
                    != 0) {
                log(Level.SEVERE, "setup", "createIdentity famadm command" +
                        " failed");
                assert false;
            }
            log(Level.FINE, "setup", "IDP user created is " + list);
            
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
     * This method will federate the SP & IDP users.
     * @param wc WebClient
     */
    public void federateUsers(WebClient wc) {
        entering("federateUsers", null);
        try {
            log(Level.FINE, "federateUsers", 
                    "Running: federateUsers");
            log(Level.FINE, "federateUsers", "Login to SP with " + 
                    configMap.get(TestConstants.KEY_SP_USER));
            consoleLogin(wc, spurl + "/UI/Login", 
                    configMap.get(TestConstants.KEY_SP_USER),
                    configMap.get(TestConstants.KEY_SP_USER_PASSWORD));
            xmlfile = baseDir + "/idwsf/federateUsers.xml";
            getxmlSPIDFFFederate(xmlfile, configMap, true);
            log(Level.FINE, "federateUsers", "Run " + xmlfile);
            task = new DefaultTaskHandler(xmlfile);
            HtmlPage page = task.execute(wc);
            log(Level.FINE, "federateUsers", "Users are federated " +
                    "successfully");
        } catch (Exception e) {
            log(Level.SEVERE, "federateUsers", e.getMessage());
            e.printStackTrace();
        }
        exiting("federateUsers");
    }
   
    /**
     * This method registers the resource offering
     * @param webClient
     * @return HtmlPagge The Resource Offering registered page is returned.
     * @throws Exception
     */
    public HtmlPage registerResourceOffering(WebClient webClient)
    throws Exception {
        entering("registerResourceOffering", null);
        try {
            HtmlPage wscpage = (HtmlPage)webClient.getPage(wscURL);
            HtmlForm form = wscpage.getFormByName("discomodify");
            HtmlInput txtidpUserDN = (HtmlInput)form.getInputByName("idpUserDN");
            txtidpUserDN.setValueAttribute(uuidIDPuser);
            wscpage = (HtmlPage)form.getInputByName("Submit").click();
            Thread.sleep(3000);
            HtmlForm discomodifyform = wscpage.getFormByName("discomodify");
            HtmlPage discomodifyPage = (HtmlPage)discomodifyform.getInputByName("Submit").click();
            if (discomodifyPage.getWebResponse().getContentAsString().
                    contains("OK")) {
                log(Level.FINE, "registerResourceOffering", "Register " +
                        "Resource Offering was successful");
            } else {
                log(Level.SEVERE, "registerResourceOffering", "Register " +
                        "Resource Offering was NOT successful");
                log(Level.FINE, "PPModify", "Register Resource Offering page : "
                        + discomodifyPage.getWebResponse().
                        getContentAsString());
                assert false;
            }
            return discomodifyPage;
        } catch (Exception e) {
            log(Level.SEVERE, "registerResourceOffering", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method will query Resource Offering starting from index.jsp
     * At the end it will return the HtmlPage of the Resource Offering query pg
     * @param webclient WebClient which holds SP & IDP user sessions.
     * @param wscURL index page of wsc sample.
     * @return HtmlPage
     * @throws Exception
     */
    public HtmlPage queryResourceOffering(WebClient webclient, String wscURL)
    throws Exception {
        entering("queryResourceOffering", null);
            HtmlPage wscindexpage = (HtmlPage)webclient.getPage(wscURL);
            HtmlForm formdiscoquerycall =
                    wscindexpage.getFormByName("discoquerycall");
            Thread.sleep(3000);
            HtmlPage discoquerypage = (HtmlPage)formdiscoquerycall.getInputByName("Submit").click();
            HtmlForm formdiscoquery = discoquerypage.getFormByName(
                    "discoquery");
            HtmlPage discoquerypage2 = (HtmlPage)formdiscoquery.getInputByName("Submit").click();
        return discoquerypage2;
    }

    /**
     * This test runs PP modify. It modifies the attribute value and checks the
     * status
     * @throws Exception 
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void PPModify()
    throws Exception {
        entering("PPModify", null);
        try {
            log(Level.FINE, "registerResourceOffering", "Running: PPModify");
            webClientTest = getWebClient();
            webClientTest.getCookieManager().setCookiesEnabled(true);

            strAttribute = configMap.get(TestConstants.KEY_ATTRIBUTE_NAME + "."
                    + testIndex);
            strAttrContainer = configMap.get(TestConstants.
                    KEY_ATTRIBUTE_CONTAINER + "." + testIndex);
            strAttrValue = configMap.get(TestConstants.KEY_ATTRIBUTE_VALUE + "."
                    + testIndex);
            Reporter.log("Testcase index: " + testIndex + ". Run PPModify " +
                    "for attribute " + strAttribute + " with value " +
                    strAttrValue);

            //Federate Users
            federateUsers(webClientTest);
            HtmlPage idpuserpage = (HtmlPage)webClientTest.getPage(idpurl);
            String pageAsString = idpuserpage.getWebResponse().
                    getContentAsString();
            int startindex = pageAsString.indexOf("id=" + configMap.get(
                    TestConstants.KEY_IDP_USER));
            int endindex = pageAsString.indexOf("</div>", startindex);
            uuidIDPuser = pageAsString.substring(startindex, endindex);

            //register the resource offering.
            HtmlPage registerROPage = registerResourceOffering(webClientTest);
            //Query the resource offering
            HtmlPage discoqueryPage = queryResourceOffering(webClientTest,
                    wscURL);
            Thread.sleep(3000);
            //Perform discovery modify
            HtmlForm ppmodifycallform = discoqueryPage.getFormByName(
                    "ppmodifycall");
            HtmlPage ppmodify = (HtmlPage)ppmodifycallform.getInputByName("Submit").click();
 
            Thread.sleep(3000);
            HtmlForm ppmodifyform = ppmodify.getFormByName("ppmodify");
            HtmlInput queryStringTxt = (HtmlInput)ppmodifyform.getInputByName(
                    "queryStr");
            if (strAttrContainer != null) {
                queryStringTxt.setValueAttribute("/PP/" + strAttrContainer + 
                        "/" + strAttribute);
            } else {
                queryStringTxt.setValueAttribute("/PP/" + strAttribute);
            }
            HtmlInput valueTxt = (HtmlInput)ppmodifyform.getInputByName(
                    "valueStr");
            valueTxt.setValueAttribute(strAttrValue);
            HtmlPage ppmodifyPageReceived = (HtmlPage)ppmodifyform.getInputByName("Submit").click();
            if (ppmodifyPageReceived.getWebResponse().getContentAsString().
                    contains(TestConstants.KEY_PP_MODIFY_RESULT)) {
                log(Level.FINE, "PPModify", "PP modify was " +
                        "successful");
            } else {
                log(Level.SEVERE, "PPModify", "PP modify was " +
                        "unsuccessful");
                log(Level.FINE, "PPModify", "PP modify page : "
                        + ppmodifyPageReceived.getWebResponse().
                        getContentAsString());
                assert false;
            }
       } catch (Exception e) {
            log(Level.SEVERE, "PPModify", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("PPModify");
    }

    /**
     * This test does PP query to check if the modified attribute value is
     * returned.
     * @throws Exception
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"PPModify"} )
    public void PPQuery()
    throws Exception {
        entering("PPQuery", null);
        try {
            log(Level.FINE, "PPQuery", "Running: PPQuery");
            Reporter.log("Testcase index: " + testIndex + ". Run PPQuery " +
                    "for attribute " + strAttribute + " with expected value " +
                    strAttrValue);
            HtmlPage discoqueryPage = queryResourceOffering(webClientTest,
                    wscURL);
            Thread.sleep(3000);
            HtmlForm ppqueryform = discoqueryPage.getFormByName("ppquerycall");
            HtmlPage ppquery = (HtmlPage)ppqueryform.getInputByName("Submit").click();

            Thread.sleep(3000);
            HtmlForm ppqueryform1 = ppquery.getFormByName("ppquery");
            HtmlInput queryStringTxt = (HtmlInput)ppqueryform1.getInputByName(
                    "queryStr");
            if (strAttrContainer != null) {
                queryStringTxt.setValueAttribute("/PP/" + strAttrContainer + 
                        "/" + strAttribute);
            } else {
                queryStringTxt.setValueAttribute("/PP/" + strAttribute);
            }
            HtmlPage ppqueryResultPage = (HtmlPage)ppqueryform1.getInputByName("Submit").click();
            if (ppqueryResultPage.getWebResponse().getContentAsString().
                    contains(strAttrValue)) {
                log(Level.FINE, "PPQuery", "PP Query was successful");
            } else {
                log(Level.SEVERE, "PPQuery", "PP Query was unsuccessful");
                log(Level.FINE, "PPQuery", "PP Query page : " +
                        ppqueryResultPage.getWebResponse().
                        getContentAsString());
                assert false;
           }
        } catch (Exception e) {
            log(Level.SEVERE, "PPQuery", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("PPQuery");
    }
   
    /**
     * This methods deletes all the users as part of cleanup
     * @throws Exception
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        ArrayList idList;
        WebClient webClient = null;
        try {
            log(Level.FINE, "cleanup", "Entering Cleanup");
            webClient = getWebClient();
            consoleLogin(webClient, spurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_SP_AMADMIN_PASSWORD));
            fmSP = new FederationManager(spurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_SP_USER));
            log(Level.FINE, "cleanup", "sp users to delete :" +
                    configMap.get(TestConstants.KEY_SP_USER));
            if (FederationManager.getExitCode(fmSP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), idList,
                    "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
                assert false;
            }
            
            // Create idp users
            consoleLogin(webClient, idpurl + "/UI/Login",
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    configMap.get(TestConstants.KEY_IDP_AMADMIN_PASSWORD));
            fmIDP = new FederationManager(idpurl);
            idList = new ArrayList();
            idList.add(configMap.get(TestConstants.KEY_IDP_USER));
            log(Level.FINE, "cleanup", "idp users to delete :" +
                    configMap.get(TestConstants.KEY_IDP_USER));
            if (FederationManager.getExitCode(fmIDP.deleteIdentities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), 
                    idList, "User")) != 0) {
                log(Level.SEVERE, "cleanup", "deleteIdentities famadm command" +
                        " failed");
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
