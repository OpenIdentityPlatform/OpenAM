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
import sun.org.mozilla.javascript.internal.RhinoException;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates scripts using the standard JSR 223 script engine framework.
 *
 * @since 12.0.0
 */
public class StandardScriptValidator implements ScriptValidator {
    private final ScriptEngineManager scriptEngineManager;

    /**
     * Constructs the script validator using the given JSR 223 script engine manager instance.
     *
     * @param scriptEngineManager the script engine manager to use for creating script engines.
     */
    @Inject
    public StandardScriptValidator(ScriptEngineManager scriptEngineManager) {
        Reject.ifNull(scriptEngineManager);
        this.scriptEngineManager = scriptEngineManager;
    }

    /**
     * {@inheritDoc}
     */
    public List<ScriptError> validateScript(ScriptObject script) {
        Reject.ifNull(script);
        final ScriptEngine scriptEngine = script.getLanguage().getScriptEngine(scriptEngineManager);
        final ArrayList<ScriptError> scriptErrorList = new ArrayList<ScriptError>();

        if (scriptEngine instanceof Compilable) {
            try {
                ((Compilable)scriptEngine).compile(script.getScript());
            } catch (ScriptException se) {
                // In an ideal world we would receive all errors in the script upon compilation,
                // but for now we can only produce them one at a time.
                scriptErrorList.add(getScriptError(script, se));
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
    private ScriptError getScriptError(ScriptObject script, ScriptException se) {
        final ScriptError error = new ScriptError();
        error.setScriptName(script.getName());

        // For some reason the JavaScript implementation of JSR 223 does not populate the ScriptException
        // with the information from the RhinoException so we have to check for that here.
        if (se.getCause() instanceof RhinoException) {
            RhinoException re = (RhinoException)se.getCause();
            error.setMessage(re.getMessage());
            error.setLineNumber(re.lineNumber());
            error.setColumnNumber(re.columnNumber());
        } else {
            error.setMessage(se.getMessage());
            error.setLineNumber(se.getLineNumber());
            error.setColumnNumber(se.getColumnNumber());
        }
        return error;
    }
}
