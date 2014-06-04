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

import javax.inject.Singleton;
import javax.script.ScriptEngineManager;

/**
 * Guice configuration for OpenAM scripting-related components.
 */
@GuiceModule
public class ScriptingGuiceModule extends AbstractModule {

    @Override
    protected void configure() {

        // JSR 223 scripting engine manager singleton.
        bind(ScriptEngineManager.class).in(Singleton.class);

    }

    @Provides
    ScriptEngineManager getScriptEngineManager() {
        return new ScriptEngineManager();
    }

}
