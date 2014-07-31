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
     * {@inheritDoc}
     */
    @Override
    public void bindVariableInGlobalScope(final String name, final Object object) {
        Reject.ifNull(name, object);
        scriptEngineManager.put(name, object);
    }

    /**
     * Evaluates scripts immediately using the configured JSR-223 script engine manager. This implementation should
     * be wrapped with a {@link org.forgerock.openam.scripting.ThreadPoolScriptEvaluator} if script interruption or
     * timeouts are required.
     *
     * @param script the script to evaluate.
     * @param bindings any additional variable bindings to set before running the script.
     * @param <T> the type of result returned from the script.
     * @return the result of evaluating the script.
     * @throws ScriptException if an error occurs in script execution.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluateScript(final ScriptObject script, final Bindings bindings) throws ScriptException {
        Reject.ifNull(script);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Evaluating script: " + script);
        }

        final ScriptEngine engine = getScriptEngineFor(script);
        final Bindings variableBindings = mergeBindings(script.getBindings(), bindings);
        final ScriptContext context = buildScriptContext(variableBindings);

        return (T) engine.eval(script.getScript(), context);
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

}
