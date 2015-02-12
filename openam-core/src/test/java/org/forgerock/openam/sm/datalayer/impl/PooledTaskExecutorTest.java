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

import static org.mockito.Mockito.*;

import java.io.Closeable;

import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.utils.ConnectionCount;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class PooledTaskExecutorTest {

    @Test
    public void testExecute() throws Exception {
        // Given
        Closeable connection = mock(Closeable.class);
        ConnectionFactory<Closeable> connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.create()).thenReturn(connection);
        when(connectionFactory.isValid(connection)).thenReturn(true);

        TokenStorageAdapter adapter = mock(TokenStorageAdapter.class);

        ConnectionCount connectionCount = mock(ConnectionCount.class);
        when(connectionCount.getConnectionCount(2, ConnectionType.RESOURCE_SETS)).thenReturn(2);

        ConnectionConfigFactory configFactory = mock(ConnectionConfigFactory.class);
        ConnectionConfig config = mock(ConnectionConfig.class);
        when(configFactory.getConfig()).thenReturn(config);
        when(config.getMaxConnections()).thenReturn(2);

        Debug debug = mock(Debug.class);
        when(debug.messageEnabled()).thenReturn(true);
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                System.out.println(Thread.currentThread().getName() + ":: " + invocation.getArguments()[0]);
                return null;
            }
        }).when(debug).message(anyString());

        // When
        final TaskExecutor executor = new PooledTaskExecutor(connectionFactory, debug, adapter,
                ConnectionType.RESOURCE_SETS, connectionCount, configFactory);
        TaskThread task1 = new TaskThread(1, executor, new LongTask());
        TaskThread task2 = new TaskThread(2, executor, new LongTask());
        TaskThread task3 = new TaskThread(3, executor, mock(Task.class));

        task1.start();
        task2.start();

        while (!(task1.running && task2.running)) {
            Thread.sleep(50);
        }

        task3.start();
        Thread.sleep(200);

        // Then
        verifyZeroInteractions(task3.task);
        ((LongTask) task2.task).run = true;
        ((LongTask) task1.task).run = true;

        Thread.sleep(200);

        verify(task3.task).execute(connection, adapter);

        task1.join();
        task2.join();
        task3.join();
    }

    private static class TaskThread extends Thread {
        private TaskExecutor executor;
        private Task task;
        private volatile boolean running = false;

        TaskThread(int taskId, TaskExecutor executor, Task task) {
            this.executor = executor;
            this.task = task;
            setName("Task " + taskId);
        }

        public void run() {
            running = true;
            try {
                executor.execute(null, task);
            } catch (DataLayerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class LongTask implements Task {

        private volatile boolean run = false;

        @Override
        public <T> void execute(T connection, TokenStorageAdapter<T> adapter) throws DataLayerException {
            while (!run) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}