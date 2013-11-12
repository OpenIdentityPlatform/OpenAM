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
 * $Id: PolicyEvaluationTest.java.v 1.1
 * 2007/04/09 12:40:00 arunav Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.StringBuffer;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class creates identites and policies and evaluates the policy.
 * The Polices with different rules, subjects, response providers and
 * conditions can be evaluated. It cleans up the identities and policies after
 * the completion of the tests.
 */
public class PolicyEvaluationTest extends TestCommon {
    private SSOToken usertoken;
    private Map mapConfig;
    private Map mapExecute;
    private Map mapEnvParams;
    private Map mapIdentity;
    private Map mapScenario;
    private int initSetup;
    private int initCleanup;
    private List idList;
    private PolicyCommon  pc;
    
    /**
     * Constructor for PolicyEvaluationTest class. Parameters are maps with
     * diff name value keys needed to create Identities, policies and
     * evaluate the policies.
     * @param map, map, map, map, int, int
     */
    public PolicyEvaluationTest(Map mI, Map mSC, Map mE, Map mEP, int j,
            int i) 
    throws Exception {
        super("PolicyEvaluationTest");
        mapIdentity = mI;
        mapExecute = mE;
        mapEnvParams = mEP;
        mapScenario = mSC;
        initSetup = j;
        initCleanup = i;
        pc = new PolicyCommon();
    }
    
    /**
     * Creates Identities and policies for policy evaluation only for
     * the first time
     * @param none
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);
        log(logLevel, "setup", "initsetup from setup" + initSetup);
        log(logLevel, "setup", "initcleanup" + initCleanup);
        Reporter.log("setup"  + mapIdentity);
        Reporter.log("setup " + mapScenario);
        Reporter.log("setup " + mapExecute);
        Reporter.log("setup " + mapEnvParams);
        try {
            if (initSetup == 0) {
                String scenarioname = (String)mapScenario.get("scenario" +
                        initCleanup + ".name");
                String realmname = (String)mapScenario.get("scenario" +
                        + initCleanup + ".realmname");
                log(logLevel, "setup", "Scenarioname" + scenarioname);
                
                //now create identities and policies needed for all the tests
                pc. createIds(mapIdentity);
                Thread.sleep(notificationSleepTime);
                pc. createPolicy(scenarioname);
            }
        }catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Evaluates the policy with token, resource and env parameters and asserts
     * the decision
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicy()
    throws Exception {
        entering("evaluatePolicy", null);
        log(logLevel, "evaluatePolicy ", null);
        log(logLevel, "evaluatePolicy ", initSetup);
        log(logLevel, "evaluatePolicy", "mapIdentity" + mapIdentity);
        log(logLevel, "evaluatePolicy", "mapscenario" +
                mapScenario);
        log(logLevel, "evaluatePolicy", "mapexecute" +
                mapExecute);
        log(logLevel, "evaluatePolicy", "env parameters" +
                mapEnvParams);
        Reporter.log("evaluatePolicy" + mapScenario);
        Reporter.log("evaluatePolicy" + mapExecute);
        Reporter.log("evaluatePolicy" +
                mapEnvParams);
        try {
            usertoken = getToken((String)mapExecute.get("username"),
                    (String)mapExecute.get("password"),
                    (String)mapExecute.get("realmname"));
            if (mapIdentity.containsKey("test" + initSetup + 
                        ".Identity." + "spcount")){
                Integer spCount = new Integer((String)mapIdentity.get
                    ("test" + initSetup + ".Identity.spcount"));
                if (spCount > 0 ) {
                   pc.setProperty(usertoken, mapIdentity, initSetup);
                } 
            }
            PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgent" +
                    "Service");
            Set actions = new HashSet();
            actions.add((String)mapExecute.get("action"));
            boolean expectedResult = Boolean.valueOf((String)mapExecute.
                    get("result"));
            boolean pResult = pe.isAllowed(usertoken, (String)mapExecute.
                    get("resourcename"), (String)mapExecute.get("action"),
                    mapEnvParams);
            PolicyDecision pd = pe.getPolicyDecision(usertoken,
                    (String)mapExecute.get("resourcename"), actions,
                    mapEnvParams);
            log(logLevel, "evaluatePolicy", "decision" + pResult);
            log(logLevel, "evaluatePolicy", pd.toXML());
            assert (pResult == expectedResult);
        }catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicy", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }finally {
            destroyToken(usertoken);
        }
        exiting("evaluatePolicy");
    }
    
    /**
     * Deletes the Identities and policy if it is the last test
     * in the scenario
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            Integer tCount = (Integer)mapIdentity.get("testcount");
            Integer sCount = (Integer)mapScenario.get("scenariocount");
            Integer pCount = (Integer)mapIdentity.get("policycount");
            log(logLevel, "cleanup", "tcount" + tCount);
            log(logLevel, "cleanup", "pcount" + pCount);
            log(logLevel, "cleanup", "initSetup" + initSetup);
            log(logLevel, "cleanup", mapIdentity);
            log(logLevel, "cleanup", "mapscenario" +
                    mapScenario);
            Reporter.log("cleanup" + mapIdentity);
            Reporter.log("cleanup" + mapScenario);
            
           /*
            * delete the policy and users only if it is last test in
            * scenario
            */
            if (initSetup == tCount-1) {
                String policyName = (String)mapScenario.get("policyname");
                pc.deletePolicies(policyName, pCount );
                pc.deleteIds(mapIdentity);               
            }
        }catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }
}
