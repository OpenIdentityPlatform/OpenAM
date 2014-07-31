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
import org.forgerock.openam.shared.guice.SharedGuiceModule;
import org.forgerock.util.Reject;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ThreadMonitor is a utility class responsible for providing a small framework to
 * ensuring that a thread is re-started in the case of an unexpected failure.
 *
 * This framework aims to support a common problem for scheduled threads where a
 * runtime exception is thrown which kills the thread and results in it not being
 * rescheduled. This framework can also be used for standard runnable threads which
 * operate in a loop and expect to stay in that loop.
 *
 * This framework will detect the abnormal thread exit, and reschedule the thread
 * to be executed again. This behaviour will continue until system shutdown is signalled,
 * or the thread exits in the expected fashion of returning from the run method.
 *
 * In the case of scheduled threads (scheduleAtFixedRate for example) the same applies
 * however the thread will never exit normally until it is cancelled.
 *
 * This function is to be used when the caller requires that their thread never fails
 * under any unexpected circumstance. This usage however should be used carefully.
 * Tight loops of failure followed by restart should be avoided. The thread being
 * invoked is responsible in this case for detecting this and be designed with restart
 * in mind.
 *
 * The ThreadMonitor will use its own internal WatchDog class, itself runnable and a
 * thread pool to provide the monitoring.
 *
 * <br>Thread Policy</br>: All {@link java.lang.Runnable} tasks provided to this
 * ThreadMonitor should respond to the standard {@link Thread#interrupt()} signal.
 * This mechanism will be used for coordinating shutdown of the task. All
 * ExecutorServices provided to this framework are assumed to respond to the system
 * shutdown signal, though this is not required for this framework to function.
 *
 * Sadly though the question of "Who will guard the guards themselves?" is intentionally
 * avoided by this class and should be considered out of scope.
 *
 * @link https://en.wikipedia.org/wiki/Quis_custodiet_ipsos_custodes%3F
 * @see com.sun.identity.common.ShutdownManager
 * @see org.forgerock.util.thread.listener.ShutdownManager
 */
public class ThreadMonitor {
    private static final String DEBUG_HEADER = "ThreadMonitor: ";
    private final ExecutorService workPool;
    private final ShutdownManager shutdownManager;
    private final Debug debug;

    /**
     * Create an instance of the ThreadMonitor with an assigned work pool of threads to use.
     *
     * @see org.forgerock.util.thread.ExecutorServiceFactory
     *
     * @param workPool A sized ExecutorService which is larger enough to handle the expected
     *                 number of jobs it needs to monitor. This ExecutorService should be
     *                 generated using the {@link org.forgerock.util.thread.ExecutorServiceFactory}
     *                 to ensure it responds to the global System Shutdown event. Non null.
     * @param shutdownManager Required to detect shutdown signals.
     * @param debug Non null, required for signalling thread failure/restart.
     */
    @Inject
    public ThreadMonitor(ExecutorService workPool,
                         ShutdownManager shutdownManager,
                         @Named(SharedGuiceModule.DEBUG_THREAD_MANAGER) Debug debug) {
        this.workPool = workPool;
        this.shutdownManager = shutdownManager;
        this.debug = debug;
    }

    /**
     * Triggers the given runnable to be executed immediately via the given ExecutorService.
     *
     * This will also generate a WatchDog thread to monitor the runnable and detect when it
     * fails abnormally. This will trigger the restart of the thread.
     *
     * @param service Non null service which will hold the runnable.
     * @param runnable Non null runnable to execute immediately.
     */
    public void watchThread(final ExecutorService service, final Runnable runnable) {
        Reject.ifNull(service, runnable);
        workPool.submit(new WatchDog(new StartThread() {
            public Future<?> start() {
                return service.submit(runnable);
            }

            @Override
            public String toString() {
                return "Executable: " + runnable.toString();
            }
        }));
    }

    /**
     * Triggers the given runnable immediately to be executed at the scheduled time.
     *
     * This will also generate a WatchDog thread to monitor the runnable and detect when it
     * fails abnormally. This will trigger the restart of the thread.
     *
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, java.util.concurrent.TimeUnit)
     *
     * @param scheduledService Non null scheduled service which will hold the runnable.
     * @param runnable Non null runnable to executed on a scheduled basis.
     * @param delay Start delay in the given TimeUnits.
     * @param duration Fixed delay in the given TimeUnits.
     * @param timeUnit TimeUnit for both delay and duration.
     */
    public void watchScheduledThread(final ScheduledExecutorService scheduledService,
                                     final Runnable runnable,
                                     final long delay,
                                     final long duration,
                                     final TimeUnit timeUnit) {
        Reject.ifNull(scheduledService, runnable);
        workPool.submit(new WatchDog(new StartThread() {
            public Future<?> start() {
                return scheduledService.scheduleAtFixedRate(runnable, delay, duration, timeUnit);
            }

            @Override
            public String toString() {
                return MessageFormat.format(
                        "ScheduledExecutable: {0} (start:{1} duration:{2} TimeUnit:{3}",
                        runnable.toString(),
                        delay,
                        duration,
                        timeUnit.toString());
            }
        }));
    }

    /**
     * WatchDog is responsible for monitoring the thread, and responding to the system shutdown event.
     * When shutdown is triggered, it will cancel any assigned task if appropriate and also prevent
     * subsequent restart.
     */
    private class WatchDog implements Runnable {
        private final StartThread startThread;
        private boolean complete = false;
        private Future<?> future;

        public WatchDog(StartThread startThread) {
            this.startThread = startThread;
            shutdownManager.addShutdownListener(new ShutdownListener() {
                public void shutdown() {
                    cancel();
                }
            });
        }

        /**
         * Ensure that the complete flag (prevent restart) and the cancelling
         * of the Future are performed atomically.
         */
        private synchronized void cancel() {
            setComplete(true);

            Future<?> future = getFuture();
            if (future != null && !future.isDone()) {
                debug("Cancelling", startThread);
                future.cancel(true);
            }
        }

        /**
         * @return May be null if called before the task has been started.
         */
        private synchronized Future<?> getFuture() {
            return future;
        }

        /**
         * @param future The future for the started task. Must not be null.
         */
        private synchronized void setFuture(Future<?> future) {
            Reject.ifNull(future);
            this.future = future;
        }

        /**
         * Synchronized because cancel may be invoked whilst complete is being modified.
         */
        private synchronized void setComplete(boolean complete) {
            this.complete = complete;
        }

        /**
         * Synchronized because cancel may be invoked whilst complete is being read.
         * @return The current atomic state of the complete flag.
         */
        private synchronized boolean isComplete() {
            return complete;
        }

        public void run() {
            while (!isComplete()) {
                debug("Starting", startThread);
                setFuture(startThread.start());
                try {
                    getFuture().get();
                    setComplete(true);
                    debug("Complete", startThread);
                } catch (InterruptedException e) {
                    debug("interrupt detected, shutting down", startThread);
                    setComplete(true);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    debug.error(DEBUG_HEADER + "Thread WatchDog detected error, restarting", e);
                } finally {
                    debug("Complete:" + isComplete(), startThread);
                }
            }
            debug("Exited", startThread);
        }
    }

    /**
     * Interface to generify and defer the initialisation of the Runnable to the caller. This
     * allows the WatchDog to function with all types of ExecutorService regardless of the
     * call arguments.
     */
    private interface StartThread {
        /**
         * @return The future based on the result of submitting a Runnable to an ExecutorService. Not null.
         */
        Future<?> start();

        /**
         * @return Identification details of the thread.
         */
        String toString();
    }

    private void debug(String state, StartThread start) {
        if (debug.messageEnabled()) {
            debug.message(DEBUG_HEADER + state + ": " + start.toString());
        }
    }
}
