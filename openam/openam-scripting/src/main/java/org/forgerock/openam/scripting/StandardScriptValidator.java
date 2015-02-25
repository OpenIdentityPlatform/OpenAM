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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.syntax.SyntaxException;
import org.forgerock.util.Reject;
import org.mozilla.javascript.EvaluatorException;

/**
 * Validates scripts using the standard JSR 223 script engine framework.
 * The validator will retrieve the appropriate javax.script.ScriptEngine and establish
 * if it is an instance of javax.script.Compilable. The Compilable instance will then
 * be used to compile the script and convert the compilation errors to {@link ScriptError}s.
 *
 * @since 12.0.0
 */
public class StandardScriptValidator implements ScriptValidator {
    private final ScriptEngineManager scriptEngineManager;

    /**
     * Constructs the script validator using the given JSR 223 script engine manager instance.
     *
     * @param scriptEngineManager the script engine manager to use for creating script engines.
     * @throws java.lang.NullPointerException if scriptEngineManager is not specified.
     */
    @Inject
    public StandardScriptValidator(StandardScriptEngineManager scriptEngineManager) {
        Reject.ifNull(scriptEngineManager);
        this.scriptEngineManager = scriptEngineManager;
    }

    /**
     * {@inheritDoc}
     */
    public List<ScriptError> validateScript(ScriptObject script) {
        Reject.ifNull(script);
        final ScriptEngine scriptEngine = script.getLanguage().getScriptEngine(scriptEngineManager);
        final List<ScriptError> scriptErrorList = new ArrayList<ScriptError>();

        if (scriptEngine instanceof Compilable) {
            try {
                ((Compilable)scriptEngine).compile(script.getScript());
            } catch (ScriptException se) {
                // In an ideal world we would receive all errors in the script upon compilation,
                // but for now we can only produce them one at a time.
                scriptErrorList.addAll(getScriptErrors(script, se));
            }
        }

        return scriptErrorList;
    }

    /**
     * Convert from ScriptException to ScriptError.
     *
     * @param script the script that was validated.
     * @param se the error thrown by the validation task.
     * @return the converted script error.
     */
    private List<ScriptError> getScriptErrors(ScriptObject script, ScriptException se) {
        final List<ScriptError> scriptErrorList = new ArrayList<ScriptError>();
        final Throwable cause = se.getCause();

        // Neither the JavaScript nor the Groovy implementation of JSR 223 populates the ScriptException
        // with useful information so we have to retrieve it from the cause exception.
        if (cause instanceof EvaluatorException) {
            final EvaluatorException ee = (EvaluatorException)cause;
            final ScriptError error = new ScriptError();
            error.setScriptName(script.getName());
            error.setMessage(ee.details());
            error.setLineNumber(ee.lineNumber());
            error.setColumnNumber(ee.columnNumber());
            scriptErrorList.add(error);
        } else if (cause instanceof MultipleCompilationErrorsException) {
            ErrorCollector errorCollector = ((MultipleCompilationErrorsException)cause).getErrorCollector();
            for (int i = 0; i < errorCollector.getErrorCount(); i++) {
                final SyntaxException syntaxException = errorCollector.getSyntaxError(i);
                final ScriptError error = new ScriptError();
                error.setScriptName(script.getName());
                error.setMessage(syntaxException.getOriginalMessage());
                error.setLineNumber(syntaxException.getLine());
                error.setColumnNumber(syntaxException.getStartColumn());
                scriptErrorList.add(error);
            }
        } else {
            final ScriptError error = new ScriptError();
            error.setScriptName(script.getName());
            error.setMessage(se.getMessage());
            error.setLineNumber(se.getLineNumber());
            error.setColumnNumber(se.getColumnNumber());
            scriptErrorList.add(error);
        }
        return scriptErrorList;
    }
}
