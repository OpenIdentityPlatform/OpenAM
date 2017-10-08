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
package org.forgerock.openam.audit.context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.forgerock.util.thread.ExecutorServiceFactory;

/**
 * Responsible for filtering the API of {@ExecutorServiceFactory} to exclude any
 * methods that do not offer a means of setting thread names and to ensure that
 * new methods added to {@ExecutorServiceFactory} are always called via
 * {@link AuditRequestContextPropagatingExecutorServiceFactory}.
 *
 * @see ExecutorServiceFactory
 * @see AuditRequestContextPropagatingExecutorServiceFactory
 *
 * @since 14.0.0
 */
public interface AMExecutorServiceFactory {

    /**
     * Generates a ScheduledExecutorService which has been pre-registered with the
     * ShutdownManager.
     *
     * @see java.util.concurrent.Executors#newScheduledThreadPool(int)
     *
     * @param poolSize The size of the ScheduledExecutorService thread pool.
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return A non null ScheduledExecutorService
     */
    ScheduledExecutorService createScheduledService(int poolSize, String threadNamePrefix);

    /**
     * Creates a fixed size Thread Pool ExecutorService which has been pre-registered with
     * the {@link org.forgerock.util.thread.listener.ShutdownManager}.
     *
     * @param pool The size of the pool to create.
     * @param factory The {@link java.util.concurrent.ThreadFactory} used to generate new threads.
     * @return Non null.
     */
    ExecutorService createFixedThreadPool(int pool, ThreadFactory factory);

    /**
     * Create a fixed size Thread Pool ExecutorService using the provided name as the prefix
     * of the thread names.
     *
     * @see #createFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     *
     * @param pool Size of the fixed pool.
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return Non null.
     */
    ExecutorService createFixedThreadPool(int pool, String threadNamePrefix);

    /**
     * Generates a Cached Thread Pool ExecutorService which has been pre-registered with the
     * ShutdownManager. The provided ThreadFactory is used by the service when creating Threads.
     *
     * @see java.util.concurrent.Executors#newCachedThreadPool(java.util.concurrent.ThreadFactory)
     *
     * @param factory The ThreadFactory that will be used when generating threads. May not be null.
     * @return A non null ExecutorService.
     */
    ExecutorService createCachedThreadPool(ThreadFactory factory);

    /**
     * Generates a Cached Thread Pool ExecutorService using the provided name as a prefix
     * of the thread names.
     *
     * @see #createCachedThreadPool(java.util.concurrent.ThreadFactory)
     *
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return Non null.
     */
    ExecutorService createCachedThreadPool(String threadNamePrefix);

    /**
     * Generates a ThreadPoolExecutor with the provided values, and registers that executor as listening for
     * shutdown messages.
     *
     * @param coreSize the number of threads to keep in the pool, even if they are idle
     * @param maxSize Max number of threads in the pool
     * @param idleTimeout When the number of threads is greater than core, maximum time that excess idle
     *                    threads will wait before terminating
     * @param timeoutTimeunit The time unit for the idleTimeout argument
     * @param runnables Queue of threads to be run
     * @param threadNamePrefix The thread name prefix to use when generating new threads.
     * @return a configured ExecutorService, registered to listen to shutdown messages.
     */
    ExecutorService createThreadPool(int coreSize, int maxSize, long idleTimeout,
            TimeUnit timeoutTimeunit, BlockingQueue<Runnable> runnables, String threadNamePrefix);

}
