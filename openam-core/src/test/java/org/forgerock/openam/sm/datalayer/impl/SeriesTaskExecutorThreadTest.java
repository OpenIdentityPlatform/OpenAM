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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl;

import static org.mockito.BDDMockito.*;

import java.util.concurrent.BlockingQueue;

import org.forgerock.openam.sm.datalayer.api.Task;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class SeriesTaskExecutorThreadTest {

    private SeriesTaskExecutorThread processor;
    private SimpleTaskExecutor mockExecutor;

    @BeforeMethod
    public void setup() {
        Thread.interrupted();
        mockExecutor = mock(SimpleTaskExecutor.class);
        processor = new SeriesTaskExecutorThread(mock(Debug.class), mockExecutor);
    }

    // NB: TaskProcessor has a threading policy around interrupted. This tear down clears the interrupted state.
    @AfterMethod
    public void tearDown() {
        Thread.interrupted();
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotStartWithoutQueueAssigned() {
        processor.run();
    }

    @Test
    public void shouldInitialiseExecutor() throws Exception {
        // Given
        processor.setQueue(generateTestQueue(mock(Task.class)));

        // When
        processor.run();

        // Then
        verify(mockExecutor).start();
    }

    @Test
    public void shouldExecuteTaskFromQueue() throws Exception {
        // Given
        Task mockTask = mock(Task.class);
        processor.setQueue(generateTestQueue(mockTask));

        // When
        processor.run();

        // Then
        verify(mockExecutor).execute(null, mockTask);
    }

    private BlockingQueue<Task> generateTestQueue(final Task first) throws InterruptedException {
        BlockingQueue<Task> queue = mock(BlockingQueue.class);
        given(queue.take()).willAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Thread.currentThread().interrupt();
                return first;
            }
        });
        return queue;
    }

}