/*
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
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class OAuth2ScopeConditionTest {

    private OAuth2ScopeCondition condition;

    @BeforeMethod
    public void setUp() {
        condition = new OAuth2ScopeCondition();
    }

    @Test
    public void conditionStateShouldParseRequiredScopes() {

        //Given

        //When
        condition.setState("{\"requiredScopes\": [\"cn\", \"givenName\"]}");

        //Then
        assertThat(condition.getRequiredScopes()).contains("cn", "givenName");
    }

    @Test
    public void conditionStateShouldContainRequiredScopes() {

        //Given
        Set<String> requiredScopes = new HashSet<String>();
        requiredScopes.add("cn");
        requiredScopes.add("givenName");
        condition.setRequiredScopes(requiredScopes);

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"requiredScopes\": [", "\"cn\"", "\"givenName\"");
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenNoRequiredScopesSetAndNoneSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenNoRequiredScopesSetAndEmptyScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put("scope", Collections.singleton(""));

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenSingleRequiredScopeSetAndEmptyScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put("scope", Collections.singleton(""));
        condition.setRequiredScopes(Collections.singleton("cn"));

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenNoRequiredScopeSetAndSingleScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put("scope", Collections.singleton("cn"));

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenSingleRequiredScopeSetAndMatchingScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put("scope", Collections.singleton("cn"));
        condition.setRequiredScopes(Collections.singleton("cn"));

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenMultipleRequiredScopesSetAndPartialMatchingScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> requiredScopes = new HashSet<String>();

        env.put("scope", Collections.singleton("cn"));
        requiredScopes.add("cn");
        requiredScopes.add("givenName");
        condition.setRequiredScopes(requiredScopes);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenMultipleRequiredScopesSetAndMatchingScopeSetInEnvironment()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> requiredScopes = new HashSet<String>();

        env.put("scope", Collections.singleton("cn givenName maidenName"));
        requiredScopes.add("cn");
        requiredScopes.add("givenName");
        condition.setRequiredScopes(requiredScopes);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }
}
