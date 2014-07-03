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

import groovy.lang.GroovyClassLoader;
import groovy.transform.ThreadInterrupt;
import javax.script.ScriptEngine;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

/**
 * This factory overrides the default getScriptEngine implementation, and ensures that we use the
 * AST Transformation Customizer to provide interruption checks at the beginning of closures, loops, etc.
 * in the executed script. This allows us to monitor the thread running the script and trigger an
 * interrupt if the script's allowed running time is up.
 */
public class GroovyEngineFactory extends GroovyScriptEngineFactory {

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

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
        GroovyClassLoader classLoader =
                new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfig);

        return new GroovyScriptEngineImpl(classLoader);
    }

}
