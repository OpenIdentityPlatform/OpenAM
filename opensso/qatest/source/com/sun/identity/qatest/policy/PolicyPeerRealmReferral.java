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
 * $Id: PolicyPeerRealmReferral.java,v 1.3 2009/08/05 21:42:36 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class has the methods to create and evaluate policies using client
 * policy evaluation API
 */

public class PolicyPeerRealmReferral extends TestCommon {
    
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
    private String strReferringPeerOrg;
    private String strLocRB = "PolicyPeerRealmReferral";
    private String strGblRB = "PolicyGlobal";
    private String strRefRB = "PolicyReferral";
    
    /**
     * Class constructor. No arguments
     */
    public PolicyPeerRealmReferral()
    throws Exception {
        super("PolicyPeerRealmReferral");
        mpc = new PolicyCommon();
        rbg = ResourceBundle.getBundle("policy" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("policy" + fileseparator + strLocRB);
        rbr = ResourceBundle.getBundle("policy" + fileseparator + strRefRB);
    }
    
    /**
     * This method sets up all the required identities, generates the xmls and
     * creates the policies for the root realm and subrealms with peerOrg 
     *  referrals in the server
     */
    @Parameters({"policyIdx", "evaluationIdx", "setup", "cleanup", "peAtOrg", 
    "referringPeerOrg", "dynamic"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String evaluationIdx, String setup, 
            String cleanup, String peAtOrg, String referringPeerOrg, 
            String dynamic)
    throws Exception {
        Object[] params = {policyIdx, evaluationIdx, setup, cleanup, dynamic,
        peAtOrg, referringPeerOrg};
        entering("setup", params);
        try {
            polIdx = new Integer(policyIdx).intValue();
            evalIdx = new Integer(evaluationIdx).intValue();
            strSetup = setup;
            strCleanup = cleanup;
            strPeAtOrg = peAtOrg;
            strReferringPeerOrg = referringPeerOrg;
            strDynamic = dynamic;
            if (strSetup.equals("true")) {
                if (strPeAtOrg.equals(realm)) {
                    mpc.createIdentities("policy" + fileseparator + strLocRB,
                            polIdx,  strPeAtOrg );
                    mpc.createPolicyXML("policy" + fileseparator + strGblRB,
                            "policy" + fileseparator + strLocRB, polIdx,
                            strLocRB +  ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
                } else {
                    mpc.createRealm("/" + strPeAtOrg);
                    mpc.createRealm("/" + referringPeerOrg);
                    mpc.createIdentities("policy" + fileseparator + strLocRB,
                            polIdx, strPeAtOrg);

                    // now create the referral policy at root realm
                    if (strDynamic.equals("false")) {
                        mpc.createReferralPolicyXML("policy" + fileseparator +
                                strGblRB, "policy" + fileseparator + strRefRB,
                                "policy" + fileseparator + strLocRB, polIdx,
                                strRefRB + ".xml");
                        assert (mpc.createPolicy(strRefRB + ".xml", "/" ));

                    //now create the referral policy at peerorg 
                        mpc.createPeerReferralPolicyXML("policy" + 
				fileseparator + strGblRB, "policy" + 
				fileseparator + strRefRB, "policy" + 
                                fileseparator + strLocRB, polIdx,
                                strRefRB + "peerOrg" +  ".xml" , 
                                strReferringPeerOrg);
                        assert (mpc.createPolicy(strRefRB + "peerOrg" + ".xml",
                                strReferringPeerOrg ));
 
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
     * This method evaluates the policies using the client policy evaluation API
     * Policy_sub, Policy_ldapFilter, Policy_sub_exclude, Policy_Wildcard, Subre
     * alm, Combination policies, policy response attributes
     */
    @Parameters({ "peAtOrg"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyAPI(String peAtOrg)
    throws Exception {
        Object[] params = {peAtOrg};
        entering("evaluatePolicyAPI", params);
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
        mpc.evaluatePolicyThroughAPI(resource, userToken, action, map,
                expResult, idIdx);
        exiting("evaluatePolicyAPI");
    }
    
    /**
     * This method cleans all the identities and policies  that were setup
     */
    @Parameters({"peAtOrg", "referringPeerOrg", "dynamic"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String peAtOrg, String referringPeerOrg, String dynamic)
    throws Exception {
        Object[] params = {peAtOrg, referringPeerOrg, dynamic};
        entering("cleanup", params);
        strDynamic = dynamic;
        try {
            if (strCleanup.equals("true")) {
                if (peAtOrg.equals(realm)) {
                    mpc.deleteIdentities("policy" + fileseparator + strLocRB,
                            polIdx, peAtOrg);
                    assert (mpc.deletePolicies("policy" + fileseparator +
                            strLocRB, polIdx, peAtOrg));
                } else {
                    mpc.deleteRealm(referringPeerOrg);
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
}
