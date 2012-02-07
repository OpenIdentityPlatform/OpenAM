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
 * $Id: InactivePolicyTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class InactivePolicyTest {
    private static final String POLICY_NAME = "InactivePolicyTestPolicy";
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());


    @BeforeClass
    public void setup() throws Exception {
        createPolicy();
    }

    private void createPolicy() throws Exception {
        Policy policy = new Policy(POLICY_NAME, "", false, false);
        PolicyManager pm = new PolicyManager(adminToken, "/");
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AuthenticatedUsers");
        Map<String, Set<String>> actionValues =
            new HashMap<String, Set<String>>();
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("GET", set);
        }
        {
            Set<String> set = new HashSet<String>();
            set.add("allow");
            actionValues.put("POST", set);
        }
        policy.addRule(new Rule("rule", "iPlanetAMWebAgentService",
            "http://www.InactivePolicyTest.com/*", actionValues));
        policy.addSubject("subject", subject);
        pm.addPolicy(policy);
    }

    @AfterClass
    public void cleanup() throws Exception {
        PolicyManager pm = new PolicyManager(adminToken, "/");
        pm.removePolicy(POLICY_NAME);
    }

    @Test
    public void test() throws Exception {
        javax.security.auth.Subject adminSubject = SubjectUtils.createSubject(
            adminToken);
        PolicyEvaluator eval = new PolicyEvaluator("/",
            "iPlanetAMWebAgentService");
        if (eval.isAllowed(adminToken,
            "http://www.InactivePolicyTest.com/abc.html",
            "GET")) {
            cleanup();
            throw new Exception("InactivePolicyTest.test failed.");
        }
    }
}
