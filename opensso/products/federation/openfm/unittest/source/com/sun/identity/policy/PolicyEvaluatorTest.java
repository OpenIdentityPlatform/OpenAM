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
 * $Id: PolicyEvaluatorTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author dennis
 */
public class PolicyEvaluatorTest {
    private static String POLICY_NAME1 = "PolicyEvaluatorTestP1";
    private static String POLICY_NAME2 = "PolicyEvaluatorTestP2";
    private static String POLICY_NAME3 = "PolicyEvaluatorTestP3";
    private static String POLICY_NAME4 = "PolicyEvaluatorTestP4";

    private static String URL_RESOURCE1 = "http://www.*.com:8080/private";
    private static String URL_RESOURCE2 = "http://www.sun.com:8080/private";
    private static String URL_RESOURCE3 = "http://www.ibm.com:8080/private";
    private static String URL_RESOURCE4 = "http://*.com:8080/private";
    private static String TEST_GRP_NAME = "policyTestGroup";
    private static String TEST_USER_NAME = "policyTestUser";

    private AMIdentity testGroup;
    private AMIdentity testUser;
    private SSOToken userSSOToken;
    private AuthContext lc;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        createUser(adminToken);
        userSSOToken = login();

        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy policy = new Policy(POLICY_NAME1, "test1 - discard",
            false, true);
        policy.addRule(createRule1());
        policy.addSubject("group", createSubject(pm));
        pm.addPolicy(policy);

        policy = new Policy(POLICY_NAME2, "test2 - discard",
            false, true);
        policy.addRule(createRule2());
        policy.addSubject("group2", createSubject(pm));
        pm.addPolicy(policy);

        policy = new Policy(POLICY_NAME3, "test3 - discard",
            false, true);
        policy.addRule(createRule3());
        policy.addSubject("group3", createSubject(pm));
        pm.addPolicy(policy);

        policy = new Policy(POLICY_NAME4, "test4 - discard",
            false, true);
        policy.addRule(createRule4());
        createGroup(adminToken);
        policy.addSubject("group4", createGroupSubject(pm));
        policy.addCondition("condition4", createIPCondition(pm));
        pm.addPolicy(policy);
    }
    
    @AfterClass
    public void cleanup() throws PolicyException, SSOException, IdRepoException{
        try {
            lc.logout();
        } catch (Exception e) {
            //ignore
        }
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        PolicyManager pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(POLICY_NAME1);
        pm.removePolicy(POLICY_NAME2);
        pm.removePolicy(POLICY_NAME3);
        pm.removePolicy(POLICY_NAME4);

        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(testGroup);
        identities.add(testUser);
        amir.deleteIdentities(identities);
    }

    @Test
    public void testIsAllowed() throws Exception {
        PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgentService");
        if (!pe.isAllowed(userSSOToken, "http://www.sun.com:8080/private", "GET")){
            throw new Exception("testIsAllowed" +
                "http://www.sun.com:8080/private evaluation failed");
        }
        
        //negative test
        if (pe.isAllowed(userSSOToken, "http://www.sun.com:8080/public", "GET")){
            throw new Exception("testIsAllowed" +
                "http://www.sun.com:8080/public evaluation failed");
        }
    }

    @Test
    public void testResourceSelf() throws Exception {
        PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgentService");
        Set<ResourceResult> resResults = pe.getResourceResults(userSSOToken,
            "http://www.sun.com:8080/private",
            ResourceResult.SELF_SCOPE, Collections.EMPTY_MAP);
        ResourceResult resResult = resResults.iterator().next();
        PolicyDecision pd = resResult.getPolicyDecision();
        Map<String, ActionDecision> decisions = pd.getActionDecisions();
        ActionDecision ad = decisions.get("GET");
        if (!ad.getValues().contains("allow")) {
            throw new Exception("testResourceSelf: " +
                "http://www.sun.com:8080/private evaluation failed");
        }
        ad = decisions.get("POST");
        if (!ad.getValues().contains("deny")) {
            throw new Exception("testResourceSelf: " +
                "http://www.sun.com:8080/private evaluation failed");
        }
    }
 
    @Test
    public void testResourceSubTree() throws Exception {
        PolicyEvaluator pe = new PolicyEvaluator("iPlanetAMWebAgentService");
        Set<ResourceResult> resResults = pe.getResourceResults(userSSOToken,
            "http://www.sun.com:8080/",
            ResourceResult.SUBTREE_SCOPE, Collections.EMPTY_MAP);
        if (resResults.size() != 2) {
            throw new Exception("testResourceSubTree: failed");
        }
    }

/*    private SimulationEvaluator createSimulator(
        javax.security.auth.Subject subject
    ) throws Exception {
        SimulationEvaluator eval = new SimulationEvaluator(
            subject, "iPlanetAMWebAgentService");
        eval.setResource("http://www.sun.com:8080/private");
        eval.setSubject(subject);
        Set<String> policyNames = new HashSet<String>();
        policyNames.add(POLICY_NAME1);
        policyNames.add(POLICY_NAME2);
        policyNames.add(POLICY_NAME3);
        eval.setPolicies(policyNames);
        return eval;
    }

    @Test
    public void testSimulationSelf() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        javax.security.auth.Subject subject = createSubject(adminToken);
        SimulationEvaluator eval = createSimulator(subject);
        eval.evaluate(false);
        List<SimulatedResult> details = eval.getSimulatedResults();
        for (SimulatedResult r : details) {
            String policyName = r.getPrivilegeName();
            if (policyName.equals(POLICY_NAME1)) {
                if (!r.isApplicable()) {
                    throw new Exception("testSimulationSelf: failed");
                }
            } else if (policyName.equals(POLICY_NAME2)) {
                if (!r.isApplicable()) {
                    throw new Exception("testSimulationSelf: failed");
                }
            } else if (policyName.equals(POLICY_NAME3)) {
                if (r.isApplicable()) {
                    throw new Exception("testSimulationSelf: failed");
                }
            }
        }
        List<Entitlement> results = eval.getEntitlements();
        for (Entitlement ent : results) {
            String res = ent.getResourceName();
            if (!res.equals("http://www.sun.com:8080/private")) {
                throw new Exception(
                    "testSimulationSelf: failed, resource name is incorrect.");
            }
            Map<String, Boolean> actionValues = ent.getActionValues();
            validateActionValues(actionValues, "GET", Boolean.TRUE);
            validateActionValues(actionValues, "POST", Boolean.FALSE);
        }

        if (!eval.getMatchedSubjectTypeNames(POLICY_NAME4).isEmpty()) {
            throw new Exception("testSimulationSelf: subject match");
        }
        if (!eval.getMatchedConditionTypeNames(POLICY_NAME4).isEmpty()) {
            throw new Exception("testSimulationSelf: condition match");
        }
    }
*/
    private void validateActionValues(
        Map<String, Boolean> actionValues,
        String key,
        Boolean value
    ) throws Exception {
        Boolean getVal = actionValues.get(key);
        if (!getVal.equals(value)) {
            throw new Exception(
                "testSimulationSelf: failed, " + key + " result is incorrect.");
        }
    }
    /*
    @Test
    public void testSimulationSubTree() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        javax.security.auth.Subject subject = createSubject(adminToken);
        SimulationEvaluator eval = createSimulator(subject);
        eval.evaluate(true);
        List<SimulatedResult> details = eval.getSimulatedResults();
        for (SimulatedResult r : details) {
            System.out.println(r.getEntitlement().getResourceName());
        }
        List<Entitlement> results = eval.getEntitlements();
        for (Entitlement ent : results) {
            String res = ent.getResourceName();
            if (res.equals(URL_RESOURCE1)) {
                Map<String, Boolean> actionValues = ent.getActionValues();
                validateActionValues(actionValues, "GET", Boolean.TRUE);
                validateActionValues(actionValues, "POST", Boolean.FALSE);
            } else if (res.equals(URL_RESOURCE2)) {
                Map<String, Boolean> actionValues = ent.getActionValues();
                validateActionValues(actionValues, "GET", Boolean.TRUE);
            }
        }
    }*/

    private Rule createRule1() throws PolicyException {
        Map<String, Set<String>> actionValues = 
            new HashMap<String, Set<String>>();
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
            URL_RESOURCE1, actionValues);
    }
    
    private Rule createRule2() throws PolicyException {
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
            set.add("allow");
        actionValues.put("POST", set);

        return new Rule("rule2", "iPlanetAMWebAgentService",
            URL_RESOURCE2, actionValues);
    }

    private Rule createRule3() throws PolicyException {
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
            set.add("allow");
        actionValues.put("POST", set);

        return new Rule("rule3", "iPlanetAMWebAgentService",
            URL_RESOURCE3, actionValues);
    }

    private Rule createRule4() throws PolicyException {
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
            set.add("allow");
        actionValues.put("POST", set);

        return new Rule("rule4", "iPlanetAMWebAgentService",
            URL_RESOURCE4, actionValues);
    }

    private Subject createSubject(PolicyManager pm) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(testUser.getUniversalId());
        subject.setValues(set);
        return subject;
    }

    private Subject createGroupSubject(PolicyManager pm)
        throws PolicyException{
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> set = new HashSet<String>();
        set.add(testGroup.getUniversalId());
        subject.setValues(set);
        return subject;
    }

    private Condition createIPCondition(PolicyManager pm)
        throws PolicyException {
        ConditionTypeManager mgr = pm.getConditionTypeManager();
        Condition cond = mgr.getCondition("IPCondition");
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("whatever.whatever");
        map.put(Condition.DNS_NAME, set);
        cond.setProperties(map);
        return cond;
    }

    private void createGroup(SSOToken adminToken)
        throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        testGroup = amir.createIdentity(IdType.GROUP, TEST_GRP_NAME,
            Collections.EMPTY_MAP);
    }

    private void createUser(SSOToken adminToken)
        throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(TEST_USER_NAME);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);

        testUser = amir.createIdentity(IdType.USER, TEST_USER_NAME,
            attrValues);
    }

    private javax.security.auth.Subject createSubject(SSOToken token) {
        Principal userP = new AuthSPrincipal(token.getTokenID().toString());
        Set userPrincipals = new HashSet(2);
        userPrincipals.add(userP);
        return new javax.security.auth.Subject(true, userPrincipals,
            Collections.EMPTY_SET, Collections.EMPTY_SET);
    }

    private SSOToken login()
        throws Exception {
        lc = new AuthContext("/");
        AuthContext.IndexType indexType = AuthContext.IndexType.MODULE_INSTANCE;
        lc.login(indexType, "DataStore");
        Callback[] callbacks = null;

        // get information requested from module
        while (lc.hasMoreRequirements()) {
            callbacks = lc.getRequirements();
            if (callbacks != null) {
                addLoginCallbackMessage(callbacks);
                lc.submitRequirements(callbacks);
            }
        }

        return (lc.getStatus() == AuthContext.Status.SUCCESS) ?
            lc.getSSOToken() : null;
    }

    private void addLoginCallbackMessage(Callback[] callbacks) {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                ((NameCallback) callbacks[i]).setName(TEST_USER_NAME);
            } else if (callbacks[i] instanceof PasswordCallback) {
                ((PasswordCallback) callbacks[i]).setPassword(
                    TEST_USER_NAME.toCharArray());
            }
        }
    }


}
