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
 * $Id: IDPPTest.java,v 1.2 2009/11/12 18:37:39 veiming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Rule;
import com.sun.identity.policy.SubjectTypeManager;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class IDPPTest {
    private static final String USER1_NAME = "IDPPTestUser1";
    private static final String GROUP1_NAME = "IDPPTestGroup1";
    private static final String URL1 = "http://www.IDPPTest.com:80/private";
    private static final String serviceType = "sunIdentityServerLibertyPPService";
    private static final String orgName = "/";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    private AMIdentity user1;
    private AMIdentity group1;
    

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }
        
        user1 = IdRepoUtils.createUser(orgName, USER1_NAME);
        group1 = IdRepoUtils.createGroup(orgName, GROUP1_NAME);
        group1.addMember(user1);

        PolicyManager policyMgr = new PolicyManager(adminToken, orgName);
        Policy policy = new Policy("IDPPTestPolicy1");

        Set values = new HashSet();
        values.add("deny");
        Map actionValues = new HashMap();
        actionValues.put("MODIFY", values);
        actionValues.put("QUERY", values);

        String resourceName = "*";
        String ruleName = "rule1";

        Rule rule = new Rule(ruleName, serviceType, resourceName,
            actionValues);

        policy.addRule(rule);


        SubjectTypeManager subjectTypeMgr =
            policyMgr.getSubjectTypeManager();
        com.sun.identity.policy.interfaces.Subject subject =
            subjectTypeMgr.getSubject("AMIdentitySubject");
        values = new HashSet();
        values.add(group1.getUniversalId());
        subject.setValues(values);

        policy.addSubject("subject1", subject, false);


        policyMgr.addPolicy(policy);

    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        PolicyManager policyMgr = new PolicyManager(adminToken, orgName);
        policyMgr.removePolicy("IDPPTestPolicy1");

        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(user1);
        identities.add(group1);
        IdRepoUtils.deleteIdentities(orgName, identities);

    }

    @Test
    public void postiveTest()
        throws Exception {
        if (!migrated) {
            return;
        }
        if (!evaluate(URL1)) {
            throw new Exception("IDPPTest.postiveTest failed");
        }
    }

    private boolean evaluate(String res)
        throws Exception {

        AuthContext lc = new AuthContext(orgName);
        lc.login();
        while (lc.hasMoreRequirements()) {
            Callback[] callbacks = lc.getRequirements();
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    nc.setName(USER1_NAME);
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    pc.setPassword(USER1_NAME.toCharArray());
                } else {
                    throw new Exception("No callback");
                }
            }
            lc.submitRequirements(callbacks);
        }

        if (lc.getStatus() != AuthContext.Status.SUCCESS) {
            return false;
        }
        SSOToken ssoToken = lc.getSSOToken();

        PolicyEvaluator evaluator = new PolicyEvaluator(serviceType);
        String resource = URL1;
        String action = "MODIFY";
        Set actions = new HashSet();
        actions.add(action);
        PolicyDecision policyDecision =  evaluator.getPolicyDecision(
            ssoToken, resource, actions, null);

        if (policyDecision == null) {
            return false;
        }
        Map actionDecisions = policyDecision.getActionDecisions();
        ActionDecision actionDecision = (ActionDecision)
            actionDecisions.get(action);

        if (actionDecision == null) {
            return false;
        }

        Set values = (Set)actionDecision.getValues();
        if ((values == null) || (values.size() != 1)) {
            return false;
        }

        String actionValue = (String)(values.iterator().next());
        return (actionValue.equals("deny"));
    }

}
