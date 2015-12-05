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

package com.sun.identity.setup;

import javax.servlet.ServletContext;

import com.google.inject.PrivateModule;
import com.sun.identity.setup.AMSetupManager;

/**
 * Root Guice module for the independent injector for system startup.
 *
 * @since 13.0.0
 */
class SystemStartupGuiceModule extends PrivateModule {

    private final ServletContext servletContext;

    /**
     * Constructs a new SystemStartupGuiceModule.
     *
     * @param servletContext The {@link ServletContext}.
     */
    SystemStartupGuiceModule(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    protected void configure() {
        bind(AMSetupManager.class);
        bind(ServletContext.class).toInstance(servletContext);

        expose(AMSetupManager.class);
    }
}
