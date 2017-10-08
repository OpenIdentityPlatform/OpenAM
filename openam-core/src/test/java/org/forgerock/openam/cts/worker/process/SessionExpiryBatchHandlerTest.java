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
import static org.forgerock.openam.cts.api.CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION;
import static org.forgerock.openam.cts.api.CTSOptions.PRE_DELETE_READ_OPTION;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.worker.process.SessionExpiryBatchHandler.StateChangeResultHandler;
import org.forgerock.openam.cts.worker.process.SessionExpiryBatchHandler.StateChangeResultHandlerFactory;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.sm.datalayer.api.OptimisticConcurrencyCheckFailedException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.util.Options;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.SessionConstraint;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.session.util.SessionUtilsWrapper;
import com.sun.identity.shared.debug.Debug;

public class SessionExpiryBatchHandlerTest {

    @Mock private Debug mockDebug;
    @Mock private InternalSessionEventBroker mockInternalSessionEventBroker;
    @Mock private LocalOperations mockLocalOperations;
    @Mock private SessionAdapter mockSessionAdapter;
    @Mock private SessionConstraint mockSessionConstraint;
    @Mock private SessionService mockSessionService;
    @Mock private SessionServiceConfig mockSessionServiceConfig;
    @Mock private SessionUtilsWrapper mockSessionUtilsWrapper;
    @Mock private StateChangeResultHandlerFactory mockResultHandlerFactory;
    @Mock private TaskDispatcher mockQueue;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void attemptsToDeleteSessionsThatHaveReachedTheirIdleTimeOut() throws Exception {
        // Given
        List<PartialToken> tokens = Collections.singletonList(mockPartialToken("tokenId"));

        // When
        newSessionExpiryBatchHandler(SessionEventType.IDLE_TIMEOUT).timeoutBatch(tokens);

        // Then
        ArgumentCaptor<Options> optionsCaptor = ArgumentCaptor.forClass(Options.class);
        verify(mockQueue).delete(eq("tokenId"), optionsCaptor.capture(), any(ResultHandler.class));
        Options options = optionsCaptor.getValue();
        assertThat(options.get(PRE_DELETE_READ_OPTION)).containsExactly(CoreTokenField.values());
        assertThat(options.get(OPTIMISTIC_CONCURRENCY_CHECK_OPTION)).isEqualTo(etagFor("tokenId"));
    }

    @Test
    public void attemptsToDeleteSessionsThatReachedTheirMaxSessionTimeOut() throws Exception {
        // Given
        List<PartialToken> tokens = Collections.singletonList(mockPartialToken("tokenId"));

        // When
        newSessionExpiryBatchHandler(SessionEventType.MAX_TIMEOUT).timeoutBatch(tokens);

        // Then
        ArgumentCaptor<Options> optionsCaptor = ArgumentCaptor.forClass(Options.class);
        verify(mockQueue).delete(eq("tokenId"), optionsCaptor.capture(), any(ResultHandler.class));
        Options options = optionsCaptor.getValue();
        assertThat(options.get(PRE_DELETE_READ_OPTION)).containsExactly(CoreTokenField.values());
        assertThat(options.get(OPTIMISTIC_CONCURRENCY_CHECK_OPTION)).isNull();
    }

    @DataProvider(name = "timeoutTypes")
    public Object[][] getTimeoutTypes() {
        return new Object[][] {
                { SessionEventType.MAX_TIMEOUT },
                { SessionEventType.IDLE_TIMEOUT }
        };
    }

    @Test(dataProvider = "timeoutTypes")
    public void returnsCountDownLatchForCallerToAwaitCompletionOfBatchProcessing(SessionEventType eventType)
            throws Exception {
        // Given
        List<PartialToken> tokens = Arrays.asList(
                mockPartialToken("tokenId-one"),
                mockPartialToken("tokenId-two"),
                mockPartialToken("tokenId-three"));

        // When
        CountDownLatch countDownLatch = newSessionExpiryBatchHandler(eventType).timeoutBatch(tokens);

        // Then
        assertThat(countDownLatch.getCount()).isEqualTo(tokens.size());
        verify(mockResultHandlerFactory).create(eq(eventType), eq(countDownLatch));
    }

    @Test(dataProvider = "timeoutTypes")
    public void resultHandlerPerformsTimeoutIfTokenDeletionSucceeds(SessionEventType eventType) throws Exception {
        // Given
        InternalSession mockInternalSession = mock(InternalSession.class);
        given(mockSessionAdapter.fromToken(any(Token.class))).willReturn(mockInternalSession);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StateChangeResultHandler handler = newStateChangeResultHandler(eventType, countDownLatch);

        PartialToken partialToken = new Token("tokenId", TokenType.SESSION).toPartialToken();

        // When
        handler.processResults(partialToken);

        // Then
        verify(mockLocalOperations).timeout(mockInternalSession, eventType);
        assertThat(countDownLatch.getCount()).as("processResults decrements CountDownLatch").isEqualTo(0);
    }

    @Test(dataProvider = "timeoutTypes")
    public void resultHandlerSkipsTimeoutIfTokenAlreadyDeleted(SessionEventType eventType) throws Exception {
        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StateChangeResultHandler handler = newStateChangeResultHandler(eventType, countDownLatch);

        PartialToken partialToken = mockPartialToken("tokenId");
        given(partialToken.canConvertToToken()).willReturn(false);

        // When
        handler.processResults(partialToken);

        // Then
        verifyNoMoreInteractions(mockSessionAdapter, mockLocalOperations);
        verify(mockDebug).message("Failed to delete token. Already deleted.");
        assertThat(countDownLatch.getCount()).as("processResults decrements CountDownLatch").isEqualTo(0);
    }

    @Test(dataProvider = "timeoutTypes")
    public void resultHandlerSkipsTimeoutIfTokenUpdatedByAnotherProcess(SessionEventType eventType) throws Exception {
        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StateChangeResultHandler handler = newStateChangeResultHandler(eventType, countDownLatch);
        OptimisticConcurrencyCheckFailedException exception = mock(OptimisticConcurrencyCheckFailedException.class);

        // When
        handler.processError(exception);

        // Then
        verifyNoMoreInteractions(mockSessionAdapter, mockLocalOperations);
        verify(mockDebug).message(eq("Failed to delete token with expired timeout. {}"), any(String.class), eq(exception));
        assertThat(countDownLatch.getCount()).as("processError decrements CountDownLatch").isEqualTo(0);
    }


    /**
     * This is almost identical to {@link #resultHandlerSkipsTimeoutIfTokenUpdatedByAnotherProcess(SessionEventType)}
     * but asserts that error level, rather than message level, logging is used when an unexpected type of exception occurs.
     */
    @Test(dataProvider = "timeoutTypes")
    public void resultHandlerSkipsTimeoutIfTokenDeletionFails(SessionEventType eventType) throws Exception {
        // Given
        CountDownLatch countDownLatch = new CountDownLatch(1);
        StateChangeResultHandler handler = newStateChangeResultHandler(eventType, countDownLatch);
        Exception exception = mock(Exception.class);

        // When
        handler.processError(exception);

        // Then
        verifyNoMoreInteractions(mockSessionAdapter, mockLocalOperations);
        verify(mockDebug).error(eq("Failed to delete token with expired timeout. {}"), any(String.class), eq(exception));
        assertThat(countDownLatch.getCount()).as("processError decrements CountDownLatch").isEqualTo(0);
    }

    private SessionExpiryBatchHandler newSessionExpiryBatchHandler(SessionEventType eventType) {
        return new SessionExpiryBatchHandler(mockQueue, eventType, mockResultHandlerFactory);
    }

    private StateChangeResultHandler newStateChangeResultHandler(SessionEventType eventType, CountDownLatch latch) {
        return new StateChangeResultHandler(
                mockDebug, mockSessionAdapter, mockLocalOperations, eventType, latch, mockSessionService,
                mockSessionServiceConfig, mockInternalSessionEventBroker, mockSessionUtilsWrapper,
                mockSessionConstraint);
    }

    private PartialToken mockPartialToken(String sessionId) {
        PartialToken mockToken = mock(PartialToken.class);
        given(mockToken.getValue(CoreTokenField.TOKEN_ID)).willReturn(sessionId);
        given(mockToken.getValue(CoreTokenField.ETAG)).willReturn(etagFor(sessionId));
        return mockToken;
    }

    private String etagFor(String sessionId) {
        return "etag:" + sessionId;
    }
}

