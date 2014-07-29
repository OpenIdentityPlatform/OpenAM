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

import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.util.Reject;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.List;

import static org.forgerock.util.Utils.joinAsString;

/**
 * JSR-223 wrapper around a Rhino {@link org.mozilla.javascript.ContextFactory}. Uses the configured
 * {@link org.mozilla.javascript.ContextFactory} to compile and evaluate scripts and so does not require use of the
 * global context factory.
 *
 * @see <a href="https://www.jcp.org/en/jsr/detail?id=223">JSR-223: Scripting for the Java Platform</a>
 */
public class RhinoScriptEngineFactory implements ScriptEngineFactory {

    /**
     * Optimisation level constant indicating that scripts should be fully interpreted and not compiled.
     */
    public static final int INTERPRETED = -1;
    private final ContextFactory contextFactory;

    private final String version;
    private final String languageVersion;

    private volatile ClassShutter classShutter;
    private volatile int optimisationLevel = INTERPRETED;

    /**
     * Constructs a script engine factory using the given context factory and class-shutter. If non-null, the given
     * class-shutter will be applied to all context objects created by this script engine factory, providing a means
     * of sandboxing scripts. This constructor will interrogate the given context factory to determine the
     * implementation and language version provided.
     *
     * @param contextFactory the Rhino context factory. May not be null.
     */
    public RhinoScriptEngineFactory(ContextFactory contextFactory) {
        Reject.ifNull(contextFactory);
        this.contextFactory = contextFactory;

        // Determine provided language/engine version
        Context context = contextFactory.enterContext();
        try {
            this.version = context.getImplementationVersion();
            this.languageVersion = String.valueOf(context.getLanguageVersion());
        } finally {
            Context.exit();
        }
    }

    /**
     * Constructs the script engine factory with a fresh context factory and no class-shutter.
     * @see #RhinoScriptEngineFactory(org.mozilla.javascript.ContextFactory)
     */
    public RhinoScriptEngineFactory() {
        this(new ContextFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEngineName() {
        return SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEngineVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getExtensions() {
        return Arrays.asList("js");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getMimeTypes() {
        return Arrays.asList("text/javascript", "application/javascript");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getNames() {
        return Arrays.asList(getEngineName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguageName() {
        return "javascript";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguageVersion() {
        return languageVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameter(final String paramName) {
        if (ScriptEngine.LANGUAGE.equals(paramName)) {
            return getLanguageName();
        } else if (ScriptEngine.ENGINE.equals(paramName)) {
            return getEngineName();
        } else if (ScriptEngine.NAME.equals(paramName)) {
            return getLanguageName();
        } else if (ScriptEngine.LANGUAGE_VERSION.equals(paramName)) {
            return getLanguageVersion();
        } else if (ScriptEngine.ENGINE_VERSION.equals(paramName)) {
            return getEngineVersion();
        } else {
            // Unspecified (includes THREADING attribute - indicates not thread safe)
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethodCallSyntax(final String object, final String method, final String... args) {
        return String.format("%s.%s(%s)", object, method, joinAsString(", ", (Object[]) args));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputStatement(final String s) {
        return String.format("logger.message(%s)", s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProgram(final String... statements) {
        return joinAsString(";\n", (Object[]) statements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptEngine getScriptEngine() {
        return new RhinoScriptEngine(this);
    }

    /**
     * Gets a fresh Rhino {@link org.mozilla.javascript.Context} object by calling
     * {@link org.mozilla.javascript.ContextFactory#enterContext()}. The returned context object will be configured
     * with any specified class shutter and optimisation level settings before being returned. Each call to this
     * method should be accompanied by a call to {@link #releaseContext(org.mozilla.javascript.Context)} when the
     * context is no longer required. It is recommended to use a try-finally pattern for this, like:
     * <pre>
     *     final Context context = factory.getContext();
     *     try {
     *         // Use context
     *     } finally {
     *         factory.releaseContext(context);
     *     }
     * </pre>
     *
     * @return the freshly configured context object.
     */
    Context getContext() {
        final Context context = contextFactory.enterContext();
        context.setOptimizationLevel(optimisationLevel);
        final ClassShutter sandbox = classShutter;
        if (sandbox != null) {
            context.setClassShutter(sandbox);
        }
        return context;
    }

    /**
     * Releases the given context object after use.
     *
     * @param context the context to release.
     */
    void releaseContext(Context context) {
        Context.exit();
    }

    /**
     * Sets the optimisation level to use for context objects created by this script engine. Use
     * {@link #INTERPRETED} to disable all optimisations and use a purely interpreted script engine.
     *
     * @param optimisationLevel the optimisation level to use.
     */
    public void setOptimisationLevel(final int optimisationLevel) {
        this.optimisationLevel = optimisationLevel;
    }

    /**
     * Sets the class shutter to be used to sandbox scripts running in this engine.
     *
     * @param classShutter the class-shutter to use. May be null to disable sandboxing.
     */
    public void setClassShutter(final ClassShutter classShutter) {
        this.classShutter = classShutter;
    }

    @Override
    public String toString() {
        return "RhinoScriptEngineFactory{" +
                "contextFactory=" + contextFactory +
                ", version='" + version + '\'' +
                ", languageVersion='" + languageVersion + '\'' +
                ", classShutter=" + classShutter +
                ", optimisationLevel=" + optimisationLevel +
                '}';
    }
}
