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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.scripting.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.rest.ScriptExceptionMappingHandler;

/**
 *
 */
@GuiceModule
public class ScriptingRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        // Scripting configuration
        bind(new TypeLiteral<ExceptionMappingHandler<ScriptException, ResourceException>>() {})
                .to(ScriptExceptionMappingHandler.class);
    }
}
