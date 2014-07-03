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
package org.forgerock.openam.scripting.timeouts;

import java.util.concurrent.Callable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This callable contains an individual script's execution.
 *
 * @param <T> the return type of the engine's evaluation
 */
public class ScriptRunner<T> implements Callable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptRunner.class);

    private final ScriptObject script;
    private final ScriptContext context;
    private final ScriptEngine engine;

    /**
     * Constructs a new ScriptRunner, for use of evaluating scripts in their own thread.
     * No argument may be null.
     */
    public ScriptRunner(ScriptEngine engine, ScriptContext context, ScriptObject script) {
        Reject.ifNull(engine, context, script);
        this.engine = engine;
        this.context = context;
        this.script = script;
    }

    /**
     * Executes a script using the configured script engine.
     *
     * @return the result of the script, or null.
     */
    @Override @SuppressWarnings("unchecked")
    public T call() throws ScriptException {
        try {
            return (T) engine.eval(script.getScript(), context);
        } catch (ScriptException e) {
            LOGGER.debug("Engine quit during script evaluation. " + e.getMessage());
            throw e;
        }
    }
}
