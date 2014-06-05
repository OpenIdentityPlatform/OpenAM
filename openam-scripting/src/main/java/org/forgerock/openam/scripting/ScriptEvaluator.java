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

import javax.script.Bindings;
import javax.script.ScriptException;

/**
 * Common component for evaluating scripts in one of the supported scripting languages.
 *
 * @since 12.0.0
 */
public interface ScriptEvaluator {

    /**
     * Evaluates the given script object using an appropriate script engine for the language of the script. Any
     * bindings associated with the script will be passed to the engine when the script is evaluated.
     * <p/>
     * There are three scope levels at which variables can be set:
     * <ol>
     *     <li><em>Global</em> variables are set using the {@link #bindVariableInGlobalScope(String, Object)} method,
     *     which makes the objects visible to all script engines. This is most appropriate for stateless API objects
     *     such as HTTP clients and other basic functionality.</li>
     *     <li><em>Script</em> variables are set in the bindings attached to a {@link ScriptObject}.</li>
     *     <li><em>Parameter</em> variables are set by the bindings parameter to this method.</li>
     * </ol>
     * The three scopes above are listed in reverse order of precedence. If a variable of a given name exists in all
     * three scopes, then the evaluator will use the parameter variable, overriding (hiding) the same-named variable in
     * the other scopes. Likewise, script variables will hide any same-named variables in the global scope.
     *
     * @param script the script to evaluate.
     * @param bindings any additional variable bindings to set before running the script.
     * @param <T> the type of result expected from the script.
     * @return the result of evaluating the script, or null if no result produced.
     * @throws ScriptException if an error occurs evaluating the script.
     * @throws ClassCastException if the result is not of the expected type.
     * @throws IllegalStateException if no scripting engine can be located for the given scripting language.
     */
    <T> T evaluateScript(ScriptObject script, Bindings bindings) throws ScriptException;

    /**
     * Binds the given object to the given variable name in the global scope of all scripts evaluated via this
     * script evaluator. This is intended to be used to bind common API components such as HTTP clients etc that should
     * be available to all scripts. For per-script state to initialise, use the bindings element on the script object
     * itself.
     *
     * @param name the name of the variable to bind the object to.
     * @param object the object to bind into the global scope of all scripts.
     * @throws java.lang.NullPointerException if either name or object is empty.
     * @throws java.lang.IllegalArgumentException if the name is an empty string.
     */
    void bindVariableInGlobalScope(String name, Object object);
}
