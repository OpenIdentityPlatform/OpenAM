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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.cts.monitoring;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.monitoring.impl.CTSMonitoringStoreImpl;
import org.forgerock.openam.cts.monitoring.impl.operations.TokenOperationsStore;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

public class CTSMonitoringStoreImplTest {

    private CTSOperationsMonitoringStore ctsOperationsMonitoringStore;

    private TokenOperationsStore tokenOperationsStore;

    @BeforeMethod
    public void setUp() {

        tokenOperationsStore = mock(TokenOperationsStore.class);
        final ExecutorService executorService = mock(ExecutorService.class);
        final Debug debug = mock(Debug.class);

        ctsOperationsMonitoringStore = new CTSMonitoringStoreImpl(tokenOperationsStore, executorService, debug);

        given(executorService.submit(any(Callable.class))).will(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Callable r = (Callable) invocation.getArguments()[0];
                r.call();
                return null;
            }
        });
    }

    @Test
    public void shouldNotAddTokenOperationForSpecificTokenTypeIfTokenIsNull() throws InterruptedException {

        //Given
        Token token = null;
        CTSOperation operation = CTSOperation.READ;

        //When
        ctsOperationsMonitoringStore.addTokenOperation(token, operation);

        //Then
        verify(tokenOperationsStore, never()).addTokenOperation(Matchers.<TokenType>anyObject(), eq(operation));
    }

    @Test
    public void shouldAddTokenOperationForSpecificTokenType() throws InterruptedException {

        //Given
        Token token = mock(Token.class);
        CTSOperation operation = CTSOperation.READ;
        TokenType tokenType = TokenType.OAUTH;

        given(token.getType()).willReturn(tokenType);

        //When
        ctsOperationsMonitoringStore.addTokenOperation(token, operation);

        //Then
        verify(tokenOperationsStore).addTokenOperation(tokenType, operation);
    }

    @Test
    public void shouldAddTokenOperation() throws InterruptedException {

        //Given
        CTSOperation operation = CTSOperation.READ;

        //When
        ctsOperationsMonitoringStore.addTokenOperation(null, operation);

        //Then
        verify(tokenOperationsStore).addTokenOperation(operation);
    }

    @Test
    public void shouldGetAverageOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getAverageOperationsPerPeriod(tokenType, operation)).willReturn(1D);

        //When
        double result = ctsOperationsMonitoringStore.getAverageOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1D);
    }

    @Test
    public void shouldGetAverageOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getAverageOperationsPerPeriod(operation)).willReturn(1D);

        //When
        double result = ctsOperationsMonitoringStore.getAverageOperationsPerPeriod(null, operation);

        //Then
        assertEquals(result, 1D);
    }

    @Test
    public void shouldGetMaximumOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getMaximumOperationsPerPeriod(tokenType, operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getMaximumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldGetMaximumOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getMaximumOperationsPerPeriod(operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getMaximumOperationsPerPeriod(null, operation);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldGetMinimumOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getMinimumOperationsPerPeriod(tokenType, operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getMinimumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldGetMinimumOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getMinimumOperationsPerPeriod(operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getMinimumOperationsPerPeriod(null, operation);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldGetOperationsCumulativeCountForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getOperationsCumulativeCount(tokenType, operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getOperationsCumulativeCount(tokenType, operation);

        //Then
        assertEquals(result, 1);
    }

    @Test
    public void shouldGetOperationsCumulativeCount() {

        //Given
        CTSOperation operation = CTSOperation.READ;

        given(tokenOperationsStore.getOperationsCumulativeCount(operation)).willReturn(1L);

        //When
        long result = ctsOperationsMonitoringStore.getOperationsCumulativeCount(null, operation);

        //Then
        assertEquals(result, 1);
    }
}
