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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class CTSWorkerInitTest {

    private ThreadMonitor mockMonitor;
    private CTSWorkerInit ctsWorkerInit;

    @BeforeMethod
    public void setup() {
        mockMonitor = mock(ThreadMonitor.class);
        CTSWorkerTaskProvider mockTaskProvider = mock(CTSWorkerTaskProvider.class);
        given(mockTaskProvider.getTasks()).willReturn(Collections.singletonList(mock(CTSWorkerTask.class)));

        ctsWorkerInit = new CTSWorkerInit(
                mockTaskProvider,
                mockMonitor,
                mock(CoreTokenConfig.class),
                mock(ScheduledExecutorService.class),
                mock(Debug.class));
    }

    @Test
    public void shouldUseMonitorToStartReaper() {
        ctsWorkerInit.startTasks();
        verify(mockMonitor).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventSubsequentStarts() {
        ctsWorkerInit.startTasks();
        ctsWorkerInit.startTasks();
    }

    @Test
    public void shouldStartMultiple() {
        ctsWorkerInit = new CTSWorkerInit(
                new CTSWorkerTestTaskProvider(),
                mockMonitor,
                mock(CoreTokenConfig.class),
                mock(ScheduledExecutorService.class),
                mock(Debug.class));

        ctsWorkerInit.startTasks();

        verify(mockMonitor, times(2)).watchScheduledThread(
                any(ScheduledExecutorService.class),
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class));
    }

    private class CTSWorkerTestTaskProvider extends CTSWorkerTaskProvider {
        CTSWorkerTestTaskProvider() {
            super(Arrays.asList(mock(CTSWorkerTask.class), mock(CTSWorkerTask.class)));
        }
    }
}