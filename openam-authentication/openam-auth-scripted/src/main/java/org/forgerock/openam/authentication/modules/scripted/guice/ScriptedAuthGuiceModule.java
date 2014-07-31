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

package org.forgerock.openam.authentication.modules.scripted.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.authentication.modules.scripted.Scripted;
import org.forgerock.openam.authentication.modules.scripted.ScriptedAuthConfigurator;
import org.forgerock.openam.scripting.ScriptEngineConfiguration;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.ThreadPoolScriptEvaluator;
import org.forgerock.util.thread.ExecutorServiceFactory;

/**
 * Guice configuration for scripted authentication.
 */
@GuiceModule
public class ScriptedAuthGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptedAuthConfigurator.class).in(Singleton.class);
    }

    /**
     * Creates the script evaluator to use for evaluating scripted auth module scripts. The evaluator returned uses a
     * thread pool to evaluate scripts (supporting script interruption), delegating to a sandboxed script evaluator.
     *
     * @param scriptEngineManager the script engine manager to use.
     * @param executorServiceFactory the factory for creating managed thread pools for script execution.
     * @param configurator the service configuration listener.
     * @return an appropriately configured script evaluator for use with scripted authentication.
     */
    @Provides @Singleton @Inject @Named(Scripted.SCRIPT_MODULE_NAME)
    public ScriptEvaluator getScriptEvaluator(final StandardScriptEngineManager scriptEngineManager,
                                              final ExecutorServiceFactory executorServiceFactory,
                                              final ScriptedAuthConfigurator configurator) {

        // Ensure configuration is up to date with service settings
        configurator.registerServiceListener();

        final ScriptEngineConfiguration configuration = scriptEngineManager.getConfiguration();

        return new ThreadPoolScriptEvaluator(scriptEngineManager,
                executorServiceFactory.createThreadPool(
                        configuration.getThreadPoolCoreSize(),
                        configuration.getThreadPoolMaxSize(),
                        configuration.getThreadPoolIdleTimeoutSeconds(),
                        TimeUnit.SECONDS,
                        getThreadPoolQueue(configuration.getThreadPoolQueueSize())
                ),
                new StandardScriptEvaluator(scriptEngineManager));
    }

    private BlockingQueue<Runnable> getThreadPoolQueue(final int size) {
        return size == ScriptEngineConfiguration.UNBOUNDED_QUEUE_SIZE
                ? new LinkedBlockingQueue<Runnable>()
                : new LinkedBlockingQueue<Runnable>(size);
    }
}
