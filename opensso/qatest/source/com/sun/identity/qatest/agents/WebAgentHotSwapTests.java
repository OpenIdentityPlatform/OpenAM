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
 * $Id: WebAgentHotSwapTests.java,v 1.6 2009/07/08 21:15:12 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class contains tests for Web Agents version 3.0 & above.
 * HotSwappable properties of the Web Agents using centralised configuration
 * is tested.
 */
public class WebAgentHotSwapTests extends TestCommon {

    private boolean executeAgainstOpenSSO;
    private String logoutURL;
    private String strScriptURL;
    private String strHotSwapRB = "HotSwapProperties";
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
    private WebClient webClient;
    private int polIdx;
    private int resIdx;
    private int iIdx;
    private int testIdx;
    private AgentsCommon mpc;
    private AMIdentity amid;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private ResourceBundle rbg;
    private ResourceBundle rbp;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private int sleepTime = 2000;
    private int pollingTime;
    private String agentURL;
    private String strAgentBeingTested;
    private String strHeaderFetchMode;
    private String agentId;
    private ResponseAttributeTests resp;
    private HotSwapProperties hotswap;
    private SessionAttributeTests session;
    private ProfileAttributeTests profile;

    /**
     * Instantiated different helper class objects
     */
    public WebAgentHotSwapTests()
            throws Exception {
        super("WebAgentHotSwapTests");
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("agents" + fileseparator + strHotSwapRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
        smsc = new SMSCommon(admintoken);
    }

    /**
     * Does the pre-test setup needed for the test. Evaluates if these tests
     * need to be executed for the agent configuration being tested. 
     */
    @Parameters({"policyIdx", "resourceIdx", "agentType", "evaluationIdx",
        "testIdx"})
    @BeforeTest(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String resourceIdx, String TCforAgent,
            String evaluationIdx, String tstIdx)
            throws Exception {
        Object[] params = {policyIdx, resourceIdx, TCforAgent,
            evaluationIdx, tstIdx};
        entering("setup", params);

        try {
            if (!idmc.isFilteredRolesSupported()) {
                Reporter.log("Skipping Test Cases, since Roles or Filtered " +
                        "Roles are not supported in this configuration");
                assert (false);
            }
            strAgentBeingTested = rbg.getString(strGblRB + ".agentType");
            agentId = rbg.getString(strGblRB + ".agentId");
            hotswap = new HotSwapProperties("agentonly", tstIdx);
            boolean isHotSwapSupported = hotswap.isHotSwapSupported();
            if (isHotSwapSupported && strAgentBeingTested.contains("WEB")) {
                amid = idmc.getFirstAMIdentity(admintoken, agentId,
                        idmc.getIdType("agentonly"), "/");
                resIdx = new Integer(resourceIdx).intValue();
                resource = rbg.getString(strGblRB + ".resource" + resIdx);
                log(Level.FINEST, "setup", "Protected Resource Name: " +
                        resource);
                logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Logout";
                strScriptURL = rbg.getString(strGblRB + ".headerEvalScriptName");
                Set set = amid.getAttribute(
                        "com.sun.identity.agents.config.agenturi.prefix");
                Iterator itr = set.iterator();
                String strUrl = "";
                while (itr.hasNext()) {
                    strUrl = (String) itr.next();
                }
                if (strUrl != null) {
                    agentURL = strUrl.substring(0, strUrl.indexOf("/amagent"));
                } else {
                    log(Level.SEVERE, "setup", "property com.sun" +
                            ".identity.client.notification.url is " +
                            "not present");
                }
                polIdx = new Integer(policyIdx).intValue();
                testIdx = new Integer(tstIdx).intValue();
                mpc.createIdentities("agents" + fileseparator + strLocRB,
                        polIdx);
                mpc.createPolicyXML("agents" + fileseparator + strGblRB,
                        "agents" + fileseparator + strLocRB, polIdx, strLocRB +
                        ".xml");
                log(Level.FINEST, "setup", "Policy XML:\n" + strLocRB +
                        ".xml");
                mpc.createPolicy(strLocRB + ".xml");
                //HotSwapIdentities and Policies
                mpc.createIdentities("agents" + fileseparator + strHotSwapRB, polIdx);
                mpc.createPolicyXML("agents" + fileseparator + strGblRB,
                        "agents" + fileseparator + strHotSwapRB, polIdx,
                        strHotSwapRB + ".xml");
                log(Level.FINEST, "setup", "Policy XML:\n" + strHotSwapRB +
                        ".xml");
                mpc.createPolicy(strHotSwapRB + ".xml");
                Thread.sleep(15000);

            } else {
                Reporter.log("Agent being tested is of type: " +
                        strAgentBeingTested);
                Reporter.log("Test Case is for agent type: 3.0WEB " +
                        ". Hence skipping tests.");
                assert (false);
            }
        } catch (Exception e) {
            cleanup();
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }

    /**
     * Gets the Respose Attribute's fetch mode from server, swaps the values
     * and instantiates the ResponseAttributeTests object
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getResponseAttrFetchMode()
            throws Exception {
        String strPropName = rbp.getString(strHotSwapRB + testIdx +
                ".responseFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx +
                ".responseFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        resp = new ResponseAttributeTests(strScriptURL, resource);
    }

    /**
     * Evaluates newly created response attribute which holds a single static
     * value
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"getResponseAttrFetchMode"})
    public void evaluateNewSingleValuedStaticResponseAttribute()
            throws Exception {
        resp.evaluateNewSingleValuedStaticResponseAttribute();
    }

    /**
     * Evaluates newly created response attribute which holds multiple static
     * value
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewSingleValuedStaticResponseAttribute"})
    public void evaluateNewMultiValuedStaticResponseAttribute()
            throws Exception {
        resp.evaluateNewMultiValuedStaticResponseAttribute();
    }

    /**
     * Evaluates newly created response attribute which holds a single dynamic
     * value
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewMultiValuedStaticResponseAttribute"})
    public void evaluateDynamicResponseAttribute()
            throws Exception {
        resp.evaluateDynamicResponseAttribute();
    }

    /**
     * Evaluates updated response attribute which holds a single dynamic value
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateDynamicResponseAttribute"})
    public void evaluateUpdatedDynamicResponseAttribute()
            throws Exception {
        resp.evaluateUpdatedDynamicResponseAttribute();
    }

    /**
     * Gets the Session Attribute's fetch mode from server, swaps the values
     * and instantiates the SessionAttributeTests object
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getSessionAttrFetchMode()
            throws Exception {
        String strPropName = rbp.getString(strHotSwapRB + testIdx +
                ".sessionFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx +
                ".sessionFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        session = new SessionAttributeTests(strScriptURL, resource);
    }

    /**
     * Evaluates a standard session attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"getSessionAttrFetchMode"})
    public void evaluateUniversalIdSessionAttribute()
            throws Exception {
        session.evaluateUniversalIdSessionAttribute();
    }

    /**
     * Evaluates newly created and updated custom session attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateUniversalIdSessionAttribute"})
    public void evaluateCustomSessionAttribute()
            throws Exception {
        session.evaluateCustomSessionAttribute();
    }

    /**
     * Gets the Profile Attribute's fetch mode from server, swaps the values
     * and instantiates the ProfileAttributeTests object
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getProfileAttrFetchMode()
            throws Exception {
        String strPropName = rbp.getString(strHotSwapRB + testIdx +
                ".profileFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx +
                ".profileFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        profile = new ProfileAttributeTests(strScriptURL, resource);
    }

    /**
     * Evaluates newly created single valued profile attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"getProfileAttrFetchMode"})
    public void evaluateNewSingleValuedProfileAttribute()
            throws Exception {
        profile.evaluateNewSingleValuedProfileAttribute();
    }

    /**
     * Evaluates newly created multi valued profile attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewSingleValuedProfileAttribute"})
    public void evaluateNewMultiValuedProfileAttribute()
            throws Exception {
        profile.evaluateNewMultiValuedProfileAttribute();
    }

    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to static roles
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewMultiValuedProfileAttribute"})
    public void evaluateNewNsRoleProfileAttribute()
            throws Exception {
        profile.evaluateNewNsRoleProfileAttribute();
    }

    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to dynamic roles
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewNsRoleProfileAttribute"})
    public void evaluateNewFilteredRoleProfileAttribute()
            throws Exception {
        profile.evaluateNewFilteredRoleProfileAttribute();
    }

    /**
     * Evaluates updated single valued profile attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateNewNsRoleProfileAttribute"})
    public void evaluateUpdatedSingleValuedProfileAttribute()
            throws Exception {
        profile.evaluateUpdatedSingleValuedProfileAttribute();
    }

    /**
     * Evaluates updated multi valued profile attribute
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateUpdatedSingleValuedProfileAttribute"})
    public void evaluateUpdatedMultiValuedProfileAttribute()
            throws Exception {
        profile.evaluateUpdatedMultiValuedProfileAttribute();
    }

    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * static roles
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateUpdatedMultiValuedProfileAttribute"})
    public void evaluateUpdatedNsRoleProfileAttribute()
            throws Exception {
        profile.evaluateUpdatedNsRoleProfileAttribute();
    }

    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * dynamic roles
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evaluateUpdatedNsRoleProfileAttribute"})
    public void evaluateUpdatedFilteredRoleProfileAttribute()
            throws Exception {
        profile.evaluateUpdatedFilteredRoleProfileAttribute();
    }

    /**
     * Evaluates accessDeniedURL Swap 
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evalAccessDeniedURL()
            throws Exception {
        entering("accessDeniedURL", null);
        HtmlPage page = null;
        String strPropName = "";
        String strPropValue = "";
        String strEvalValue = "";
        WebClient webClient = new WebClient();
        try {
            strPropName = rbp.getString(strHotSwapRB + testIdx +
                    ".accessDeniedURL");
            strPropValue = rbp.getString(strHotSwapRB + testIdx +
                    ".accessDeniedURLValue");
            strEvalValue = rbp.getString(strHotSwapRB + testIdx +
                    ".accessDeniedURLEvalValue");
            log(Level.FINE, "accessDeniedURI", "strAgentSampleURI : " +
                    agentURL + ",strPropValue : " + strPropValue);
            if (!strPropValue.equals("")) {
                strPropValue = rbg.getString(strPropValue);
            }
            hotswap.hotSwapProperty(strPropName, strPropValue);
            webClient.getCookieManager().setCookiesEnabled(true);
            page = consoleLogin(webClient, resource, "hsuser0",
                    "hsuser0");
            iIdx = -1;
            String strUrl = agentURL + "/allow17.html";
            log(Level.FINE, "evalAccessDeniedURL", "accessDeniedURL is " +
                    strUrl);
            Reporter.log("Resource: " + strUrl);
            Reporter.log("Username: " + "hsuser0");
            Reporter.log("Password: " + "hsuser0");
            Reporter.log("Expected Result: " + strEvalValue);
            page = (HtmlPage) webClient.getPage(new URL(strUrl));
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, strEvalValue);
            assert (iIdx != -1);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException ee) {
            log(Level.SEVERE, "evalAccessDeniedURL", ee.getMessage());
        } catch (com.gargoylesoftware.htmlunit.ScriptException sse) {
            assert (true);
        } catch (Exception e) {
            log(Level.SEVERE, "evalAccessDeniedURL", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("accessDeniedURL");
    }

    /**
     * Evaluates evalNotEnfURL Swap 
     */
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"evalAccessDeniedURL"})
    public void evalNotEnfURL()
            throws Exception {
        entering("evalNotEnfURL", null);
        String strPropName = "";
        String strPropValue = "";
        String strEvalValue = "";
        WebClient webClient = new WebClient();
        try {
            strPropName = rbp.getString(strHotSwapRB + testIdx +
                    ".notenfURL");
            strPropValue = rbp.getString(strHotSwapRB + testIdx +
                    ".notenfURLValue");
            strEvalValue = rbp.getString(strHotSwapRB + testIdx +
                    ".notenfURLEvalValue");
            log(Level.FINE, "accessDeniedURI", "strAgentSampleURI : " +
                    agentURL + ",strPropValue : " + strPropValue);
            if (!strPropValue.equals("")) {
                strPropValue = rbg.getString(strPropValue);
            }
            hotswap.hotSwapProperty(strPropName, strPropValue);
            URL Url = new URL(strPropValue);
            log(Level.FINE, "evalNotEnfURL", "evalNotEnfURL is " +
                    Url.toString());
            Reporter.log("Resource: " + Url.toString());
            Reporter.log("Expected Result: " + strEvalValue);
            webClient.getCookieManager().setCookiesEnabled(true);
            boolean isFound = false;
            long time = System.currentTimeMillis();
            HtmlPage page;
            while (System.currentTimeMillis() - time < (pollingTime) &&
                    !isFound) {
                page = (HtmlPage) webClient.getPage(Url);
                iIdx = -1;
                iIdx = getHtmlPageStringIndex(page, strEvalValue, false);
                if (iIdx != -1) {
                    isFound = true;
                }
                log(Level.FINE, "evalNotEnfURL", "isFound=" + isFound);
            }
            page = (HtmlPage) webClient.getPage(Url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, strEvalValue);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evalNotEnfURL", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evalNotEnfURL");
    }

    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    @AfterTest(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        try {
            hotswap.restoreDefaults(testIdx);

            // If profile, session & resp objects are null then the test 
            // has failed in setup and only identities & policies need to 
            // be deleted.
            if (profile != null) {
                profile.cleanup();
            } else {
                if (idmc.searchIdentities(admintoken, "pauser",
                        IdType.USER).size() != 0) {
                    idmc.deleteIdentity(admintoken, realm, IdType.USER,
                            "pauser");
                }
            }
            if (resp != null) {
                resp.cleanup();
            } else {
                if (idmc.searchIdentities(admintoken, "rauser",
                        IdType.USER).size() != 0) {
                    idmc.deleteIdentity(admintoken, realm, IdType.USER,
                            "rauser");
                }
            }
            if (session != null) {
                session.cleanup();
            } else {
                if (idmc.searchIdentities(admintoken, "sauser",
                        IdType.USER).size() != 0) {
                    idmc.deleteIdentity(admintoken, realm, IdType.USER,
                            "sauser");
                }
            }
            if (executeAgainstOpenSSO) {
                mpc.deletePolicies("agents" + fileseparator + strLocRB, polIdx);
                mpc.deletePolicies("agents" + fileseparator + strHotSwapRB,
                        polIdx);
            } else {
                log(Level.FINE, "cleanup", "Executing against non OpenSSO" +
                        " install");
            }
            mpc.deleteIdentities("agents" + fileseparator + strHotSwapRB,
                    polIdx);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("cleanup");
    }
}
