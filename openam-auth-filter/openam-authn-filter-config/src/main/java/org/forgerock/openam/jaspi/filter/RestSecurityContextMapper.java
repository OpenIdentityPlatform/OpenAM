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

import org.forgerock.jaspi.filter.AuthNFilter;
import org.forgerock.json.resource.servlet.SecurityContextFactory;

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
 * Maps the Commons AuthN Filter security parameters to CREST security parameters.
 *
 * {@link SecurityContextFactory}
 *
 * @author Phill Cunnington
 */
public class RestSecurityContextMapper implements Filter {

    /**
     * Does nothing.
     *
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Converts the Commons AuthN Filter "org.forgerock.authentication.principal" and
     * "org.forgerock.authentication.context" request header and attribute into the expected CREST
     * "org.forgerock.security.authcid" and "org.forgerock.security.authzid" request attributes.
     *
     * These two CREST request attributes will be picked up by the CREST framework and create a SecurityContext
     * with these two values, which can then be accessed by CREST resources.
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

        String authcid = request.getHeader(AuthNFilter.ATTRIBUTE_AUTH_PRINCIPAL);
        Map<String, Object> authzid = (Map<String, Object>) request.getAttribute(AuthNFilter.ATTRIBUTE_AUTH_CONTEXT);

        request.setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHCID, authcid);
        request.setAttribute(SecurityContextFactory.ATTRIBUTE_AUTHZID, authzid);

        filterChain.doFilter(request, servletResponse);
    }

    /**
     * Does nothing.
     *
     * {@inheritDoc}
     */
    public void destroy() {
    }
}
