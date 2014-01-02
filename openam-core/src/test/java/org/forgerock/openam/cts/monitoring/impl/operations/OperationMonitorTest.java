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

package org.forgerock.openam.cts.monitoring.impl.operations;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class OperationMonitorTest {

    private OperationMonitor operationMonitor;

    private OperationRateWindow rateWindow;

    @BeforeMethod
    public void setUp() {

        OperationMonitor.Timer timer = mock(OperationMonitor.Timer.class);
        rateWindow = mock(OperationRateWindow.class);

        operationMonitor = new OperationMonitor(timer, rateWindow);
    }

    @Test
    public void shouldGetCountReturningZeroWhenNoIncrement() {

        //Given

        //When
        long count = operationMonitor.getCount();

        //Then
        assertEquals(count, 0);
    }

    @Test
    public void shouldGetAverageRate() {

        //Given
        given(rateWindow.getAverageRate()).willReturn(1D);

        //When
        double rate = operationMonitor.getAverageRate();

        //Then
        assertEquals(rate, 1D);
    }

    @Test
    public void shouldGetMinRate() {

        //Given
        given(rateWindow.getMinRate()).willReturn(1L);

        //When
        long rate = operationMonitor.getMinRate();

        //Then
        assertEquals(rate, 1L);
    }

    @Test
    public void shouldGetMaxRate() {

        //Given
        given(rateWindow.getMaxRate()).willReturn(1L);

        //When
        long rate = operationMonitor.getMaxRate();

        //Then
        assertEquals(rate, 1L);
    }

    @Test
    public void shouldGetCountReturningOneAfterIncrement() {

        //Given
        operationMonitor.increment();

        //When
        long count = operationMonitor.getCount();

        //Then
        assertEquals(count, 1);
    }

    @Test
    public void shouldGetCountReturningOneAfterMultipleIncrement() {

        //Given
        operationMonitor.increment();
        operationMonitor.increment();
        operationMonitor.increment();
        operationMonitor.increment();
        operationMonitor.increment();
        operationMonitor.increment();

        //When
        long count = operationMonitor.getCount();

        //Then
        assertEquals(count, 6);
    }

    @Test
    public void recalculatingRateShouldNotBlockGetRateIfNotModifying() {

        //Given
        final OperationMonitor operationMonitor = new OperationMonitor(new TestCurrentMillis(), rateWindow);

        //When
        new Runnable() {
            public void run() {
                operationMonitor.increment();
            }
        };
        double rate = operationMonitor.getAverageRate();

        //Then
        assertEquals(rate, 0D);
    }

    private static class TestCurrentMillis extends OperationMonitor.Timer {
        long now() {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return System.currentTimeMillis();
        }
    }
}
