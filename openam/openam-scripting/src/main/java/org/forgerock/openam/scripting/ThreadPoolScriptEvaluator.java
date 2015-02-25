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

package org.forgerock.openam.scripting;

import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.Bindings;
import javax.script.ScriptException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Evaluates scripts in a configured thread pool so that they can be interrupted after a timeout has occurred.
 */
@Singleton
public final class ThreadPoolScriptEvaluator implements ScriptEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolScriptEvaluator.class);

    private final StandardScriptEngineManager scriptEngineManager;
    private final ExecutorService threadPool;
    private final ScriptEvaluator delegate;

    /**
     * Constructs a script evaluator that uses a configurable thread pool to execute scripts, delegating actual script
     * execution to another script evaluator. Registers a configuration listener to adjust the thread pool according
     * to current application settings.
     *
     * @param scriptEngineManager the manager object to listen for configuration changes. Not null.
     * @param threadPool the thread pool to use for evaluating scripts. Not null.
     * @param delegate the script evaluator to use to evaluate scripts from the thread pool. Not null.
     */
    @Inject
    public ThreadPoolScriptEvaluator(final StandardScriptEngineManager scriptEngineManager,
                                     final ExecutorService threadPool,
                                     final ScriptEvaluator delegate) {
        Reject.ifNull(scriptEngineManager, threadPool, delegate);

        this.scriptEngineManager = scriptEngineManager;
        this.threadPool = threadPool;
        this.delegate = delegate;

        scriptEngineManager.addConfigurationListener(new ThreadPoolConfigurator(threadPool));
    }

    /**
     * This implementation of evaluateScript runs any provided script in its own thread.
     *
     * If timeouts have been configured in the script engine manager which this evaluator uses then
     * the threads will be interrupted (or otherwise stop themselves depending on the engine's implementation)
     * after the timeout period has expired if they have not already returned.
     *
     * @see org.forgerock.openam.scripting.factories.GroovyEngineFactory
     *
     * @param script {@inheritDoc}
     * @param bindings {@inheritDoc}
     * @throws ScriptException if anything went wrong during the script's execution
     */
    @Override
    public <T> T evaluateScript(final ScriptObject script, final Bindings bindings) throws ScriptException {

        final Future<T> future = threadPool.submit(new ScriptExecutorTask<T>(script, bindings));
        final long timeout = scriptEngineManager.getConfiguration().getScriptExecutionTimeout();
        try {
            if (timeout == ScriptEngineConfiguration.NO_TIMEOUT) {
                return future.get();
            } else {
                return future.get(timeout, TimeUnit.SECONDS);
            }
        } catch (ExecutionException ex) {
            LOGGER.error("Script terminated with exception", ex);
            throw new ScriptException(ex);
        } catch (TimeoutException ex) {
            LOGGER.warn("Script timed out");
            throw new ScriptException(ex);
        } catch (InterruptedException ex) {
            // Reset interrupted status for callers
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while waiting for script result");
            throw new ScriptException(ex);
        } finally {
            // Harmless if task has already completed
            future.cancel(true);
        }
    }

    @Override
    public void bindVariableInGlobalScope(final String name, final Object object) {
        delegate.bindVariableInGlobalScope(name, object);
    }

    /**
     * Script engine configuration listener that resizes the script engine thread pool in response to configuration
     * changes. If the thread pool implementation supports re-configuration then this will resize the core and
     * maximum thread sizes. This typically takes effect as threads are returned to the pool or new threads are
     * requested so may resize over a period of time. If the thread pool is not reconfigurable, or if the new
     * configuration parameters are not valid, then an error is logged and the pool is left in its original
     * configuration.
     * <p/>
     * NB: The queue size is not reconfigurable, so changes will take effect only on server restart. All other settings
     * can be changed without a restart and the pool will adjust over time to the new settings.
     */
    private static final class ThreadPoolConfigurator implements StandardScriptEngineManager.ConfigurationListener {
        private final ExecutorService executorService;

        ThreadPoolConfigurator(final ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void onConfigurationChange(final ScriptEngineConfiguration newConfiguration) {
            try {
                // This may throw a ClassCastException if implementation changes - handled below
                final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executorService;

                if (threadPool.getCorePoolSize() != newConfiguration.getThreadPoolCoreSize() ||
                    threadPool.getMaximumPoolSize() != newConfiguration.getThreadPoolMaxSize() ||
                    threadPool.getKeepAliveTime(TimeUnit.SECONDS) != newConfiguration.getThreadPoolIdleTimeoutSeconds())
                {

                    LOGGER.debug("Reconfiguring script evaluation thread pool. " +
                                    "Core pool size: old=%d, new=%d. " +
                                    "Max pool size: old=%d, new=%d. " +
                                    "Idle timeout (seconds): old=%d, new=%d.",
                            threadPool.getCorePoolSize(), newConfiguration.getThreadPoolCoreSize(),
                            threadPool.getMaximumPoolSize(), newConfiguration.getThreadPoolMaxSize(),
                            threadPool.getKeepAliveTime(TimeUnit.SECONDS),
                            newConfiguration.getThreadPoolIdleTimeoutSeconds());

                    threadPool.setCorePoolSize(newConfiguration.getThreadPoolCoreSize());
                    threadPool.setMaximumPoolSize(newConfiguration.getThreadPoolMaxSize());
                    threadPool.setKeepAliveTime(newConfiguration.getThreadPoolIdleTimeoutSeconds(), TimeUnit.SECONDS);
                }

            } catch (ClassCastException ex) {
                LOGGER.warn("Unable to reconfigure script evaluation thread pool - pool is not reconfigurable");
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Attempt to configure script evaluation thread pool with invalid parameters", ex);
            }
        }
    }

    /**
     * Task for executing a script in a background thread using the configured delegate script evaluator.
     *
     * @param <T> the type of result that is expected to be returned.
     */
    private final class ScriptExecutorTask<T> implements Callable<T> {
        private final ScriptObject scriptObject;
        private final Bindings bindings;

        private ScriptExecutorTask(final ScriptObject scriptObject, final Bindings bindings) {
            Reject.ifNull(scriptObject);
            this.scriptObject = scriptObject;
            this.bindings = bindings;
        }

        @Override
        public T call() throws ScriptException {
            return delegate.evaluateScript(scriptObject, bindings);
        }
    }

}
