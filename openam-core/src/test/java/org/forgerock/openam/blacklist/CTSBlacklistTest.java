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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.blacklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledExecutorService;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.services.naming.WebtopNamingQuery;
import org.assertj.core.api.ThrowableAssert;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.tokens.TokenType;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSBlacklistTest {
    private static final String SID = "session123";
    private static final long PURGE_DELAY = 1000L;
    private static final long POLL_INTERVAL = 1000L;

    @Mock
    private CTSPersistentStore mockCts;

    @Mock
    private Session mockSession;

    @Mock
    private ThreadMonitor mockThreadMonitor;

    @Mock
    private ScheduledExecutorService mockScheduler;

    @Mock
    private WebtopNamingQuery mockServerConfig;

    @Mock
    private SessionServiceConfig mockServiceConfig;

    private CTSBlacklist<Blacklistable> testBlacklist;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        given(mockServerConfig.getAMServerID()).willReturn("testServer1");
        testBlacklist = new CTSBlacklist<>(mockCts, TokenType.SESSION_BLACKLIST, mockScheduler, mockThreadMonitor,
                mockServerConfig, PURGE_DELAY, POLL_INTERVAL);

        given(mockSession.getStableStorageID()).willReturn(SID);
    }

    @Test
    public void shouldReturnTrueForBlacklistedSessions() throws Exception {
        // Given
        given(mockCts.read(SID)).willReturn(new Token(SID, TokenType.SESSION_BLACKLIST));

        // When
        final boolean result = testBlacklist.isBlacklisted(mockSession);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldReturnFalseForNonBlacklistedSessions() throws Exception {
        // Given
        given(mockCts.read(SID)).willReturn(null);

        // When
        final boolean result = testBlacklist.isBlacklisted(mockSession);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldConvertCtsReadExceptions() throws Exception {
        // Given
        given(mockCts.read(SID)).willThrow(new CoreTokenException("test"));

        // When/Then
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    @Override
                    public void call() throws Throwable {
                        testBlacklist.isBlacklisted(mockSession);
                    }
                })
                .isInstanceOf(BlacklistException.class);
    }

    @Test
    public void shouldStoreBlacklistedSessionsInCts() throws Exception {
        // Given
        given(mockSession.getBlacklistExpiryTime()).willReturn(1234L);

        // When
        testBlacklist.blacklist(mockSession);

        // Then
        ArgumentCaptor<Token> storedToken = ArgumentCaptor.forClass(Token.class);
        verify(mockCts).create(storedToken.capture());
        assertThat(storedToken.getValue().getTokenId()).isEqualTo(SID);
        assertThat(storedToken.getValue().getType()).isEqualTo(TokenType.SESSION_BLACKLIST);
        assertThat(storedToken.getValue().getExpiryTimestamp().getTimeInMillis()).isEqualTo(1234L + PURGE_DELAY);
    }

    @Test
    public void shouldPropagateSessionExceptions() throws Exception {
        // Given
        given(mockSession.getBlacklistExpiryTime()).willThrow(new BlacklistException("test"));

        // When/Then
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    @Override
                    public void call() throws Throwable {
                        testBlacklist.blacklist(mockSession);
                    }
                })
                .isInstanceOf(BlacklistException.class);
    }

    @Test
    public void shouldConvertCtsExceptions() throws Exception {
        // Given
        given(mockSession.getTimeLeft()).willReturn(3L);
        willThrow(new CoreTokenException("test")).given(mockCts).create(any(Token.class));

        // When/Then
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    @Override
                    public void call() throws Throwable {
                        testBlacklist.blacklist(mockSession);
                    }
                })
                .isInstanceOf(BlacklistException.class);
    }
}