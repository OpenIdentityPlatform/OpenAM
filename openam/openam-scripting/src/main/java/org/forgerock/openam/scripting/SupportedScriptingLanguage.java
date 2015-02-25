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

import org.forgerock.guice.core.InjectorHolder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Standard scripting languages supported by OpenAM.
 *
 * @since 12.0.0
 */
public enum SupportedScriptingLanguage implements ScriptingLanguage {
    /**
     * Javascript/ECMAScript support based on the Rhino engine.
     */
    JAVASCRIPT {
        /**
         * {@inheritDoc}
         */
        public ScriptEngine getScriptEngine(final ScriptEngineManager scriptEngineManager) {
            return scriptEngineManager.getEngineByName(JAVASCRIPT_ENGINE_NAME);
        }

        /**
         * {@inheritDoc}
         */
        public ScriptValidator getScriptValidator() {
            return InjectorHolder.getInstance(ScriptValidator.class);
        }
    },

    /**
     * Groovy script support based on the main Groovy distribution.
     *
     * @see <a href="http://groovy.codehaus.org">Groovy</a>
     */
    GROOVY {
        public ScriptEngine getScriptEngine(final ScriptEngineManager scriptEngineManager) {
            return scriptEngineManager.getEngineByName(GROOVY_ENGINE_NAME);
        }

        public ScriptValidator getScriptValidator() {
            return InjectorHolder.getInstance(ScriptValidator.class);
        }

    }
    ;

    /**
     * JSR 223 engine name to use for Javascript support. We use a distributed Rhino copy on all
     * platforms for consistency across JVM versions and vendors.
     */
    public static final String JAVASCRIPT_ENGINE_NAME = "rhino";

    /**
     * JSR 223 engine name for Groovy support.
     */
    public static final String GROOVY_ENGINE_NAME = "groovy";
}
