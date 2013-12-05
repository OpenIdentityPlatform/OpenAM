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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.AuthZFilter;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.forgerockrest.RestDispatcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

/**
 * Dispatcher for Rest Resource authorization.
 * <p>
 * As realms, users, groups and agents REST resources are dynamically routed based on the realm they are in, means
 * that the authorization filter cannot be applied specifically at, ie /json/realms/{realm}, as the realm being accessed
 * might be under a sub-realm, ie /json/subrealm/realms/{realm}.
 * <p>
 * Because of this this class contains the mapping logic for the type of resource to the authroization to be applied to
 * it.
 *
 * @author Phill Cunnington
 * @since 12.0.0
 */
public class RestAuthorizationDispatcherFilter implements Filter {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private static final String INIT_PARAM_REALMS_AUTHZ_CONFIGURATOR = "realmsAuthzConfigurator";
    private static final String INIT_PARAM_USERS_AUTHZ_CONFIGURATOR = "usersAuthzConfigurator";
    private static final String INIT_PARAM_GROUPS_AUTHZ_CONFIGURATOR = "groupsAuthzConfigurator";
    private static final String INIT_PARAM_AGENTS_AUTHZ_CONFIGURATOR = "agentsAuthzConfigurator";

    private final RestDispatcher restDispatcher;
    private final AuthZFilter authZFilter;
    private FilterConfig filterConfig;

    private String realmsAuthzConfiguratorClassName;
    private String usersAuthzConfiguratorClassName;
    private String groupsAuthzConfiguratorClassName;
    private String agentsAuthzConfiguratorClassName;

    /**
     * Constructs an instance of the RestAuthorizationDispatcherFilter.
     */
    public RestAuthorizationDispatcherFilter() {
        this.restDispatcher = RestDispatcher.getInstance();
        authZFilter = new AuthZFilter();
    }

    /**
     * Constructs an instance of the RestAuthorizationDispatcherFilter.
     * <p>
     * Used for test purposes.
     *
     * @param authZFilter An instance of the AuthZFilter.
     */
    RestAuthorizationDispatcherFilter(RestDispatcher restDispatcher, AuthZFilter authZFilter) {
        this.restDispatcher = restDispatcher;
        this.authZFilter = authZFilter;
    }

    /**
     * Gets the names of the AdminAuthorizationConfigurator class and PassthroughAuthorizationConfigurator.
     *
     * @param filterConfig The FilterConfig.
     * @throws ServletException If either of the init params are not defined.
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        realmsAuthzConfiguratorClassName = filterConfig.getInitParameter(INIT_PARAM_REALMS_AUTHZ_CONFIGURATOR);
        usersAuthzConfiguratorClassName = filterConfig.getInitParameter(INIT_PARAM_USERS_AUTHZ_CONFIGURATOR);
        groupsAuthzConfiguratorClassName = filterConfig.getInitParameter(INIT_PARAM_GROUPS_AUTHZ_CONFIGURATOR);
        agentsAuthzConfiguratorClassName = filterConfig.getInitParameter(INIT_PARAM_AGENTS_AUTHZ_CONFIGURATOR);

        if (realmsAuthzConfiguratorClassName == null || usersAuthzConfiguratorClassName == null
                || groupsAuthzConfiguratorClassName == null || agentsAuthzConfiguratorClassName == null) {
            String message = INIT_PARAM_REALMS_AUTHZ_CONFIGURATOR  + ", "
                    + INIT_PARAM_USERS_AUTHZ_CONFIGURATOR + ", " + INIT_PARAM_GROUPS_AUTHZ_CONFIGURATOR
                    + " and " + INIT_PARAM_AGENTS_AUTHZ_CONFIGURATOR + " init params must be set!";
            DEBUG.error(message);
            throw new ServletException(message);
        }
    }

    /**
     * Determines the resource type being accessed and makes a call to the authorization filter with the required
     * authorization configuration for that resource.
     *
     * @param servletRequest The ServletRequest. Must be of type HttpServletRequest.
     * @param servletResponse The ServletResponse.
     * @param chain The FilterChain.
     * @throws IOException If an error occurs authorizing the request.
     * @throws ServletException If an error occurs authorizing the request.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest)) {
            throw new ServletException("Request must be of types HttpServletRequest");
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        try {
            final String resourcePath = request.getPathInfo();
            Map<String, String> details = restDispatcher.getRequestDetails(resourcePath == null ? "/" : resourcePath);

            String endpoint = details.get("resourceName");

            if (RestDispatcher.REALMS.equalsIgnoreCase(endpoint)) {
                authorize(realmsAuthzConfiguratorClassName, request, servletResponse, chain);
            } else if (RestDispatcher.USERS.equalsIgnoreCase(endpoint)) {
                authorize(usersAuthzConfiguratorClassName, request, servletResponse, chain);
            } else if (RestDispatcher.GROUPS.equalsIgnoreCase(endpoint)) {
                authorize(groupsAuthzConfiguratorClassName, request, servletResponse, chain);
            } else if (RestDispatcher.AGENTS.equalsIgnoreCase(endpoint)) {
                authorize(agentsAuthzConfiguratorClassName, request, servletResponse, chain);
            }
        } catch (NotFoundException e) {
            // Endpoint not found so cannot perform any authorization
            DEBUG.message("Resource " + request.getPathInfo() + " not found. Not performing authorization on request.");
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * Calls the commons Authorization Filter with the desired authorization configurator class name to configure
     * the authorization logic with.
     *
     * @param authzConfiguratorClassName The Authorization configurator class name.
     * @param request The ServletRequest.
     * @param response The ServletResponse.
     * @param chain The FilterChain.
     * @throws IOException If an error occurs authorizing the request.
     * @throws ServletException If an error occurs authorizing the request.
     */
    private void authorize(final String authzConfiguratorClassName, ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        authZFilter.init(new RestAuthorizationDispatcherFilterConfig(filterConfig, authzConfiguratorClassName));

        authZFilter.doFilter(request, response, chain);
        authZFilter.destroy();
    }

    /**
     * Destroys any internal state.
     */
    public void destroy() {
        filterConfig = null;
        realmsAuthzConfiguratorClassName = null;
        usersAuthzConfiguratorClassName = null;
        groupsAuthzConfiguratorClassName = null;
        agentsAuthzConfiguratorClassName = null;
    }
}
