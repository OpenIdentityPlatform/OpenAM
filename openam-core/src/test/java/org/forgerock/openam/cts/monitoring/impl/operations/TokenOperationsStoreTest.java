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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.monitoring.impl.operations;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.TokenType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TokenOperationsStoreTest {

    private TokenOperationsStore tokenOperationsStore;

    private TokenOperationsStore.OperationStoreFactory operationStoreFactory;
    private Map<TokenType, OperationStore> tokenOperations;
    private OperationStore operationStore;
    private OperationStore operationFailureStore;

    @BeforeMethod
    public void setUp() {

        operationStoreFactory = mock(TokenOperationsStore.OperationStoreFactory.class);
        tokenOperations = new HashMap<TokenType, OperationStore>();
        operationStore = mock(OperationStore.class);
        operationFailureStore = new OperationStore();

        tokenOperationsStore = new TokenOperationsStore(operationStoreFactory, tokenOperations, operationStore, operationFailureStore);
    }

    @Test
    public void shouldCreateTokenOperationsStoreWithDefaultConstructor() {

        //Given

        //When
        TokenOperationsStore localTokenOperationsStore = new TokenOperationsStore();

        //Then
        assertNotNull(localTokenOperationsStore);
    }

    @Test
    public void shouldAddTokenOperationForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        given(operationStoreFactory.createOperationStore()).willReturn(typeOperationStore);

        //When
        tokenOperationsStore.addTokenOperation(tokenType, operation, true);

        //Then
        assertTrue(tokenOperations.containsKey(TokenType.OAUTH));
        verify(typeOperationStore).add(operation);
    }

    @Test
    public void shouldAddTokenOperationForSpecificTokenTypeToExistingOperationStore() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        tokenOperations.put(TokenType.OAUTH, typeOperationStore);

        //When
        tokenOperationsStore.addTokenOperation(tokenType, operation, true);

        //Then
        verifyZeroInteractions(operationStoreFactory);
        verify(typeOperationStore).add(operation);
    }

    @Test
    public void shouldAddTokenOperationForSpecificTokenTypeUsingDefaultOperationStoreFactory() {

        //Given
        TokenOperationsStore.OperationStoreFactory operationStoreFactory =
                new TokenOperationsStore.OperationStoreFactory();
        TokenOperationsStore localTokenOperationsStore = new TokenOperationsStore(operationStoreFactory,
                tokenOperations, operationStore, operationFailureStore);

        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;

        //When
        localTokenOperationsStore.addTokenOperation(tokenType, operation, true);

        //Then
        assertTrue(tokenOperations.containsKey(TokenType.OAUTH));
    }

    @Test
    public void shouldAddTokenOperation() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        //When
        tokenOperationsStore.addTokenOperation(operation, true);

        //Then
        verify(operationStore).add(operation);
    }

    @Test
    public void shouldGetAverageOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        tokenOperations.put(TokenType.OAUTH, typeOperationStore);
        given(typeOperationStore.getAverageRate(operation)).willReturn(1D);

        //When
        double result = tokenOperationsStore.getAverageOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1D);
    }

    @Test
    public void shouldGetAverageFailureRateForOperation() {
        // Given
        // Use mock failure store for this test for simplicity
        operationFailureStore = mock(OperationStore.class);
        tokenOperationsStore = new TokenOperationsStore(operationStoreFactory, tokenOperations, operationStore, operationFailureStore);
        CTSOperation operation = CTSOperation.READ;
        double failureRate = 3.14159;

        given(operationFailureStore.getAverageRate(operation)).willReturn(failureRate);

        // When
        double result = tokenOperationsStore.getAverageOperationFailuresPerPeriod(operation);

        // Then
        assertEquals(result, failureRate);
    }

    @Test
    public void getAverageOperationsPerPeriodForSpecificTokenTypeShouldReturnZeroIfTokenTypeNotSet() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;

        //When
        double result = tokenOperationsStore.getAverageOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 0D);
    }

    @Test
    public void shouldGetAverageOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        given(operationStore.getAverageRate(operation)).willReturn(1D);

        //When
        double result = tokenOperationsStore.getAverageOperationsPerPeriod(operation);

        //Then
        assertEquals(result, 1D);
    }

    @Test
    public void shouldGetMaximumOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        tokenOperations.put(TokenType.OAUTH, typeOperationStore);
        given(typeOperationStore.getMaxRate(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getMaximumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getMaximumOperationsPerPeriodForSpecificTokenTypeShouldReturnZeroIfTokenTypeNotSet() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = tokenOperationsStore.getMaximumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 0L);
    }

    @Test
    public void shouldGetMaximumOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        given(operationStore.getMaxRate(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getMaximumOperationsPerPeriod(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void shouldGetMinimumOperationsPerPeriodForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        tokenOperations.put(TokenType.OAUTH, typeOperationStore);
        given(typeOperationStore.getMinRate(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getMinimumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getMinimumOperationsPerPeriodForSpecificTokenTypeShouldReturnZeroIfTokenTypeNotSet() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = tokenOperationsStore.getMinimumOperationsPerPeriod(tokenType, operation);

        //Then
        assertEquals(result, 0L);
    }

    @Test
    public void shouldGetMinimumOperationsPerPeriod() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        given(operationStore.getMinRate(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getMinimumOperationsPerPeriod(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void shouldGetOperationsCumulativeCountForSpecificTokenType() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;
        OperationStore typeOperationStore = mock(OperationStore.class);

        tokenOperations.put(TokenType.OAUTH, typeOperationStore);
        given(typeOperationStore.getCount(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getOperationsCumulativeCount(tokenType, operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getOperationsCumulativeCountForSpecificTokenTypeShouldReturnZeroIfTokenTypeNotSet() {

        //Given
        TokenType tokenType = TokenType.OAUTH;
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = tokenOperationsStore.getOperationsCumulativeCount(tokenType, operation);

        //Then
        assertEquals(result, 0L);
    }

    @Test
    public void shouldGetOperationsCumulativeCount() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        given(operationStore.getCount(operation)).willReturn(1L);

        //When
        long result = tokenOperationsStore.getOperationsCumulativeCount(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void shouldUpdateFailureCountOnFailure() {
        // Given
        CTSOperation operation = CTSOperation.READ;
        long originalFailureCount = tokenOperationsStore.getOperationFailuresCumulativeCount(operation);

        // When
        tokenOperationsStore.addTokenOperation(operation, false);

        // Then
        assertEquals(originalFailureCount + 1, tokenOperationsStore.getOperationFailuresCumulativeCount(operation));
    }

    @Test
    public void shouldNotUpdateFailureCountOnSuccess() {
        // Given
        CTSOperation operation = CTSOperation.READ;
        long originalFailureCount = tokenOperationsStore.getOperationFailuresCumulativeCount(operation);

        // When
        tokenOperationsStore.addTokenOperation(operation, true);

        // Then
        assertEquals(originalFailureCount, tokenOperationsStore.getOperationFailuresCumulativeCount(operation));
    }

    @Test
    public void shouldUpdateFailureCountForAllTokenTypes() {
        // Given
        CTSOperation operation = CTSOperation.READ;
        long originalFailureCount = tokenOperationsStore.getOperationFailuresCumulativeCount(operation);
        tokenOperations.put(TokenType.REST, new OperationStore());
        tokenOperations.put(TokenType.OAUTH, new OperationStore());

        // When
        tokenOperationsStore.addTokenOperation(TokenType.REST, operation, false);
        tokenOperationsStore.addTokenOperation(TokenType.OAUTH, operation, false);

        // Then
        assertEquals(originalFailureCount + 2, tokenOperationsStore.getOperationFailuresCumulativeCount(operation));
    }

    @Test
    public void shouldHaveSeparateFailureCountsPerOperation() {
        // Given

        // When
        tokenOperationsStore.addTokenOperation(CTSOperation.CREATE, false);
        tokenOperationsStore.addTokenOperation(CTSOperation.DELETE, false);
        tokenOperationsStore.addTokenOperation(CTSOperation.DELETE, false);

        // Then
        assertEquals(1, tokenOperationsStore.getOperationFailuresCumulativeCount(CTSOperation.CREATE));
        assertEquals(2, tokenOperationsStore.getOperationFailuresCumulativeCount(CTSOperation.DELETE));
    }
}
