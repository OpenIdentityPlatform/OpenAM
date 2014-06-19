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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Pluggable interface for adding support for particular scripting languages. Provides a single point to get hold of
 * script engines, validation objects, and so on.
 *
 * @since 12.0.0
 */
public interface ScriptingLanguage {

    /**
     * Gets a script engine for evaluating scripts in this scripting language.
     *
     * @param scriptEngineManager the JSR 223 script engine manager to use to retrieve the script engine.
     * @return the JSR 223 scripting engine for this language.
     */
    ScriptEngine getScriptEngine(ScriptEngineManager scriptEngineManager);

    /**
     * Gets a script validator for validating scripts in this scripting language.
     *
     * @return the validator for this scripting language.
     */
    ScriptValidator getScriptValidator();
}
