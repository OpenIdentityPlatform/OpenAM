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

package org.forgerock.openam.scripting.factories;

import org.forgerock.util.Reject;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.Reader;

/**
 * Applies a sandbox when executing groovy scripts. Delegates to an actual script engine after registering the
 * sandbox interceptor. Ensures sandbox is removed again after each script evaluation.
 */
final class SandboxedGroovyScriptEngine extends AbstractScriptEngine implements Compilable {
    private final GroovyEngineFactory factory;
    private final ScriptEngine realEngine;
    private final GroovyValueFilter sandbox;

    /**
     * Constructs the sandboxed script engine with the given parent factory, delegate script engine and sandbox.
     *
     * @param factory the parent script engine factory that created this engine.
     * @param realEngine the real script engine to use to evaluate scripts.
     * @param sandbox the sandbox to apply when executing a script.
     */
    SandboxedGroovyScriptEngine(final GroovyEngineFactory factory, final ScriptEngine realEngine,
                                final GroovyValueFilter sandbox) {
        Reject.ifNull(factory, realEngine, sandbox);
        this.realEngine = realEngine;
        this.sandbox = sandbox;
        this.factory = factory;
    }

    /**
     * Evaluates a script in the configured sandbox.
     *
     * {@inheritDoc}
     */
    @Override
    public Object eval(final String script, final ScriptContext scriptContext) throws ScriptException {
        sandbox.register();
        try {
            return realEngine.eval(script, scriptContext);
        } finally {
            sandbox.unregister();
        }
    }

    /**
     * Evaluates a script in the configured sandbox.
     *
     * {@inheritDoc}
     */
    @Override
    public Object eval(final Reader reader, final ScriptContext scriptContext) throws ScriptException {
        sandbox.register();
        try {
            return realEngine.eval(reader, scriptContext);
        } finally {
            sandbox.unregister();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bindings createBindings() {
        return realEngine.createBindings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    /**
     * Compiles the given script and ensures the compiled script is sandboxed.
     *
     * {@inheritDoc}
     */
    @Override
    public CompiledScript compile(final String script) throws ScriptException {
        return new SandboxedCompiledScript(((Compilable) realEngine).compile(script));
    }

    /**
     * Compiles the given script and ensures the compiled script is sandboxed.
     *
     * {@inheritDoc}
     */
    @Override
    public CompiledScript compile(final Reader reader) throws ScriptException {
        return new SandboxedCompiledScript(((Compilable) realEngine).compile(reader));
    }

    GroovyValueFilter getSandbox() {
        return sandbox;
    }

    /**
     * Compiled script wrapper that ensures that the sandbox is applied when executing the script.
     */
    private final class SandboxedCompiledScript extends CompiledScript {
        private final CompiledScript realCompiledScript;

        private SandboxedCompiledScript(final CompiledScript realCompiledScript) {
            Reject.ifNull(realCompiledScript);
            this.realCompiledScript = realCompiledScript;
        }

        /**
         * Evaluates the compiled script in the sandbox.
         *
         * {@inheritDoc}
         */
        @Override
        public Object eval(final ScriptContext scriptContext) throws ScriptException {
            sandbox.register();
            try {
                return realCompiledScript.eval(scriptContext);
            } finally {
                sandbox.unregister();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScriptEngine getEngine() {
            return SandboxedGroovyScriptEngine.this;
        }
    }
}
