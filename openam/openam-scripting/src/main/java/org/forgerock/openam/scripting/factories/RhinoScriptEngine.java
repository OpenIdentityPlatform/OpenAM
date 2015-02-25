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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * JSR-223 wrapped Rhino script engine. Evaluates scripts using the {@link org.mozilla.javascript.ContextFactory}
 * associated with the parent {@link org.forgerock.openam.scripting.factories.RhinoScriptEngineFactory}.
 *
 * @see <a href="https://www.jcp.org/en/jsr/detail?id=223">JSR-223: Scripting for the Java Platform</a>
 */
class RhinoScriptEngine extends AbstractScriptEngine implements Compilable {
    private final RhinoScriptEngineFactory factory;

    /**
     * Constructs a script engine with the given parent engine factory.
     *
     * @param factory the parent script engine factory. Must not be null.
     */
    RhinoScriptEngine(RhinoScriptEngineFactory factory) {
        Reject.ifNull(factory);
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object eval(final String source, final ScriptContext scriptContext) throws ScriptException {
        return eval(new StringReader(source), scriptContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object eval(final Reader reader, final ScriptContext scriptContext) throws ScriptException {
        Reject.ifNull(reader, scriptContext);
        Object result = null;
        final Context context = factory.getContext();
        try {
            final Scriptable scope = getScope(context, scriptContext);

            final String filename = getFilename(scriptContext);
            result = context.evaluateReader(scope, reader, filename, 1, null);

        } catch (RhinoException ex) {
            throw convertException(ex);
        } catch (IOException ex) {
            throw new ScriptException(ex);
        } finally {
            factory.releaseContext(context);
        }

        return result;
    }

    /**
     * Evaluates a pre-compiled script against this script engine. This should only be called by
     * {@link org.forgerock.openam.scripting.factories.RhinoCompiledScript#eval(javax.script.ScriptContext)}.
     *
     * @param compiledScript The compiled script. Must not be null.
     * @param scriptContext The JSR 223 script context. Must not be null.
     * @return the result of evaluating the compiled script.
     * @throws ScriptException if an error occurs during script execution.
     */
    Object evalCompiled(final Script compiledScript, final ScriptContext scriptContext) throws ScriptException {
        Reject.ifNull(compiledScript, scriptContext);

        Object result = null;
        final Context context = factory.getContext();
        try {
            final Scriptable scope = getScope(context, scriptContext);
            result = compiledScript.exec(context, scope);
        } catch (RhinoException ex) {
            throw convertException(ex);
        } finally {
            factory.releaseContext(context);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RhinoScriptEngineFactory getFactory() {
        return factory;
    }

    /**
     * Determines the filename to use for reporting errors in a script. Searches for the
     * {@link javax.script.ScriptEngine#FILENAME} attribute in either the script context engine scope bindings, or in
     * the bindings on this engine itself. If both of these are null, then returns the string
     * {@literal "<Unknown source>"}.
     *
     * @param scriptContext The script context. May be null.
     * @return the filename of the script being evaluated or {@literal "<Unknown source>"} if not known.
     */
    private String getFilename(ScriptContext scriptContext) {
        String filename = null;
        if (scriptContext != null) {
            Bindings engineBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            if (engineBindings != null) {
                filename = (String) engineBindings.get(ScriptEngine.FILENAME);
            }
        }
        if (filename == null) {
            filename = (String) get(ScriptEngine.FILENAME);
        }
        if (filename == null) {
            filename = "<Unknown source>";
        }
        return filename;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompiledScript compile(final String source) throws ScriptException {
        return compile(new StringReader(source));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompiledScript compile(final Reader reader) throws ScriptException {
        Reject.ifNull(reader);

        final Context context = factory.getContext();
        try {
            // Use configured ScriptContext from parent class, if specified
            final String filename = getFilename(getContext());
            final Script compiledScript = context.compileReader(reader, filename, 1, null);

            return new RhinoCompiledScript(this, compiledScript);
        } catch (RhinoException ex) {
            throw convertException(ex);
        } catch (IOException ex) {
            throw new ScriptException(ex);
        } finally {
            factory.releaseContext(context);
        }
    }

    /**
     * Builds a Rhino variable scope that includes all of the scopes defined in the given script context as well as
     * the standard Rhino top-level environment. Also binds the variable {@code context} to point to the JSR 223
     * ScriptContext object, as per the JSR 223 spec.
     *
     * @param context the Rhino context to build the scope for.
     * @param scriptContext the JSR 223 script context.
     * @return a Rhino scope containing the given ScriptContext bindings and standard Rhino top-level bindings.
     */
    private Scriptable getScope(final Context context, final ScriptContext scriptContext) {
        final Scriptable scope = new ScriptContextScope(scriptContext);
        final ScriptableObject topLevel = context.initStandardObjects();
        scope.setPrototype(topLevel);
        scope.put("context", scope, scriptContext);
        return scope;
    }

    /**
     * Converts a RhinoException into an equivalent JSR 223 ScriptException, copying across source file and line
     * number information. The RhinoException will be initialised as the cause of the ScriptException.
     *
     * @param ex the RhinoException to convert.
     * @return An equivalent JSR 223 ScriptException.
     */
    private ScriptException convertException(RhinoException ex) {
        final ScriptException se = new ScriptException(ex.getMessage(), ex.sourceName(), ex.lineNumber(),
                ex.columnNumber());
        se.initCause(ex);
        return se;
    }

    @Override
    public String toString() {
        return "RhinoScriptEngine{factory=" + factory + '}';
    }
}