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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.guice;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Servlet Context Listener to start the Guice initialisation when the Container starts up.
 *
 * @author Phill Cunnington
 */
public class GuiceInitialisationFilter implements ServletContextListener {

    /**
     * Tries to get the Guice Injector instance from the InjectorHolder, therefore kickstarting the Guice
     * initialisation.
     *
     * @param servletContextEvent {@inheritDoc}
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        InjectorHolder.getInjector();
    }

    /**
     * Not implemented to do anything.
     *
     * @param servletContextEvent {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
