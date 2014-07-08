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

import org.forgerock.openam.scripting.factories.GroovyEngineFactory;
import org.forgerock.openam.scripting.factories.RhinoScriptEngineFactory;
import org.forgerock.openam.scripting.timeouts.ObservedContextFactory;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptEngineManager;
import java.util.concurrent.TimeUnit;

/**
 * A singleton implementation of the {@link ScriptEngineManager}, this is augmented to support
 * the needs to limit a generic script engine returned by the manager to a specific timeout.
 *
 * After constructing the manager, configureTimeout must be called if the scripts run via engines
 * returned from this manager are to abide by the intended timeout.
 */
@Singleton
public class StandardScriptEngineManager extends ScriptEngineManager {

    private final static int DEFAULT_TIMEOUT = 0; //no timeout

    private volatile int timeout = DEFAULT_TIMEOUT;

    /**
     * Constructs and configures the engine manager.
     */
    @Inject
    public StandardScriptEngineManager() {
        final RhinoScriptEngineFactory rhino = new RhinoScriptEngineFactory(new ObservedContextFactory(this), null);
        rhino.setOptimisationLevel(RhinoScriptEngineFactory.INTERPRETED);

        registerEngineName(SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME, rhino);
        registerEngineName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME, new GroovyEngineFactory());
    }

    /**
     * Configures the timeout for the scripting engine.
     *
     * We perform the required actions for all supported engines in this method to enable interruption/time limiting.
     * As the different engines approach this issue in individual ways, we keep a record of the timeout set accessible
     * in this class.
     *
     * The Rhino engine is configured with a custom ContextFactory
     * ({@link org.forgerock.openam.scripting.timeouts.ObservedContextFactory}) that has a reference to this script
     * manager instance. It reads the timeout from here to ensure it always sees the most up-to-date setting.
     *
     * The Groovy engine does not require any further set up past that done in {@link GroovyEngineFactory},
     * but does require that the timeout value is queryable.
     *
     * @param timeout the maximum number of seconds for which any given script should run. Must be >= 0.
     */
    public void configureTimeout(int timeout) {
        Reject.ifTrue(timeout < 0);

        this.timeout = timeout;
    }

    /**
     * Returns the length of the timeout configured for this engine manager in milliseconds. If the timeout is not in
     * effect or has not yet been configured, return 0.
     *
     * @return time in ms
     */
    public long getTimeoutMillis() {
        return TimeUnit.SECONDS.toMillis(getTimeoutSeconds());
    }

    /**
     * Returns the length of the timeout configured for this engine manager in seconds. If the timeout is not in
     * effect or has not yet been configured, return 0.
     *
     * @return time in s
     */
    public int getTimeoutSeconds() {
        return timeout > DEFAULT_TIMEOUT ? timeout : DEFAULT_TIMEOUT;
    }

}