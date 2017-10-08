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

package org.forgerock.openam.audit.context;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.forgerock.util.thread.ExecutorServiceFactory;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 * {@link ExecutorServiceFactory} decorator that ensures all ExecutorServices propagate
 * {@link AuditRequestContext} from task publishing thread to task consuming thread.
 *
 * @since 13.0.0
 */
public class AuditRequestContextPropagatingExecutorServiceFactory implements AMExecutorServiceFactory {

    private final AMExecutorServiceFactory delegate;

    /**
     * Constructs a new {@code AuditRequestContextPropagatingExecutorServiceFactory}.
     *
     * @param shutdownManager The {@code ShutdownManager}.
     */
    public AuditRequestContextPropagatingExecutorServiceFactory(ShutdownManager shutdownManager) {
        delegate = new ExtendedExecutorServiceFactory(shutdownManager);
    }

    @Override
    public ScheduledExecutorService createScheduledService(int poolSize, String threadNamePrefix) {
        return decorate(delegate.createScheduledService(poolSize, threadNamePrefix));
    }

    @Override
    public ExecutorService createFixedThreadPool(int pool, ThreadFactory factory) {
        return decorate(delegate.createFixedThreadPool(pool, factory));
    }

    @Override
    public ExecutorService createFixedThreadPool(int pool, String threadName) {
        return decorate(delegate.createFixedThreadPool(pool, threadName));
    }

    @Override
    public ExecutorService createCachedThreadPool(ThreadFactory factory) {
        return decorate(delegate.createCachedThreadPool(factory));
    }

    @Override
    public ExecutorService createCachedThreadPool(String threadName) {
        return decorate(delegate.createCachedThreadPool(threadName));
    }

    @Override
    public ExecutorService createThreadPool(int coreSize, int maxSize, long idleTimeout,
            TimeUnit timeoutTimeunit, BlockingQueue<Runnable> runnables, String threadNamePrefix) {
        return decorate(delegate.createThreadPool(
                coreSize, maxSize, idleTimeout, timeoutTimeunit, runnables, threadNamePrefix));
    }

    private ExecutorService decorate(ExecutorService delegate) {
        return new AuditRequestContextPropagatingExecutorService(delegate);
    }

    private ScheduledExecutorService decorate(ScheduledExecutorService delegate) {
        return new AuditRequestContextPropagatingScheduledExecutorService(delegate);
    }

}
