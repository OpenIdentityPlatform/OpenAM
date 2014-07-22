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

package org.forgerock.openam.scripting.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptValidator;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.StandardScriptValidator;
import org.forgerock.openam.scripting.ThreadPoolScriptEvaluator;
import org.forgerock.openam.shared.concurrency.ExecutorServiceFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Guice configuration for OpenAM scripting-related components.
 */
@GuiceModule
public class ScriptingGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ScriptValidator.class).to(StandardScriptValidator.class);
    }

    @Provides @Singleton @Inject
    public ScriptEvaluator getScriptEvaluator(ExecutorServiceFactory executorServiceFactory,
                                              StandardScriptEngineManager scriptEngineManager) {
        return new ThreadPoolScriptEvaluator(scriptEngineManager, executorServiceFactory,
                new StandardScriptEvaluator(scriptEngineManager));
    }
}
