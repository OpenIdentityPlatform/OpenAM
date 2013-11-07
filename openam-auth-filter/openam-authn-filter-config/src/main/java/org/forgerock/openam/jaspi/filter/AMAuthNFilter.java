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

package org.forgerock.openam.jaspi.filter;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.jaspi.filter.AuthNFilter;
import org.forgerock.openam.forgerockrest.RestDispatcher;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

/**
 * Adds a check to see if the REST endpoint being hit is the authentication endpoint and if is then will skip
 * the authentication check. All other REST endpoints will be protected resulting in the request requiring a
 * SSOToken cookie set.
 *
 * @author Phill Cunnington
 */
public class AMAuthNFilter extends AuthNFilter {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private final EndpointMatcher endpointMatcher;

    /**
     * Constructs an instance of the AMAuthNFilter.
     */
    public AMAuthNFilter() {
        this(RestDispatcher.getInstance());
    }

    /**
     * Constructs an instance of the AMAuthNFilter.
     * <p>
     * Used by tests.
     *
     * @param restDispatcher An instance of the RestDispatcher.
     */
    public AMAuthNFilter(RestDispatcher restDispatcher) {
        endpointMatcher = new EndpointMatcher("/json", restDispatcher);

        /*
         * Only the REST Authentication Endpoint is unprotected. Other endpoints that don't need to be authenticated
         * as a "real" user can use the anonymous use to authenticate first, then use an Authorization Filter to
         * allow the anonymous user access.
         * <p>
         * Only need to specific the actual endpoint for the root path that this filter is registered against.
         */
        endpointMatcher.endpoint("/authenticate", HttpMethod.GET);
        endpointMatcher.endpoint("/authenticate", HttpMethod.POST);
        endpointMatcher.endpoint("/users", HttpMethod.POST, "_action", "register", "confirm", "forgotPassword",
                "forgotPasswordReset", "anonymousCreate");
        endpointMatcher.endpoint("/serverinfo", HttpMethod.GET);
    }

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
    }

    /**
     * Parses the URI and request method from the request and creates an UnprotectedEndpoint object and compares it
     * against the set of valid UnprotectedEndpoints. If it matches then the request is passed down the filter chain,
     * if it doesn't match then the Commons Authentication Filter is given the request to authenticate it first.
     *
     * @param servletRequest {@inheritDoc}
     * @param servletResponse {@inheritDoc}
     * @param filterChain {@inheritDoc}
     * @throws IOException {@inheritDoc}
     * @throws ServletException {@inheritDoc}
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());

        if (endpointMatcher.match(request)) {
            DEBUG.message("Path: " + path + " Method: " + request.getMethod() + " Added as exception. Not protected.");
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            DEBUG.message("Path: " + path + " Method: " + request.getMethod() + " Protected resource.");
            super.doFilter(servletRequest, servletResponse, filterChain);
        }
    }
}
