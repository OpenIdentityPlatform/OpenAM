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
 * $Id: HeaderAttributeTests.java,v 1.12 2009/01/26 23:45:49 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.SMSCommon;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;


///jsxFunction_reload(true)
/**
 * This class tests Header attributes related to Session, Profile
 * and Response. Attributes are tested using a webapp or a cgi script
 * which can read the header attributes in the browser. Attributes
 * are tested for new and updated values for different profile.
 */
public class HeaderAttributeTests extends TestCommon {
    
    private boolean executeAgainstOpenSSO;
    private String logoutURL;
    private String strScriptURL;
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
    private WebClient webClient;
    private int polIdx;
    private int resIdx;
    private int iIdx;
    private AgentsCommon mpc;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private ResourceBundle rbg;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private int sleepTime = 2000;
    private int pollingTime;
    private String strAgentType;
    private String strHeaderFetchMode;

    /**
     * Instantiated different helper class objects
     */
    public HeaderAttributeTests() 
    throws Exception{
        super("HeaderAttributeTests");
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
        smsc = new SMSCommon(admintoken);
    }
    
    public HeaderAttributeTests(String strFetchMode) 
    throws Exception{
        super("HeaderAttributeTests");
        strHeaderFetchMode = strFetchMode;
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
        smsc = new SMSCommon(admintoken);
        
    }
    
    /**
     * Sets up policy and creates users required by the policy
     */
    @Parameters({"policyIdx", "resourceIdx"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String resourceIdx)
    throws Exception {
        Object[] params = {policyIdx, resourceIdx};
        entering("setup", params);

        try {
            strAgentType = rbg.getString(strGblRB + ".agentType");
            if (strHeaderFetchMode  == null){
                strHeaderFetchMode = rbg.getString(strGblRB + ".headerFetchMode");
            }
            if (strAgentType.contains("J2EE") || 
                    strAgentType.contains("WEBLOGIC")) {
                strScriptURL = rbg.getString(strGblRB + 
                        ".headerEvalScriptName");
                strScriptURL = strScriptURL.substring(0,
                        strScriptURL.length()-1);
                strScriptURL = strScriptURL + "?fetch_mode=" + 
                        strHeaderFetchMode;
            } else {
                strScriptURL = rbg.getString(strGblRB 
                        + ".headerEvalScriptName");
            }
            
            log(Level.FINEST, "setup", "Header Script URL: " + strScriptURL);
            url = new URL(strScriptURL);

            resIdx = new Integer(resourceIdx).intValue();

            resource = rbg.getString(strGblRB + ".resource" + resIdx);
            log(Level.FINEST, "setup", "Protected Resource Name: " + resource);

            logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                    "/UI/Logout";

            polIdx = new Integer(policyIdx).intValue();
            mpc.createIdentities(strLocRB, polIdx);
            if (executeAgainstOpenSSO) {
                mpc.createPolicyXML(strGblRB, strLocRB, polIdx, strLocRB +
                        ".xml");
                log(Level.FINEST, "setup", "Policy XML:\n" + strLocRB +
                        ".xml");
                mpc.createPolicy(strLocRB + ".xml");
            } else
                log(Level.FINE, "setup", "Executing against non OpenSSO" +
                        " Install");
            Thread.sleep(15000);
        } catch (Exception e) {
            cleanup();
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }

        exiting("setup");
    }
    
    /**
     * Evaluates newly created response attribute which holds a single static
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateNewSingleValuedStaticResponseAttribute()
    throws Exception {
        entering("evaluateNewSingleValuedStaticResponseAttribute", null);
     
        webClient = new WebClient();
        //webClient.setCookiesEnabled(true);
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_STATSINGLE:10");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewSingleValuedStaticResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }

        exiting("evaluateNewSingleValuedStaticResponseAttribute");
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
        entering("evaluateNewMultiValuedStaticResponseAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_STATMULTIPLE:20|30");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewMultiValuedStaticResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }

        exiting("evaluateNewMultiValuedStaticResponseAttribute");
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
        entering("evaluateDynamicResponseAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_RESPONSE_CN:rauser");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateDynamicResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }

        exiting("evaluateDynamicResponseAttribute");
    }

    /**
     * Evaluates updated response attribute which holds a single dynamic value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},dependsOnMethods={
        "evaluateDynamicResponseAttribute"})
        public void evaluateUpdatedDynamicResponseAttribute()
    throws Exception {
        entering("evaluateUpdatedDynamicResponseAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "rauser",
                    "rauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_RESPONSE_CN:rauser");
            assert (iIdx != -1);

            Set set = new HashSet();
            set.add("1");
            smsc.updateSvcAttribute("iPlanetAMPolicyConfigService",
                    "iplanet-am-policy-config-subjects-result-ttl", set,
                    "Organization");

            Map map = new HashMap();
            set = new HashSet();
            set.add("rauserupdated");
            map.put("cn", set);
            log(Level.FINEST, "evaluateUpdatedDynamicResponseAttribute",
                    "Update Attribute List: " + map);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken, "rauser"
                    , IdType.USER, realm), map);

            Thread.sleep(pollingTime);

            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_RESPONSE_CN:rauserupdated");
            assert (iIdx != -1);

        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedDynamicResponseAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }

        exiting("evaluateUpdatedDynamicResponseAttribute");
    }


    /**
     * Evaluates a standard session attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateUniversalIdSessionAttribute()
    throws Exception {
        entering("evaluateUniversalIdSessionAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "sauser",
                    "sauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            String strUniveraslId = "id=sauser," +
                    rbg.getString(strGblRB +
                    ".uuid.suffix.AMIdentitySubject.User") + "," + basedn;
            log(Level.FINEST,
                    "evaluateUniversalIdSessionAttribute", "strUniveraslId: " +
                    strUniveraslId);
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_SESSION_UNIVERSALIDENTIFIER:" + strUniveraslId);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUniversalIdSessionAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUniversalIdSessionAttribute");
    }

    /**
     * Evaluates newly created and updated custom session attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUniversalIdSessionAttribute"})
    public void evaluateCustomSessionAttribute()
    throws Exception {
        entering("evaluateCustomSessionAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "sauser",
                    "sauser");
            page = (HtmlPage)webClient.getPage(url);
            //webClient.setCookiesEnabled(true);
            String strCookie = rbg.getString(strGblRB + ".serverCookieName");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "strCookie: " +
                    strCookie);
            String s0 = page.asXml();
            int i1;
            String s3;
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s0:\n" + s0);
            i1 = s0.indexOf(strCookie + ":");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "i1: " + i1);
            String s1 = s0.substring(i1, s0.length());
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s1:\n" + s1);
            int i2 = s1.indexOf("|");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "i2: " + i2);
            s3 = s1.substring(strCookie.length() + 1, i2);
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s3: " + s3);
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            String s3_decoded;
            log(Level.FINEST, "evaluateCustomSessionAttribute", 
                    "usertoken before decode=" + s3);
            if (s3.indexOf("%") != -1) {
                s3_decoded = URLDecoder.decode(s3);
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "usertoken after decode=" + s3_decoded);
                if((s3_decoded.indexOf("%") != -1)){
                log(Level.FINEST, "evaluateCustomSessionAttribute", 
                        "usertoken after decode has %");
                    assert (s3_decoded.indexOf("%") != -1);   
                }
                 usertoken = stMgr.createSSOToken(s3_decoded);
            } else {
                usertoken = stMgr.createSSOToken(s3);
            }
            String strProperty;
            if (validateToken(usertoken)) {
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "UserId property value: " +
                        usertoken.getProperty("UserId"));
                usertoken.setProperty("MyProperty", "val1");
                strProperty = usertoken.getProperty("MyProperty");
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "Session property value: " + strProperty);
                assert (strProperty.equals("val1"));
            } else {
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "User token is invalid");
                assert false;
            }

            Thread.sleep(pollingTime);

            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_SESSION_MYPROPERTY:val1");
            assert (iIdx != -1);

            usertoken.setProperty("MyProperty", "val2");
            Thread.sleep(sleepTime);
            strProperty = usertoken.getProperty("MyProperty");
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "Session property value: " + strProperty);
            assert (strProperty.equals("val2"));

            Thread.sleep(pollingTime);

            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_SESSION_MYPROPERTY:val2");
            assert (iIdx != -1);
     
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedSessionAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            destroyToken(usertoken);
        }
        exiting("evaluateUpdatedSessionAttribute");
    }


    /**
     * Evaluates newly created static single valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateNewSingleValuedProfileAttribute()
    throws Exception {
        entering("evaluateNewSingleValuedProfileAttribute", null);

        webClient = new WebClient();
        try {
            Reporter.log("Test description: " + "evaluateNewSingleValuedProfileAttribute");   
            Reporter.log("Resource: " + url);   
            Reporter.log("Username: " + "pauser");   
            Reporter.log("Password: " + "pauser");   
            Reporter.log("Expected Result: " + "HTTP_PROFILE_CN:pauser"); 
            idmc.createIdentity(admintoken, realm, IdType.ROLE, "parole1",
                    new HashMap());
            idmc.addUserMember(admintoken, "pauser", "parole1", IdType.ROLE);
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("(objectclass=person)");
            map.put("nsRoleFilter", set);
            idmc.createIdentity(admintoken, realm, IdType.FILTEREDROLE,
                    "filparole1", map);
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:pauser");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewSingleValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewSingleValuedProfileAttribute");
    }

    /**
     * Evaluates newly created static multi valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewSingleValuedProfileAttribute"})
    public void evaluateNewMultiValuedProfileAttribute()
    throws Exception {
        entering("evaluateNewMultiValuedProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page
                    , "HTTP_PROFILE_ALIAS:pauseralias1|pauseralias2");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewMultiValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewMultiValuedProfileAttribute");
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
        entering("evaluateNewNsRoleProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            String strNsrole = "HTTP_PROFILE_NSROLE:" +
                    "cn=containerdefaulttemplaterole," + basedn +
                    "|cn=filparole1," + basedn + "|cn=parole1," + basedn;
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE: " + strNsrole);
            iIdx = getHtmlPageStringIndex(page, strNsrole);
            assert (iIdx != -1);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=parole1");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewNsRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewNsRoleProfileAttribute");
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
        entering("evaluateNewFilteredRoleProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            String strNsrole = "HTTP_PROFILE_NSROLE:" +
                    "cn=containerdefaulttemplaterole," + basedn +
                    "|cn=filparole1," + basedn + "|cn=parole1," + basedn;
            log(Level.FINEST, "evaluateNewFilteredRoleProfileAttribute",
                    "NSROLE: " + strNsrole);
            iIdx = getHtmlPageStringIndex(page, strNsrole);
            assert (iIdx != -1);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=filparole1");
            assert (iIdx != -1);
            iIdx = -1;
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewFilteredRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewFilteredRoleProfileAttribute");
    }

    /**
     * Evaluates updated static single valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateNewNsRoleProfileAttribute"})
    public void evaluateUpdatedSingleValuedProfileAttribute()
    throws Exception {
        entering("evaluateUpdatedSingleValuedProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:pauser");
            assert (iIdx != -1);

            Map map = new HashMap();
            Set set = new HashSet();
            set.add("pauserupdated");
            map.put("cn", set);
            log(Level.FINEST, "evaluateUpdatedSingleValuedProfileAttribute",
                    "Update Attribute List: " + map);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken, "pauser",
                    IdType.USER, realm), map);

            Thread.sleep(pollingTime);

            usertoken = getToken("pauser", "pauser", basedn);
            set = idmc.getIdentityAttribute(usertoken, "iPlanetAMUserService",
                    "cn");
            assert (set.contains("pauserupdated"));
            page = (HtmlPage)webClient.getPage(url);
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:" +
                    "pauserupdated");
            Thread.sleep(pollingTime);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:" +
                    "pauserupdated");
            assert (iIdx != -1);

        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedSingleValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            destroyToken(usertoken);
        }
        exiting("evaluateUpdatedSingleValuedProfileAttribute");
    }

    /**
     * Evaluates updated static multi valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods={"evaluateUpdatedSingleValuedProfileAttribute"})
    public void evaluateUpdatedMultiValuedProfileAttribute()
    throws Exception {
        entering("evaluateUpdatedMultiValuedProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_ALIAS:" +
                    "pauseralias1|pauseralias2");
            assert (iIdx != -1);

            Map map = new HashMap();
            Set set = new HashSet();
            set.add("pauseralias3");
            map.put("iplanet-am-user-alias-list", set);
            log(Level.FINEST, "evaluateUpdatedMultiValuedProfileAttribute",
                    "Update Attribute List: " + map);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken, "pauser",
                    IdType.USER, realm), map);
            Thread.sleep(pollingTime);
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_ALIAS:" +
                    "pauseralias3");
            assert (iIdx != -1);

        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedMultiValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedMultiValuedProfileAttribute");
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
        entering("evaluateUpdatedNsRoleProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            String strNsrole = "HTTP_PROFILE_NSROLE:" +
                    "cn=containerdefaulttemplaterole," + basedn +
                    "|cn=filparole1," + basedn + "|cn=parole1," + basedn;
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE: "+ strNsrole);
            iIdx = getHtmlPageStringIndex(page, strNsrole);
            assert (iIdx != -1);
            iIdx = getHtmlPageStringIndex(page, "cn=parole1");
            assert (iIdx != -1);

            idmc.createIdentity(admintoken, realm, IdType.ROLE, "parole2",
                    new HashMap());
            idmc.addUserMember(admintoken, "pauser", "parole2", IdType.ROLE);

            Thread.sleep(pollingTime);
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            strNsrole = "HTTP_PROFILE_NSROLE:" +
                    "cn=containerdefaulttemplaterole," + basedn +
                    "|cn=filparole1," + basedn +
                    "|cn=parole1," + basedn + "|cn=parole2," + basedn;
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE: "+ strNsrole);
            iIdx = getHtmlPageStringIndex(page, strNsrole);
            assert (iIdx != -1);
            iIdx = getHtmlPageStringIndex(page, "cn=parole2");
            assert (iIdx != -1);

        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedNsRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedNsRoleProfileAttribute");
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
        entering("evaluateUpdatedFilteredRoleProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=filparole1");
            assert (iIdx != -1);

            Map map = new HashMap();
            Set set = new HashSet();
            set.add("(mail=abc@def.com)");
            map.put("nsRoleFilter", set);
            log(Level.FINEST, "evaluateUpdatedFilteredRoleProfileAttribute",
                    "Update Attribute List: " + map);
            log(Level.FINEST, "evaluateUpdatedFilteredRoleProfileAttribute", 
                    "Recreating adminToken");
            destroyToken(admintoken);
            admintoken = getToken(adminUser, adminPassword, basedn);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken,
                    "filparole1", IdType.FILTEREDROLE, realm), map);
            Thread.sleep(pollingTime);

            page = (HtmlPage)webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=filparole1");
            assert (iIdx != -1);

        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedFilteredRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedFilteredRoleProfileAttribute");
    }

    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
 
        try {
  /*          if (executeAgainstOpenSSO)
                mpc.deletePolicies(strLocRB, polIdx);
            else 
                log(Level.FINE, "cleanup", "Executing against non OpenSSO" +
                        " Install");
*/
            Set set = new HashSet();
            set.add("10");
/*            smsc.updateSvcAttribute("iPlanetAMPolicyConfigService",
                    "iplanet-am-policy-config-subjects-result-ttl", set,
                    "Organization");

            smsc.removeServiceAttributeValues("iPlanetAMPolicyConfigService",
                    "sun-am-policy-dynamic-response-attributes",
                    "Organization");

            if ((idmc.searchIdentities(admintoken, "pauser",
                    IdType.USER)).size() != 0)
                idmc.deleteIdentity(admintoken, realm, IdType.USER, "pauser");
            if (idmc.searchIdentities(admintoken, "sauser",
                    IdType.USER).size() != 0)
                idmc.deleteIdentity(admintoken, realm, IdType.USER, "sauser");
            if (idmc.searchIdentities(admintoken, "rauser",
                    IdType.USER).size() != 0)
               idmc.deleteIdentity(admintoken, realm, IdType.USER, "rauser");
            if (idmc.searchIdentities(admintoken, "parole1",
                    IdType.ROLE).size() != 0)
                idmc.deleteIdentity(admintoken, realm, IdType.ROLE, "parole1");
            if (idmc.searchIdentities(admintoken, "parole2",
                    IdType.ROLE).size() != 0)
                idmc.deleteIdentity(admintoken, realm, IdType.ROLE, "parole2");
 */
             } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }

        exiting("cleanup");
    }

    /**
     * Deletes filtered roles. This is only valid for datastores supporting the
     * filtered roles.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanupForDS()
    throws Exception {
        entering("cleanupForDS", null);

        SSOToken locAdminToken = getToken(adminUser, adminPassword, basedn);
        try {
/*            if (idmc.searchIdentities(locAdminToken, "filparole1",
                    IdType.FILTEREDROLE).size() != 0)
                idmc.deleteIdentity(locAdminToken, realm, IdType.FILTEREDROLE,
                        "filparole1");
  */          
        } catch (Exception e) {
            log(Level.SEVERE, "cleanupForDS", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(locAdminToken);
        }

        exiting("cleanupForDS");
    }
}
