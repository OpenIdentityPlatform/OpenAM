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
import org.mozilla.javascript.Script;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Compiled Rhino Javascript script.
 */
final class RhinoCompiledScript extends CompiledScript {
    private final RhinoScriptEngine engine;
    private final Script compiledScript;

    /**
     * Constructs the compiled script with the given script engine and compiled Rhino script object.
     *
     * @param engine the script engine to use to run the compiled script. Must not be null.
     * @param compiledScript the compiled script itself. Must not be null.
     */
    RhinoCompiledScript(final RhinoScriptEngine engine, final Script compiledScript) {
        Reject.ifNull(engine, compiledScript);
        this.engine = engine;
        this.compiledScript = compiledScript;
    }

    /**
     * {@inheritDoc}
     *
     * @param scriptContext The script context in which to evaluate the script. Cannot be null.
     */
    @Override
    public Object eval(final ScriptContext scriptContext) throws ScriptException {
        return engine.evalCompiled(compiledScript, scriptContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RhinoCompiledScript that = (RhinoCompiledScript) o;

        return compiledScript.equals(that.compiledScript) && engine.equals(that.engine);
    }

    @Override
    public int hashCode() {
        int result = engine.hashCode();
        result = 31 * result + compiledScript.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RhinoCompiledScript{engine=" + engine + ", compiledScript=" + compiledScript + '}';
    }
}
