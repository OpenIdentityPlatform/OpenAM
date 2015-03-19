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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sso.providers.stateless;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.providers.dpro.SSOPrincipal;
import com.iplanet.sso.providers.dpro.SSOSessionListener;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.security.Principal;

public class StatelessSSOTokenTest {

    @Mock
    private StatelessSession mockSession;

    private StatelessSSOToken statelessSSOToken;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        statelessSSOToken = new StatelessSSOToken(mockSession);
    }

    @Test
    public void shouldBeInvalidIfTimedOut() throws Exception {
        given(mockSession.isTimedOut()).willReturn(true);

        assertFalse(statelessSSOToken.isValid());
    }

    @Test
    public void shouldReturnCorrectSSOPrincipal() throws Exception {
        // Given
        String name = "fred";
        given(mockSession.getProperty(ISAuthConstants.PRINCIPAL)).willReturn(name);

        // When
        Principal result = statelessSSOToken.getPrincipal();

        // Then
        assertThat(result).isInstanceOf(SSOPrincipal.class).isEqualTo(new SSOPrincipal(name));
    }

    @Test
    public void shouldReturnAuthTypeIfOnlyOne() throws Exception {
        // Given
        String authType = "DataStore";
        given(mockSession.getProperty(ISAuthConstants.AUTH_TYPE)).willReturn(authType);

        // When
        String result = statelessSSOToken.getAuthType();

        // Then
        assertThat(result).isEqualTo(authType);
    }

    @Test
    public void shouldReturnFirstAuthTypeIfSeveral() throws Exception {
        // Given
        String firstAuthType = "FirstType";
        String authTypes = firstAuthType + "|OtherType|AnotherType";
        given(mockSession.getProperty(ISAuthConstants.AUTH_TYPE)).willReturn(authTypes);

        // When
        String result = statelessSSOToken.getAuthType();

        // Then
        assertThat(result).isEqualTo(firstAuthType);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldThrowExceptionIfAuthTypeUnknown() throws Exception {
        statelessSSOToken.getAuthType();
    }

    @Test
    public void shouldReturnCorrectAuthLevelIfNotRealmQualified() throws Exception {
        // Given
        int authLevel = 42;
        given(mockSession.getProperty(ISAuthConstants.AUTH_LEVEL)).willReturn(Integer.toString(authLevel));

        // When
        int result = statelessSSOToken.getAuthLevel();

        // Then
        assertThat(result).isEqualTo(authLevel);
    }

    @Test
    public void shouldReturnCorrectAuthLevelIfRealmQualified() throws Exception {
        // Given
        int authLevel = 42;
        given(mockSession.getProperty(ISAuthConstants.AUTH_LEVEL)).willReturn("/foo:" + Integer.toString(authLevel));

        // When
        int result = statelessSSOToken.getAuthLevel();

        // Then
        assertThat(result).isEqualTo(authLevel);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldThrowExceptionIfAuthLevelUnknown() throws Exception {
        statelessSSOToken.getAuthLevel();
    }

    @Test
    public void shouldGetClientIpAddress() throws Exception {
        // Given
        String ip = "10.0.0.9";
        given(mockSession.getProperty(ISAuthConstants.HOST)).willReturn(ip);

        // When
        InetAddress result = statelessSSOToken.getIPAddress();

        // Then
        assertThat(result.getHostAddress()).isEqualTo(ip);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldThrowExceptionForMissingClientIp() throws Exception {
        statelessSSOToken.getIPAddress();
    }

    @Test
    public void shouldGetClientHostname() throws Exception {
        // Given
        String host = "sso.example.com";
        given(mockSession.getProperty(ISAuthConstants.HOST_NAME)).willReturn(host);

        // When
        String result = statelessSSOToken.getHostName();

        // Then
        assertThat(result).isEqualTo(host);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldThrowExceptionForUnknownHostname() throws Exception {
        statelessSSOToken.getHostName();
    }

    @Test
    public void shouldGetTimeLeftFromSession() throws Exception {
        // Given
        long timeLeft = 123l;
        given(mockSession.getTimeLeft()).willReturn(timeLeft);

        // When
        long result = statelessSSOToken.getTimeLeft();

        // Then
        assertThat(result).isEqualTo(timeLeft);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldPropagateExceptionsWhenReadingTimeLeft() throws Exception {
        given(mockSession.getTimeLeft()).willThrow(new SessionException("test"));
        statelessSSOToken.getTimeLeft();
    }

    @Test
    public void shouldGetIdleTimeFromSession() throws Exception {
        // Given
        long timeIdle = 1234l;
        given(mockSession.getIdleTime()).willReturn(timeIdle);

        // When
        long result = statelessSSOToken.getIdleTime();

        // Then
        assertThat(result).isEqualTo(timeIdle);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldPropagateExceptionsWhenReadingTimeIdle() throws Exception {
        given(mockSession.getIdleTime()).willThrow(new SessionException("test"));
        statelessSSOToken.getIdleTime();
    }

    @Test
    public void shouldReadMaxSessionTimeFromSession() throws Exception {
        // Given
        long maxSessionTime = 42l;
        given(mockSession.getMaxSessionTime()).willReturn(maxSessionTime);

        // When
        long result = statelessSSOToken.getMaxSessionTime();

        // Then
        assertThat(result).isEqualTo(maxSessionTime);
    }

    @Test
    public void shouldReadMaxIdleTimeFromSession() throws Exception {
        // Given
        long maxIdleTime = 53l;
        given(mockSession.getMaxIdleTime()).willReturn(maxIdleTime);

        // When
        long result = statelessSSOToken.getMaxIdleTime();

        // Then
        assertThat(result).isEqualTo(maxIdleTime);
    }

    @Test
    public void shouldUseSessionIdAsTokenId() throws Exception {
        // Given
        String sessionId = "test session id";
        given(mockSession.getID()).willReturn(new SessionID(sessionId));

        // When
        SSOTokenID result = statelessSSOToken.getTokenID();

        // Then
        assertThat(result.toString()).isEqualTo(sessionId);
    }

    @Test
    public void shouldSetPropertiesOnTheSession() throws Exception {
        // Given
        String propertyName = "testProperty";
        String propertyValue = "testValue";

        // When
        statelessSSOToken.setProperty(propertyName, propertyValue);

        // Then
        verify(mockSession).setProperty(propertyName, propertyValue);
    }

    @Test
    public void shouldReadPropertiesFromSession() throws Exception {
        // Given
        String propertyName = "testProperty";
        String propertyValue = "testValue";
        given(mockSession.getProperty(propertyName)).willReturn(propertyValue);

        // When
        String result = statelessSSOToken.getProperty(propertyName);

        // Then
        assertThat(result).isEqualTo(propertyValue);
    }

    @Test
    public void shouldReadPropertiesNormallyIfPresentAndStateIgnored() throws Exception {
        // Given
        String propertyName = "testProperty";
        String propertyValue = "testValue";
        given(mockSession.getProperty(propertyName)).willReturn(propertyValue);

        // When
        String result = statelessSSOToken.getProperty(propertyName, true);

        // Then
        assertThat(result).isEqualTo(propertyValue);
    }

    @Test
    public void shouldReadPropertiesNormallyIfPresentAndStateNotIgnored() throws Exception {
        // Given
        String propertyName = "testProperty";
        String propertyValue = "testValue";
        given(mockSession.getProperty(propertyName)).willReturn(propertyValue);

        // When
        String result = statelessSSOToken.getProperty(propertyName, false);

        // Then
        assertThat(result).isEqualTo(propertyValue);
    }

    @Test
    public void shouldFallBackOnUnvalidatedPropertiesIfStateIgnored() throws Exception {
        // Given
        String propertyName = "testProperty";
        given(mockSession.getProperty(propertyName)).willThrow(new SessionException("test"));
        String unvalidatedPropertyValue = "unvalidatedValue";
        given(mockSession.getPropertyWithoutValidation(propertyName)).willReturn(unvalidatedPropertyValue);

        // When
        String result = statelessSSOToken.getProperty(propertyName, true);

        // Then
        assertThat(result).isEqualTo(unvalidatedPropertyValue);
    }

    @Test(expectedExceptions = SSOException.class)
    public void shouldPropagateExceptionIfStateNotIgnoredAndPropertyError() throws Exception {
        // Given
        String propertyName = "testProperty";
        given(mockSession.getProperty(propertyName)).willThrow(new SessionException("test"));

        // When
        statelessSSOToken.getProperty(propertyName, false);

        // Then - exception
    }

    @Test
    public void shouldAddListenersToSession() throws Exception {
        // Given
        SSOTokenListener listener = mock(SSOTokenListener.class);

        // When
        statelessSSOToken.addSSOTokenListener(listener);

        // Then
        verify(mockSession).addSessionListener(any(SSOSessionListener.class));
    }
}