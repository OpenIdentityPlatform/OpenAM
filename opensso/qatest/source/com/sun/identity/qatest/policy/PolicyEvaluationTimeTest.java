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
 * $Id: PolicyEvaluationTimeTest.java,v 1.7 2009/08/05 21:42:36 rmisra Exp $:
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class creates reads the properties from the resource bundle and creates
 * identites and policies and evaluates the policy. It gets the system
 * time and dynamically generates the policy xml with different time conditions
 * and rules and subjects. It cleans up the identities and policies after
 * the completion of the tests.
 */
public class PolicyEvaluationTimeTest extends TestCommon {
    private SSOToken usertoken;
    private Map mapIdentity;
    private PolicyCommon  pc;
    public static String newline = System.getProperty("line.separator");
    public static String fileseparator = System.getProperty("file.separator");
    
    /**
     * Constructor for PolicyEvaluationTimeTest class. It creates a map
     * from resource bundle and adds the testcount to the map
     * @param none
     */
    public PolicyEvaluationTimeTest() 
    throws Exception {
        super("PolicyEvaluationTimeTest");
        pc = new PolicyCommon();
        mapIdentity = new HashMap();
        try {
            mapIdentity = getMapFromResourceBundle("policy" + fileseparator +
                    "policyevaluationtimetest");
            Integer testCount = new Integer((String)
            mapIdentity.get("testcount"));
            mapIdentity.put("testcount", testCount);
        } catch (Exception e) {
            log(Level.SEVERE, "PolicyEvaluationTimeTest", e.getMessage(), null);
            e.printStackTrace();
        }
    }
    
    /**
     * Creates Identities for policy evaluation
     * @param none
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup() 
    throws Exception {
        entering("setup", null);
        log(logLevel, "setup",  mapIdentity);
        Reporter.log( "setup"  + mapIdentity);
        try {
            log(logLevel, "setup", "PolicyEvaluationTimeTest");
            
            //now create identities and policies needed for all the tests
            pc. createIds(mapIdentity);
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: day condition = today to today
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDayPositive()
    throws Exception {
        entering("evaluatePolicyDayPositive", null);
        try {
            int testCaseId = 0;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_daypositive";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            String conditionDayBegin = (formatter.format
                    (rightNow.getTime())).toLowerCase();
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 0);
            String conditionDayEnd = (formatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId + ".tconditiontype",
                    "day");
            policyXmlParams.put("test" + testCaseId +
                    ".beginday", conditionDayBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".endday", conditionDayEnd);
            policyXmlParams.put("test" + testCaseId + ".zone",
                    "America/Los_Angeles");
            policyXmlParams.put("test" + testCaseId + ".expectedresult",
                    expectedResult);
            policyXmlParams.put("test" + testCaseId + ".policyname",
                    policyName);
            Reporter.log("evaluatePolicyDayPositive" + policyXmlParams);
            log(logLevel, "evaluatePolicyDayPositive", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDayPositive",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDayPositive");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters, asserts
     * the decision and deletes the policies
     * Condition1: day condition = today + 5th day
     * Condition2: auth scheme = ldap
     * Result: The user should be denied
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDayNegative()
    throws Exception {
        entering("evaluatePolicyDayNegative", null);
        try {
            int testCaseId = 1;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = false;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_dayNegative";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 5);
            String conditionDay = (formatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "day");
            policyXmlParams.put("test" + testCaseId +
                    ".beginday", conditionDay);
            policyXmlParams.put("test" + testCaseId +
                    ".endday", conditionDay);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDayNegative" + policyXmlParams);
            log(logLevel, "evaluatePolicyDayNegative", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDayNegative",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDayNegative");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: day condition = range from today to today + 4
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDayRange()
    throws Exception {
        entering("evaluatePolicyDayRange", null);
        try {
            int testCaseId = 2;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_dayrange";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 4);
            String conditionDay = (formatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionDayEnd = (formatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "day");
            policyXmlParams.put("test" + testCaseId +
                    ".beginday", conditionDay);
            policyXmlParams.put("test" + testCaseId +
                    ".endday", conditionDayEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDayRange" + policyXmlParams);
            log(logLevel, "evaluatePolicyDayRange", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
            
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDayRange", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDayRange");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: day condition = range from today + 1  to today + 4
     * Condition2: auth scheme = ldap
     * Result: The user should be denied
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDayRangeNegative()
    throws Exception {
        entering("evaluatePolicyDayRangeNegative", null);
        try {
            int testCaseId = 3;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = false;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_dayrangeNegative";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("EEE");
            Calendar begining = (Calendar) rightNow.clone();
            begining.add(Calendar.DAY_OF_YEAR, + 1);
            Calendar working = (Calendar) rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 4);
            String conditionDay = (formatter.format
                    (begining.getTime())).toLowerCase();
            String conditionDayEnd = (formatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "day");
            policyXmlParams.put("test" + testCaseId +
                    ".beginday", conditionDay);
            policyXmlParams.put("test" + testCaseId +
                    ".endday", conditionDayEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDayRangeNegative" + policyXmlParams);
            log(logLevel, "evaluatePolicyDayRangeNegative", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            assert (policyResult == expectedResult);
            pc.deletePolicies(policyName, 1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDayRangeNegative",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDayRangeNegative");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: date condition = range from today's date to after 5 days
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDatePositiveRange()
    throws Exception {
        entering("evaluatePolicyDatePositiveRange", null);
        try{
            int testCaseId = 4;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_datePositiveRange";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd");
            String conditionDateBegin = formatter.format(rightNow.getTime());
            Calendar working = (Calendar) rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 5);
            String conditionDateEnd = formatter.format(working.getTime());
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "date");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDatePositiveRange" + policyXmlParams);
            log(logLevel, "evaluatePolicyDatePositiveRange",  policyXmlParams);
            
            //now evaluate the policy and delete the policy
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDatePositiveRange",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDatePositiveRange");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: date condition = range from tomorrow to tomorro + 5 days
     * Condition2: auth scheme = ldap
     * Result: The user should be denied
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDateNegativeRange()
    throws Exception {
        entering("evaluatePolicyDateNegativeRange", null);
        try{
            int testCaseId = 5;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = false;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_dateNegativeRange";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd");
            Calendar tomorrow = (Calendar) rightNow.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, + 1);
            String conditionDateBegin = formatter.format(tomorrow.getTime());
            Calendar working = (Calendar) rightNow.clone();
            working.add(Calendar.DAY_OF_YEAR, + 5);
            String conditionDateEnd = formatter.format(working.getTime());
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "date");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDateNegative" + policyXmlParams);
            log(logLevel, "evaluatePolicyDateNegativeRange ", policyXmlParams);
            
            //now evaluate the policy and delete the policy
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDateNegativeRange",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDateNegativeRange");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision and deletes the policy
     * Condition1: date condition = begin and end today
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDatePositive()
    throws Exception {
        entering("evaluatePolicyDatePositive", null);
        try{
            int testCaseId = 6;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_datePositive";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd");
            String conditionDateBegin = formatter.format(rightNow.getTime());
            String conditionDateEnd = formatter.format(rightNow.getTime());
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "date");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDatePositive" + policyXmlParams);
            log(logLevel, "evaluatePolicyDatePositive", policyXmlParams);
            
            //now evaluate the policy and delete the policy
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDatePositive",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDatePositive");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision and deletes the policy
     * Condition1: date condition = begin date and end date tomorrow
     * Condition2: auth scheme = ldap
     * Result: The user should be denied
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyDateNegative()
    throws Exception {
        entering("evaluatePolicyDateNegative", null);
        try{
            int testCaseId = 7;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = false;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_dateNegative";
            Calendar rightNow = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd");
            Calendar tomorrow = (Calendar) rightNow.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, + 1);
            String conditionDateBegin = formatter.format(tomorrow.getTime());
            String conditionDateEnd = formatter.format(tomorrow.getTime());
            
            //add the params needed for policy creation
            policyXmlParams.put("test" + testCaseId +
                    ".tconditiontype", "date");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".expectedresult", expectedResult);
            policyXmlParams.put("test" + testCaseId +
                    ".policyname", policyName);
            Reporter.log("evaluatePolicyDateNegative" + policyXmlParams);
            log(logLevel, "evaluatePolicyDateNegative", policyXmlParams);
            
            //now evaluate the policy and delete the policy
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDateNegative",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDateNegative");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: time condition with hour along with date negative range
     * Condition2: auth scheme = ldap
     * Result: The user should be denied
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyTimeNegative()
    throws Exception {
        entering("evaluatePolicyTimeNegative", null);
        try {
            int testCaseId = 8;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = false;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_TimeNegative";
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy:MM:dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            Calendar rightNow = Calendar.getInstance();
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.HOUR_OF_DAY, + 5);
            Calendar later = (Calendar)rightNow.clone();
            later.add(Calendar.HOUR_OF_DAY, + 10);
            String conditionDateBegin = (dateFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionDateEnd = (dateFormatter.format
                    (later.getTime())).toLowerCase();
            String conditionTimeBegin = (timeFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionTimeEnd = (timeFormatter.format
                    (later.getTime())).toLowerCase();
            
            //add the params needed for time condition
            policyXmlParams.put("test" + testCaseId + ".tconditiontype",
                    "time");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".begintime", conditionTimeBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".endtime", conditionTimeEnd);
            
            // add the params needed for evaluation
            policyXmlParams.put("test" + testCaseId + ".expectedresult",
                    expectedResult);
            policyXmlParams.put("test" + testCaseId + ".policyname",
                    policyName);
            Reporter.log("evaluatePolicyTimeNegative" + policyXmlParams);
            log(logLevel, "evaluatePolicyTimeNegative", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyTimeNegative",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyTimeNegative");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: time condition with hour along with date postivie range
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyTimePositive()
    throws Exception {
        entering("evaluatePolicyTimePositive", null);
        try {
            int testCaseId = 9;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_TimePositive";
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy:MM:dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            Calendar rightNow = Calendar.getInstance();
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.HOUR_OF_DAY, + 20);
            String conditionDateBegin = (dateFormatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionDateEnd = (dateFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionTimeBegin = (timeFormatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionTimeEnd = (timeFormatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for time condition
            policyXmlParams.put("test" + testCaseId + ".tconditiontype",
                    "time");
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".begintime", conditionTimeBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".endtime", conditionTimeEnd);
            
            // add the params needed for evaluation
            policyXmlParams.put("test" + testCaseId + ".expectedresult",
                    expectedResult);
            policyXmlParams.put("test" + testCaseId + ".policyname",
                    policyName);
            Reporter.log("evaluatePolicyTimePositive" + policyXmlParams);
            log(logLevel, "evaluatePolicyTimePositive", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyTimePositive",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyTimePositive");
    }
    
    /**
     * Creates the policy xml, loads in the server and  Evaluates
     * the policy with token, resource and env parameters and asserts
     * the decision.
     * Condition1: time condition with hour, day, date and zone
     * Condition2: auth scheme = ldap
     * Result: The user should be allowed
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void evaluatePolicyMultiTime()
    throws Exception {
        entering("evaluatePolicyMultiTime", null);
        try {
            int testCaseId = 10;
            Map policyXmlParams = new HashMap();
            boolean expectedResult = true;
            boolean policyResult ;
            String policyName = "policyevaluationtimetest_MultiTime";
            SimpleDateFormat dayFormatter = new SimpleDateFormat("EEE");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy:MM:dd");
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
            SimpleDateFormat zoneFormatter = new SimpleDateFormat("Z");
            Calendar rightNow = Calendar.getInstance();
            Calendar working = (Calendar)rightNow.clone();
            working.add(Calendar.HOUR_OF_DAY, + 20);
            String conditionDayBegin = (dayFormatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionDayEnd = (dayFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionDateBegin = (dateFormatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionDateEnd = (dateFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionTimeBegin = (timeFormatter.format
                    (rightNow.getTime())).toLowerCase();
            String conditionTimeEnd = (timeFormatter.format
                    (working.getTime())).toLowerCase();
            String conditionZone = (zoneFormatter.format
                    (working.getTime())).toLowerCase();
            
            //add the params needed for time condition
            policyXmlParams.put("test" + testCaseId + ".tconditiontype",
                    "multi");
            policyXmlParams.put("test" + testCaseId +
                    ".beginday", conditionDayBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".endday", conditionDayEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".begindate", conditionDateBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".enddate", conditionDateEnd);
            policyXmlParams.put("test" + testCaseId +
                    ".begintime", conditionTimeBegin);
            policyXmlParams.put("test" + testCaseId +
                    ".endtime", conditionTimeEnd);
            policyXmlParams.put("test" + testCaseId + ".zone",
                    conditionZone );
            
            // add the params needed for evaluation
            policyXmlParams.put("test" + testCaseId + ".expectedresult",
                    expectedResult);
            policyXmlParams.put("test" + testCaseId + ".policyname",
                    policyName);
            Reporter.log("evaluatePolicyDayMultiTime" + policyXmlParams);
            log(logLevel, "evaluatePolicyDayMultiTime", policyXmlParams);
            policyResult = evaluatePolicy(testCaseId, policyXmlParams);
            pc.deletePolicies(policyName, 1);
            assert (policyResult == expectedResult);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicyDayMultiTime",
                    e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("evaluatePolicyDayMultiTime");
    }
    
    /**
     * Construct env params map and  Evaluates
     * the policy with token, resource and env parameters and returns
     * the decision.
     * @param int , map
     */
    public boolean evaluatePolicy(int testCaseId, Map pXmlParams)
    throws Exception {
        entering("evaluatePolicy", null);
        log(logLevel, "evaluatePolicy", "mapIdentity" + mapIdentity);
        Map envParams = new HashMap();
        String REQUEST_AUTH_SCHEMES = "requestAuthSchemes";
        try {
            
            //add the params needed for policy creation
            String policyName = (String)pXmlParams.get("test" +
                    testCaseId + ".policyname");
            
            //create the policy xml and load the policy in the server
            createPolicyXmlMultiTime(mapIdentity, pXmlParams, testCaseId);
            pc. createPolicy(policyName);
            
            //user params for the policy evaluation
            String userName = (String)mapIdentity.get("test" + testCaseId +
                    ".Identity" + ".username");
            String userPword = (String)mapIdentity.get("test" + testCaseId +
                    ".Identity" + ".password");
            String realmName = (String)mapIdentity.get("test" + testCaseId +
                    ".Identity" + ".realmname");
            String resourceName = (String)mapIdentity.get("test" + testCaseId +
                    ".resourcename");
            String action = (String)mapIdentity.get("test" + testCaseId +
                    ".action");
            usertoken = getToken(userName, userPword, realmName);
            PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgent" +
                    "Service");
            
            //params for env map
            Set actions = new HashSet();
            actions.add(action);
            String authscheme = (String)mapIdentity.get("test" +
                    testCaseId +".authscheme");
            if ((authscheme != null) && (!authscheme.equals(""))) {
                Set authschemeSet = new HashSet();
                authschemeSet.add(authscheme);
                envParams.put(REQUEST_AUTH_SCHEMES,
                        authschemeSet);
            }
            boolean pResult = pe.isAllowed(usertoken, resourceName,
                    action, envParams );
            PolicyDecision pd = pe.getPolicyDecision(usertoken, resourceName,
                    actions, envParams);
            log(logLevel, "evaluatePolicy", "decision:" + pResult);
            log(logLevel, "evaluatePolicy", pd.toXML());
            return pResult;
        } catch (Exception e) {
            log(Level.SEVERE, "evaluatePolicy", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(usertoken);
        }
    }
    
    /**
     * Deletes the Identities
     * @param map (loaded with Identities to be deleted)
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            pc.deleteIds(mapIdentity);
        }catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage(), null);
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }
    
    /**
     * Construct the policy xml file with the properties passed from diff maps
     * and test case id
     * @param map, map , int
     */
    public void createPolicyXmlMultiTime( Map idMap, Map policyMap,
            int tcId)
    throws IOException {
        entering("createPolicyXmlMultiTime", null);
        try{
            int testCaseId = tcId;
            String startDayAttrName = "StartDay";
            String endDayAttrName = "EndDay";
            String startDateAttrName = "StartDate";
            String endDateAttrName = "EndDate";
            String startTimeAttrName = "StartTime";
            String endTimeAttrName = "EndTime";
            String zoneAttrName = "EnforcementTimeZone";
            
            String policyName = (String)policyMap.get("test" + testCaseId +
                    ".policyname");
            String policyXmlFileName = policyName + ".xml" ;
            String absFileName = getBaseDir() + fileseparator + "xml" +
                    fileseparator + "policy" + fileseparator +
                    policyXmlFileName;
            FileWriter fstream = new FileWriter(absFileName);
            BufferedWriter out = new BufferedWriter(fstream);
            String policyResource = (String)mapIdentity.get("test" +
                    testCaseId + ".resourcename");
            String pUserName = (String)mapIdentity.get("test" + testCaseId +
                    ".Identity" + ".username");
            String action = (String)mapIdentity.get("test" + testCaseId +
                    ".action" );
            String actionValue = (String)mapIdentity.get("test" + testCaseId +
                    ".actionvalue" );
            log(logLevel,  "policyXmlFileName", policyXmlFileName);
            
            //get these values from policyXmlParams
            String timeCondType = (String)policyMap.get("test" + testCaseId +
                    ".tconditiontype");
            String startDayAttrValue = (String)policyMap.get("test" +
                    testCaseId +  ".beginday");
            String endDayAttrValue = (String)policyMap.get("test" + testCaseId
                    +  ".endday");
            String startDateAttrValue = (String)policyMap.get("test" +
                    testCaseId +  ".begindate");
            
            String endDateAttrValue = (String)policyMap.get("test" +
                    testCaseId + ".enddate");
            String startTimeAttrValue = (String)policyMap.get("test" +
                    testCaseId + ".begintime");
            String endTimeAttrValue = (String)policyMap.get("test" +
                    testCaseId + ".endtime");
            String zoneAttrValue = (String)policyMap.get("test" + testCaseId +
                    ".zone");
            
            // now generate the policy xml file
            out.write("<!DOCTYPE Policies PUBLIC \"-//Sun Java System " +
                    "Access Manager");
            out.write("7.1 2006Q3 Admin CLI DTD//EN \" ");
            out.write(newline);
            out.write("\"jar://com/sun/identity/policy/policyAdmin.dtd\" >");
            out.write(newline);
            out.write("<Policies>" + newline);
            out.write("<Policy name=\"" + policyName + "0" + "\"" +
                    "  referralPolicy=\"false\" active=\"true\" >" + newline);
            out.write("<Rule name=\"r1\">" + newline);
            out.write("<ServiceName name=\"iPlanetAMWebAgentService\" />"
                    + newline);
            out.write("<ResourceName name=" + "\"" + policyResource + "\"/>" +
                    newline);
            out.write("<AttributeValuePair>" + newline);
            out.write("<Attribute name=" + "\"" + action + "\"/>");
            out.write("<Value>" + actionValue + "</Value>");
            out.write("</AttributeValuePair>" + newline +
                    "<AttributeValuePair>");
            out.write(" <Attribute name=\"POST\"/> <Value>allow</Value>");
            out.write("</AttributeValuePair>" + newline);
            out.write("</Rule>" + newline);
            out.write("<Subjects name=\"multiple Subjects\" " +
                    "description=\"\">");
            out.write(newline);
            out.write("<Subject name=\"AmidUser\" type=\"AMIdentitySubject\" " +
                    "includeType=\"inclusive\">" + newline);
            out.write("<AttributeValuePair><Attribute name=\"Values\"/>" +
                    "<Value>");
            out.write(newline);
            out.write("id=" + pUserName + ",ou=user," + basedn + "</Value>" +
                    newline);
            out.write(" </AttributeValuePair>");
            out.write(newline);
            out.write("</Subject>" + newline);
            out.write("</Subjects>" + newline);
            out.write("<Conditions name=\"Conditions1\" description=\"\">" +
                    newline);
            out.write(" <Condition name=\"condition2\" " +
                    "type=\"AuthSchemeCondition\">" + newline);
            out.write("<AttributeValuePair><Attribute name=\"AuthScheme\"/>" +
                    newline);
            out.write("<Value>ldap</Value>" + newline);
            out.write("</AttributeValuePair>" + newline);
            out.write("</Condition>" + newline);
            
            //now add the time conditions
            if (timeCondType. equals("day")){
                if (startDayAttrValue != null && endDayAttrValue != null) {
                    //String startDayAttrName = "StartDay";
                    //String endDayAttrName = "EndDay";
                    out.write(" <Condition name=\"daycondition\" " +
                            "type=\"SimpleTimeCondition\">" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startDayAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startDayAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endDayAttrName + "\"/>" + newline);
                    out.write("<Value>" + endDayAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    if (zoneAttrValue != null) {
                        out.write("<AttributeValuePair><Attribute name=\"" +
                                zoneAttrName + "\"/>" +  newline);
                        out.write("<Value>" + zoneAttrValue + "</Value>" +
                                newline);
                        out.write("</AttributeValuePair>" + newline);
                    }
                    out.write("</Condition>" + newline);
                }
            }
            if (timeCondType. equals("date")){
                if (startDateAttrValue != null && endDateAttrValue != null) {
                    // String startDateAttrName = "StartDate";
                    // String endDateAttrName = "EndDate";
                    out.write(" <Condition name=\"datecondition\" " +
                            "type=\"SimpleTimeCondition\">" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startDateAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endDateAttrName + "\"/>" + newline);
                    out.write("<Value>" + endDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    if (zoneAttrValue != null) {
                        out.write(" <Condition name=\"zonecondition\" " +
                                "type=\"SimpleTimeCondition\">" + newline);
                        out.write("<AttributeValuePair><Attribute name=\"" +
                                zoneAttrName + "\"/>" +  newline);
                        out.write("<Value>" + zoneAttrValue + "</Value>" +
                                newline);
                        out.write("</AttributeValuePair>" + newline);
                        out.write("</Condition>" + newline);
                    }
                    out.write("</Condition>" + newline);
                }
            }
            if (timeCondType. equals("time")){
                if (startTimeAttrValue != null && endTimeAttrValue != null) {
                    
                    out.write(" <Condition name=\"Timecondition\" " +
                            "type=\"SimpleTimeCondition\">" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startTimeAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startTimeAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endTimeAttrName + "\"/>" + newline);
                    out.write("<Value>" + endTimeAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startDateAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endDateAttrName + "\"/>" + newline);
                    out.write("<Value>" + endDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    if (zoneAttrValue != null) {
                        out.write(" <Condition name=\"zonecondition\" " +
                                "type=\"SimpleTimeCondition\">" + newline);
                        out.write("<AttributeValuePair><Attribute name=\"" +
                                zoneAttrName + "\"/>" +  newline);
                        out.write("<Value>" + zoneAttrValue + "</Value>" +
                                newline);
                        out.write("</AttributeValuePair>" + newline);
                        out.write("</Condition>" + newline);
                    }
                    out.write("</Condition>" + newline);
                }
            }
            if (timeCondType. equals("multi")){
                out.write(" <Condition name=\"Timecondition\" " +
                        "type=\"SimpleTimeCondition\">" + newline);
                if (startTimeAttrValue != null && endTimeAttrValue != null) {
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startTimeAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startTimeAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endTimeAttrName + "\"/>" + newline);
                    out.write("<Value>" + endTimeAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                }
                if (startDateAttrValue != null && endDateAttrValue != null) {
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startDateAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endDateAttrName + "\"/>" + newline);
                    out.write("<Value>" + endDateAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                }
                if (startDayAttrValue != null && endDayAttrValue != null) {
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            startDayAttrName + "\"/>" +  newline);
                    out.write("<Value>" + startDayAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            endDayAttrName + "\"/>" + newline);
                    out.write("<Value>" + endDayAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                }
                if (zoneAttrValue != null) {
                    out.write("<AttributeValuePair><Attribute name=\"" +
                            zoneAttrName + "\"/>" +  newline);
                    out.write("<Value>" + zoneAttrValue + "</Value>" +
                            newline);
                    out.write("</AttributeValuePair>" + newline);
                }
                out.write("</Condition>" + newline);
            }
            out.write("</Conditions>" + newline);
            out.write("</Policy>" + newline);
            out.write("</Policies>" + newline);
            out.flush();
            out.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        exiting("createPolicyXmlMultiTime");
    }
}
