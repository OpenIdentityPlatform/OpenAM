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
 * $Id: AgentAuthModuleTests.java,v 1.1 2009/07/23 06:11:08 arunav Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.ResourceBundle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;


/**
 * This class has the methods to create and evaluate policies using client
 * policy evaluation API
 */

public class AgentAuthModuleTests extends TestCommon {
    
    private int polIdx;
    private int evalIdx;
    private String strSetup;
    private String strCleanup;
    private String strPeAtOrg;
    private String strDynamic;
    private String strDynamicRefValue;
    private PolicyCommon mpc;
    private ResourceBundle rbg;
    private ResourceBundle rbp;
    private ResourceBundle rbr;
    private String strReferringOrg;
    private boolean executeGatewayMode;
    private String strLocRB = "AgentsAuthModuleTests";
    private String strGblRB = "agentsGlobal";
    private String strRefRB = "AgentsReferral";
    private AuthenticationCommon authCommon;
    private WebClient webClient;
    private String testLogoutURL;
    private Map<String,String> globalUpdateMap;
    
    /**
     * Class constructor. No arguments
     */
    public AgentAuthModuleTests()
    throws Exception {
        super("AgentAuthModuleTests");
        mpc = new PolicyCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("agents" + fileseparator + strLocRB);
        rbr = ResourceBundle.getBundle("agents" + fileseparator + strRefRB);
        testLogoutURL = protocol + ":" + "//" + host + ":" + port +
                uri + "/UI/Logout";
        authCommon = new AuthenticationCommon("agents");
        globalUpdateMap = new HashMap();
    }
    
    /**
     * This method sets up all the required identities, generates the xmls and
     * creates the policies in the server
     * @param policyIdx - Index of the policy tests
     * @param evaluationIdx - Index of the evaluation for the policy tests
     * @param setup - setup needed or not
     * @param cleanup - cleanup needed or not
     * @param peAtorg - The org where the policy evaluation should happen
     * @param dynamic - Whether the referral is dynamic or not
     */
    @Parameters({"policyIdx","evaluationIdx","setup","cleanup", "peAtOrg",
    "dynamic"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String evaluationIdx, String setup
            , String cleanup, String peAtOrg, String dynamic)
            throws Exception {
        Object[] params = {policyIdx, evaluationIdx, setup, cleanup, dynamic,
        peAtOrg};
        entering("setup", params);
        try {
            polIdx = new Integer(policyIdx).intValue();
            evalIdx = new Integer(evaluationIdx).intValue();
            strSetup = setup;
            strCleanup = cleanup;
            strPeAtOrg = peAtOrg;
            strDynamic = dynamic;
            if (strSetup.equals("true")) {
                authCommon.createAuthInstancesMap();
                updateGlobalAuthData(strLocRB, policyIdx);
                authCommon.createAuthInstances();
                int noOfAuthServices = new Integer(rbp.getString(strLocRB +
                        policyIdx + ".noOfAuthServices")).intValue();
                if (noOfAuthServices > 0 ) {
                    createAuthService(strLocRB, policyIdx);
                }
                if (strPeAtOrg.equals(realm)) {
                    mpc.createIdentities("agents" + fileseparator +
                            strLocRB, polIdx,  strPeAtOrg );
                    mpc.createPolicyXML("agents" + fileseparator + strGblRB,
                            "agents" + fileseparator + strLocRB, polIdx,
                            strLocRB + ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
                    Thread.sleep(notificationSleepTime);
                } else {
                    mpc.createRealm("/" + strPeAtOrg);
                    mpc.createIdentities(strLocRB, polIdx, strPeAtOrg);
                    
                    if (strDynamic.equals("false")) {
                        mpc.createReferralPolicyXML(strGblRB, strRefRB,
                                strLocRB, polIdx, strRefRB +  ".xml");
                        strReferringOrg = rbr.getString(strLocRB + polIdx +
                                ".referringOrg");
                        assert (mpc.createPolicy(strRefRB + ".xml",
                                strReferringOrg ));
                        Thread.sleep(notificationSleepTime);
                    } else {
                        strDynamicRefValue = "true";
                        mpc.setDynamicReferral(strDynamicRefValue);
                        mpc.createDynamicReferral(strGblRB, strRefRB, strLocRB,
                                polIdx, strPeAtOrg);
                        Thread.sleep(notificationSleepTime);
                    }
                    mpc.createPolicyXML(strGblRB, strLocRB, polIdx,
                            strLocRB + ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
                    Thread.sleep(notificationSleepTime);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Evaluates policy through the agent.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
    "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyAgents()
    throws Exception {
        entering("evaluatePolicyAgents", null);
        String strEvalIdx = strLocRB + polIdx + ".evaluation" + evalIdx;
        String resource = rbg.getString(rbp.getString(strEvalIdx +
                ".resource"));
        String usernameIdx = rbp.getString(strEvalIdx + ".username");
        String username = rbp.getString(usernameIdx);
        String passwordIdx = rbp.getString(strEvalIdx + ".password");
        String password = rbp.getString(passwordIdx);
        String expResult = rbp.getString(strEvalIdx + ".expectedResult");
        String description = rbp.getString(strEvalIdx + ".description");
        
        Reporter.log("Test description: " + description);
        Reporter.log("Resource: " + resource);
        Reporter.log("Username: " + username);
        Reporter.log("Password: " + password);
        Reporter.log("Expected Result: " + expResult);
        try {
            String xmlFile = createPolicyEvalXML(strEvalIdx);
            log(Level.FINEST, "evaluatePolicyAgents",
                    "evaluatePolicyAgents XML file:" + xmlFile);
            DefaultTaskHandler task = new DefaultTaskHandler(xmlFile);
            webClient = new WebClient();
            Page page = task.execute(webClient);
            log(Level.FINEST, "evaluatePolicyAgents",
                    "evaluatePolicyAgents page after login" +
                    page.getWebResponse().getContentAsString());
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyAgents", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, testLogoutURL);
        }
        exiting("evaluatePolicyAgents");
    }
    
    /**
     * This method cleans all the identities and policies  that were setup
     * @param peAtorg - The org where the policy evaluation should happen
     * @param dynamic - Whether the referral is dynamic or not
     */
    @Parameters({"peAtOrg", "dynamic"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
    "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String peAtOrg, String dynamic)
    throws Exception {
        Object[] params = {peAtOrg, dynamic};
        entering("cleanup", params);
        strDynamic = dynamic;
        try {
            if (strCleanup.equals("true")) {
                deleteAuthService(strLocRB, Integer.toString(polIdx));
                authCommon.deleteAuthInstances();
                if (peAtOrg.equals(realm)) {
                    mpc.deleteIdentities("agents" + fileseparator + strLocRB,
                            polIdx, peAtOrg);
                    assert (mpc.deletePolicies("agents" + fileseparator
                            + strLocRB, polIdx, peAtOrg));
                } else {
                    mpc.deleteIdentities("agents" + fileseparator +
                            strLocRB, polIdx, peAtOrg);
                    mpc.deleteRealm(peAtOrg);
                    if (strDynamic.equals("false")) {
                        mpc.deleteReferralPolicies("agents" + fileseparator
                                + strLocRB, "agents" + fileseparator +
                                strRefRB, polIdx);
                    } else {
                        strDynamicRefValue = "false";
                        mpc.setDynamicReferral(strDynamicRefValue);
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }
    
    /**
     * This method updates the GlobalAuthData with policy test authentication
     * data
     * @param strLocRB -agents resource bundle used to update the GlobalAuthData
     * @param policyIdx -which policy index data to be read
     */
    private void updateGlobalAuthData(String strLocRB, String policyIdx)
    throws Exception {
        String moduleType = null;
        String moduleInstanceString = "";
        try {
            ResourceBundle rb = ResourceBundle.getBundle("agents" +
                    fileseparator + strLocRB);
            String strPolIdx = strLocRB + policyIdx;
            
            int noOfauthModules = new Integer(rb.getString(strPolIdx +
                    ".noOfAuthModules")).intValue();
            for (int i = 0; i < noOfauthModules; i++) {
                moduleType = rb.getString( strPolIdx +
                        ".AuthModule" + i  + ".type");
                String noOfauthInstances = rb.getString( strPolIdx +
                        ".AuthModule" + i + ".noOfInstances");
                moduleInstanceString = moduleInstanceString + moduleType + ","
                        + noOfauthInstances;
                if (i < noOfauthModules -1){
                    moduleInstanceString = moduleInstanceString + "|";
                }
            }
            globalUpdateMap.put("instances-to-create", moduleInstanceString);
            authCommon.setPropsInGlobalAuthInstancesMap(globalUpdateMap);
        } catch (Exception e) {
            log(Level.SEVERE, "updateGlobalAuthData", e.getMessage());
            e.printStackTrace();
        } finally {
            Reporter.log("updateGlobalAuthData: instances-to-create :" +
                    moduleInstanceString);
        }
    }
    
    /**
     * This method creates Authentication service required by policy test
     * @param strLocRB -agents resource bundle used to create the Auth Service
     * @param policyIdx -which policy index data to be read
     */
    private void createAuthService(String strLocRB, String policyIdx)
    throws Exception {
        String chainServiceName = null;
        try {
            ResourceBundle rb = ResourceBundle.getBundle("agents" +
                    fileseparator + strLocRB);
            String strPolIdx = strLocRB + policyIdx;
            int noOfAuthServices = new Integer(rbp.getString(strLocRB +
                    policyIdx + ".noOfAuthServices")).intValue();
            
            for (int i = 0; i < noOfAuthServices; i++) {
                chainServiceName = rb.getString( strPolIdx +
                        ".AuthService" + i  + ".name");
                String chainServiceDescription = rb.getString(strPolIdx  +
                        ".AuthService" + i  + ".description");
                Map configMap = new HashMap();
                String chainModInstances = rb.getString(strPolIdx +
                        ".AuthService" + i + ".instances");
                String[] configInstances = chainModInstances.split("\\|");
                authCommon.createAuthConfig(realm, chainServiceName,
                        configInstances, configMap);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "createAuthService", e.getMessage());
            e.printStackTrace();
        } finally {
            Reporter.log("createAuthService:  :" + chainServiceName);
        }
    }
    
    /**
     * This method deletes Authentication service created by policy test
     * @param strLocRB -agents resource bundle used to delete the Auth Service
     * @param policyIdx -which policy index data to be read
     */
    private void deleteAuthService(String strLocRB, String policyIdx)
    throws Exception {
        String chainServiceName = null;
        try {
            ResourceBundle rb = ResourceBundle.getBundle("agents" +
                    fileseparator + strLocRB);
            String strPolIdx = strLocRB + policyIdx;
            int noOfAuthServices = new Integer(rbp.getString(strLocRB +
                    policyIdx + ".noOfAuthServices")).intValue();
            for (int i = 0; i < noOfAuthServices; i++) {
                chainServiceName = rb.getString( strPolIdx +
                        ".AuthService" + i  + ".name");
                authCommon.deleteAuthConfig(realm, chainServiceName);
            }
        } catch (Exception e) {
            log(Level.SEVERE, "createAuthService", e.getMessage());
            e.printStackTrace();
        } finally {
            Reporter.log("createAuthService:" + chainServiceName);
        }
    }
    
    /**
     * Creates the Login XML with required modules
     * @param Map contains test related data
     * @param true if is is negative test
     * @return xml file name
     */
    public String createPolicyEvalXML(String strEvalIdx)
    throws Exception {
        
        String resourceURL = rbg.getString(rbp.getString(strEvalIdx +
                ".resource"));
        String usernameIdx = rbp.getString(strEvalIdx + ".username");
        String userName = rbp.getString(usernameIdx);
        String passwordIdx = rbp.getString(strEvalIdx + ".password");
        String passWord = rbp.getString(passwordIdx);
        String expResult = rbp.getString(strEvalIdx + ".expectedResult");
        executeGatewayMode = new Boolean(rbg.getString("com.sun.identity." +
                "agents." + "config.gateway.enable")).booleanValue();
        int moduleCount = new Integer(rbp.getString(strEvalIdx +
                ".noOfModules"));
        String baseDirectory = getTestBase();
        String fileName = baseDirectory + strEvalIdx + "policyEval.xml";
        PrintWriter out = new PrintWriter(new BufferedWriter
                (new FileWriter(fileName)));
        out.write("<url href=\"" + resourceURL );
        out.write("\">");
        out.write(newline);
        for (int i = 0; i < moduleCount; i ++) {
            if (executeGatewayMode) {
                continue;
            }
            out.write("<form name=\"Login\" IDButton=\"\" >");
            out.write(newline);
            out.write("<input name=\"IDToken1\" value=\"" + userName + "\" />");
            out.write(newline);
            out.write("<input name=\"IDToken2\" value=\"" + passWord + "\" />");
            out.write(newline);
            if (i == (moduleCount - 1)) {
                out.write("<result text=\"" + expResult + "\" />");
                out.write(newline);
            }
            out.write("</form>");
            out.write(newline);
        }
        out.write("</url>");
        out.write(newline);
        out.flush();
        out.close();
        return fileName;
    }
}
