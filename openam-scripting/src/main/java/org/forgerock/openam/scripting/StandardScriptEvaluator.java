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

import org.codehaus.groovy.control.io.NullWriter;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.scripting.timeouts.ScriptRunner;
import org.forgerock.openam.shared.concurrency.ExecutorServiceFactory;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Evaluates scripts using the standard JSR 223 script engine framework.
 *
 * Each script is run in its own thread, watched by the spawning thread. If the script-thread
 * takes longer to run than the global timeout set for scripts in this class's script engine manager,
 * that thread's execution must stop.
 *
 * Different ScriptEngines handle the stopping/interruption differently: Groovy relies on
 * us sending its thread an interrupt signal, while JavaScript has its own timer which is checked on
 * each processed instruction.
 *
 * @since 12.0.0
 */
public class StandardScriptEvaluator implements ScriptEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardScriptEvaluator.class);

    private static ExecutorService threadPool;

    private static ExecutorServiceFactory executorServiceFactory =
            InjectorHolder.getInstance(ExecutorServiceFactory.class);

    private final StandardScriptEngineManager scriptEngineManager;

    /**
     * Constructs the script evaluator using the given JSR 223 script engine manager instance.
     *
     * @param scriptEngineManager the script engine manager to use for creating script engines. May not be null.
     */
    @Inject
    public StandardScriptEvaluator(StandardScriptEngineManager scriptEngineManager) {
        Reject.ifNull(scriptEngineManager);
        this.scriptEngineManager = scriptEngineManager;
    }

    /**
     * The engine manager for configuring timeout
     */
    public StandardScriptEngineManager getEngineManager() {
        return scriptEngineManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bindVariableInGlobalScope(final String name, final Object object) {
        Reject.ifNull(name, object);
        scriptEngineManager.put(name, object);
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
        Reject.ifNull(script);

        if (getThreadPool() == null) {
            LOGGER.debug("Script module incorrectly configured.");
            throw new IllegalStateException("ThreadPool must be configured before use.");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Evaluating script: " + script);
        }

        final ScriptEngine engine = getScriptEngineFor(script);
        final Bindings variableBindings = mergeBindings(script.getBindings(), bindings);
        final ScriptContext context = buildScriptContext(variableBindings);

        return beginTask(engine, context, script);
    }

    /**
     * Performs the task, gathering a thread from the threadPool and - if enabled - watches the
     * script-executing thread from this one until the timeout has occured.
     *
     * If no timeout is enabled, and the script contains an infinite loop, these threads will
     * never end.
     *
     * This method throws a {@link ScriptException} if any part of the script's execution failed - either
     * through interrupt or internal error. This is thrown out of the Script Evaluator so that the calling code
     * can determine whether or not to trust any state queryable about the engine (it is recommended to not trust
     * any state set by a script which has not fully completed).
     */
    private <T> T beginTask(ScriptEngine engine, ScriptContext context, ScriptObject script) throws ScriptException {

        final boolean timeoutDisabled = scriptEngineManager.getTimeoutMillis() == 0;

        @SuppressWarnings("unchecked")
        Future<T> result = getThreadPool().submit(new ScriptRunner(engine, context, script));
        T myAnswer = null;

        try {
            if (timeoutDisabled) {
                myAnswer = result.get(); //will run forever if infinite loop in script...
            } else {
                myAnswer = result.get(scriptEngineManager.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Script interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new ScriptException(e);
        } catch (ExecutionException e) {
            LOGGER.debug("Script execution failed: " + e.getMessage());
            throw new ScriptException(e);
        } catch (TimeoutException e) {
            LOGGER.debug("Script timed out: " + e.getMessage());
            throw new ScriptException(e);
        } finally {
            result.cancel(true); //if already completed this does no harm
        }

        return myAnswer;
    }

    /**
     * Returns a configured script engine suitable for executing the given script.
     *
     * @param script the script to get a script engine for. May not be null.
     * @return the configured script engine for this script.
     */
    private ScriptEngine getScriptEngineFor(final ScriptObject script) {
        Reject.ifNull(script);

        final ScriptEngine engine = script.getLanguage().getScriptEngine(scriptEngineManager);
        if (engine == null) {
            throw new IllegalStateException("Unable to get script engine for language: " + script.getLanguage());
        }
        return engine;
    }


    /**
     * Merges all sets of variable bindings into a single scope to use when evaluating the script. Bindings later in
     * the list will override bindings earlier in the list.
     *
     * @param allBindings the set of all variable bindings to merge. Cannot be null.
     * @return the merged set of all variable bindings.
     */
    private Bindings mergeBindings(Bindings...allBindings) {
        Bindings result = new SimpleBindings();
        for (Bindings scope : allBindings) {
            if (scope != null) {
                result = new ChainedBindings(result, scope);
            }
        }
        return result;
    }

    /**
     * Build the script context for evaluating a script, using the given set of variables for the engine scope.
     *
     * @param engineScope the variable bindings to use for the engine scope.
     * @return the configured script context.
     */
    private ScriptContext buildScriptContext(Bindings engineScope) {
        final ScriptContext context = new SimpleScriptContext();
        context.setBindings(engineScope, ScriptContext.ENGINE_SCOPE);
        context.setBindings(scriptEngineManager.getBindings(), ScriptContext.GLOBAL_SCOPE);
        // Replace reader/writer instances with null versions
        context.setReader(null);
        // Groovy expects these writers to be non-null, so use the Groovy-supplied NullWriter instance
        context.setWriter(NullWriter.DEFAULT);
        context.setErrorWriter(NullWriter.DEFAULT);
        return context;
    }

    /**
     * Alters the static Thread Pool for this class. The internal queue will be the same size as the
     * maximum number of threads to operate on. This method is only callable once. Subsequent calls will be
     * rejected throwing an illegal state exception.
     *
     * @param newCoreSize size of the core thread pool generated. Must be greater than 0.
     * @param newMaxSize size of the maximum thread pool generated Must be greater than 0.
     */
    public static synchronized void configureThreadPool(int newCoreSize, int newMaxSize) {
        Reject.ifTrue(newCoreSize <= 0 || newMaxSize <= 0);

        if (threadPool == null) {
            threadPool = executorServiceFactory.createThreadPool(newCoreSize, newMaxSize, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(newMaxSize));
        } else {
            throw new IllegalStateException("Unable to re-configure the script module's thread pool once set up.");
        }

    }

    /**
     * This is used for overriding the thread pool for tests. Should not be used externally.
     *
     * @return The static threadPool.
     */
    ExecutorService getThreadPool() {
        return threadPool;
    }

}
