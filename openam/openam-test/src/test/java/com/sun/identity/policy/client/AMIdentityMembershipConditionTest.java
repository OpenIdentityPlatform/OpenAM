/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMIdentityMembershipConditionTest.java,v 1.3 2008/06/25 05:44:20 qcheng Exp $
 *
 */

package com.sun.identity.policy.client;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import com.sun.identity.policy.TokenUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import java.util.logging.Level;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class AMIdentityMembershipConditionTest extends UnitTestBase {

    public AMIdentityMembershipConditionTest() {
        super("OpenSSO-AMIdentityMembershipConditionTest");
    }

    @Test(groups={"policy-client"})
    @Parameters({"orgName", "userName", "password", "serviceName",
            "resourceName", "actionName", "invocatorUuid"})
    public void testGetPolicyDecision(String orgName, 
            String userName, String password, 
            String serviceName, String resourceName, String actionName, 
            String invocatorUuid) throws Exception {
        entering("testGetPolicyDecision()", null);
        log(Level.INFO, "orgName:", orgName);
        log(Level.INFO, "userName:", userName);
        log(Level.INFO, "password:", password);
        log(Level.INFO, "serviceName:", serviceName);
        log(Level.INFO, "resourceName:", resourceName);
        log(Level.INFO, "actionName:", actionName);
        log(Level.INFO, "invocatorUuid:", invocatorUuid);
        SSOToken token = TokenUtils.getSessionToken(orgName, 
                userName, password);
        log(Level.INFO, "Created ssoToken", "\n");

        PolicyEvaluator pe = PolicyEvaluatorFactory.getInstance().
                    getPolicyEvaluator(serviceName);

        Map env = new HashMap();
        Set attrSet = new HashSet();
        attrSet.add(invocatorUuid);
        env.put("invocatorPrincipalUuid", attrSet);
        log(Level.INFO, "env Map:" + env, "\n");

        Set actions = new HashSet();
        actions.add(actionName);
        PolicyDecision pd = pe.getPolicyDecision(token, resourceName, 
                actions, env);
        log(Level.INFO, "PolicyDecision XML:", pd.toXML());
        entering("testGetPolicyDecision()", null);
    }

}

