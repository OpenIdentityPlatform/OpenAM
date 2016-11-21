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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.util.Reject;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * Subclass of {@link ExecutorServiceFactory} which allows {@link ScheduledExecutorService} threads to be named.
 * <p>
 * This is a temporary class which is only needed as Commons has reached feature freeze. When changes to Commons
 * are permitted, the single public method in this class should be rolled up into {@link ExecutorServiceFactory}
 * (COMMONS-137) and this subclass should be deleted (AME-12776).
 */
public class AMExecutorServiceFactory extends ExecutorServiceFactory {

    private final ShutdownManager shutdownManager;

    /**
     * Create an instance of the factory.
     *
     * @param shutdownManager Required to ensure each ExecutorService will be shutdown.
     */
    public AMExecutorServiceFactory(ShutdownManager shutdownManager) {
        super(shutdownManager);
        this.shutdownManager = shutdownManager;
    }

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
    public ScheduledExecutorService createScheduledService(int poolSize, String threadNamePrefix) {
        final ScheduledExecutorService service =
                Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory(threadNamePrefix));
        registerShutdown(service);
        return service;
    }

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
    public ExecutorService createThreadPool(int coreSize, int maxSize, long idleTimeout,
            TimeUnit timeoutTimeunit, BlockingQueue<Runnable> runnables, String threadNamePrefix) {
        Reject.ifTrue(coreSize < 0);
        Reject.ifTrue(maxSize < coreSize || maxSize <= 0);
        Reject.ifTrue(idleTimeout < 0);

        ExecutorService service = new ThreadPoolExecutor(coreSize, maxSize, idleTimeout, timeoutTimeunit,
                runnables, new NamedThreadFactory(threadNamePrefix));
        registerShutdown(service);
        return service;
    }
    /**
     * Registers a listener to trigger shutdown of the ExecutorService.
     * @param service Non null ExecutorService to register.
     */
    private void registerShutdown(final ExecutorService service) {
        shutdownManager.addShutdownListener(
                new ShutdownListener() {
                    public void shutdown() {
                        service.shutdownNow();
                    }
                });
    }

    /**
     * Used to generate threads with a provided name. Each new thread will
     * have its generated number appended to the end of it, in the form -X, where
     * X is incremented once for each thread created.
     */
    private class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger count = new AtomicInteger(0);
        private final String name;

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" +  count.getAndIncrement());
        }
    }
}
