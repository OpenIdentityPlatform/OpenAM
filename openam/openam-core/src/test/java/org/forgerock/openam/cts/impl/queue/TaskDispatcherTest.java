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
package org.forgerock.openam.cts.impl.queue;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.config.QueueConfiguration;
import org.forgerock.openam.cts.impl.task.CreateTask;
import org.forgerock.openam.cts.impl.task.Task;
import org.forgerock.openam.cts.impl.task.TaskFactory;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TaskDispatcherTest {

    private TaskDispatcher queue;
    private QueueConfiguration mockConfiguration;
    private ThreadMonitor mockMonitor;
    private TaskProcessorFactory mockProcessorFactory;
    private TaskFactory mockTaskFactory;
    private ExecutorService mockService;
    private Token mockToken;
    private ResultHandler mockHandler;

    @BeforeMethod
    public void setup() {
        mockService = mock(ExecutorService.class);
        mockTaskFactory = mock(TaskFactory.class);
        mockProcessorFactory = mock(TaskProcessorFactory.class);
        mockMonitor = mock(ThreadMonitor.class);
        mockHandler = mock(ResultHandler.class);

        mockConfiguration = mock(QueueConfiguration.class);
        given(mockConfiguration.getQueueSize()).willReturn(10);

        mockToken = mock(Token.class);
        given(mockToken.getTokenId()).willReturn("badger");

        queue = new TaskDispatcher(
                mockService,
                mockTaskFactory,
                mockProcessorFactory,
                mockMonitor,
                mockConfiguration,
                mock(Debug.class));
    }

    @Test
    public void shouldUseConfigurationToInitialise() throws CoreTokenException {
        queue.startDispatcher();
        verify(mockConfiguration).getProcessors();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnCreate() throws CoreTokenException {
        queue.create(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnUpdate() throws CoreTokenException {
        queue.update(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullStringOnDelete() throws CoreTokenException {
        queue.delete(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnRead() throws CoreTokenException {
        queue.read(null, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnQuery() throws CoreTokenException {
        queue.query(null, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnPartialQuery() throws CoreTokenException {
        queue.partialQuery(null, null);
    }

    @Test
    public void shouldStartTaskProcessorsWithThreadMonitor() throws CoreTokenException {
        // Given
        int processors = 3;
        given(mockProcessorFactory.create(any(BlockingQueue.class))).willReturn(mock(TaskProcessor.class));
        given(mockConfiguration.getProcessors()).willReturn(processors);

        // When
        queue.startDispatcher();

        // Then
        verify(mockMonitor, times(processors)).watchThread(any(ExecutorService.class), any(Runnable.class));
    }

    @Test
    public void shouldPlaceTaskOnQueueForEachOperation() throws CoreTokenException {
        // Given
        int processors = 2;

        // Capture the blocking queue that is provided to the mock processor
        ArgumentCaptor<BlockingQueue> captor = ArgumentCaptor.forClass(BlockingQueue.class);
        given(mockProcessorFactory.create(captor.capture())).willReturn(mock(TaskProcessor.class));
        given(mockConfiguration.getProcessors()).willReturn(processors);

        given(mockTaskFactory.create(any(Token.class), any(ResultHandler.class))).willReturn(mock(CreateTask.class));

        queue.startDispatcher();

        // When
        queue.create(mockToken, mockHandler);

        // Then
        assertThat(captor.getValue().size()).isEqualTo(1);
    }

    @Test
    public void shouldCatchTimeoutWhenOfferingTaskToQueue() throws CoreTokenException {
        // Given
        given(mockConfiguration.getQueueTimeout()).willReturn(0);
        given(mockConfiguration.getQueueSize()).willReturn(1);
        given(mockConfiguration.getProcessors()).willReturn(2);

        CreateTask task = mock(CreateTask.class);
        given(mockTaskFactory.create(any(Token.class), any(ResultHandler.class))).willReturn(task);

        queue.startDispatcher();
        queue.create(mockToken, mockHandler); // First create fills the queue

        // When
        CoreTokenException result = null;
        try {
            queue.create(mockToken, mockHandler); // Second create causes timeout.
        } catch (CoreTokenException e) {
            result = e;
        }

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    public void shouldPerformPartialQuery() throws CoreTokenException {
        // Given
        TokenFilter filter = new TokenFilterBuilder().build();

        given(mockConfiguration.getProcessors()).willReturn(2);
        queue.startDispatcher();
        given(mockTaskFactory.partialQuery(
                any(TokenFilter.class),
                any(ResultHandler.class))).willReturn(mock(Task.class));

        // When
        queue.partialQuery(filter, mock(ResultHandler.class));

        // Then
        verify(mockTaskFactory).partialQuery(eq(filter), any(ResultHandler.class));
    }
}