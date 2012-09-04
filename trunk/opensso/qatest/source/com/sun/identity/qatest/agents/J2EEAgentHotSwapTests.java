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
 * $Id: J2EEAgentHotSwapTests.java,v 1.7 2009/01/26 23:45:49 nithyas Exp $
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
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.SMSConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class tests is for J2EE agents version 3.0 and above.
 * HotSwappable properties agents with centralised configuration are tested.
 */

public class J2EEAgentHotSwapTests  extends TestCommon {
    
    private boolean executeAgainstOpenSSO;
    private String logoutURL;
    private String strScriptURL;
    private String strHotSwapRB = "HotSwapProperties";
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
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
    private String strAgentBeingTested;
    private String strHeaderFetchMode;
    private String agentId;
    private String agentURL;
    private ResponseAttributeTests resp;
    private HotSwapProperties hotswap;
    private SessionAttributeTests session;
    private ProfileAttributeTests profile;

    /**
     * Instantiated different helper class objects
     */
    public J2EEAgentHotSwapTests() 
    throws Exception{
        super("J2EEAgentHotSwapTests");
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
    @BeforeTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String resourceIdx, 
                String tcForAgentType, String evaluationIdx, String tstIdx)
    throws Exception {
        Object[] params = {policyIdx, resourceIdx, tcForAgentType, 
            evaluationIdx, tstIdx};
        entering("setup", params);
        try {
            if (!idmc.isFilteredRolesSupported()) {
                Reporter.log("Skipping test cases, since roles or filtered " + 
                        "roles are not supported in this configuration");
                assert(false);                        
            }
            strAgentBeingTested = rbg.getString(strGblRB + ".agentType");
            agentId = rbg.getString(strGblRB + ".agentId");
            hotswap = new HotSwapProperties("agentonly", tstIdx);
            boolean isHotSwapSupported = hotswap.isHotSwapSupported();
            if (isHotSwapSupported && (strAgentBeingTested.contains("J2EE")
                || strAgentBeingTested.contains("WEBLOGIC"))) { 
                amid = idmc.getFirstAMIdentity(admintoken, agentId, 
                    idmc.getIdType("agentonly"), "/");
                resIdx = new Integer(resourceIdx).intValue();
                testIdx = new Integer(tstIdx).intValue();
                resource = rbg.getString(strGblRB + ".resource" + resIdx);
                log(Level.FINEST, "setup", "Protected resource name: " + 
                        resource);
                logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                        "/UI/Logout";
                Set set = amid.getAttribute(
                        "com.sun.identity.client.notification.url");
                Iterator itr = set.iterator();
                String strUrl = "";
                while ( itr.hasNext()) {
                    strUrl  = (String)itr.next();
                }
                if (strUrl != null) {
                     agentURL = strUrl.substring(0, 
                             strUrl.indexOf("/agentapp/notification"));
                } else {
                    log(Level.SEVERE, "setup", "Property com.sun" + 
                            ".identity.client.notification.url is " + 
                            "not present");
                    assert(false);
                }
                polIdx = new Integer(policyIdx).intValue();
                mpc.createIdentities("agents" + fileseparator + strLocRB, 
                        polIdx);
                mpc.createPolicyXML("agents" + fileseparator + strGblRB, 
                        "agents" + fileseparator + strLocRB, polIdx, strLocRB +
                        ".xml");
                log(Level.FINEST, "setup", "Policy XML:\n" + strLocRB +
                        ".xml");
                mpc.createPolicy(strLocRB + ".xml");
                
                //HotSwapIdentities and Policies
                mpc.createIdentities("agents" + fileseparator + strHotSwapRB, 
                        polIdx);
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
                Reporter.log("Test Case is for agent: "+ tcForAgentType +
                        ". Hence skipping tests.");
                assert(false);
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
     * Forms the URL for the JSP page with the correct query string
     */
    private String formJ2EEScriptURL(String strHeaderFetchMode) 
        throws Exception {
        String url;
        log(Level.FINEST, "formJ2EEScriptURL", "strHeaderFetchMode: " + 
                strHeaderFetchMode);
        strScriptURL = rbg.getString(strGblRB + ".headerEvalScriptName");
        strScriptURL = strScriptURL.substring(0, strScriptURL.length() - 1);
        strScriptURL = strScriptURL + "?fetch_mode=" + strHeaderFetchMode;
        log(Level.FINEST, "formJ2EEScriptURL", "Header Script URL: " + 
                strScriptURL);
        return strScriptURL;
    }
            
    /**
     * Gets the Respose Attribute's fetch mode from server and instantiates the 
     * ResponseAttributeTests object
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getResponseAttrFetchMode()
    throws Exception {
        Set set;
        Iterator itr;
        String strPropName = rbp.getString(strHotSwapRB + testIdx + 
                ".responseFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx +
               ".responseFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        Thread.sleep(3000);        
        set = amid.getAttribute(
                "com.sun.identity.agents.config.response.attribute.fetch.mode");
        itr = set.iterator();
        while ( itr.hasNext()) {
            strHeaderFetchMode = (String)itr.next();
        }
        strScriptURL = formJ2EEScriptURL(strHeaderFetchMode);
        resp = new ResponseAttributeTests(strScriptURL, resource); 
        log(Level.FINE, "getResponseAttrFetchMode", "Response Attribute fetch " 
                + "mode is: " + strHeaderFetchMode);
    }
    
    /**
     * Evaluates newly created response attribute which holds a single static
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"getResponseAttrFetchMode"})
    public void evaluateNewSingleValuedStaticResponseAttribute()
    throws Exception {
        resp.evaluateNewSingleValuedStaticResponseAttribute();
    }

    /**
     * Evaluates newly created response attribute which holds multiple static
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewSingleValuedStaticResponseAttribute"})
    public void evaluateNewMultiValuedStaticResponseAttribute()
    throws Exception {
        resp.evaluateNewMultiValuedStaticResponseAttribute();
    }
    
    /**
     * Evaluates newly created response attribute which holds a single dynamic
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewMultiValuedStaticResponseAttribute"})
    public void evaluateDynamicResponseAttribute()
    throws Exception {
        resp.evaluateDynamicResponseAttribute();

    }
      
    /**
     * Evaluates updated response attribute which holds a single dynamic value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
        dependsOnMethods={"evaluateDynamicResponseAttribute"})
    public void evaluateUpdatedDynamicResponseAttribute()
    throws Exception {
        resp.evaluateUpdatedDynamicResponseAttribute();
    }
     
    /**
     * Gets the Session Attribute's fetch mode from server and instantiates the 
     * SessionAttributeTests object
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getSessionAttrFetchMode()
    throws Exception {
        Set set;
        Iterator itr;
        String strPropName = rbp.getString(strHotSwapRB + testIdx + 
                ".sessionFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx + 
               ".sessionFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        Thread.sleep(3000);
        set = amid.getAttribute(
                "com.sun.identity.agents.config.session.attribute.fetch.mode");
        itr = set.iterator();
        while ( itr.hasNext()) {
            strHeaderFetchMode = (String)itr.next();
        }
        strScriptURL = formJ2EEScriptURL(strHeaderFetchMode);
        session = new SessionAttributeTests(strScriptURL, resource); 
        log(Level.FINE, "getSessionAttrFetchMode", "Session Attribute fetch " + 
                "mode is:" + strHeaderFetchMode);
    }
    
    /**
     * Evaluates a standard session attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"getSessionAttrFetchMode"})
    public void evaluateUniversalIdSessionAttribute()
    throws Exception {
        session.evaluateUniversalIdSessionAttribute();
    }           

    /**
     * Evaluates newly created and updated custom session attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUniversalIdSessionAttribute"})
    public void evaluateCustomSessionAttribute()
    throws Exception {
        session.evaluateCustomSessionAttribute();
    }    

    /**
     * Gets the Profile Attribute's fetch mode from server and instantiates the 
     * ProfileAttributeTests object
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void getProfileAttrFetchMode()
    throws Exception {
        String strPropName = rbp.getString(strHotSwapRB + testIdx + 
               ".profileFetch");
        String strPropValue = rbp.getString(strHotSwapRB + testIdx + 
               ".profileFetchValue");
        hotswap.hotSwapProperty(strPropName, strPropValue);
        Thread.sleep(3000);                
        Set set;
        Iterator itr;
        set = amid.getAttribute(
                "com.sun.identity.agents.config.profile.attribute.fetch.mode");
        itr = set.iterator();
        while ( itr.hasNext()) {
            strHeaderFetchMode = (String)itr.next();
        }
        strScriptURL = formJ2EEScriptURL(strHeaderFetchMode);
        profile = new ProfileAttributeTests(strScriptURL, resource); 
        log(Level.FINE, "getProfileAttrFetchMode", "Profile Attribute fetch " + 
                "mode is " + strHeaderFetchMode);
    }
    
    /**
     * Evaluates newly created single valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"getProfileAttrFetchMode"})
    public void evaluateNewSingleValuedProfileAttribute()
    throws Exception {
        profile.evaluateNewSingleValuedProfileAttribute();
    }
    
    /**
     * Evaluates newly created multi valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewSingleValuedProfileAttribute"})
    public void evaluateNewMultiValuedProfileAttribute()
    throws Exception {
        profile.evaluateNewMultiValuedProfileAttribute();
    }
    
    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to static roles
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewMultiValuedProfileAttribute"})
    public void evaluateNewNsRoleProfileAttribute()
    throws Exception {
        profile.evaluateNewNsRoleProfileAttribute();
    }
    
    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to dynamic roles
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewNsRoleProfileAttribute"})
    public void evaluateNewFilteredRoleProfileAttribute()
    throws Exception {
        profile.evaluateNewFilteredRoleProfileAttribute();
    }
    
    /**
     * Evaluates updated single valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewNsRoleProfileAttribute"})
    public void evaluateUpdatedSingleValuedProfileAttribute()
    throws Exception {
        profile.evaluateUpdatedSingleValuedProfileAttribute();
    }
    
    /**
     * Evaluates updated multi valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUpdatedSingleValuedProfileAttribute"})
    public void evaluateUpdatedMultiValuedProfileAttribute()
    throws Exception {
        profile.evaluateUpdatedMultiValuedProfileAttribute();
    }
    
    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * static roles
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUpdatedMultiValuedProfileAttribute"})
    public void evaluateUpdatedNsRoleProfileAttribute()
    throws Exception {
        profile.evaluateUpdatedNsRoleProfileAttribute();
    }
    
    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * dynamic roles
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUpdatedNsRoleProfileAttribute"})
    public void evaluateUpdatedFilteredRoleProfileAttribute()
    throws Exception {
        profile.evaluateUpdatedFilteredRoleProfileAttribute();
    }

    /**
     * Swaps the customised access denied URI and evaluates the cahnge
     * by trying to access a resource, which the user cannot access.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evalAccessDeniedURI()
    throws Exception {
        entering("accessDeniedURI",null);
        HtmlPage page = null;
        String strPropName="";
        String strPropValue="";
        String strEvalValue="";
        String strKey="";
        WebClient webClient = new WebClient();
        try {
            strPropName = rbp.getString(strHotSwapRB + testIdx + 
                   ".accessDeniedURI");
            strPropValue = rbp.getString(strHotSwapRB + testIdx + 
                   ".accessDeniedURIValue");
            log(Level.FINE,"accessDeniedURI", "strAgentSampleURI : " + 
                    agentURL + ",strPropValue : " + strPropValue);
            if (!strPropValue.equals("")) {
                if (strPropValue.contains("]=")) {
                    strKey = strPropValue.substring(0, 
                            strPropValue.indexOf("="));
                    String strResVal = strPropValue.substring(
                            strPropValue.indexOf("=") + 1, strPropValue.length());
                    strPropValue = rbg.getString(strResVal);            
                    strPropValue = strKey + "=" + strPropValue; 
                } else {
                    strPropValue = rbg.getString(strPropValue);
                }
            }
            log(Level.FINE,"accessDeniedURI", "strAgentSampleURI : " + 
                    agentURL + ",strPropValue : " + strPropValue);
            strEvalValue = rbp.getString(strHotSwapRB + testIdx + 
                   ".accessDeniedURIEvalValue");
            hotswap.hotSwapProperty(strPropName, strPropValue);
            Thread.sleep(3000);
            webClient.getCookieManager().setCookiesEnabled(true);
            page = consoleLogin(webClient, resource, "hsuser0",
                "hsuser0");
            iIdx = -1;
            
            // Access any resource that is not in the not enf URI and hsuser0 
            // doesnt have access
            String strUrl = agentURL + "/agentsample/resources/allow17.html";
            log(Level.FINE,"accessDeniedURI", "Resource being accessed is : " + 
                    strUrl + ", and hsuser0 should not be allowed access.");
            Reporter.log("Resource: " + strUrl);   
            Reporter.log("Username: " + "hsuser0");   
            Reporter.log("Password: " + "hsuser0");   
            Reporter.log("Expected Result: " + strEvalValue); 
            page = (HtmlPage)webClient.getPage(new URL(strUrl));                
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, strEvalValue);
            if (strEvalValue.equals("Forbidden") && iIdx == -1) {
                 iIdx = getHtmlPageStringIndex(page, "Access Denied");
            }
            assert (iIdx != -1);
        } catch (com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException 
                ee) {
        } catch (Exception e) {
            log(Level.SEVERE, "accessDeniedURI", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("accessDeniedURI");
    }

    /**
     * Swaps the Not Enforced URI and evaluates that not enforced URI's are 
     * not protected
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evalAccessDeniedURI"})
    public void evalNotEnfURI()
    throws Exception {
        entering("evalNotEnfURI", null);
        String strPropName="";
        String strPropValue="";
        String strEvalValue="";
        String strResVal="";
        WebClient webClient = new WebClient();
        try {
            strPropName = rbp.getString(strHotSwapRB + testIdx + 
                   ".notenfURI");
            strPropValue = rbp.getString(strHotSwapRB + testIdx + 
                   ".notenfURIValue");
            strResVal = rbg.getString(strPropValue);
            log(Level.FINE,"accessDeniedURI", "strAgentSampleURI : " + 
                    agentURL + ",strPropValue : " + strPropValue);
            if (!strPropValue.equals("")) {
            strResVal = rbg.getString(strPropValue);            
            strPropValue = strResVal.substring(
                            agentURL.length(),  
                    strResVal.length());
            }
            strEvalValue = rbp.getString(strHotSwapRB + testIdx + 
                   ".notenfURIEvalValue");
            hotswap.hotSwapProperty(strPropName, strPropValue);
            Thread.sleep(3000);
            URL Url = new URL (agentURL + strPropValue);
            log(Level.FINE,"evalNotEnfURI", "New evalNotEnfURI is : " + 
                    Url.toString());
            Reporter.log("Resource: " + Url.toString());   
            Reporter.log("Expected Result: " + strEvalValue); 
            Set set = amid.getAttribute(strPropName);
            log(Level.FINE,"evalNotEnfURI", "List of not enf URI's " +
                    "from amid object is : " + set.toString());
            webClient.getCookieManager().setCookiesEnabled(true);
            boolean isFound = false;
            long time = System.currentTimeMillis();
            HtmlPage page;
            while (System.currentTimeMillis() - time < (pollingTime/3) &&
                !isFound) {
                page = (HtmlPage)webClient.getPage(Url);
                iIdx = -1;
                iIdx = getHtmlPageStringIndex(page, strEvalValue, false);
                if (iIdx != -1)
                    isFound = true;
            }
            page = (HtmlPage)webClient.getPage(Url);                
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, strEvalValue);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evalNotEnfURI", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evalNotEnfURI");
    }

    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    @AfterTest(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
         "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
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
                        IdType.USER).size() != 0)
                   idmc.deleteIdentity(admintoken, realm, IdType.USER, 
                           "pauser");
            }
            if (resp != null) { 
                resp.cleanup();
            } else {
                if (idmc.searchIdentities(admintoken, "rauser",
                        IdType.USER).size() != 0)
                   idmc.deleteIdentity(admintoken, realm, IdType.USER,
                           "rauser");
                if (idmc.searchIdentities(admintoken, "rauser1",
                        IdType.USER).size() != 0)
                   idmc.deleteIdentity(admintoken, realm, IdType.USER,
                           "rauser1");
            }
            if (session != null) { 
                session.cleanup();
            } else {
                if (idmc.searchIdentities(admintoken, "sauser",
                        IdType.USER).size() != 0)
                   idmc.deleteIdentity(admintoken, realm, IdType.USER,
                           "sauser");
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
