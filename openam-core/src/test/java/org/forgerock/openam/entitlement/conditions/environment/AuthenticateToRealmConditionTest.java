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

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTHENTICATE_TO_REALM_CONDITION_ADVICE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTHENTICATED_TO_REALMS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AuthenticateToRealmConditionTest {

    private AuthenticateToRealmCondition condition;

    private EntitlementCoreWrapper coreWrapper;

    @BeforeMethod
    public void setUp() {

        Debug debug = mock(Debug.class);
        coreWrapper = mock(EntitlementCoreWrapper.class);

        condition = new AuthenticateToRealmCondition(debug, coreWrapper);
    }

    @Test
    public void conditionStateShouldParseAuthenticateToRealm() {

        //Given

        //When
        condition.setState("{\"authenticateToRealm\": \"REALM\"}");

        //Then
        assertThat(condition.getAuthenticateToRealm()).isEqualTo("REALM");
    }

    @Test
    public void conditionStateShouldContainAuthenticateToRealm() {

        //Given
        condition.setAuthenticateToRealm("REALM");

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"authenticateToRealm\": \"REALM\"");
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void conditionShouldThrowEntitlementExceptionWhenValidatingWithNoAuthenticateToRealmSet()
            throws EntitlementException {

        //Given

        //When
        condition.validate();

        //Then
        //Expected EntitlementException
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentContainsRealmsAndMatches() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> realms = new HashSet<String>();

        realms.add("REALM");
        env.put(REQUEST_AUTHENTICATED_TO_REALMS, realms);
        condition.setState("{\"authenticateToRealm\": \"REALM\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenEnvironmentContainsRealmsAndDoesNotMatch()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> realms = new HashSet<String>();

        realms.add("OTHER_REALM");
        env.put(REQUEST_AUTHENTICATED_TO_REALMS, realms);
        condition.setState("{\"authenticateToRealm\": \"REALM\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_REALM_CONDITION_ADVICE,
                Collections.singleton("REALM")));
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentDoesNotContainRealmsAndMatches()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> realms = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        realms.add("REALM");
        subject.getPrivateCredentials().add(ssoToken);
        given(coreWrapper.getAuthenticatedRealms(ssoToken)).willReturn(realms);
        condition.setState("{\"authenticateToRealm\": \"REALM\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenEnvironmentDoesNotContainServicesAndDoesNotMatch()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> realms = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        realms.add("OTHER_REALM");
        subject.getPrivateCredentials().add(ssoToken);
        given(coreWrapper.getAuthenticatedRealms(ssoToken)).willReturn(realms);
        condition.setState("{\"authenticateToRealm\": \"REALM\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_REALM_CONDITION_ADVICE,
                Collections.singleton("REALM")));
    }
}
