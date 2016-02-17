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
 * $Id: DecisionMergeTest.java,v 1.1 2009/08/19 05:41:00 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions copyright 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResourceResult;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class DecisionMergeTest {
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = true;

    @BeforeClass
    public void setup() throws EntitlementException {
        if (!migrated) {
            return;
        }

        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", true);
        Entitlement e1 = new Entitlement("http://www.DecisionMergeTest.com/a/*",
            actionValues);
        EntitlementSubject sbj = new AuthenticatedUsers();

        Privilege p1 = Privilege.getNewInstance();
        p1.setName("DecisionMergeTestPolicy1");
        p1.setEntitlement(e1);
        p1.setSubject(sbj);
        PrivilegeManager mgr = PrivilegeManager.getInstance("/", adminSubject);
        mgr.add(p1);

        Map<String, Boolean> actionValues2 = new HashMap<String, Boolean>();
        actionValues2.put("GET", false);
        Entitlement e2 = new Entitlement("http://www.DecisionMergeTest.com/a/b/*",
            actionValues2);
//        EntitlementCondition ec = new IPCondition("100.0.0.0", "100.0.0.1");
        Privilege p2 = Privilege.getNewInstance();
        p2.setName("DecisionMergeTestPolicy2");
        p2.setEntitlement(e2);
        p2.setSubject(sbj);
        mgr.add(p2);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        PrivilegeManager mgr = PrivilegeManager.getInstance("/", adminSubject);
        mgr.remove("DecisionMergeTestPolicy1");
        mgr.remove("DecisionMergeTestPolicy2");
    }

    
    public void test() throws EntitlementException {
        Evaluator eval = new Evaluator(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        Set<String> actions = new HashSet<String>();
        actions.add("GET");
        Entitlement e = new Entitlement("http://www.DecisionMergeTest.com/a/b/c", actions);
        eval.hasEntitlement("/", adminSubject, e, Collections.EMPTY_MAP);
    }

    @Test
    public void testOldAPI() throws SSOException, PolicyException {
        PolicyEvaluator pe = new PolicyEvaluator("/",
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME);
        Set<String> actions = new HashSet<String>();
        actions.add("GET");
        Set<ResourceResult> res = pe.getResourceResults(adminToken,
            "http://www.DecisionMergeTest.com",
            ResourceResult.SUBTREE_SCOPE, Collections.EMPTY_MAP);

        for (ResourceResult r : res) {
            PolicyDecision pd = r.getPolicyDecision();
            pd.toString();
        }
    }
}
