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

package org.forgerock.openam.shared.audit.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ExecutorService decorator that propagates {@link AuditRequestContext} from task publishing thread to task consuming thread.
 *
 * @since 13.0.0
 */
public class AuditRequestContextPropagatingExecutorService implements ConfigurableExecutorService {

    final ExecutorService delegate;

    public AuditRequestContextPropagatingExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(new AuditRequestContextPropagatingCallable<>(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(new AuditRequestContextPropagatingRunnable(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(wrapAll(tasks), timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapAll(tasks));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapAll(tasks), timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    /**
     * @return True if the <code>ExecutorService</code> delegate supports configuration.
     * @see #getConfigurator()
     */
    @Override
    public boolean isConfigurable() {
        return delegate instanceof ThreadPoolExecutor;
    }

    @Override
    public ExecutorServiceConfigurator getConfigurator() {
        if (!isConfigurable()) {
            throw new IllegalStateException("ExecutorService cannot be configured.");
        }
        return new ExecutorServiceConfigurator((ThreadPoolExecutor) delegate);
    }

    final Runnable wrap(Runnable delegate) {
        return new AuditRequestContextPropagatingRunnable(delegate);
    }

    final <T> Callable<T> wrap(Callable<T> delegate) {
        return new AuditRequestContextPropagatingCallable<>(delegate);
    }

    @SuppressWarnings("unchecked")
    final <T> Collection<? extends Callable<T>> wrapAll(Collection<? extends Callable<T>> tasks) {
        Collection<Callable<T>> results = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            results.add(new AuditRequestContextPropagatingCallable<>(task));
        }
        return results;
    }

}