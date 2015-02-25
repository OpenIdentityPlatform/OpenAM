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
package org.forgerock.openam.shared.concurrency;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.*;

import static org.mockito.BDDMockito.*;

public class ThreadMonitorTest {

    private ExecutorService mockWorkPool;
    private ShutdownManager mockShutdownWrapper;
    private Debug mockDebug;
    private ThreadMonitor monitor;

    @BeforeMethod
    public void setup() {
        mockWorkPool = immediateExecutor();
        mockShutdownWrapper = mock(ShutdownManager.class);
        mockDebug = mock(Debug.class);

        monitor = new ThreadMonitor(
                mockWorkPool,
                mockShutdownWrapper,
                mockDebug);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotExecuteNullRunnable() {
        monitor.watchThread(mockWorkPool, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullExecutorService() {
        monitor.watchThread(null, mock(Runnable.class));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullScheduledRunnable() {
        monitor.watchScheduledThread(mock(ScheduledExecutorService.class), null, 0, 0, TimeUnit.MILLISECONDS);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldNotAllowNullScheduledExecutorService() {
        monitor.watchScheduledThread(null, mock(Runnable.class), 0, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldSubmitRunnableToProvidedExecutor() {
        // Given
        Runnable mockRunnable = mock(Runnable.class);
        ExecutorService mockService = mockNoOpExecutor();

        // When
        monitor.watchThread(mockService, mockRunnable);

        // Then
        verify(mockService).submit(eq(mockRunnable));
    }

    @Test
    public void shouldSubmitRunnableToScheduledExecutor() {
        // Given
        Runnable mockRunnable = mock(Runnable.class);
        ScheduledExecutorService mockScheduledService = mockNoOpScheduledExecutor();

        // When
        monitor.watchScheduledThread(mockScheduledService, mockRunnable, 1, 1, TimeUnit.DAYS);

        // Then
        verify(mockScheduledService).scheduleAtFixedRate(eq(mockRunnable), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldRespondToShutdownSignalBeforeExecute() {
        // Given
        Runnable mockRunnable = mock(Runnable.class);

        /**
         * Setup the shutdownManager so that when addListener is invoked, it will in the same
         * thread, call the shutdown listener.
         *
         * For the test this will then move the WatchDog into a completed state before the
         * Thread has had a chance to execute, therefore preventing execution of the runnable.
         */
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                ShutdownListener listener = (ShutdownListener) invocationOnMock.getArguments()[0];
                listener.shutdown();
                return null;
            }
        }).when(mockShutdownWrapper).addShutdownListener(any(ShutdownListener.class));

        // When
        monitor.watchThread(immediateExecutor(), mockRunnable);

        // Then
        verify(mockRunnable, times(0)).run();
    }

    @Test
    public void shouldRespondToShutdownDuringExecution() throws ExecutionException, InterruptedException {
        // Given
        /**
         * Runnable and corresponding Future will be injected into ThreadMonitor.
         * When the Runnable is executed, it will trigger the shutdown signal
         * (using the captured Listener).
         */
        Runnable mockRunnable = mock(Runnable.class);
        Future mockFuture = mock(Future.class);

        // Capture the shutdown listener
        final ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
        doNothing().when(mockShutdownWrapper).addShutdownListener(captor.capture());

        /**
         * Wire the runnable to trigger shutdown on ShutdownListener, which when
         * in this single-threaded unit test environment has a 'happens-after'
         * relationship.
         */
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                captor.getValue().shutdown();
                return null;
            }
        }).when(mockRunnable).run();

        // When
        monitor.watchThread(immediateExecutor(mockFuture), mockRunnable);

        // Then
        verify(mockFuture).get();
    }


    private ExecutorService immediateExecutor() {
        return immediateExecutor(mock(Future.class));
    }

    private ExecutorService immediateExecutor(final Future mockFuture) {
        ExecutorService r = mock(ExecutorService.class);
        given(r.submit(any(Runnable.class))).will(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Runnable runnable = (Runnable) invocationOnMock.getArguments()[0];
                runnable.run();
                return mockFuture;
            }
        });
        return r;
    }

    private ExecutorService mockNoOpExecutor() {
        ExecutorService r = mock(ExecutorService.class);
        given(r.submit(any(Runnable.class))).willReturn(mock(Future.class));
        return r;
    }

    private ScheduledExecutorService mockNoOpScheduledExecutor() {
        ScheduledExecutorService r = mock(ScheduledExecutorService.class);
        given(r.scheduleAtFixedRate(
                any(Runnable.class),
                anyLong(),
                anyLong(),
                any(TimeUnit.class)))
                .willReturn(mock(ScheduledFuture.class));
        return r;
    }

    /**
     * Demonstration Main Method to validate understanding and demonstrate functionality.
     *
     * Will setup a ThreadMonitor with a real ExecutorService which is then used to run
     * an intentionally faulty task. The task will fail, and the ThreadMonitor will detect
     * and restart the thread.
     *
     * @param args Not used.
     */
    public static void main(String... args) {
        // Subvert a Debugger
        Debug subvertedDebugger = mock(Debug.class);
        BDDMockito.doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String msg = (String) invocationOnMock.getArguments()[0];
                Throwable error = (Throwable) invocationOnMock.getArguments()[1];
                System.out.println(msg);
                error.printStackTrace(System.out);
                return null;
            }
        }).when(subvertedDebugger).error(anyString(), any(Throwable.class));

        ExecutorService workPool = Executors.newCachedThreadPool();
        ThreadMonitor threadMonitor = new ThreadMonitor(
                workPool,
                com.sun.identity.common.ShutdownManager.getInstance(),
                subvertedDebugger);

        threadMonitor.watchThread(workPool, new Runnable(){
            public void run() {
                try {
                    Thread.sleep(1000);
                    throw new IllegalStateException("Halp! I've had a fail!");
                } catch (InterruptedException e) {
                    System.out.println("Cancelled");
                }
            }
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        workPool.shutdown();
    }
}