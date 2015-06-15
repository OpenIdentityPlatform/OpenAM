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
package org.forgerock.openam.headers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SetHeadersFilter is a servlet Filter for setting arbitrary headers for web resources. This filter adds headers keys
 * and values to a response and is configurable from the web descriptor file {@code web.xml}.
 */
public class SetHeadersFilter implements Filter {

    private static final String EXCLUDES = "excludes";
    private final Map<String, String> headerKeyValues = new HashMap<String, String>();
    private final Set<String> excludes = new HashSet<String>();
    private int contextPathLength = 0;

    /**
     * Initializes the filter based on the {@link FilterConfig}.
     * The "excludes" init parameter is used to prevent the filter from setting the headers when accessing certain URIs.
     * Any other init parameter specified in web.xml will be handled as a headername-headervalue pair that should be
     * added to the HttpServletResponse.
     *
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        if (config != null) {
            contextPathLength = config.getServletContext().getContextPath().length();
            Enumeration<String> initParams = config.getInitParameterNames();
            while (initParams.hasMoreElements()) {
                String key = initParams.nextElement();
                String value = config.getInitParameter(key);
                if (EXCLUDES.equals(key)) {
                    excludes.addAll(Arrays.asList(value.split(",")));
                } else {
                    headerKeyValues.put(key, value);
                }
            }
        }
    }

    /**
     * Set HTTP Headers based on the values in the filterConfig init-parameters.
     * 
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        if (!excludes.contains(httpServletRequest.getRequestURI().substring(contextPathLength))) {
            for (Map.Entry<String, String> entry : headerKeyValues.entrySet()) {
                httpServletResponse.addHeader(entry.getKey(), entry.getValue());
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
