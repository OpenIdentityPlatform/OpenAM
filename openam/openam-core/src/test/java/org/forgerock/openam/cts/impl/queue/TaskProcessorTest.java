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
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.task.Task;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;

import static org.mockito.BDDMockito.*;

public class TaskProcessorTest {

    private TaskProcessor processor;
    private ConnectionFactory mockFactory;
    private LDAPAdapter mockAdapter;

    @BeforeMethod
    public void setup() {
        Thread.interrupted();
        mockFactory = mock(ConnectionFactory.class);
        mockAdapter = mock(LDAPAdapter.class);
        processor = new TaskProcessor(mockFactory, mockAdapter, mock(Debug.class));
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
    public void shouldInitialiseConnection() throws InterruptedException, ErrorResultException {
        // Given
        processor.setQueue(generateTestQueue(mock(Task.class)));

        // When
        processor.run();

        // Then
        verify(mockFactory).getConnection();
    }

    @Test
    public void shouldCloseConnection() throws InterruptedException, ErrorResultException {
        // Given
        processor.setQueue(generateTestQueue(mock(Task.class)));
        Connection mockConnection = mock(Connection.class);
        given(mockFactory.getConnection()).willReturn(mockConnection);

        // When
        processor.run();

        // Then
        verify(mockConnection).close();
    }

    @Test
    public void shouldExecuteTaskFromQueue() throws InterruptedException, CoreTokenException {
        // Given
        Task mockTask = mock(Task.class);
        processor.setQueue(generateTestQueue(mockTask));

        // When
        processor.run();

        // Then
        verify(mockTask).execute(any(Connection.class), any(LDAPAdapter.class));
    }

    @Test
    public void shouldCloseConnectionForEachTask() throws InterruptedException, ErrorResultException {
        // Given
        processor.setQueue(generateTestQueue(mock(Task.class), mock(Task.class)));
        Connection mockConnection = mock(Connection.class);
        given(mockFactory.getConnection()).willReturn(mockConnection);

        // When
        processor.run();

        // Then
        verify(mockFactory, times(2)).getConnection();
        verify(mockConnection, times(2)).close();

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

    private BlockingQueue<Task> generateTestQueue(final Task first, final Task second) throws InterruptedException {
        BlockingQueue<Task> queue = mock(BlockingQueue.class);
        given(queue.take())
                .willReturn(first)
                .willAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Thread.currentThread().interrupt();
                        return second;
                    }
                });
        return queue;
    }
}