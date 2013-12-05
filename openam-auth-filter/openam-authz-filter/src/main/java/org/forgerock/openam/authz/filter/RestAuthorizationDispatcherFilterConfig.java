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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Filter Config wrapper which adds in the Authorization Configurator class name as the "configurator" init parameter
 * name.
 *
 * @author Phill Cunnington
 * @since 12.0.0
 */
public class RestAuthorizationDispatcherFilterConfig implements FilterConfig {

    private static final String INIT_PARAM_AUTHZ_CONFIGURATOR = "configurator";

    private final FilterConfig filterConfig;
    private final String authzConfiguratorClassName;

    /**
     * Constructs an instance of the RestAuthorizationDispatcherFilterConfig.
     *
     * @param filterConfig The FilterConfig instance to wrap.
     * @param authzConfiguratorClassName The Authorization Configurator class name.
     */
    public RestAuthorizationDispatcherFilterConfig(final FilterConfig filterConfig,
            final String authzConfiguratorClassName) {
        this.filterConfig = filterConfig;
        this.authzConfiguratorClassName = authzConfiguratorClassName;
    }

    /**
     * {@inheritDoc}
     */
    public String getFilterName() {
        return filterConfig.getFilterName();
    }

    /**
     * {@inheritDoc}
     */
    public ServletContext getServletContext() {
        return filterConfig.getServletContext();
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the name parameter is, "configurator", then the AuthorizationConfigurator defined for this
     * RestAuthorizationDispatcherFilterConfig will be returned.
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     */
    public String getInitParameter(String name) {
        if (INIT_PARAM_AUTHZ_CONFIGURATOR.equals(name)) {
            return authzConfiguratorClassName;
        }
        return filterConfig.getInitParameter(name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enumeration will include the "configurator" defined for this RestAuthorizationDispatcherFilterConfig.
     *
     * @return {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getInitParameterNames() {
        List<String> names = Collections.list(filterConfig.getInitParameterNames());
        names.add(INIT_PARAM_AUTHZ_CONFIGURATOR);
        return Collections.enumeration(names);
    }
}
