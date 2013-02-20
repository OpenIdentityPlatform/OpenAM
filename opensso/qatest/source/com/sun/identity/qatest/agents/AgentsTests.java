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
 * $Id: AgentsTests.java,v 1.6 2009/01/26 23:45:46 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class evaluates general policy tests though the
 * for different kinds of policy rules, subjects, conditions
 * and response providers.
 */
public class AgentsTests extends TestCommon {
    
    private int polIdx;
    private int evalIdx;
    private String strSetup;
    private String strCleanup;
    private AgentsCommon mpc;
    private boolean executeAgainstOpenSSO;
    private ResourceBundle rbg;
    private ResourceBundle rbp;
    private String strLocRB = "AgentsTests";
    private String strGblRB = "agentsGlobal";

    /**
     * Class constructor. Instantiates the ResourceBundles and other
     * common class objects needed by the tests.
     */
    public AgentsTests() 
    throws Exception{
        super("AgentsTests");
        mpc = new AgentsCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("agents" + fileseparator + strLocRB);
        executeAgainstOpenSSO = new Boolean(rbg.getString(strGblRB +
                ".executeAgainstOpenSSO")).booleanValue();
    }
    
    /**
     * Creates the policy on the server.
     */
    @Parameters({"policyIdx","evaluationIdx","setup","cleanup"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String evaluationIdx, String setup
            , String cleanup)
    throws Exception {
        Object[] params = {policyIdx, evaluationIdx, setup, cleanup};
        entering("setup", params);
        if (executeAgainstOpenSSO) {
            try {
                polIdx = new Integer(policyIdx).intValue();
                evalIdx = new Integer(evaluationIdx).intValue();
                strSetup = setup;
                strCleanup = cleanup;
                if (strSetup.equals("true")) {
                    mpc.createIdentities("agents" + fileseparator + strLocRB, 
                            polIdx);
                    mpc.createPolicyXML("agents" + fileseparator + strGblRB, 
                            "agents" + fileseparator + strLocRB, polIdx, 
                            strLocRB + ".xml");
                    mpc.createPolicy(strLocRB + ".xml");
                }
            } catch (Exception e) {
                log(Level.SEVERE, "setup", e.getMessage(), null);
                e.printStackTrace();
                throw e;
            }
        } else
            log(logLevel, "setup", "Executing against non OpenSSO Install");
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

        mpc.evaluatePolicyThroughAgents(resource, username, password,
                expResult);

        exiting("evaluatePolicyAgents");
    }

    /**
     * Deletes policies and users.
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        if (executeAgainstOpenSSO) {
            try {
                if (strCleanup.equals("true")) {
                    mpc.deleteIdentities("agents" + fileseparator + strLocRB, 
                            polIdx);
                    mpc.deletePolicies("agents" + fileseparator + strLocRB, 
                            polIdx);
                }
            } catch (Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage(), null);
                e.printStackTrace();
                throw e;
            }
        } else
            log(logLevel, "cleanup", "Executing against non OpenSSO Install");
        exiting("cleanup");
    }
}
