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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.worker.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_MAX_TIMED_OUT;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Provider;

import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.share.SessionInfo;

public class SessionExpiryBatchHandlerTest {

    private SessionExpiryBatchHandler handler;
    private TaskDispatcher mockQueue;
    private SessionAccessManager mockSessionAccessManager;
    private SessionAuditor mockSessionAuditor;

    private ArgumentCaptor<Token> tokenArgumentCaptor;
    private ArgumentCaptor<ResultHandler> resultHandlerArgumentCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        mockQueue = mock(TaskDispatcher.class);
        mockSessionAccessManager = mock(SessionAccessManager.class);
        mockSessionAuditor = mock(SessionAuditor.class);
        Provider<SessionAccessManager> sessionAccessManagerProvider = mock(Provider.class);
        given(sessionAccessManagerProvider.get()).willReturn(mockSessionAccessManager);

        handler = SessionExpiryBatchHandler.forMaxSessionTimeExpired(
                mockQueue, sessionAccessManagerProvider, mockSessionAuditor);

        tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
        resultHandlerArgumentCaptor = ArgumentCaptor.forClass(ResultHandler.class);
    }

    @Test
    public void attemptsToUpdateSessionStateOfAllTokensInBatch() throws Exception {
        // Given
        List<PartialToken> tokens = Arrays.asList(partialToken("one"), partialToken("two"), partialToken("three"));

        // When
        handler.timeoutBatch(tokens);

        // Then
        assertAttemptsToUpdateSessionStateOfAllTokens(tokens, "DESTROYED");
    }

    @Test
    public void returnsCountDownLatchForCallerToAwaitCompletionOfBatchProcessing() throws CoreTokenException {
        // Given
        List<PartialToken> tokens = Arrays.asList(partialToken("one"), partialToken("two"), partialToken("three"));

        // When
        CountDownLatch countDownLatch = handler.timeoutBatch(tokens);

        // Then
        assertThat(countDownLatch.getCount()).isEqualTo(tokens.size());
    }

    @Test
    public void stateChangeResultHandlerPublishesNotificationsAndAuditEventsIfSessionStateUpdateSucceeds() throws Exception {
        // Given
        List<PartialToken> tokens = Collections.singletonList(partialToken("one"));
        CountDownLatch countDownLatch = handler.timeoutBatch(tokens);
        verify(mockQueue, times(1)).update(tokenArgumentCaptor.capture(), resultHandlerArgumentCaptor.capture());
        InternalSession mockSession = mock(InternalSession.class);
        given(mockSessionAccessManager.getInternalSession(any(SessionID.class))).willReturn(mockSession);

        // When
        resultHandlerArgumentCaptor.getValue().processResults(tokenArgumentCaptor.getValue());

        // Then
        assertThat(countDownLatch.getCount()).isZero();
        verify(mockSession, times(1)).changeStateAndNotify(SessionEventType.MAX_TIMEOUT);
        verify(mockSessionAuditor, times(1)).auditActivity(any(SessionInfo.class), eq(AM_SESSION_MAX_TIMED_OUT));
    }

    private void assertAttemptsToUpdateSessionStateOfAllTokens(List<PartialToken> tokens, String newState) throws Exception {
        verify(mockQueue, times(tokens.size())).update(tokenArgumentCaptor.capture(), any(ResultHandler.class));
        Iterator<PartialToken> partialTokenIterator = tokens.iterator();
        for (final Token token : tokenArgumentCaptor.getAllValues()) {
            final String expectedTokenId = partialTokenIterator.next().getValue(CoreTokenField.TOKEN_ID);
            assertThat(token.getAttribute(CoreTokenField.TOKEN_ID)).isEqualTo(expectedTokenId);
            assertThat(token.getAttribute(CoreTokenField.TOKEN_TYPE)).isEqualTo(TokenType.SESSION);
            assertThat(token.getAttribute(CoreTokenField.ETAG)).isEqualTo("etag:" + expectedTokenId);
            assertThat(token.getAttribute(SessionTokenField.SESSION_STATE.getField())).isEqualTo(newState);
        }
        assertThat(partialTokenIterator.hasNext()).isFalse();
    }

    private PartialToken partialToken(String sessionId) {
        PartialToken mockToken = mock(PartialToken.class);
        given(mockToken.getValue(CoreTokenField.TOKEN_ID)).willReturn(sessionId);
        given(mockToken.getValue(CoreTokenField.ETAG)).willReturn("etag:" + sessionId);
        return mockToken;
    }
}

