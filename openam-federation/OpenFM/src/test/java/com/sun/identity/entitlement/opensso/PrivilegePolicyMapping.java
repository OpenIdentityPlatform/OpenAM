/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: PrivilegePolicyMapping.java,v 1.2 2009/11/12 18:37:40 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.policy.ConditionTypeManager;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.policy.plugins.PrivilegeCondition;
import com.sun.identity.policy.plugins.PrivilegeSubject;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.unittest.UnittestLog;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class PrivilegePolicyMapping {
    private final static String TEST_USER_NAME =
        "PrivilegePolicyMappingTestUser";
    private final static String POLICY_NAME = "PrivilegePolicyMappingPolicy";
    private final static String RES_NAME =
        "http://whatever.privilegepolicymapping.com:80";
    private final static String EXCLUDED_RES =
        "http://whatever.privilegepolicymapping.com:80/cb";

    private AMIdentity testUser;
    private Policy policy;
    private Privilege privilege;
    private Map<String, Set<String>> ipConditionEnvMap;
    private Map<String, Set<String>> ipConditionEnvMap1;
    private Map<String, Set<String>> actionValues;

    @BeforeClass
    public void setup() throws Exception {
        try {
            UnittestLog.logMessage("PrivilegePolicyMapping.setUp():" +
                    "entered");
            ipConditionEnvMap = new HashMap<String, Set<String>>();
            Set<String> set = new HashSet<String>();
            set.add("whatever.whatever");
            ipConditionEnvMap.put(Condition.DNS_NAME, set);

            ipConditionEnvMap1 = new HashMap<String, Set<String>>();
            set = new HashSet<String>();
            set.add("whatever1.whatever1");
            ipConditionEnvMap1.put(Condition.DNS_NAME, set);

            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            testUser = IdRepoUtils.createUser("/", TEST_USER_NAME);
            PolicyManager pm = new PolicyManager(adminToken, "/");
            policy = new Policy(POLICY_NAME, "", false, true);
            policy.addRule(createRule());
            policy.addSubject("subjectName", createSubject(pm));
            policy.addCondition("conditionName", createIPCondition(pm));
            policy.addCondition("conditionName1", createIPCondition1(pm));
            pm.addPolicy(policy);
        } catch (Exception e) {
            UnittestLog.logError("PrivilegePolicyMapping.setUp();"
                    + "Exception STACKTRACE:"
                    + e.getMessage());
            StackTraceElement[] elems = e.getStackTrace();
            for (StackTraceElement elem : elems) {
                UnittestLog.logMessage(elem.toString());
            }
            UnittestLog.logMessage("END STACKTRACE");
            throw e;
        }
    }

    @AfterClass
    public void cleanup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        IdRepoUtils.deleteIdentity("/", testUser);
        PolicyManager pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(POLICY_NAME);
    }

    @Test (dependsOnMethods = {"policyToPrivilege"})
    public void privilegeToPolicy() throws Exception {
        Policy p = PrivilegeUtils.privilegeToPolicy("/", privilege);
        Set<String> ruleNames = p.getRuleNames();
        for (String ruleName : ruleNames ) {
            Rule r = p.getRule(ruleName);
            if (!RES_NAME.equals(r.getResourceName())) {
                throw new Exception(
                    "PrivilegePolicyMapping.privilegeToPolicy: resource is incorrect");
            }
            if (!actionValues.equals(r.getActionValues())) {
                throw new Exception(
                    "PrivilegePolicyMapping.privilegeToPolicy: action value is incorrect");
            }
        }

        Set<String> subjectNames = p.getSubjectNames();
        for (String subjectName : subjectNames) {
            Subject sbj = p.getSubject(subjectName);
            if (!(sbj instanceof PrivilegeSubject)) {
                throw new Exception(
                    "PrivilegePolicyMapping.privilegeToPolicy: not instance of privilege subject");
            }
        }

        Set<String> conditionNames = p.getConditionNames();
        if (conditionNames.size() != 1) {
            throw new Exception(
                "PrivilegePolicyMapping.privilegeToPolicy: number of condition is incorrect");
        }

        for (String conditionName : conditionNames) {
            Condition cond = p.getCondition(conditionName);
            if (!(cond instanceof PrivilegeCondition)) {
                throw new Exception(
                    "PrivilegePolicyMapping.privilegeToPolicy: not instance of privilege condition");
            }
        }

    }

    @Test
    public void policyToPrivilege() throws Exception {
        Set<IPrivilege> privileges = PrivilegeUtils.policyToPrivileges(policy);
        if (privileges.isEmpty()) {
            throw new Exception(
                "PrivilegePolicyMapping.policyToPrivilege: cannot get privilege");
        }

        privilege = (Privilege)privileges.iterator().next();

        EntitlementCondition cond = privilege.getCondition();
        if (!(cond instanceof OrCondition)) {
            throw new Exception(
                "PrivilegePolicyMapping.policyToPrivilege: condition is not AND condition");
        }
        OrCondition pOrCond = (OrCondition)cond;

        for (EntitlementCondition ec : pOrCond.getEConditions()) {
            if (!(ec instanceof PolicyCondition)) {
                throw new Exception(
                    "PrivilegePolicyMapping.policyToPrivilege: condition is not policy condition");
            }
            PolicyCondition pCond = (PolicyCondition) ec;
            Map<String, Set<String>> pCondProp = pCond.getProperties();
            if (!pCondProp.equals(ipConditionEnvMap) &&
                !pCondProp.equals(ipConditionEnvMap1)
            ) {
                throw new Exception(
                    "PrivilegePolicyMapping.policyToPrivilege: condition values are not correct");
            }
        }

        EntitlementSubject sbj = privilege.getSubject();
        if (!(sbj instanceof PolicySubject)) {
            throw new Exception(
                "PrivilegePolicyMapping.policyToPrivilege: subject is not privilege subject");
        }
        PolicySubject pSbj = (PolicySubject)sbj;
        Set pSbjValue = pSbj.getValues();
        if ((pSbjValue == null) || pSbjValue.isEmpty()) {
            throw new Exception(
                "PrivilegePolicyMapping.policyToPrivilege: subject value is empty");
        }
        if (!pSbjValue.contains(testUser.getUniversalId())) {
            throw new Exception(
                "PrivilegePolicyMapping.policyToPrivilege: subject value is incorrect");
        }
    }

    private Rule createRule() throws PolicyException {
        actionValues = new HashMap<String, Set<String>>();
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("GET", set);
        }
        {
            Set<String> set = new HashSet<String>();
            set.add("deny");
            actionValues.put("POST", set);
        }

        return new Rule("rule1", "iPlanetAMWebAgentService",
            RES_NAME, actionValues);
    }

    private Subject createSubject(PolicyManager pm) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(testUser.getUniversalId());
        subject.setValues(set);
        return subject;
    }

    private Condition createIPCondition(PolicyManager pm)
        throws PolicyException {
        ConditionTypeManager mgr = pm.getConditionTypeManager();
        Condition cond = mgr.getCondition("IPCondition");
        cond.setProperties(ipConditionEnvMap);
        return cond;
    }

    private Condition createIPCondition1(PolicyManager pm)
        throws PolicyException {
        ConditionTypeManager mgr = pm.getConditionTypeManager();
        Condition cond = mgr.getCondition("IPCondition");
        cond.setProperties(ipConditionEnvMap1);
        return cond;
    }
}
