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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SetHeadersFilter is a servlet Filter for setting headers in static files such as HTML. This filter adds headers keys
 * and values to a response and is configurable from the OpenAM web descriptor file {@code web.xml}
 */
public class SetHeadersFilter implements Filter {

    private Map<String, String> headerKeyValues = new HashMap<String, String>();

    /**
     * Populate the map containing the headers keys and values based on the {@link FilterConfig}. {@inheritDoc}
     */
    @Override
    public void init(FilterConfig config) throws ServletException {

        if (config != null) {
            Enumeration<String> initParams = config.getInitParameterNames();
            while (initParams.hasMoreElements()) {
                String headerKey = initParams.nextElement();
                headerKeyValues.put(headerKey, config.getInitParameter(headerKey));
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

        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        HttpServletRequest httpServleRequest = (HttpServletRequest) servletRequest;
        for (Map.Entry<String, String> entry : headerKeyValues.entrySet()) {
            httpServletResponse.addHeader(entry.getKey(), entry.getValue());
        }
        filterChain.doFilter(httpServleRequest, httpServletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
