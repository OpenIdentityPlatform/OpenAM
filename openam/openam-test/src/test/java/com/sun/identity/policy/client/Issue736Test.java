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
 * $Id: Issue736Test.java,v 1.2 2008/06/25 05:44:20 qcheng Exp $
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

import com.sun.identity.policy.interfaces.Condition;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Level;

import org.testng.annotations.Test;
import org.testng.annotations.Parameters;


/**
 * Test to verify fix for 
 * Issue 736 Realm Alias Referrals enabled at the root org throws error for the 
 * root org policy evaluation
 *
 * Test set up:
 *
 * Enable Realm Alias Referral in Policy Config Service global properites
 * Define an allow policy for http://host1.sample.com:80/banner.html allowing
 * access to all authenticated users.
 * 
 * There should not be any realm with realm alias value of host1.sample.com
 *
 */
public class Issue736Test extends UnitTestBase {

    public Issue736Test() {
        super("OpenSSO-client Issue736Test");
    }

    @Test(groups={"policy-client"})
    @Parameters({"orgName", "userName", "password", "serviceName",
             "actionName"})
    public void testGetPolicyDecision(String orgName, 
            String userName, String password, 
            String serviceName, String actionName) 
            throws Exception {
        entering("Issue736Test.testGetPolicyDecision()", null);
        String resourceName = "http://host1.sample.com:80/banner.html";
        log(Level.INFO, "orgName:", orgName);
        log(Level.INFO, "userName:", userName);
        log(Level.INFO, "password:", password);
        log(Level.INFO, "serviceName:", serviceName);
        log(Level.INFO, "resourceName:", resourceName);
        log(Level.INFO, "actionName:", actionName);
        SSOToken token = TokenUtils.getSessionToken(orgName, 
                userName, password);
        log(Level.INFO, "Created ssoToken", "\n");

        PolicyEvaluator pe = PolicyEvaluatorFactory.getInstance().
                    getPolicyEvaluator(serviceName);

        Set actions = new HashSet();
        actions.add(actionName);
        PolicyDecision pd = pe.getPolicyDecision(token, resourceName, 
                actions, null); //null envMap
        log(Level.INFO, "PolicyDecision XML:", pd.toXML());
        entering("testGetPolicyDecision()", null);
    }


}

