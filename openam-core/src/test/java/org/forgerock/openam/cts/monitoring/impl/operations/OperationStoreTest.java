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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OperationStoreTest {

    private OperationStore operationStore;

    private OperationStore.OperationRateFactory operationRateFactory;
    private Map<CTSOperation, OperationMonitor> operationRate;

    @BeforeMethod
    public void setUp() {

        operationRateFactory = mock(OperationStore.OperationRateFactory.class);
        operationRate = new HashMap<CTSOperation, OperationMonitor>();

        operationStore = new OperationStore(operationRateFactory, operationRate);
    }

    @Test
    public void shouldAddOperation() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        given(operationRateFactory.createOperationRate()).willReturn(opRate);

        //When
        operationStore.add(operation);

        //Then
        assertTrue(operationRate.containsKey(CTSOperation.CREATE));
        verify(opRate).increment();
    }

    @Test
    public void shouldAddOperationToExistingOperationStore() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        operationRate.put(CTSOperation.CREATE, opRate);

        //When
        operationStore.add(operation);

        //Then
        verifyZeroInteractions(operationRateFactory);
        verify(opRate).increment();
    }

    @Test
    public void shouldAddTokenOperationForSpecificTokenTypeUsingDefaultOperationStoreFactory() {

        //Given
        OperationStore.OperationRateFactory operationRateFactory = new OperationStore.OperationRateFactory();
        OperationStore localOperationStore = new OperationStore(operationRateFactory, operationRate);

        CTSOperation operation = CTSOperation.CREATE;

        //When
        localOperationStore.add(operation);

        //Then
        assertTrue(operationRate.containsKey(CTSOperation.CREATE));
    }

    @Test
    public void shouldOnlyAllowSingleThreadToAddOperationRateToOperationRateMap() throws InterruptedException {

        //Given
        final OperationMonitor opRate = mock(OperationMonitor.class);
        OperationStore.OperationRateFactory operationRateFactory = new OperationStore.OperationRateFactory() {
            @Override
            OperationMonitor createOperationRate() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return opRate;
            }
        };
        final OperationStore localOperationStore = new OperationStore(operationRateFactory, operationRate);

        final CTSOperation operation = CTSOperation.CREATE;

        Runnable runnable = new Runnable() {
            public void run() {
                localOperationStore.add(operation);
            }
        };
        //When
        Thread t1 = new Thread(runnable);
        t1.setName("t1");
        Thread t2 = new Thread(runnable);
        t2.setName("t2");
        t1.start();
        t2.start();

        //Then
        t1.join();
        t2.join();
        verify(opRate, times(2)).increment();
        assertEquals(operationRate.size(), 1);
    }

    @Test
    public void shouldGetAverageRate() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        operationRate.put(operation, opRate);
        given(opRate.getAverageRate()).willReturn(1D);

        //When
        double result = operationStore.getAverageRate(operation);

        //Then
        assertEquals(result, 1D);
    }

    @Test
    public void getAverageRateShouldReturnZeroIfOperationNotSet() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        //When
        double result = operationStore.getAverageRate(operation);

        //Then
        assertEquals(result, 0D);
    }

    @Test
    public void shouldGetMaximumRate() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        operationRate.put(operation, opRate);
        given(opRate.getMaxRate()).willReturn(1L);

        //When
        long result = operationStore.getMaxRate(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getMaximumRateShouldReturnZeroIfOperationNotSet() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = operationStore.getMaxRate(operation);

        //Then
        assertEquals(result, 0L);
    }

    @Test
    public void shouldGetMinimumRate() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        operationRate.put(operation, opRate);
        given(opRate.getMinRate()).willReturn(1L);

        //When
        long result = operationStore.getMinRate(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getMinimumRateShouldReturnZeroIfOperationNotSet() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = operationStore.getMinRate(operation);

        //Then
        assertEquals(result, 0L);
    }

    @Test
    public void shouldGetCount() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;
        OperationMonitor opRate = mock(OperationMonitor.class);

        operationRate.put(operation, opRate);
        given(opRate.getCount()).willReturn(1L);

        //When
        long result = operationStore.getCount(operation);

        //Then
        assertEquals(result, 1L);
    }

    @Test
    public void getCountShouldReturnZeroIfOperationNotSet() {

        //Given
        CTSOperation operation = CTSOperation.CREATE;

        //When
        long result = operationStore.getCount(operation);

        //Then
        assertEquals(result, 0L);
    }
}
