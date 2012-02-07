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
 * $Id: PolicyEvaluationTestFactory.java,v 1.2 2008/06/26 20:16:33 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.policy;

import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.Factory;

/**
 * This class reads the properties from the resource bundle and splits into
 * different maps as needed. Calls evaluation test class for each test case by
 * passing the needed maps for evalaution
 *
 */
public class PolicyEvaluationTestFactory extends TestCommon {
    
    private Map policyTestMap ;
    
    /**
     * Empty constructor
     * @param none
     */
    public PolicyEvaluationTestFactory() {
        super("PolicyEvaluationTestFactory");
    }
    
    /**
     * Evaluates the policy with given parameters. Calls policytestevaluation
     * with parameters needed for each test
     * Testcase IDs : AM_Policy_AuthCondition, AM_Policy_ExclusiveSubjects
     * AM_Policy_DnsCondition, AM_Policy_Rules, AM_Policy_AmSubjects
     * @param none
     */
    @Factory
    public Object[] testPolicyEvaluation()
    throws Exception {
        entering("testPolicyEvaluation", null);
        Map policyTestMap ;
        Map localMapScenario;
        Map localMapIdentity;
        Map localMapTestData;
        Map localMapExecute;
        Map localMapEnvParams;
        List result= new ArrayList();
        String REQUEST_AUTH_LEVEL = "requestAuthLevel";
        String REQUEST_AUTH_SCHEMES = "requestAuthSchemes";
        String REQUEST_IP = "requestIp";
        String REQUEST_DNS_NAME = "requestDnsName";
        String REQUEST_TIME = "requestTime";
        String REQUEST_DAY = "requestDay";
        String REQUEST_TIME_ZONE = "requestTimeZone";
        try {
            ResourceBundle client =
                    ResourceBundle.getBundle("policy" + fileseparator +
                    "policyevaluationtest");
            policyTestMap = new HashMap<String, String>();
            for (Enumeration e = client.getKeys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                String value = (String)client.getString(key);
                policyTestMap.put(key, value);
            }
            Integer sCount = new Integer((String)policyTestMap.
                    get("scenariocount"));
            for (int i = 0; i < sCount.intValue(); i++) {
                localMapScenario = new HashMap();
                localMapScenario.put(("scenario" + i + ".realmname"),
                        policyTestMap.get("scenario" + i + ".realmname"));
                localMapScenario.put(("scenario" + i +  ".name"),
                        policyTestMap. get("scenario" + i + ".name"));
                localMapScenario.put(("policyname"), policyTestMap.
                        get("scenario" + i + ".name"));
                localMapScenario.put("scenarionumber", i);
                log(logLevel, "testPolicyEvaluation",
                        "executing the following " +  "Scenario:" +
                        localMapScenario);
                String rbName = (String)policyTestMap.get("scenario" + i +
                        ".name");
                localMapScenario.put("scenariocount", sCount);
                ResourceBundle scenario = ResourceBundle.getBundle("policy" +
                        fileseparator + rbName);
                Integer testCount = new Integer((String)scenario.
                        getString("testcount"));
                Integer policyCount = new Integer((String)scenario.
                        getString("policycount"));
                localMapIdentity = new HashMap();
                localMapIdentity.put("testcount", testCount);
                localMapIdentity.put("policycount", policyCount);
                localMapTestData = new HashMap();
                for (Enumeration e = scenario.getKeys();
                e.hasMoreElements(); ) {
                    String key = (String)e.nextElement();
                    String value = (String)scenario.getString(key);
                    localMapTestData.put(key, value);
                    if (key. contains("Identity")) {
                        localMapIdentity.put(key, value);
                    }
                }
                log(logLevel,"testPolicyEvaluation", "localMap" +
                        "Identity:" + localMapIdentity);
                
                /** now get the scenario file and split it to diff maps and
                 * pass them to the main test.
                 * Scenario map-- all the data related to scenario
                 * Identity map-- all the data related to identities creation
                 * Env map-- all the env params needed for policy evaluation
                 * Execute map-- all the other params needed for policy
                 * evaluation
                 */
                for (int j = 0; j<testCount.intValue(); j++) {
                    localMapExecute = new HashMap();
                    localMapExecute.put("username", localMapTestData.get
                            ("test" + j + ".Identity"+".username"));
                    localMapExecute.put("resourcename", localMapTestData.get
                            ("test"  + j + ".resourcename"));
                    localMapExecute.put("realmname", localMapTestData.get
                            ("test" + j + ".Identity" + ".realmname"));
                    localMapExecute.put("password", localMapTestData.get
                            ("test" + j +".Identity" + ".password"));
                    localMapExecute.put("result", localMapTestData.get
                            ("test" + j + ".result"));
                    localMapExecute.put("action", localMapTestData.get
                            ("test" + j + ".action"));
                    localMapExecute.put("desc", localMapTestData.get
                            ("test" + j + ".desc"));
                    localMapExecute.put("testid", localMapScenario.get
                            ("policyname") + "_testcase_" + j);
                    log(logLevel, "testPolicyEvaluation",
                            "local Map Execute params:" + localMapExecute);
                    
                    // now populate the env map with the values for each test
                    localMapEnvParams= new HashMap();
                    String authlevel = (String)localMapTestData.
                            get("test" + j + ".authlevel");
                    if ((authlevel != null) && (!authlevel.equals(""))) {
                        Set authlevelSet = new HashSet();
                        authlevelSet.add(authlevel);
                        localMapEnvParams.put(REQUEST_AUTH_LEVEL,
                                authlevelSet);
                    }
                    String authscheme = (String)localMapTestData.get("test" +
                            j +".authscheme");
                    if ((authscheme != null) && (!authscheme.equals(""))) {
                        Set authschemeSet = new HashSet();
                        authschemeSet.add(authscheme);
                        localMapEnvParams.put(REQUEST_AUTH_SCHEMES,
                                authschemeSet);
                    }
                    String ip = (String)localMapTestData.get("test" + j +
                            ".requestip");
                    if ((ip != null) && (!ip.equals(""))) {
                        Set ipSet = new HashSet();
                        ipSet.add(ip);
                        localMapEnvParams.put(REQUEST_IP, ipSet);
                    }
                    String dnsname = (String)localMapTestData.get("test" +
                            j+ ".dnsname");
                    if ((dnsname != null) && (!dnsname.equals(""))) {
                        Set dnsSet = new HashSet();
                        dnsSet.add(dnsname);
                        localMapEnvParams.put(REQUEST_DNS_NAME, dnsSet);
                    }
                    String day = (String)localMapTestData.get("test" +
                            j + ".day");
                    if ((day != null) && (!day.equals(""))) {
                        Set daySet = new HashSet();
                        daySet.add(day);
                        localMapEnvParams.put(REQUEST_DAY, daySet);
                    }
                    String time = (String)localMapTestData.get("test" + j
                            + ".time");
                    Set timeSet = new HashSet();
                    if ((time != null) && (!time.equals(""))) {
                        if (time.equalsIgnoreCase("currenttime")) {
                            log(logLevel, "testPolicyEvaluation",
                                    "invalid time." +"The current time " +
                                    "is used instead");
                            String timeString=new Long(System.
                                    currentTimeMillis()).toString();
                            timeSet.add(timeString);
                        }else {
                            timeSet.add(time);
                        }
                        localMapEnvParams.put(REQUEST_TIME, timeSet);
                    }
                    log(logLevel, "testPolicyEvaluation", "mapIdentity" +
                            localMapIdentity);
                    log(logLevel, "testPolicyEvaluation", "mapscenario" +
                            localMapScenario);
                    log(logLevel, "testPolicyEvaluation", "mapexecute" +
                            localMapExecute);
                    log(logLevel, "testPolicyEvaluation", "env parameters" +
                            localMapEnvParams);
                    result.add(new PolicyEvaluationTest(localMapIdentity,
                            localMapScenario, localMapExecute,
                            localMapEnvParams, j, i));
                }
            }
        } catch (MissingResourceException m) {
            log(Level.SEVERE, "testPolicyEvaluation", m.getMessage(),
                    null);
            m.printStackTrace();
            throw m;
        } catch (Exception e){
            log(Level.SEVERE, "testPolicyEvaluation", e.getMessage(),
                    null);
            e.printStackTrace();
            throw e;
        }
        exiting("testPolicyEvaluation");
        return result.toArray();
    }
}
