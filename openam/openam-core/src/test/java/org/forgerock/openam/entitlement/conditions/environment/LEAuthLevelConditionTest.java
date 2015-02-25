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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class LEAuthLevelConditionTest {

    private LEAuthLevelCondition condition;

    private CoreWrapper authUtils;

    @BeforeMethod
    public void setUp() {

        Debug debug = mock(Debug.class);
        authUtils = mock(CoreWrapper.class);

        condition = new LEAuthLevelCondition(debug, authUtils);
    }

    @Test
    public void conditionStateShouldParseAuthLevel() {

        //Given

        //When
        condition.setState("{\"authLevel\": 5}");

        //Then
        assertThat(condition.getAuthLevel()).isEqualTo(5);
    }

    @Test
    public void conditionStateShouldContainAuthLevel() {

        //Given
        condition.setAuthLevel(5);

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"authLevel\":", "5");
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void conditionShouldThrowEntitlementExceptionWhenEvaluatingWithNoAuthLevelSet() throws EntitlementException {

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
    public void conditionShouldEvaluateToTrueWhenUsingRequestAuthLevelsFromEnvironmentWithRealmAndIsLE()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> requestAuthLevels = new HashSet<String>();

        given(authUtils.getRealmFromRealmQualifiedData("5")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("3")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("4")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("6")).willReturn("OTHER_REALM");
        given(authUtils.getDataFromRealmQualifiedData("3")).willReturn("3");
        given(authUtils.getDataFromRealmQualifiedData("4")).willReturn("4");
        given(authUtils.getDataFromRealmQualifiedData("6")).willReturn("6");

        requestAuthLevels.add("3");
        requestAuthLevels.add("4");
        requestAuthLevels.add("6");
        env.put("requestAuthLevel", requestAuthLevels);
        condition.setState("{\"authLevel\": 5}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenUsingRequestAuthLevelsFromEnvironmentWithoutRealmAndNotLE()
            throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> requestAuthLevels = new HashSet<String>();

        given(authUtils.getDataFromRealmQualifiedData("3")).willReturn("3");
        given(authUtils.getDataFromRealmQualifiedData("4")).willReturn("4");
        given(authUtils.getDataFromRealmQualifiedData("6")).willReturn("6");

        requestAuthLevels.add("3");
        requestAuthLevels.add("4");
        requestAuthLevels.add("6");
        env.put("requestAuthLevel", requestAuthLevels);
        condition.setState("{\"authLevel\": 5}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenUsingAuthLevelsFromSSOTokenWithRealmAndIsLE()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        Set<String> authenticatedLevels = new HashSet<String>();

        given(authUtils.getRealmFromRealmQualifiedData("5")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("3")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("4")).willReturn("REALM");
        given(authUtils.getRealmFromRealmQualifiedData("6")).willReturn("OTHER_REALM");
        given(authUtils.getDataFromRealmQualifiedData("3")).willReturn("3");
        given(authUtils.getDataFromRealmQualifiedData("4")).willReturn("4");
        given(authUtils.getDataFromRealmQualifiedData("6")).willReturn("6");
        condition.setState("{\"authLevel\": 5}");

        subject.getPrivateCredentials().add(ssoToken);
        authenticatedLevels.add("3");
        authenticatedLevels.add("4");
        authenticatedLevels.add("6");
        given(authUtils.getRealmQualifiedAuthenticatedLevels(ssoToken)).willReturn(authenticatedLevels);


        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenUsingAuthLevelsFromSSOTokenWithRealmAndNotLE()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        Set<String> authenticatedLevels = new HashSet<String>();

        given(authUtils.getDataFromRealmQualifiedData("3")).willReturn("3");
        given(authUtils.getDataFromRealmQualifiedData("4")).willReturn("4");
        given(authUtils.getDataFromRealmQualifiedData("6")).willReturn("6");
        condition.setState("{\"authLevel\": 5}");

        subject.getPrivateCredentials().add(ssoToken);
        authenticatedLevels.add("3");
        authenticatedLevels.add("4");
        authenticatedLevels.add("6");
        given(authUtils.getAuthenticatedLevels(ssoToken)).willReturn(authenticatedLevels);


        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }
}
