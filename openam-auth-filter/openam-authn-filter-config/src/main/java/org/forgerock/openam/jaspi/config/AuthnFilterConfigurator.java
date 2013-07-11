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

package org.forgerock.openam.jaspi.config;

import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.jaspi.container.config.ConfigurationManager;
import org.forgerock.openam.jaspi.modules.session.LocalSSOTokenSessionModule;

import javax.security.auth.message.AuthException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Collections;

/**
 * Initialises the Authn Filter to protect REST endpoints in OpenAM.
 *
 * @author Phill Cunnington
 */
public class AuthnFilterConfigurator implements ServletContextListener {

    /**
     * Initialises the Authn Filter with the Local SSOToken Session Module and no Authentication Modules.
     *
     * @param servletContextEvent {@inheritDoc}
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        Configuration configuration = new Configuration();
        configuration.addAuthContext("all")
                .setSessionModule(LocalSSOTokenSessionModule.class, Collections.EMPTY_MAP)
                .done();

        configureAuthnFilter(configuration);
    }

    /**
     * Performs the configure call to the AuthnFilter.
     *
     * @param configuration The Configuration object containing the Session and Map of Authentication Modules to
     *                      configure the AuthnFilter to use.
     */
    protected void configureAuthnFilter(Configuration configuration) {

        try {
            ConfigurationManager.configure(configuration);
        } catch (AuthException e) {
            throw new RuntimeException("Error initialising the AuthnFilter", e);
        }
    }

    /**
     * Nothing to do here.
     *
     * @param servletContextEvent {@inheritDoc}
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}