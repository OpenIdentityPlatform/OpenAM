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

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

import org.forgerock.openam.utils.TimeTravelUtil;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.concurrent.Future;

public class PollingWaitAssistantTest {

    private static final int EXPECTED_LONG_POLL_WAIT_IN_MILLISECONDS = 20000;
    private static final int EXPECTED_MEDIUM_POLL_WAIT_IN_MILLISECONDS = 10000;
    private static final int EXPECTED_SHORT_POLL_WAIT_IN_MILLISECONDS = 4000;

    private static final long TIMEOUT = 30000;
    private Future<Object> mockFuture = Mockito.mock(Future.class);

    private long longElapsedThreshold = TIMEOUT / 2;
    private long mediumElapsedThreshold = TIMEOUT / 4;


    @Test
    public void checkThatNotStartedStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

        // when

        // then
        assertThat(assistant.getPollingWaitState()).isEqualTo(PollingWaitAssistant.PollingWaitState.NOT_STARTED);
    }

    @Test
    public void checkThatTooEarlyStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

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
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

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
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        TimeTravelUtil.fastForward(4000);

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.WAITING);
    }

    @Test
    public void checkThatCompleteStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

        // when
        when(mockFuture.isDone()).thenReturn(true);
        assistant.start(mockFuture);

        TimeTravelUtil.fastForward(4000);


        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.COMPLETE);
    }

    @Test
    public void checkThatTimeoutStateIsReturnedWhenAppropriate() {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        TimeTravelUtil.fastForward(TIMEOUT + 10);

        // then
        assertThat(assistant.getPollingWaitState())
                .isEqualTo(PollingWaitAssistant.PollingWaitState.TIMEOUT);
    }

    @DataProvider
    public  Object[][] expectedWaitPeriods() {
        return new Object[][] {
                {new Long(100), new Long(EXPECTED_SHORT_POLL_WAIT_IN_MILLISECONDS)},
                {new Long(mediumElapsedThreshold - 100), new Long(EXPECTED_SHORT_POLL_WAIT_IN_MILLISECONDS)},
                {new Long(mediumElapsedThreshold + 100), new Long(EXPECTED_MEDIUM_POLL_WAIT_IN_MILLISECONDS)},
                {new Long(longElapsedThreshold - 100), new Long(EXPECTED_MEDIUM_POLL_WAIT_IN_MILLISECONDS)},
                {new Long(longElapsedThreshold + 100), new Long(EXPECTED_LONG_POLL_WAIT_IN_MILLISECONDS)},
                {new Long(longElapsedThreshold + 4000), new Long(EXPECTED_LONG_POLL_WAIT_IN_MILLISECONDS)},
        };
    }

    @Test (dataProvider = "expectedWaitPeriods")
    public void checkThatCorrectWaitPeriodIsReturned(Long waitLength, Long expectedWaitPeriod) {
        // given
        PollingWaitAssistant assistant = new PollingWaitAssistant(TIMEOUT);

        // when
        when(mockFuture.isDone()).thenReturn(false);
        assistant.start(mockFuture);

        TimeTravelUtil.fastForward(waitLength);

        // then
        assertThat(assistant.getWaitPeriod()).isEqualTo(expectedWaitPeriod);
    }
}
