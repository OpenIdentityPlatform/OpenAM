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
package org.forgerock.openam.cts.monitoring.impl.connections;

import org.forgerock.openam.cts.monitoring.TestCurrentMillis;
import org.forgerock.openam.shared.monitoring.RateTimer;
import org.forgerock.openam.shared.monitoring.RateWindow;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class ConnectionsMonitorTest {

    private ConnectionMonitor connectionMonitor;

    private RateWindow rateWindow;

    @BeforeMethod
    public void setUp() {

        RateTimer timer = mock(RateTimer.class);
        rateWindow = mock(RateWindow.class);

        connectionMonitor = new ConnectionMonitor(timer, rateWindow);
    }

    @Test
    public void shouldGetCountReturningZeroWhenNoIncrement() {

        //Given

        //When
        long count = connectionMonitor.getCumulativeCount();

        //Then
        assertEquals(count, 0);
    }

    @Test
    public void shouldGetAverageRate() {

        final double SOLUTION = 5D;

        //Given
        given(rateWindow.getAverageRate()).willReturn(SOLUTION);

        //When
        double rate = connectionMonitor.getAverageRate();

        //Then
        assertEquals(rate, SOLUTION);
    }

    @Test
    public void shouldGetMinRate() {

        final long SOLUTION = 3L;

        //Given
        given(rateWindow.getMinRate()).willReturn(SOLUTION);

        //When
        long rate = connectionMonitor.getMinimumRate();

        //Then
        assertEquals(rate, SOLUTION);
    }

    @Test
    public void shouldGetMaxRate() {

        final long SOLUTION = 4L;

        //Given
        given(rateWindow.getMaxRate()).willReturn(SOLUTION);

        //When
        long rate = connectionMonitor.getMaximumRate();

        //Then
        assertEquals(rate, SOLUTION);
    }

    @Test
    public void shouldGetCountReturningOneAfterIncrement() {

        //Given
        connectionMonitor.add();

        //When
        long count = connectionMonitor.getCumulativeCount();

        //Then
        assertEquals(count, 1);
    }

    @Test
    public void shouldGetCountReturningSixAfterMultipleIncrement() {

        //Given
        final int NUM = 6;

        for(int i = 0; i < NUM; i++) {
            connectionMonitor.add();
        }

        //When
        long count = connectionMonitor.getCumulativeCount();

        //Then
        assertEquals(count, NUM);
    }

    @Test
    public void recalculatingRateShouldNotBlockGetRateIfNotModifying() {

        //Given
        RateTimer timer = new TestCurrentMillis();
        RateWindow rateWindow = new RateWindow(timer, 10, 1000L);

        final ConnectionMonitor monitor = new ConnectionMonitor(timer, rateWindow);

        //When
        new Thread(new Runnable() {
            public void run() {
                monitor.add();
            }
        }).start();

        double rate = monitor.getAverageRate();

        //Then
        assertEquals(rate, 0D);
    }

}
