/*
 * Copyright 2013 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */

package com.sun.identity.policy.plugins;

import com.sun.identity.policy.ConditionDecision;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author mark.dereeper@forgerock.com
 */
public class AuthenticateToRealmConditionTest {

    @Test
    public void testValidRealm() throws Exception {

        Map<String, Set<String>> properties = new HashMap<String, Set<String>>(1);
        Set<String> realm = new HashSet<String>(1);
        realm.add("/ValidRealm");
        properties.put(AuthenticateToRealmCondition.AUTHENTICATE_TO_REALM, realm);

        AuthenticateToRealmCondition condition = new AuthenticateToRealmCondition();
        condition.setProperties(properties);

        Set<String> passedRealm = new HashSet<String>(3);
        passedRealm.add("/Realm");
        passedRealm.add("/ValidRealm");
        passedRealm.add("/AnotherRealm");
        Map<String, Set<String>> env = new HashMap<String, Set<String>>(1);
        env.put(AuthenticateToRealmCondition.REQUEST_AUTHENTICATED_TO_REALMS, passedRealm);

        ConditionDecision conditionDecision = condition.getConditionDecision(null, env);

        assertTrue(conditionDecision.isAllowed());
    }

    @Test
    public void testValidMixedCaseRealm() throws Exception {

        Map<String, Set<String>> properties = new HashMap<String, Set<String>>(1);
        Set<String> realm = new HashSet<String>(1);
        realm.add("/vAliDrEalM");
        properties.put(AuthenticateToRealmCondition.AUTHENTICATE_TO_REALM, realm);

        AuthenticateToRealmCondition condition = new AuthenticateToRealmCondition();
        condition.setProperties(properties);

        Set<String> passedRealm = new HashSet<String>(1);
        passedRealm.add("/ValidRealm");
        Map<String, Set<String>> env = new HashMap<String, Set<String>>(1);
        env.put(AuthenticateToRealmCondition.REQUEST_AUTHENTICATED_TO_REALMS, passedRealm);

        ConditionDecision conditionDecision = condition.getConditionDecision(null, env);

        assertTrue(conditionDecision.isAllowed());
    }

    @Test
    public void testValidLowerCaseRealm() throws Exception {

        Map<String, Set<String>> properties = new HashMap<String, Set<String>>(1);
        Set<String> realm = new HashSet<String>(1);
        realm.add("/validrealm");
        properties.put(AuthenticateToRealmCondition.AUTHENTICATE_TO_REALM, realm);

        AuthenticateToRealmCondition condition = new AuthenticateToRealmCondition();
        condition.setProperties(properties);

        Set<String> passedRealm = new HashSet<String>(1);
        passedRealm.add("/ValidRealm");
        Map<String, Set<String>> env = new HashMap<String, Set<String>>(1);
        env.put(AuthenticateToRealmCondition.REQUEST_AUTHENTICATED_TO_REALMS, passedRealm);

        ConditionDecision conditionDecision = condition.getConditionDecision(null, env);

        assertTrue(conditionDecision.isAllowed());
    }

    @Test
    public void testInValidRealm() throws Exception {

        Map<String, Set<String>> properties = new HashMap<String, Set<String>>(1);
        Set<String> realm = new HashSet<String>(1);
        realm.add("/InvalidRealm");
        properties.put(AuthenticateToRealmCondition.AUTHENTICATE_TO_REALM, realm);

        AuthenticateToRealmCondition condition = new AuthenticateToRealmCondition();
        condition.setProperties(properties);

        Set<String> passedRealm = new HashSet<String>(1);
        passedRealm.add("/ValidRealm");
        Map<String, Set<String>> env = new HashMap<String, Set<String>>(1);
        env.put(AuthenticateToRealmCondition.REQUEST_AUTHENTICATED_TO_REALMS, passedRealm);

        ConditionDecision conditionDecision = condition.getConditionDecision(null, env);

        assertFalse(conditionDecision.isAllowed());
    }
}
