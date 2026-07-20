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
* Copyright 2014-2015 ForgeRock AS.
* Portions copyright 2026 3A Systems, LLC
 */
package org.forgerock.openam.scripting.factories;

import groovy.lang.GroovyClassLoader;
import groovy.transform.ThreadInterrupt;
import javax.script.ScriptEngine;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.forgerock.util.Reject;
import org.kohsuke.groovy.sandbox.GroovyValueFilter;
import org.kohsuke.groovy.sandbox.SandboxTransformer;

/**
 * This factory overrides the default getScriptEngine implementation, and ensures that we use the
 * AST Transformation Customizer to provide interruption checks at the beginning of closures, loops, etc.
 * in the executed script. This allows us to monitor the thread running the script and trigger an
 * interrupt if the script's allowed running time is up.
 */
public class GroovyEngineFactory extends GroovyScriptEngineFactory {
    private volatile GroovyValueFilter sandbox;

    private final GroovyScriptEngineImpl groovyScriptEngine;

    static {
        // Disable Grape/Ivy dependency resolution globally. The runtime sandbox cannot intercept @Grab because it is a
        // compile-time AST transform that fetches and loads arbitrary artefacts; RejectASTTransformsCustomizer rejects
        // the annotation, and this is belt-and-suspenders in case any other compilation path is added.
        System.setProperty("groovy.grape.enable", "false");
    }

    public GroovyEngineFactory() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        // Apply sandbox before any other customisation, otherwise sandbox will be applied to implementation details.
        compilerConfig.addCompilationCustomizers(new SandboxTransformer());
        // The runtime sandbox above only governs script execution. Reject compile-time AST transforms such as
        // @ASTTest and @Grab, which would otherwise run arbitrary code during compilation (i.e. on validate/create),
        // escaping the sandbox entirely. See RejectASTTransformsCustomizer for details.
        compilerConfig.addCompilationCustomizers(new RejectASTTransformsCustomizer());
        compilerConfig.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
        // Defense in depth: enable the indirect-import check so that fully-qualified references cannot bypass any
        // import restrictions. The runtime GroovyValueFilter remains the primary control over what scripts may access.
        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        secureASTCustomizer.setIndirectImportCheckEnabled(true);
        compilerConfig.addCompilationCustomizers(secureASTCustomizer);
        GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(),
                compilerConfig);
        groovyScriptEngine = new GroovyScriptEngineImpl(classLoader);
    }

    /**
     * Instantiates and returns an instance of {@link GroovyScriptEngineImpl} passing in
     * a new {@link GroovyClassLoader} with an AST transformation customizer that will ensure
     * that interrupt checks are inserted into the compiled code (at the start of closures, loops, etc.).
     *
     * Scripts run through engines provided by this function will be interruptable.
     *
     * @return an interruptable groovy script engine implementation.
     */
    @Override
    public ScriptEngine getScriptEngine() {
        return new SandboxedGroovyScriptEngine(this, groovyScriptEngine, sandbox);
    }

    /**
     * Sets the Groovy value filter to use for sandboxing scripts. The filter is called every time an
     * object is accessed by the script to verify that the access is allowed.
     *
     * @param sandbox the new sandbox to use.
     */
    public void setSandbox(final GroovyValueFilter sandbox) {
        Reject.ifNull(sandbox);
        this.sandbox = sandbox;
    }
}
