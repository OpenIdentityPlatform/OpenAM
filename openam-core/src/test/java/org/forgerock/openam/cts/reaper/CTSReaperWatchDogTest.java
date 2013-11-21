/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.cts.reaper;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CTSReaperWatchDogTest {

    private CTSReaperWatchDog watchDog;
    private CTSReaper mockReaper;
    private CoreTokenConfig mockConfig;
    private ReaperShutdownMonitor mockMonitor;
    private ScheduledExecutorService scheduledService;
    private ExecutorService executorService;

    @BeforeMethod
    public void setup() {
        mockReaper = mock(CTSReaper.class);
        mockConfig = mock(CoreTokenConfig.class);
        mockMonitor = mock(ReaperShutdownMonitor.class);
        executorService = mock(ExecutorService.class);
        scheduledService = mock(ScheduledExecutorService.class);

        given(mockConfig.getRunPeriod()).willReturn(100);
        // Note! Dummy implementation to trigger Watcher runnable
        given(executorService.submit(any(Runnable.class))).will(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                return null;
            }
        });

        watchDog = new CTSReaperWatchDog(
                mockReaper,
                mockConfig,
                mockMonitor,
                scheduledService,
                executorService,
                mock(Debug.class));
    }

    @Test
    public void shouldScheduleReaper() {
        // Given
        given(scheduledService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mock(ScheduledFuture.class));
        // Second call ends the test
        given(mockMonitor.isShutdown()).willReturn(false).willReturn(false).willReturn(true);

        // When
        watchDog.startReaper();

        // Then
        verify(scheduledService).scheduleAtFixedRate(eq(mockReaper), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldPassScheduledFutureToShutdownMonitor() {
        // Given
        ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        given(scheduledService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mockFuture);

        // Second call ends the test
        given(mockMonitor.isShutdown()).willReturn(false).willReturn(false).willReturn(true);

        // When
        watchDog.startReaper();

        // Then
        verify(mockMonitor).setFuture(eq(mockFuture));
    }

    @Test
    public void shouldRescheduleReaperOnFailure() throws ExecutionException, InterruptedException {
        // Given
        ScheduledFuture explodingFuture = mock(ScheduledFuture.class);
        given(explodingFuture.get()).willThrow(mock(ExecutionException.class));

        given(scheduledService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(explodingFuture);
        // Third call ends the test
        given(mockMonitor.isShutdown()).willReturn(false).willReturn(false).willReturn(false).willReturn(false).willReturn(true);

        // When
        watchDog.startReaper();

        // Then
        verify(scheduledService, times(2)).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldRescheduleWithADelay() throws ExecutionException, InterruptedException {
        // Given
        given(scheduledService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mock(ScheduledFuture.class));
        // Second call ends the test
        given(mockMonitor.isShutdown()).willReturn(false).willReturn(false).willReturn(true);

        // When
        watchDog.startReaper();

        // Then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(scheduledService).scheduleAtFixedRate(eq(mockReaper), captor.capture(), anyLong(), any(TimeUnit.class));
        Long delay = captor.getValue();
        assertThat(delay).isGreaterThan(0);
    }
}
