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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Guice;

/**
 * <p>Servlet context listener which will initialise the {@link SystemStartupInjectorHolder} with
 * the system startup Guice injector.</p>
 *
 * This listener MUST be called before any other system startup listener, filter or servlet is
 * initialised.
 *
 * @since 13.0.0
 */
public class SystemStartupInjectorListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        SystemStartupInjectorHolder.initialise(
                Guice.createInjector(new SystemStartupGuiceModule(event.getServletContext())));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
