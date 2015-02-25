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

package org.forgerock.openam.monitoring;

import org.forgerock.openam.shared.monitoring.RateTimer;
import org.forgerock.openam.shared.monitoring.RateWindow;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;

public class RateWindowTest {

    private static final long SAMPLE_RATE = 1000L;

    private RateTimer timer;

    @BeforeMethod
    public void setUp() {
        timer = mock(RateTimer.class);
    }

    private RateWindow createRateWindow(final long sampleRate, final int windowSize) {
        return new RateWindow(timer, windowSize, sampleRate);
    }

    private RateWindow createRateWindow(final int windowSize) {
        return createRateWindow(SAMPLE_RATE, windowSize);
    }

    private long getNowTimestamp(final long sampleRate) {
        return sampleRate * 10;
    }

    @Test
    public void shouldAddRateToFirstWindowSlot() {

        //Given
        RateWindow rateWindow = createRateWindow(1);
        long timestamp = getNowTimestamp(SAMPLE_RATE);

        //When
        rateWindow.incrementForTimestamp(timestamp);

        //Then
        assertEquals(rateWindow.getAverageRate(), 1D);
    }

    @Test
    public void shouldNotUpdatePastRateIfBeforeWindowStart() {

        //Given
        RateWindow rateWindow = createRateWindow(1);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 - SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp1 + SAMPLE_RATE);

        rateWindow.incrementForTimestamp(timestamp1);

        //When
        rateWindow.incrementForTimestamp(timestamp2);

        //Then
        assertEquals(rateWindow.getMaxRate(), 0L);
    }

    @Test
    public void shouldUpdatePreviousRate() {

        //Given
        RateWindow rateWindow = createRateWindow(4);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);

        //When
        rateWindow.incrementForTimestamp(timestamp1);

        //Then
        assertEquals(rateWindow.getMaxRate(), 2L);
    }

    @Test
    public void shouldUpdatePreviousPreviousRate() {

        //Given
        RateWindow rateWindow = createRateWindow(4);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        rateWindow.incrementForTimestamp(timestamp1);

        //Then
        assertEquals(rateWindow.getMaxRate(), 2L);
    }

    @Test
    public void shouldAddRatesToSameWindowSlotWithSameTimestamp() {

        //Given
        RateWindow rateWindow = createRateWindow(1);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);

        rateWindow.incrementForTimestamp(timestamp1);

        //When
        rateWindow.incrementForTimestamp(timestamp1);

        //Then
        assertEquals(rateWindow.getAverageRate(), 2D);
    }

    @Test
    public void shouldAddRatesToSameWindowSlotWithTimestampsAtEitherEndOfSampleRate() {

        //Given
        RateWindow rateWindow = createRateWindow(1);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE - 1;

        rateWindow.incrementForTimestamp(timestamp1);

        //When
        rateWindow.incrementForTimestamp(timestamp2);

        //Then
        assertEquals(rateWindow.getAverageRate(), 2D);
    }

    @Test
    public void shouldAddRatesToDifferentWindowSlots() {

        //Given
        RateWindow rateWindow = createRateWindow(1);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);

        //When
        rateWindow.incrementForTimestamp(timestamp2);

        //Then
        assertEquals(rateWindow.getAverageRate(), 1D);
    }

    @Test
    public void shouldAddRatesToDifferentWindowSlotsWithAnEmptySlotBetween() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + (SAMPLE_RATE * 2);

        rateWindow.incrementForTimestamp(timestamp1);

        //When
        rateWindow.incrementForTimestamp(timestamp2);

        //Then
        assertEquals(rateWindow.getAverageRate(), 0.5);
    }

    @Test
    public void shouldGetAverageRateWhenTimeHasPassedLatestIndex() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp1 + (SAMPLE_RATE * 2));

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);

        //When
        double rate = rateWindow.getAverageRate();

        //Then
        assertEquals(rate, 0.5D);
    }

    @Test
    public void shouldGetAverageRateWhenTimeIsInLatestIndex() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp2);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);

        //When
        double rate = rateWindow.getAverageRate();

        //Then
        assertEquals(rate, 0.5D);
    }

    @Test
    public void shouldGetAverageRateWhenTimeIsJustInLatestIndex() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp2 + SAMPLE_RATE - 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);

        //When
        double rate = rateWindow.getAverageRate();

        //Then
        assertEquals(rate, 0.5D);
    }

    @Test
    public void shouldGetMinRateWhenNoRateSet() {

        //Given
        RateWindow rateWindow = createRateWindow(2);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 0L);
    }

    @Test
    public void shouldGetMinRate() {

        //Given
        RateWindow rateWindow = createRateWindow(3);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 1L);
    }

    @Test
    public void shouldGetMinRateWhenWindowMoves() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 2L);
    }

    @Test
    public void shouldGetMinRateWhenTimeHasPassedCurrentIndex() {

        //Given
        RateWindow rateWindow = createRateWindow(3);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp2 + SAMPLE_RATE - 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 1L);
    }

    @Test
    public void shouldGetMinRateEvenWhenWindowHasMovedUnderIt() {

        //Given
        RateWindow rateWindow = createRateWindow(4);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;
        long timestamp4 = timestamp3 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp3 + 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.getMinRate();
        rateWindow.incrementForTimestamp(timestamp4);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 1L);
    }

    @Test
    public void shouldGetMinRateEvenWhenWindowHasMovedTwiceUnderIt() {

        //Given
        RateWindow rateWindow = createRateWindow(4);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;
        long timestamp4 = timestamp3 + (SAMPLE_RATE * 2);

        given(timer.now()).willReturn(timestamp3 + 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.getMinRate();
        rateWindow.incrementForTimestamp(timestamp4);

        //When
        long rate = rateWindow.getMinRate();

        //Then
        assertEquals(rate, 0L);
    }

    @Test
    public void shouldGetMaxRateWhenNoRateSet() {

        //Given
        RateWindow rateWindow = createRateWindow(2);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 0L);
    }

    @Test
    public void shouldGetMaxRate() {

        //Given
        RateWindow rateWindow = createRateWindow(3);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 6L);
    }

    @Test
    public void shouldGetMaxRateWhenWindowMoves() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp3 + 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 3L);
    }

    @Test
    public void shouldGetMaxRateWhenTimeHasPassedCurrentIndex() {

        //Given
        RateWindow rateWindow = createRateWindow(3);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp3 + SAMPLE_RATE - 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 6L);
    }

    @Test
    public void shouldGetMaxRateEvenWhenWindowHasMovedUnderIt() {

        //Given
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;
        long timestamp4 = timestamp3 + SAMPLE_RATE;

        given(timer.now()).willReturn(timestamp3 + 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.getMaxRate();
        rateWindow.incrementForTimestamp(timestamp4);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 3L);
    }

    @Test
    public void shouldGetMaxRateEvenWhenWindowHasMovedTwiceUnderIt() {

        //Given
        RateWindow rateWindow = createRateWindow(4);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;
        long timestamp4 = timestamp3 + (SAMPLE_RATE * 2);

        given(timer.now()).willReturn(timestamp3 + 1);

        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp2);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.incrementForTimestamp(timestamp3);
        rateWindow.getMaxRate();
        rateWindow.incrementForTimestamp(timestamp4);

        //When
        long rate = rateWindow.getMaxRate();

        //Then
        assertEquals(rate, 3L);
    }

    @Test
    public void windowShouldBeExtendedWhenCalculatingMaxRate() {
        RateWindow rateWindow = createRateWindow(2);
        long timestamp1 = getNowTimestamp(SAMPLE_RATE);
        long timestamp2 = timestamp1 + SAMPLE_RATE;
        long timestamp3 = timestamp2 + SAMPLE_RATE;
        long timestamp4 = timestamp3 + (SAMPLE_RATE * 2);

        given(timer.now()).willReturn(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);
        rateWindow.incrementForTimestamp(timestamp1);

        given(timer.now()).willReturn(timestamp2);
        rateWindow.incrementForTimestamp(timestamp2);
        assertThat(rateWindow.getMaxRate()).isEqualTo(3);

        given(timer.now()).willReturn(timestamp3);
        assertThat(rateWindow.getMaxRate()).isEqualTo(1);
        
        given(timer.now()).willReturn(timestamp4);
        assertThat(rateWindow.getMaxRate()).isEqualTo(0);
    }
}
