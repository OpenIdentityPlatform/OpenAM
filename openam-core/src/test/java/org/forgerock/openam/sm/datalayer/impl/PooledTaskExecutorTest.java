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
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import javax.inject.Provider;

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

        Provider<SimpleTaskExecutor> simpleTaskExecutorProvider = mock(Provider.class);
        when(simpleTaskExecutorProvider.get()).thenAnswer(new Answer<SimpleTaskExecutor<?>>() {
            public SimpleTaskExecutor<?> answer(InvocationOnMock invocation) throws Throwable {
                return new SimpleTaskExecutor<Object>(mock(ConnectionFactory.class), null, null);
            }
        });

        Semaphore semaphore = new Semaphore(2, true);

        // When
        final TaskExecutor executor = new PooledTaskExecutor(simpleTaskExecutorProvider, debug,
                ConnectionType.RESOURCE_SETS, connectionCount, configFactory, semaphore);
        LongTask longTask1 = new LongTask();
        TaskThread task1 = new TaskThread(1, executor, longTask1);
        LongTask longTask2 = new LongTask();
        TaskThread task2 = new TaskThread(2, executor, longTask2);
        TaskThread task3 = new TaskThread(3, executor, mock(Task.class));

        task1.start();
        task2.start();

        while (semaphore.availablePermits() > 0) {
            Thread.sleep(50);
        }

        task3.start();

        long timeout = System.currentTimeMillis() + 5000;
        while (!semaphore.hasQueuedThreads()) {
            Thread.sleep(50);
            if (System.currentTimeMillis() > timeout) {
                fail("Where did my thread go?");
            }
        }

        // Then
        verifyZeroInteractions(task3.task);

        longTask2.unblock();
        longTask1.unblock();

        task1.join();
        task2.join();
        task3.join();

        verify(task3.task).execute(null, null);
        verify(simpleTaskExecutorProvider, times(2)).get();
    }

    private static class TaskThread extends Thread {
        private TaskExecutor executor;
        private Task task;

        TaskThread(int taskId, TaskExecutor executor, Task task) {
            this.executor = executor;
            this.task = task;
            setName("Task " + taskId);
        }

        public void run() {
            try {
                executor.execute(null, task);
            } catch (DataLayerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class LongTask implements Task {

        private AtomicBoolean locked = new AtomicBoolean(false);
        private Thread executingThread;

        @Override
        public <T> void execute(T connection, TokenStorageAdapter<T> adapter) throws DataLayerException {
            this.executingThread = Thread.currentThread();
            locked.set(true);
            while (!locked.compareAndSet(false, true)) {
                LockSupport.park(this);
            }
        }

        public void unblock() {
            locked.set(false);
            LockSupport.unpark(executingThread);
        }

    }


}