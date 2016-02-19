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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.Mockito.*;

import javax.inject.Provider;
import java.text.MessageFormat;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sm.ConnectionConfig;
import org.forgerock.openam.sm.ConnectionConfigFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

public class PooledTaskExecutorTest {

    @Test
    public void testExecute() throws Exception {
        // Given
        ConnectionConfigFactory configFactory = mock(ConnectionConfigFactory.class);
        ConnectionConfig config = mock(ConnectionConfig.class);
        when(configFactory.getConfig(any(ConnectionType.class))).thenReturn(config);
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
                ConnectionType.RESOURCE_SETS, configFactory, semaphore);
        LongTask longTask1 = new LongTask();
        TaskThread task1 = new TaskThread(1, executor, longTask1);
        LongTask longTask2 = new LongTask();
        TaskThread task2 = new TaskThread(2, executor, longTask2);
        TaskThread task3 = new TaskThread(3, executor, mock(Task.class));

        debug("Starting task 1");
        task1.start();
        debug("Starting task 2");
        task2.start();

        while (semaphore.availablePermits() > 0) {
            debug("Waiting for no available permits. Currently got: {0}", semaphore.availablePermits());
            Thread.sleep(50);
        }

        debug("Tasks 1 and 2 should now be executing and will shortly be blocked - starting task 3");
        task3.start();

        long timeout = currentTimeMillis() + 5000;
        while (!semaphore.hasQueuedThreads()) {
            debug("Waiting for task 3 to be queued on semaphore");
            Thread.sleep(50);
            if (currentTimeMillis() > timeout) {
                fail("Where did my thread go?");
            }
        }
        debug("Task 3 now queued on semaphore");

        // Then
        verifyZeroInteractions(task3.task);

        // When
        debug("Unblocking task 2");
        longTask2.unblock();
        debug("Unblocking task 1");
        longTask1.unblock();

        // Then
        debug("Waiting for tasks to complete");
        task1.join(TimeUnit.SECONDS.toMillis(10));
        task2.join(TimeUnit.SECONDS.toMillis(10));
        task3.join(TimeUnit.SECONDS.toMillis(10));

        assertThat(task1.isAlive()).as("Task 1 thread running").isFalse();
        assertThat(task2.isAlive()).as("Task 2 thread running").isFalse();
        assertThat(task3.isAlive()).as("Task 3 thread running").isFalse();

        verify(task3.task).execute(null, null);
        verify(simpleTaskExecutorProvider, times(2)).get();
        assertThat(semaphore.availablePermits()).isEqualTo(2);
    }

    private static class TaskThread extends Thread {
        private TaskExecutor executor;
        private Task task;

        TaskThread(int taskId, TaskExecutor executor, Task task) {
            this.executor = executor;
            this.task = task;
            setName("Task " + taskId);
            setDaemon(true);
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
            debug("Locking");
            locked.set(true);
            while (!locked.compareAndSet(false, true)) {
                debug("Task still locked - parking thread");
                LockSupport.park(this);
                debug("Thread unparked");
            }
            debug("Thread unlocked - continuing");
        }

        @Override
        public void processError(DataLayerException error) {}

        public void unblock() {
            debug("Setting task unlocked");
            locked.set(false);
            debug("Unparking thread {0}", executingThread);
            LockSupport.unpark(executingThread);
            debug("Unparked thread {0}", executingThread);
        }

    }

    private static void debug(String message, Object... params) {
        System.out.println("PooledTaskExecutorTest " + Thread.currentThread() + " :: " +
                MessageFormat.format(message, params));
    }

}
