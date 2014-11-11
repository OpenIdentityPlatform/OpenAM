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
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.time.TimeService;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.openam.entitlement.conditions.environment.SessionCondition.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class SessionConditionTest {

    private SessionCondition condition;

    private CoreWrapper coreWrapper;
    private TimeService timeService;

    @BeforeMethod
    public void setUp() {

        Debug debug = mock(Debug.class);
        coreWrapper = mock(CoreWrapper.class);
        timeService = mock(TimeService.class);

        condition = new SessionCondition(debug, coreWrapper, timeService);
    }

    @Test
    public void conditionStateShouldParseProperties() {

        //Given

        //When
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //Then
        assertThat(condition.getMaxSessionTime()).isEqualTo(5);
        assertThat(condition.isTerminateSession()).isFalse();
    }

    @Test
    public void conditionStateShouldContainProperties() {

        //Given
        condition.setMaxSessionTime(5);
        condition.setTerminateSession(false);

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"maxSessionTime\":", "5", "\"terminateSession\":", "false");
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenSubjectHasNoSSOToken() throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
        assertThat(decision.getTimeToLive()).isEqualTo(Long.MAX_VALUE);
        verify(coreWrapper, never()).destroyToken(Matchers.<SSOToken>anyObject());
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenEnvironmentHasTokenCreationTimeLessThanMaxSessionTime()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        long tokenCreationTime = now - (5 * 60000) + 1;

        given(timeService.now()).willReturn(now);
        env.put(REQUEST_SESSION_CREATION_TIME, Collections.singleton(tokenCreationTime + ""));
        subject.getPrivateCredentials().add(ssoToken);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
        assertThat(decision.getTimeToLive()).isEqualTo(tokenCreationTime + (5 * 60000));
        verify(coreWrapper, never()).destroyToken(ssoToken);
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenEnvironmentHasTokenCreationTimeEqualToMaxSessionTime()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        long tokenCreationTime = now - (5 * 60000);

        given(timeService.now()).willReturn(now);
        env.put(REQUEST_SESSION_CREATION_TIME, Collections.singleton(tokenCreationTime + ""));
        subject.getPrivateCredentials().add(ssoToken);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(SESSION_CONDITION_ADVICE,
                Collections.singleton(ADVICE_DENY)));
        assertThat(decision.getTimeToLive()).isEqualTo(Long.MAX_VALUE);
        verify(coreWrapper, never()).destroyToken(ssoToken);
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenEnvHasTokenCreationTimeEqualToMaxSessionTimeWithTerminateAdvice()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        long tokenCreationTime = now - (5 * 60000);

        given(timeService.now()).willReturn(now);
        env.put(REQUEST_SESSION_CREATION_TIME, Collections.singleton(tokenCreationTime + ""));
        subject.getPrivateCredentials().add(ssoToken);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": true}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        Set<String> expectedAdvice = new HashSet<String>();
        expectedAdvice.add(ADVICE_DENY);
        expectedAdvice.add(ADVICE_TERMINATE_SESSION);
        assertThat(decision.getAdvices()).containsOnly(entry(SESSION_CONDITION_ADVICE, expectedAdvice));
        assertThat(decision.getTimeToLive()).isEqualTo(Long.MAX_VALUE);
        verify(coreWrapper).destroyToken(ssoToken);
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenSSOTokenCreationTimeLessThanMaxSessionTime()
            throws EntitlementException, SSOException, ParseException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        String tokenCreationTime = DateUtils.dateToString(new Date(now - (5 * 60000) + 60000));

        given(timeService.now()).willReturn(now);
        subject.getPrivateCredentials().add(ssoToken);
        given(ssoToken.getProperty("authInstant")).willReturn(tokenCreationTime);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
        assertThat(decision.getTimeToLive())
                .isEqualTo(DateUtils.stringToDate(tokenCreationTime).getTime() + (5 * 60000));
        verify(coreWrapper, never()).destroyToken(ssoToken);
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenSSOTokenCreationTimeEqualToMaxSessionTime()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        String tokenCreationTime = DateUtils.dateToString(new Date(now - (5 * 60000)));

        given(timeService.now()).willReturn(now);
        subject.getPrivateCredentials().add(ssoToken);
        given(ssoToken.getProperty("authInstant")).willReturn(tokenCreationTime);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": false}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).containsOnly(entry(SESSION_CONDITION_ADVICE,
                Collections.singleton(ADVICE_DENY)));
        assertThat(decision.getTimeToLive()).isEqualTo(Long.MAX_VALUE);
        verify(coreWrapper, never()).destroyToken(ssoToken);
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenSSOTokenCreationTimeEqualToMaxSessionTimeWithTerminateSessionAdvice()
            throws EntitlementException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        SSOToken ssoToken = mock(SSOToken.class);
        long now = System.currentTimeMillis();
        String tokenCreationTime = DateUtils.dateToString(new Date(now - (5 * 60000)));

        given(timeService.now()).willReturn(now);
        subject.getPrivateCredentials().add(ssoToken);
        given(ssoToken.getProperty("authInstant")).willReturn(tokenCreationTime);
        condition.setState("{\"maxSessionTime\": 5, \"terminateSession\": true}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        Set<String> expectedAdvice = new HashSet<String>();
        expectedAdvice.add(ADVICE_DENY);
        expectedAdvice.add(ADVICE_TERMINATE_SESSION);
        assertThat(decision.getAdvices()).containsOnly(entry(SESSION_CONDITION_ADVICE, expectedAdvice));
        assertThat(decision.getTimeToLive()).isEqualTo(Long.MAX_VALUE);
        verify(coreWrapper).destroyToken(ssoToken);
    }
}
