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
 * $Id: ProxyPETest.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
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
import com.sun.identity.unittest.UnittestLog;
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

public class ProxyPETest {
    private static String POLICY_NAME1 = "ProxyPETestP1";

    private static String URL_RESOURCE1 = "http://www.sun.com:80/banner.html";
    private static String TEST_USER_NAME = "proxyPETestUser";

    private AMIdentity testUser;
    private SSOToken userSSOToken;
    private AuthContext lc;

    @BeforeClass
    public void setup() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        //createUser(adminToken);
        userSSOToken = login();

        PolicyManager pm = new PolicyManager(adminToken, "/");
        Policy policy = new Policy(POLICY_NAME1, "test1 - discard",
            false, true);
        policy.addRule(createRule1());
        policy.addSubject("au", createAuthenticatedUsersSubject(pm));
        policy.addCondition("as", createAuthSchemeCondition(pm));
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

        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(testUser);
        //amir.deleteIdentities(identities);
    }

    @Test
    public void testGetDecisionIgnoreSubjects() throws Exception {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ProxyPolicyEvaluator pe = ProxyPolicyEvaluatorFactory.getInstance().
                getProxyPolicyEvaluator(adminToken, "iPlanetAMWebAgentService");
                Set actionNames = new HashSet();
        actionNames.add("GET");
        PolicyDecision pd = pe.getPolicyDecisionIgnoreSubjects(URL_RESOURCE1, 
                actionNames, null);
        UnittestLog.logMessage("ProxyPETest.testGetDecisionIgnoreSubjects():decision="
                + pd.toXML());
        Set expectedAdvice = new HashSet();
        expectedAdvice.add("LDAP");
        ActionDecision ad = (ActionDecision)pd.getActionDecisions().get("GET");
        Set advice = (Set)ad.getAdvices().
                get(Condition.AUTH_SCHEME_CONDITION_ADVICE);
        if (!expectedAdvice.equals(advice)) {
            UnittestLog.logMessage("ProxyPETest.testGetDecisionIgnoreSubjects()"
                    + "Expected advice=" + expectedAdvice);
            UnittestLog.logMessage("ProxyPETest.testGetDecisionIgnoreSubjects()"
                    + "advice received=" + advice);
            throw new Exception("Expected advice not found");
        }
    }

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
    
    private Subject createAuthenticatedUsersSubject(PolicyManager pm) 
            throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AuthenticatedUsers");
        return subject;
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

   private Condition createAuthSchemeCondition(PolicyManager pm)
        throws PolicyException {
        ConditionTypeManager mgr = pm.getConditionTypeManager();
        Condition cond = mgr.getCondition("AuthSchemeCondition");
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add("LDAP");
        map.put(Condition.AUTH_SCHEME, set);
        cond.setProperties(map);
        return cond;
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
