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
 * $Id: GeneralAgentTests.java,v 1.11 2009/03/17 20:28:32 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class tests the following features for agents:
 * (1) remote user from session token
 * (2) remote user anonymous
 * (3) not enfroced by including the cgi script
 * (4) session notification events
       o session termination
       o logout
 * (5) access denied url
 * (6) case ignore for resource
 */
public class GeneralAgentTests extends TestCommon {
    
    private boolean executeAgainstOpenSSO;
    private String strScriptURL;
    private String logoutURL;
    private String strLocRB = "GeneralAgentTests";
    private String strGblRB = "agentsGlobal";
    private String resourceProtected;
    private String resourceNotProtected;
    private String resourceCase;
    private URL url;
    private WebClient webClient;
    private int polIdx;
    private int iIdx;
    private int pollingTime;
    private AgentsCommon mpc;
    private IDMCommon idmc;
    private ResourceBundle rbg;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private HtmlPage page;
    private String strAgentType;

    /**
     * Instantiated different helper class objects
     */
    public GeneralAgentTests() 
    throws Exception {
        super("GeneralAgentTests");
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
    }
    
    /**
     * Sets up policy and creates users required by the policy
     */
    @Parameters({"policyIdx", "resourcePIdx", "resourceNPIdx",
    "resourceCaseIdx"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String resourcePIdx,
            String resourceNPIdx, String resourceCaseIdx)
    throws Exception {
        Object[] params = {policyIdx, resourcePIdx, resourceNPIdx,
        resourceCaseIdx};
        entering("setup", params);
        admintoken = getToken(adminUser, adminPassword, basedn);
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
            "/UI/Logout";
        strAgentType = rbg.getString(strGblRB + ".agentType");
        log(Level.FINEST, "setup", "Agent type - " + strAgentType );        
        strScriptURL = rbg.getString(strGblRB + ".headerEvalScriptName");
        if (strAgentType.contains("J2EE") || 
            strAgentType.contains("WEBLOGIC")) {
            String strHeaderFetchMode = rbg.getString(strGblRB + 
                                ".headerFetchMode");
            strScriptURL = strScriptURL.substring(0, strScriptURL.length() - 1);
            strScriptURL = strScriptURL + "fetch_mode=" + 
                    strHeaderFetchMode;
        }
        log(Level.FINEST, "setup", "Header script URL: " + strScriptURL);
        url = new URL(strScriptURL);
        int resPIdx = new Integer(resourcePIdx).intValue();
        int resNPIdx = new Integer(resourceNPIdx).intValue();
        int resCaseIdx = new Integer(resourceCaseIdx).intValue();
        resourceProtected = rbg.getString(strGblRB + ".resource" + resPIdx);
        resourceNotProtected = rbg.getString(strGblRB + ".resource" + resNPIdx);
        resourceCase = rbg.getString(strGblRB + ".resource" + resCaseIdx);
        log(Level.FINEST, "setup", "Protected resource name : " +
                resourceProtected);
        log(Level.FINEST, "setup", "Unprotected resource name : " +
                resourceNotProtected);
        log(Level.FINEST, "setup", "Case sensitive resource name : " +
                resourceCase);
        polIdx = new Integer(policyIdx).intValue();
        mpc.createIdentities("agents" + fileseparator + strLocRB, polIdx);
        if (executeAgainstOpenSSO) {
            mpc.createPolicyXML("agents" + fileseparator + strGblRB, 
                    "agents" + fileseparator + strLocRB, polIdx,
                    strLocRB + ".xml");
            log(Level.FINEST, "setup", "Policy XML:\n" + strLocRB + ".xml");
           mpc.createPolicy(strLocRB + ".xml");
        } else
            log(Level.FINE, "setup", "Executing against non OpenSSO install");
        exiting("setup");
    }
    
    /**
     * Validates the value of REMOTE_USER for authenticated user.
     */ 
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateRemoteUser()
    throws Exception {
        entering("evaluetRemoteUser", null);
     
        webClient = new WebClient();
        try {
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests",
                    "generalagenttests");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "REMOTE_USER:generalagenttests");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluetRemoteUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateRemoteUser");
    }

    /**
     * Checks if the Agent Being tested, supports Anonymous Users, i.e. Agent 
     * is a 2.2 Web Agent
     */
    private boolean isAnonymousSupported()
        throws Exception {
        boolean isSupported = false;
            try {
                if (strAgentType.equals("2.2WEB")) {
                    isSupported = true;
                }
            } catch (Exception e) {
                log(Level.SEVERE, "isAnonymousSupported", e.getMessage());
                e.printStackTrace();
                throw e;
         }
        return isSupported;
    }

    /**
     * Validates the value of REMOTE_USER for anonymous user.
     */ 
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateAnonymous()
    throws Exception {
        entering("evaluateAnonymous", null);
        boolean checkSupport = isAnonymousSupported();
        if (checkSupport){
            webClient = new WebClient();
            try {
                URL urlLoc = new URL(resourceNotProtected);
                page = (HtmlPage)webClient.getPage(urlLoc);
                log(Level.FINEST, "evaluateAnonymous", "Resource Page :\n" +
                        page.asXml());
                page = (HtmlPage)webClient.getPage(url);
                iIdx = -1;
                iIdx = getHtmlPageStringIndex(page, "REMOTE_USER:anonymous");
                assert (iIdx != -1);
            } catch (Exception e) {
                log(Level.SEVERE, "evaluateAnonymous", e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            log(Level.FINEST, "evaluateAnonymous",
                        "REMOTE_USER test for anonymous user not " +
                        "valid for J2EE Agents");
        }
            exiting("evaluateAnonymous");
    }

    /**
     * Validates a not enforced resources.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateNotEnforced()
    throws Exception {
        entering("evaluateNotEnforced", null);
        webClient = new WebClient();
        try {
            URL urlLoc = new URL(resourceNotProtected);
            page = (HtmlPage)webClient.getPage(urlLoc);
            log(Level.FINEST, "evaluateNotEnforced", "Resource Page :\n" +
                    page.asXml());
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Notenforced Page");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNotEnforced", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("evaluateNotEnforced");
    }

    /**
     * Validates case sensitivity for resource name.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateCaseSensitive()
    throws Exception {
        entering("evaluateCaseSensitive", null);
        webClient = new WebClient();
        webClient.getCookieManager().setCookiesEnabled(true);
        URL urlLoc = new URL(resourceCase);
        try {
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests", "generalagenttests");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Allow Page");
            assert (iIdx != -1);
            log(Level.SEVERE, "evaluateCaseSensitive", "resourceCase:" + 
                    resourceCase);
            page = (HtmlPage)webClient.getPage(urlLoc);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,"Access Denied");
            assert (iIdx != -1);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                ee) {
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateCaseSensitive", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
           exiting("evaluateCaseSensitive");
    }

    /**
     * Validates that agent gets notification if user session is 
     * terminated on the server.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateSessionTermination()
    throws Exception {
        entering("evaluateSessionTermination", null);
        webClient = new WebClient();
        try {
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests", "generalagenttests");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Allow Page");
            assert (iIdx != -1);
            SSOToken ssotoken = getUserToken(admintoken, "generalagenttests");
            destroyToken(admintoken, ssotoken);
            Thread.sleep(pollingTime);
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests",
                    "generalagenttests");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Allow Page");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateSessionTermination", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateSessionTermination");
    }

    /**
     * Validates that agent gets notification if user logs out 
     * from the server.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateSessionLogout()
    throws Exception {
        entering("evaluateSessionLogout", null);
        webClient = new WebClient();
        try {
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests",
                    "generalagenttests");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Allow Page");
            assert (iIdx != -1);
            consoleLogout(webClient, logoutURL);
            Thread.sleep(pollingTime);
            page = consoleLogin(webClient, resourceProtected,
                    "generalagenttests",
                    "generalagenttests");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "Allow Page");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateSessionLogout", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateSessionLogout");
    }

    /**
     * Deletes policies, user identities and destroys amadmin token.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        if (executeAgainstOpenSSO)
            mpc.deletePolicies("agents" + fileseparator + strLocRB, polIdx);
        else 
            log(Level.FINE, "cleanup", "Executing against non OpenSSO install");
        idmc.deleteIdentity(admintoken, realm, IdType.USER, 
                "generalagenttests");
        destroyToken(admintoken);
        exiting("cleanup");
    }
}
