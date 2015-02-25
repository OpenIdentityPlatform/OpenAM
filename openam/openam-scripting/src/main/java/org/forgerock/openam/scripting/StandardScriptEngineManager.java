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
import org.forgerock.openam.scripting.sandbox.GroovySandboxValueFilter;
import org.forgerock.openam.scripting.sandbox.RhinoSandboxClassShutter;
import org.forgerock.openam.scripting.timeouts.ObservedContextFactory;
import org.forgerock.util.Reject;
import org.mozilla.javascript.ClassShutter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptEngineManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A singleton implementation of the {@link ScriptEngineManager}, this is augmented to support
 * a publish/subscribe (observer) pattern for propagating configuration changes to individual language implementations
 * and other listeners. This is used to adjust sandboxing and thread pool implementations in response to application
 * configuration changes.
 */
@Singleton
public final class StandardScriptEngineManager extends ScriptEngineManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardScriptEngineManager.class);

    /**
     * Default configuration. Uses system security manager, no script timeouts, and a sandbox configuration that
     * disallows access to all Java classes.
     */
    private static final ScriptEngineConfiguration DEFAULT_CONFIGURATION =
            ScriptEngineConfiguration.builder()
                .withTimeout(ScriptEngineConfiguration.NO_TIMEOUT, TimeUnit.SECONDS)
                .withSystemSecurityManager()
                .build();

    /**
     * The current configuration. Configuration objects are themselves immutable and updates of this variable are
     * atomic, ensuring that components always see a consistent configuration state.
     */
    private volatile ScriptEngineConfiguration configuration = DEFAULT_CONFIGURATION;

    /**
     * Set of listeners registered to receive updates whenever the configuration changes. Synchronized to ensure a
     * consistent view of the set is seen when publishing events.
     */
    private final Set<ConfigurationListener> listeners
            = Collections.synchronizedSet(new HashSet<ConfigurationListener>());

    /**
     * Constructs and configures the engine manager.
     */
    @Inject
    public StandardScriptEngineManager() {

        // Configure Rhino JS and Groovy script engine factories with sandboxing
        final RhinoScriptEngineFactory rhino = new RhinoScriptEngineFactory(new ObservedContextFactory(this));
        rhino.setOptimisationLevel(RhinoScriptEngineFactory.INTERPRETED);
        // Set an empty sandbox for now - will deny access to everything. App will set correct configuration before use.
        final ClassShutter sandbox = new RhinoSandboxClassShutter(System.getSecurityManager(),
                Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList());
        rhino.setClassShutter(sandbox);

        final GroovyEngineFactory groovy = new GroovyEngineFactory();
        groovy.setSandbox(new GroovySandboxValueFilter(sandbox));

        // Add a listener to configure sandbox from configuration changes
        addConfigurationListener(new SandboxConfigurationListener(rhino, groovy));

        registerEngineName(SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME, rhino);
        registerEngineName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME, groovy);

        setConfiguration(DEFAULT_CONFIGURATION);
    }

    /**
     * Sets the configuration to use when evaluating any scripts. This method will synchronously publish the new
     * configuration to all registered listeners and atomically update the configuration stored in this manager.
     *
     * @param newConfiguration the new configuration to use.
     */
    public void setConfiguration(ScriptEngineConfiguration newConfiguration) {
        Reject.ifNull(newConfiguration);

        // Broadcast change to all registered listeners
        synchronized (listeners) {
            // Update the configuration within the synchronized block to ensure that the latest configuration always
            // reflects the last configuration that was broadcast to listeners.
            this.configuration = newConfiguration;

            for (final ConfigurationListener listener : listeners) {
                try {
                    listener.onConfigurationChange(newConfiguration);
                } catch (RuntimeException ex) {
                    LOGGER.error("Script configuration listener failed with exception", ex);
                }
            }
        }
    }

    /**
     * Get the current script engine configuration in use for evaluating scripts. The configuration object is
     * immutable and is guaranteed to be a consistent reflection of the last configuration that was set. Calls to
     * {@link #setConfiguration(ScriptEngineConfiguration)} always 'happen-before' any subsequent calls to this method.
     *
     * @return the current configuration.
     */
    public ScriptEngineConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Adds an observer to be called whenever the current script engine configuration changes. The listener will be
     * called immediately after the configuration is updated and is guaranteed to be passed a consistent (immutable)
     * object representing the new configuration. If the configuration is updated by multiple threads in quick
     * succession, then the final configuration is guaranteed to be the configuration that was most recently published
     * to any subscribers.
     *
     * @param listener the configuration listener to register.
     * @return true if the listener was registered, or false if it was already registered.
     */
    public boolean addConfigurationListener(ConfigurationListener listener) {
        Reject.ifNull(listener);
        // Call the listener immediately with the current configuration
        listener.onConfigurationChange(getConfiguration());
        return this.listeners.add(listener);
    }

    /**
     * Removes a configuration listener from the list of active subscribers. No subsequent configuration changes will
     * be published to this listener.
     *
     * @param listener the listener to remove.
     * @return true if the listener was removed, or false if it was not previously registered.
     */
    public boolean removeConfigurationListener(ConfigurationListener listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Observer pattern interface for listening to changes in the script engine configuration.
     */
    public interface ConfigurationListener {
        /**
         * Indicates that the script engine configuration has changed and that the listener should update settings
         * appropriately.
         *
         * @param newConfiguration the new script engine configuration. Never null.
         */
        void onConfigurationChange(ScriptEngineConfiguration newConfiguration);
    }

    /**
     * Listens for configuration changes and configures the Rhino and Groovy sandbox to match current values.
     */
    private static final class SandboxConfigurationListener implements ConfigurationListener {
        private final RhinoScriptEngineFactory rhinoScriptEngineFactory;
        private final GroovyEngineFactory groovyEngineFactory;

        private SandboxConfigurationListener(final RhinoScriptEngineFactory rhinoScriptEngineFactory,
                                             final GroovyEngineFactory groovy) {
            Reject.ifNull(rhinoScriptEngineFactory, groovy);
            this.rhinoScriptEngineFactory = rhinoScriptEngineFactory;
            this.groovyEngineFactory = groovy;
        }

        /**
         * Updates the Rhino and Groovy sandbox implementations to match the current configuration entries.
         *
         * @param newConfiguration the new script engine configuration. Never null.
         */
        @Override
        public void onConfigurationChange(final ScriptEngineConfiguration newConfiguration) {
            LOGGER.debug("Configuring sandbox: %s", newConfiguration);

            final ClassShutter sandbox = new RhinoSandboxClassShutter(
                    newConfiguration.getSecurityManager(),
                    newConfiguration.getClassWhiteList(),
                    newConfiguration.getClassBlackList());

            rhinoScriptEngineFactory.setClassShutter(sandbox);
            groovyEngineFactory.setSandbox(new GroovySandboxValueFilter(sandbox));
        }
    }
}