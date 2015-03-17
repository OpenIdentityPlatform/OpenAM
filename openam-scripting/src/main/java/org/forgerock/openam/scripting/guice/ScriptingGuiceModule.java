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
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.scripting.ScriptValidator;
import org.forgerock.openam.scripting.StandardScriptValidator;
import org.forgerock.openam.scripting.datastore.ScriptConfigurationDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStore;
import org.forgerock.openam.scripting.datastore.ScriptingDataStoreFactory;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptConfigurationService;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice configuration for OpenAM scripting-related components.
 */
@GuiceModule
public class ScriptingGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ScriptValidator.class).to(StandardScriptValidator.class);

        bind(Logger.class).annotatedWith(Names.named("ScriptLogger")).toInstance(LoggerFactory.getLogger("Scripting"));

        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<ScriptingService<ScriptConfiguration>>() {},
                        ScriptConfigurationService.class)
                .build(new TypeLiteral<ScriptingServiceFactory<ScriptConfiguration>>() {}));

        install(new FactoryModuleBuilder()
                .implement(new TypeLiteral<ScriptingDataStore<ScriptConfiguration>>() {},
                        ScriptConfigurationDataStore.class)
                .build(new TypeLiteral<ScriptingDataStoreFactory<ScriptConfiguration>>() {}));
    }

}
