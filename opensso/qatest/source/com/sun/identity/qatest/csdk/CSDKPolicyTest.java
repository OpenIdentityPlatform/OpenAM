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
 * with the fields enclosed by brackets [] replaced by224
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: 
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.csdk;

import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.CSDKCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;


/**
 * This class tests the policy related functions of C SDK api
 */
public class CSDKPolicyTest extends TestCommon {

    private int polIdx;
    private int evalIdx;
    private String strSetup;
    private String strCleanup;
    private String strPeAtOrg;    
    private PolicyCommon mpc;
    private ResourceBundle rbg;
    private ResourceBundle rbp;     
    private String strLocRB = "CSDKPolicyTest";
    private String strGblRB = "PolicyGlobal";   
    private String libraryPath;
    private String directoryPath;
    private String bootstrapFile;
    private String configurationFile;
    private CSDKCommon cc;

    /**
     * Class constructor. No arguments
     */
    public CSDKPolicyTest()
            throws Exception {
        super("PolicyTests");
        mpc = new PolicyCommon();
        cc = new CSDKCommon();
        rbg = ResourceBundle.getBundle("policy" + fileseparator + strGblRB);
        rbp = ResourceBundle.getBundle("csdk" + fileseparator + strLocRB);       
    }

    /**
     * This method sets up all the required identities, generates the xmls and
     * creates the policies in the server
     */
    @Parameters({"policyIdx", "evaluationIdx", "setup", "cleanup", "peAtOrg",
        "dynamic"})
    @BeforeClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String policyIdx, String evaluationIdx, String setup,
                String cleanup, String peAtOrg, String dynamic)
            throws Exception {
        Object[] params = {policyIdx, evaluationIdx, setup, cleanup, dynamic,
            peAtOrg};
        entering("setup", params);
        Map ldMap = cc.getLibraryPath();
        libraryPath = (String) ldMap.get("libraryPath");
        directoryPath = (String) ldMap.get("directoryPath");
        bootstrapFile = cc.getBootStrapFilePath();
        configurationFile = cc.getConfigurationFilePath();
        try {
            polIdx = new Integer(policyIdx).intValue();
            evalIdx = new Integer(evaluationIdx).intValue();
            strSetup = setup;
            strCleanup = cleanup;
            strPeAtOrg = peAtOrg;           
            if (strSetup.equals("true")) {               
                    mpc.createIdentities("csdk" + fileseparator + strLocRB,
                            polIdx, strPeAtOrg);
                    mpc.createPolicyXML("policy" + fileseparator + strGblRB,
                            "csdk" + fileseparator + strLocRB, polIdx,
                            strLocRB + ".xml", strPeAtOrg);
                    assert (mpc.createPolicy(strLocRB + ".xml", strPeAtOrg));
                    Thread.sleep(notificationSleepTime);                
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            cleanup(peAtOrg , dynamic);
            throw e;
        }
        exiting("setup");
    }

    /**
     * This method evaluates the policies using the C SDK API
     */
    @Parameters({"peAtOrg", "conditionName" , "dynamic" })
    @Test(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyAPI(String peAtOrg, String conditionName,
            String dynamic )
            throws Exception {
        Object[] params = {peAtOrg};
        entering("evaluatePolicyAPI", params);
        String results;
        String error;
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
        Map map = mpc.getPolicyEnvParamMap("csdk" + fileseparator + strLocRB,
                polIdx, evalIdx);
        Reporter.log("Test description: " + description);
        Reporter.log("Resource: " + resource);
        Reporter.log("Username: " + username);
        Reporter.log("Password: " + password);
        Reporter.log("Action: " + action);
        Reporter.log("Env Param: " + map);
        Reporter.log("Expected Result: " + expResult);
        SSOToken userToken = getToken(username, password, peAtOrg);
        try {
            String condition;
            if (map != null) {
                HashSet set = (HashSet) map.get(conditionName);
                Iterator it = set.iterator();
                condition = (String) it.next();
            } else {
                condition = "" ;
            }
            ProcessBuilder pb = new ProcessBuilder(directoryPath +
                    fileseparator + "am_policy_test", bootstrapFile ,
                    configurationFile , userToken.getTokenID().toString(),
                    resource, action, condition);
            pb.environment().put("LD_LIBRARY_PATH", libraryPath);
            pb.directory(new File(directoryPath));
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader
                    (new InputStreamReader(p.getInputStream()));
            StringBuffer sbResults = new StringBuffer();
            while ((results = stdInput.readLine()) != null) {                
                sbResults = sbResults.append(results);
            }
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            while ((error = stdError.readLine()) != null) {
                 sbResults = sbResults.append(error);
            }
            if (sbResults.toString().contains(expResult)) {
                assert true;
            } else {
                assert false;
            }
            log(Level.FINEST, "evaluatePolicyAPI", sbResults);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyAPI", e.getMessage());
            cleanup(peAtOrg , dynamic);
            throw e;
        } finally {
            destroyToken(userToken);
        }
        exiting("evaluatePolicyAPI");
    }

    /**
     * This method cleans all the identities and policies  that were setup
     */
    @Parameters({"peAtOrg", "dynamic"})
    @AfterClass(groups = {"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String peAtOrg, String dynamic)
            throws Exception {
        Object[] params = {peAtOrg, dynamic};
        entering("cleanup", params);        
        try {
            if (strCleanup.equals("true")) {                
                    mpc.deleteIdentities("csdk" + fileseparator + strLocRB,
                            polIdx, peAtOrg);
                    assert (mpc.deletePolicies("csdk" + fileseparator +
                            strLocRB, polIdx, peAtOrg));               
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());            
            throw e;
        }
        exiting("cleanup");
    }
}
