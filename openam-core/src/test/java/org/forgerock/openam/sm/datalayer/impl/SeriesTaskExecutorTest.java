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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.impl.tasks.CreateTask;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class SeriesTaskExecutorTest {

    private ExecutorService executorService;
    private SeriesTaskExecutor executor;
    private SeriesTaskExecutorThreadFactory processorFactory;
    private ThreadMonitor monitor;
    private CTSQueueConfiguration configuration;

    @BeforeMethod
    public void setup() throws Exception {
        executorService = mock(ExecutorService.class);
        processorFactory = mock(SeriesTaskExecutorThreadFactory.class);
        monitor = mock(ThreadMonitor.class);
        configuration = mock(CTSQueueConfiguration.class);
        given(configuration.getQueueSize()).willReturn(10);

        executor = new SeriesTaskExecutor(executorService, processorFactory, monitor, configuration, mock(Debug.class));
    }

    @Test
    public void testExecute() throws Exception {

    }

    @Test
    public void shouldStartTaskProcessorsWithThreadMonitor() throws Exception {
        // Given
        int processors = 3;
        given(processorFactory.create(any(BlockingQueue.class))).willReturn(mock(SeriesTaskExecutorThread.class));
        given(configuration.getProcessors()).willReturn(processors);

        // When
        executor.start();

        // Then
        verify(monitor, times(processors)).watchThread(any(ExecutorService.class), any(Runnable.class));
    }

    @Test
    public void shouldPlaceTaskOnQueueForEachOperation() throws Exception {
        // Given
        int processors = 1;

        // Capture the blocking queue that is provided to the mock processor
        ArgumentCaptor<BlockingQueue> captor = ArgumentCaptor.forClass(BlockingQueue.class);
        given(processorFactory.create(captor.capture())).willReturn(mock(SeriesTaskExecutorThread.class));
        given(configuration.getProcessors()).willReturn(processors);

        executor.start();

        // When
        executor.execute("123", mock(Task.class));

        // Then
        assertThat(captor.getValue().size()).isEqualTo(1);
    }


    @Test
    public void shouldCatchTimeoutWhenOfferingTaskToQueue() throws Exception {
        // Given
        given(configuration.getQueueTimeout()).willReturn(0);
        given(configuration.getQueueSize()).willReturn(1);
        given(configuration.getProcessors()).willReturn(2);

        CreateTask task = mock(CreateTask.class);

        executor.start();
        executor.execute("123", task); // First create fills the queue

        // When
        DataLayerException result = null;
        try {
            executor.execute("123", task); // Second create causes timeout.
            fail("Expected exception");
        } catch (DataLayerException e) {
            result = e;
        }

        // Then
        assertThat(result).isNotNull();
    }
}