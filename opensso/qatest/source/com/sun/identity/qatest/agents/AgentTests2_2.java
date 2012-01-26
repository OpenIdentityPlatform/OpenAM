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
 * $Id: AgentTests2_2.java,v 1.5 2009/01/26 23:45:47 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;
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
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class contains tests for Web/J2EE Agents version 2.2
 */
public class AgentTests2_2 extends TestCommon {

    private boolean executeAgainstOpenSSO;
    private String strScriptURL;
    private String strLocRB = "HeaderAttributeTests";
    private String strGblRB = "agentsGlobal";
    private String resource;
    private int polIdx;
    private int resIdx;
    private AgentsCommon mpc;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private ResourceBundle rbg;
    private SSOToken admintoken;
    private int pollingTime;
    private String strAgentBeingTested;
    private String strHeaderFetchMode;
    private ResponseAttributeTests resp;
    private SessionAttributeTests session;
    private ProfileAttributeTests profile;

    /**
     * Instantiated different helper class objects
     */
    public AgentTests2_2() 
    throws Exception {
        super("AgentTests2_2");
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
     * Does the pre-test setup needed for the test. Evaluates if thes tests
     * need to be run for the agent configuration being tested. 
     */
    @Parameters({"policyIdx", "resourceIdx", "agentType", "evaluationIdx"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String resourceIdx, String tcForAgentType,
            String evaluationIdx)
    throws Exception {
        Object[] params = {policyIdx, resourceIdx, tcForAgentType, 
            evaluationIdx};
        entering("setup", params);
        try {
            strAgentBeingTested = rbg.getString(strGblRB + ".agentType");
            if (!idmc.isFilteredRolesSupported()) {
                Reporter.log("Exiting since Roles or Filtered Roles " + 
                        "are not supported in this configuration");
                assert(false);                        
            }
            if (strAgentBeingTested.contains("2.2")) { 
                if (strHeaderFetchMode  == null) {
                    strHeaderFetchMode = rbg.getString(strGblRB + 
                            ".headerFetchMode");
                }
                strScriptURL = rbg.getString(strGblRB 
                        + ".headerEvalScriptName");
                if (strAgentBeingTested.contains("J2EE") ||
                        strAgentBeingTested.contains("WEBLOGIC")) {
                    strScriptURL = strScriptURL.substring(0,
                            strScriptURL.length() - 1);
                    strScriptURL = strScriptURL + "fetch_mode=" + 
                            strHeaderFetchMode;
                }
                log(Level.FINEST, "setup", "Header script uRL: " + 
                        strScriptURL);
                resIdx = new Integer(resourceIdx).intValue();
                resource = rbg.getString(strGblRB + ".resource" + resIdx);
                log(Level.FINEST, "setup", "Protected resource name: " + 
                        resource);
                polIdx = new Integer(policyIdx).intValue();
                if (executeAgainstOpenSSO) {
                    mpc.createIdentities("agents" + fileseparator + strLocRB, 
                            polIdx);
                    mpc.createPolicyXML("agents" + fileseparator + strGblRB, 
                            "agents" + fileseparator + strLocRB, polIdx, 
                            strLocRB +
                            ".xml");
                    log(Level.FINEST, "setup", "Policy XML:\n" + strLocRB +
                            ".xml");
                    mpc.createPolicy(strLocRB + ".xml");
                    } else {
                        log(Level.FINE, "setup", "Executing against non " +
                        "OpenSSO install");
                    }
                Thread.sleep(15000);
            } else {
                Reporter.log("Agent being tested is of type: " + 
                        strAgentBeingTested);
                Reporter.log(". Test Case is for type: " + tcForAgentType + 
                        ". Hence skipping tests");
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
     * Evaluates newly created response attribute which holds a single static
     * value
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateNewSingleValuedStaticResponseAttribute()
    throws Exception {
        resp = new ResponseAttributeTests(strScriptURL, resource); 
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
     * Evaluates a standard session attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateUniversalIdSessionAttribute()
    throws Exception {
        session = new SessionAttributeTests(strScriptURL, resource);                 
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
     * Evaluates newly created single valued profile attribute
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluateNewSingleValuedProfileAttribute()
    throws Exception {
        profile = new ProfileAttributeTests(strScriptURL, resource); 
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
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            profile.cleanup();
            resp.cleanup();
            session.cleanup();            
            if (executeAgainstOpenSSO) {
                mpc.deletePolicies("agents" + fileseparator + strLocRB, polIdx);            
            } else {
                log(Level.FINE, "cleanup", "Executing against non OpenSSO" +
                        " install");
            }
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
