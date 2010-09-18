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
 * $Id: PolicyRespTest.java,v 1.8 2009/08/05 21:42:36 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.ResourceBundle;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class has the methods to create and evaluate policies with the response
 * provider attributes using client policy evaluation API
 */

public class PolicyRespTest extends TestCommon {
    
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
    private String strLocRB = "PolicyRespTest";
    private String strGblRB = "PolicyGlobal";
    private String strRefRB = "PolicyReferral";
    
    /**
     * Class constructor. Sets class variables.
     */
    public PolicyRespTest()
    throws Exception {
        super("PolicyRespTest");
        mpc = new PolicyCommon();
        rbg = ResourceBundle.getBundle("policy" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("policy" + fileseparator + strLocRB);
        rbr = ResourceBundle.getBundle("policy" + fileseparator + strRefRB);
    }
    
    /**
     * Setup method which will invoked based on the test parameters.
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
                if (strPeAtOrg.equals(realm)) {
                    mpc.createIdentities("policy" + fileseparator + strLocRB,
                            polIdx,  strPeAtOrg );
                    mpc.createPolicyXML("policy" + fileseparator + strGblRB,
                            "policy" + fileseparator + strLocRB, polIdx,
                            strLocRB + ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
                } else {
                    mpc.createRealm("/" + strPeAtOrg);
                    mpc.createIdentities("policy" + fileseparator + strLocRB,
                            polIdx, strPeAtOrg);
                    
                    if (strDynamic.equals("false")) {
                        mpc.createReferralPolicyXML("policy" + fileseparator +
                                strGblRB, "policy" + fileseparator + strRefRB,
                                "policy" + fileseparator + strLocRB, polIdx,
                                strRefRB +  ".xml");
                        strReferringOrg = rbr.getString(strLocRB + polIdx +
                                ".referringOrg");
                        assert (mpc.createPolicy(strRefRB + ".xml", 
                                strReferringOrg ));
                    } else {
                        strDynamicRefValue = "true";
                        mpc.setDynamicReferral(strDynamicRefValue);
                        mpc.createDynamicReferral("policy" + fileseparator +
                                strGblRB, "policy" + fileseparator + strRefRB,
                                "policy" + fileseparator + strLocRB, polIdx,
                                strPeAtOrg);
                    }
                    mpc.createPolicyXML("policy" + fileseparator + strGblRB,
                            "policy" + fileseparator + strLocRB, polIdx,
                            strLocRB + ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
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
     * Evaluates the policy and for the valid decision
     * all of them are defined and present in the system
     *
     */
    @Parameters({ "peAtOrg", "evalIdx"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyWithRespTest(String peAtOrg)
    throws Exception {
        Object[] params = {peAtOrg, evalIdx};
        entering("evaluatePolicyWithRespTest", params);
        String strEvalIdx = strLocRB + polIdx + ".evaluation" + evalIdx;
        String resource = rbg.getString(rbp.getString(strEvalIdx +
                ".resource"));
        String usernameIdx = rbp.getString(strEvalIdx + ".username");
        String username = rbp.getString(usernameIdx);
        String passwordIdx = rbp.getString(strEvalIdx + ".password");
        String password = rbp.getString(passwordIdx);
        String action = rbp.getString(strEvalIdx + ".action");
        String expResult = rbp.getString(strEvalIdx + ".expectedResult");
        String description = rbp.getString(strEvalIdx + ".description");
        
        boolean evalResult ;
        
        int idIdx = mpc.getIdentityIndex(usernameIdx);
        Map map = mpc.getPolicyEnvParamMap("policy" + fileseparator + strLocRB,
                polIdx, evalIdx);
        
        Reporter.log("Test description: " + description);
        Reporter.log("Resource: " + resource);
        Reporter.log("Username: " + username);
        Reporter.log("Password: " + password);
        Reporter.log("Action: " + action);
        Reporter.log("Env Param: " + map);
        Reporter.log("Expected Result: " + expResult);
        
        SSOToken userToken = getToken(username, password, peAtOrg);
        mpc.setProperty("policy" + fileseparator + strLocRB, userToken, polIdx,
                idIdx);
        
        PolicyEvaluator pe =
                new PolicyEvaluator("iPlanetAMWebAgentService");
        Set actions = new HashSet();
        actions.add(action);
        boolean pResult = pe.isAllowed(userToken, resource, action, map);
        PolicyDecision pd = pe.getPolicyDecision(userToken, resource,
                actions, map);
        boolean expectedResult = new Boolean(expResult).booleanValue();
        
        //verify the resp only for the expected policy result
        if (pResult == expectedResult) {
            evalResult = verifyRespMap(strLocRB, polIdx, evalIdx, pd);
            assert evalResult;
        } else
            assert (pResult == expectedResult);
        exiting("evaluatePolicyWithRespTest");
    }
    
    /**
     * Cleans up the policies
     *
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
                if (peAtOrg.equals(realm)) {
                    mpc.deleteIdentities("policy" + fileseparator + strLocRB,
                            polIdx, peAtOrg);
                    assert (mpc.deletePolicies("policy" + fileseparator +
                            strLocRB, polIdx, peAtOrg));
                    mpc.deleteDynamicAttr("policy" + fileseparator + strLocRB,
                            polIdx, peAtOrg);                    
                } else {
                    mpc.deleteIdentities("policy" + fileseparator + strLocRB,
                            polIdx, peAtOrg);
                    mpc.deleteRealm(peAtOrg);
                    if (strDynamic.equals("false")) {
                        mpc.deleteReferralPolicies("policy" + fileseparator +
                                strLocRB, "policy" + fileseparator + strRefRB,
                                polIdx);
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
     * verifies the resp attributes and returns boolean
     */
    public boolean verifyRespMap(String strLocRB, int polIdx, int evalIdx,
            PolicyDecision pd)
    throws Exception {
        
        Map polrespAttrMap = new HashMap();
        boolean verifyRespResult = false;
        try {
            ResourceBundle rb = ResourceBundle.getBundle("policy" +
                    fileseparator + strLocRB);
            String glbPolIdx = strLocRB + polIdx;
            String locEvalIdx = glbPolIdx + ".evaluation";
            
            //populate the map with the values from property files in key,
            //list format
            int noOfRespAttrs = new Integer(rbp.getString(locEvalIdx +
                    evalIdx + ".noOfResponseAttributes")).intValue();
            if (noOfRespAttrs != 0) {
                String name;
                int noOfVal;
                for (int i = 0; i < noOfRespAttrs; i++) {
                    name =  rbp.getString(locEvalIdx + evalIdx +
                            ".responseattribute" + i + ".name");
                    noOfVal =  new Integer(rbp.getString(locEvalIdx + evalIdx +
                            ".responseattribute" + i +
                            ".noOfValues")).intValue();
                    Set set = null;
                    if (noOfVal != 0) {
                        set = new HashSet();
                        String strVal;
                        for (int j = 0; j < noOfVal; j++) {
                            strVal =  rbp.getString(locEvalIdx + evalIdx +
                                    ".responseattribute" + i + ".value" + j);
                            set.add(strVal);
                        }
                    }
                    polrespAttrMap.put(name, set);
                }
            }
            //now compare the maps
            Map decRespAttrMap = new HashMap();
            decRespAttrMap = pd.getResponseAttributes();
            log(Level.FINE, "verifyRespMap:decRespAttrMap entryset",
                    decRespAttrMap);
            log(Level.FINE, "verifyRespMap:polRespAttrMap entryset",
                    polrespAttrMap);
            if (decRespAttrMap.equals(polrespAttrMap)) {
                verifyRespResult = true;
            } else {
                verifyRespResult = false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "verifyRespMap", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return verifyRespResult;
    }
}
