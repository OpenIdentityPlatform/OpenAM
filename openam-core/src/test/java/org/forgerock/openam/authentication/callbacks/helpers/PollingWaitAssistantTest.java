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
package org.forgerock.openam.authentication.callbacks.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.openam.utils.TimeTravelUtil.FastForwardTimeService;
import org.forgerock.util.time.TimeService;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PollingWaitAssistantTest {

    private static final long LONG_POLL = 20000;
    private static final long MEDIUM_POLL = 10000;
    private static final long SHORT_POLL = 4000;

    private static final long TIMEOUT = 30000;
    private Future mockFuture = Mockito.mock(Future.class);

    @BeforeMethod
    public void setup() {
        TimeTravelUtil.setBackingTimeService(FastForwardTimeService.INSTANCE);
    }

    @AfterMethod
    public void tearDown() {
        TimeTravelUtil.setBackingTimeService(TimeService.SYSTEM);
    }

    @Test
    public void checkThatNotStartedStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when

        // then
        assertThat(assistant.getPollingWaitState()).isEqualTo(PollingWaitAssistant.PollingWaitState.NOT_STARTED);
    }

    @Test
    public void checkThatTooEarlyStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.TOO_EARLY);
    }

    @Test
    public void checkThatSpammedStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        assistant.getPollingWaitState();
        assistant.getPollingWaitState();
        assistant.getPollingWaitState();

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.SPAMMED);
    }

    @Test
    public void checkThatWaitingStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        FastForwardTimeService.INSTANCE.fastForward(4, TimeUnit.SECONDS);

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.WAITING);
    }

    @Test
    public void checkThatCompleteStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(true);
        assistant.start(mockFuture);

        FastForwardTimeService.INSTANCE.fastForward(4, TimeUnit.SECONDS);


        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.COMPLETE);
    }

    @Test
    public void checkThatTimeoutStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        FastForwardTimeService.INSTANCE.fastForward(TIMEOUT + 10, TimeUnit.MILLISECONDS);

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.TIMEOUT);
    }

    @DataProvider
    public  Object[][] expectedWaitPeriods() {
        long mediumElapsedThreshold = TIMEOUT / 4;
        long longElapsedThreshold = TIMEOUT / 2;

        return new Object[][] {
                {100L, SHORT_POLL},
                {mediumElapsedThreshold - 100, SHORT_POLL},
                {mediumElapsedThreshold + 100, MEDIUM_POLL},
                {longElapsedThreshold - 100, MEDIUM_POLL},
                {longElapsedThreshold + 100, LONG_POLL},
                {longElapsedThreshold + 4000, LONG_POLL},
        };
    }

    @Test (dataProvider = "expectedWaitPeriods")
    public void checkThatCorrectWaitPeriodIsReturned(Long waitLength, Long expectedWaitPeriod) {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT, SHORT_POLL, MEDIUM_POLL, LONG_POLL);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        FastForwardTimeService.INSTANCE.fastForward(waitLength, TimeUnit.MILLISECONDS);

        // then
        assertThat(assistant.getWaitPeriod()).isEqualTo(expectedWaitPeriod);
    }
}
