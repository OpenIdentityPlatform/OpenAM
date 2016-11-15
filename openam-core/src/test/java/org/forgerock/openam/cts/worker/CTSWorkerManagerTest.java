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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.worker;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class CTSWorkerManagerTest {

    public static final int RUN_PERIOD = 600000;
    private ThreadMonitor mockMonitor;
    private CTSWorkerManager ctsWorkerManager;
    private CoreTokenConfig mockCoreTokenConfig;
    private ExecutorServiceFactory mockExecutorServiceFactory;

    @BeforeMethod
    public void setup() {
        mockMonitor = mock(ThreadMonitor.class);
        mockCoreTokenConfig = mock(CoreTokenConfig.class);
        mockExecutorServiceFactory = mock(ExecutorServiceFactory.class);
        CTSWorkerTaskProvider mockTaskProvider = mock(CTSWorkerTaskProvider.class);
        given(mockTaskProvider.getTasks()).willReturn(Collections.singletonList(mock(CTSWorkerTask.class)));
        given(mockExecutorServiceFactory.createScheduledService(anyInt()))
                .willReturn(mock(ScheduledExecutorService.class));
        given(mockCoreTokenConfig.getRunPeriod()).willReturn(RUN_PERIOD);

        ctsWorkerManager = new CTSWorkerManager(
                mockTaskProvider,
                mockMonitor,
                mockCoreTokenConfig,
                mockExecutorServiceFactory,
                mock(Debug.class));
    }

    @Test
    public void shouldUseMonitorToStartReaper() {
        ctsWorkerManager.startTasks();
        verify(mockMonitor).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSubsequentStarts() {
        ctsWorkerManager.startTasks();
        ctsWorkerManager.startTasks();
    }

    @Test
    public void shouldStartMultiple() {
        ctsWorkerManager = new CTSWorkerManager(
                new CTSWorkerTestTaskProvider(),
                mockMonitor,
                mockCoreTokenConfig,
                mockExecutorServiceFactory,
                mock(Debug.class));

        ctsWorkerManager.startTasks();

        verify(mockMonitor, times(2)).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test
    public void shouldNotRestartWhenRunPeriodIsNotChanged() {

        //given
        ctsWorkerManager.startTasks();
        Mockito.reset(mockMonitor);
        given(mockCoreTokenConfig.getRunPeriod()).willReturn(RUN_PERIOD);

        //when
        ctsWorkerManager.configChanged();

        //then
        verify(mockMonitor, never()).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test
    public void shouldRestartWhenRunPeriodIsChanged() {

        //given
        ctsWorkerManager.startTasks();
        Mockito.reset(mockMonitor);
        int newRunPeriod = RUN_PERIOD + 10000;
        given(mockCoreTokenConfig.getRunPeriod()).willReturn(newRunPeriod);

        //when
        ctsWorkerManager.configChanged();

        //then
        verify(mockMonitor).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                eq(((long) newRunPeriod)),
                eq(((long) newRunPeriod)),
                any(TimeUnit.class));
    }

    private class CTSWorkerTestTaskProvider extends CTSWorkerTaskProvider {
        CTSWorkerTestTaskProvider() {
            super(Arrays.asList(mock(CTSWorkerTask.class), mock(CTSWorkerTask.class)));
        }
    }
}