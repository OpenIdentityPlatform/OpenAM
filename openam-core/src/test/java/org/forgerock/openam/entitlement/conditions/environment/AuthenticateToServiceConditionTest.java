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
import org.forgerock.openam.core.CoreWrapper;
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
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTHENTICATED_TO_SERVICES;

public class AuthenticateToServiceConditionTest {

    private AuthenticateToServiceCondition condition;

    private CoreWrapper coreWrapper;
    private EntitlementCoreWrapper entitlementCoreWrapper;

    @BeforeMethod
    public void setUp() {

        Debug debug = mock(Debug.class);
        coreWrapper = mock(CoreWrapper.class);
        entitlementCoreWrapper = mock(EntitlementCoreWrapper.class);

        condition = new AuthenticateToServiceCondition(debug, coreWrapper, entitlementCoreWrapper);
    }

    @Test
    public void conditionStateShouldParseAuthenticateToService() {

        //Given

        //When
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //Then
        assertThat(condition.getAuthenticateToService()).isEqualTo("SERVICE_NAME");
    }

    @Test
    public void conditionStateShouldContainAuthenticateToService() {

        //Given
        condition.setAuthenticateToService("SERVICE_NAME");

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"authenticateToService\": \"SERVICE_NAME\"");
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void conditionShouldThrowEntitlementExceptionWhenEvaluatingWithNoAuthenticateToServiceSet()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        //When
        condition.evaluate(realm, subject, resourceName, env);

        //Then
        //Expected EntitlementException
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentContainsServicesAndMatches() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();

        given(coreWrapper.getRealmFromRealmQualifiedData("SERVICE_NAME")).willReturn("REALM");
        services.add("SERVICE_NAME");
        env.put(REQUEST_AUTHENTICATED_TO_SERVICES, services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenEnvironmentContainsServicesAndDoesNotMatch()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();

        given(coreWrapper.getRealmFromRealmQualifiedData("SERVICE_NAME")).willReturn("REALM");
        services.add("OTHER_SERVICE_NAME");
        env.put(REQUEST_AUTHENTICATED_TO_SERVICES, services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                Collections.singleton("REALM:SERVICE_NAME")));
    }

    @Test
    public void conditionShouldNotAlterARealmQualifiedServiceString()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();

        given(coreWrapper.getRealmFromRealmQualifiedData("SERVICE_NAME")).willReturn("REALM");
        services.add("OTHER_SERVICE_NAME");
        env.put(REQUEST_AUTHENTICATED_TO_SERVICES, services);
        condition.setState("{\"authenticateToService\": \"REALM:SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                Collections.singleton("REALM:SERVICE_NAME")));
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentContainsServicesAndRealmIsPresentAndMatches()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();

        given(coreWrapper.getDataFromRealmQualifiedData("OTHER_SERVICE_NAME")).willReturn("SERVICE_NAME");
        services.add("OTHER_SERVICE_NAME");
        env.put(REQUEST_AUTHENTICATED_TO_SERVICES, services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentContainsServicesAndRealmIsPresentAndDoesNotMatch()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();

        given(coreWrapper.getDataFromRealmQualifiedData("OTHER_SERVICE_NAME")).willReturn("OTHER_SERVICE_NAME");
        services.add("OTHER_SERVICE_NAME");
        env.put(REQUEST_AUTHENTICATED_TO_SERVICES, services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                Collections.singleton("REALM:SERVICE_NAME")));
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentDoesNotContainServicesAndMatches()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        given(coreWrapper.getRealmFromRealmQualifiedData("SERVICE_NAME")).willReturn("REALM");
        services.add("SERVICE_NAME");
        subject.getPrivateCredentials().add(ssoToken);
        given(entitlementCoreWrapper.getRealmQualifiedAuthenticatedServices(ssoToken)).willReturn(services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

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
        Set<String> services = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        given(coreWrapper.getRealmFromRealmQualifiedData("SERVICE_NAME")).willReturn("REALM");
        services.add("OTHER_SERVICE_NAME");
        subject.getPrivateCredentials().add(ssoToken);
        given(entitlementCoreWrapper.getRealmQualifiedAuthenticatedServices(ssoToken)).willReturn(services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                Collections.singleton("REALM:SERVICE_NAME")));
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentDoesNotContainServicesAndRealmIsPresentAndMatches()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        given(coreWrapper.getDataFromRealmQualifiedData("OTHER_SERVICE_NAME")).willReturn("SERVICE_NAME");
        services.add("OTHER_SERVICE_NAME");
        subject.getPrivateCredentials().add(ssoToken);
        given(entitlementCoreWrapper.getRealmQualifiedAuthenticatedServices(ssoToken)).willReturn(services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentDoesNotContainServicesAndRealmIsPresentAndDoesNotMatch()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> services = new HashSet<String>();
        SSOToken ssoToken = mock(SSOToken.class);

        given(coreWrapper.getDataFromRealmQualifiedData("OTHER_SERVICE_NAME")).willReturn("OTHER_SERVICE_NAME");
        services.add("OTHER_SERVICE_NAME");
        subject.getPrivateCredentials().add(ssoToken);
        given(entitlementCoreWrapper.getRealmQualifiedAuthenticatedServices(ssoToken)).willReturn(services);
        condition.setState("{\"authenticateToService\": \"SERVICE_NAME\"}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                Collections.singleton("REALM:SERVICE_NAME")));
    }
}
