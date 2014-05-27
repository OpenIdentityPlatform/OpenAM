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
package org.forgerock.openam.entitlement.monitoring;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EvaluationTimingStoreTest {

    private EvaluationTimingStore testTimingStore;
    private EntitlementConfigurationWrapper mockWrapper = mock(EntitlementConfigurationWrapper.class);


    @BeforeMethod
    public void setUp() {
        given(mockWrapper.getPolicyWindowSize()).willReturn(100);

        testTimingStore = new EvaluationTimingStore(mockWrapper);
    }

    @Test
    public void testEmptyStoreReturnsZeroAverage() {
        //given

        //when
        long avg = testTimingStore.getDurationAverage();

        //then
        assertEquals(avg, 0);
    }

    @Test
    public void testAverageWindow() {
        //given
        testTimingStore.addTiming(1000000, null, null, null, null);

        for(int i = 0; i < testTimingStore.getMaxEntries(); i++) {
            testTimingStore.addTiming(0, null, null, null, null);
        }

        //when
        long avg = testTimingStore.getDurationAverage();
        long slowest = testTimingStore.getSlowestEvaluationDuration();

        //then
        assertEquals(avg, 0);
        assertEquals(slowest, 0);
    }

    @Test
    public void testAverageCalculationFiveEntry() {
        //given
        testTimingStore.addTiming(100, null, null, null, null);
        testTimingStore.addTiming(100, null, null, null, null);
        testTimingStore.addTiming(100, null, null, null, null);
        testTimingStore.addTiming(100, null, null, null, null);
        testTimingStore.addTiming(50, null, null, null, null);

        //when
        long avg = testTimingStore.getDurationAverage();
        long slowest = testTimingStore.getSlowestEvaluationDuration();

        //then
        assertEquals(avg, 90);
        assertEquals(slowest, 100);
    }

    @Test
    public void testAverageCalculationTwoEntryReverseOrder() {
        //given
        testTimingStore.addTiming(0, null, null, null, null);
        testTimingStore.addTiming(100, null, null, null, null);

        //when
        long avg = testTimingStore.getDurationAverage();
        long slowest = testTimingStore.getSlowestEvaluationDuration();

        //then
        assertEquals(avg, 50);
        assertEquals(slowest, 100);
    }

    @Test
    public void testAverageCalculationTwoEntry() {
        //given
        testTimingStore.addTiming(100, null, null, null, null);
        testTimingStore.addTiming(0, null, null, null, null);

        //when
        long avg = testTimingStore.getDurationAverage();
        long slowest = testTimingStore.getSlowestEvaluationDuration();

        //then
        assertEquals(avg, 50);
        assertEquals(slowest, 100);
    }

    @Test
    public void testAverageCalculationOneEntry() {
        //given
        testTimingStore.addTiming(100, null, null, null, null);

        //when
        long avg = testTimingStore.getDurationAverage();
        long slowest = testTimingStore.getSlowestEvaluationDuration();

        //then
        assertEquals(avg, 100);
        assertEquals(slowest, 100);
    }

}
