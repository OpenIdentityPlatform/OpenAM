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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.cors;

import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.cors.utils.CSVHelper;
import org.forgerock.util.Reject;

/**
 * A Servlet Filter implementation of the CORS Service, initialized using a FilterConfig.
 *
 * This Filter forces requests to abide by the CORS standard using the {@link CORSService}.
 */
public class CORSFilter implements Filter {

    private CORSService service;
    private final CSVHelper csvHelper = new CSVHelper();

    private final static int DEFAULT_TIMEOUT = 600; //10 mins

    /**
     * Required default constructor
     */
    public CORSFilter() { }

    /**
     * Testing constructor
     * @param service Service through which to validate requests and alter responses applying the CORS spec.
     */
    CORSFilter(final CORSService service) {
        this.service = service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

        Reject.ifTrue(filterConfig == null, "Configuration must not be null.");

        final String allowedOrigins = filterConfig.getInitParameter(CORSConstants.ORIGINS_KEY);
        final String allowedMethods = filterConfig.getInitParameter(CORSConstants.METHODS_KEY);
        final String allowedHeaders = filterConfig.getInitParameter(CORSConstants.HEADERS_KEY);
        final String exposedHeaderStr = filterConfig.getInitParameter(CORSConstants.EXPOSE_HEADERS_KEY);
        final String expectedHostname = filterConfig.getInitParameter(CORSConstants.EXPECTED_HOSTNAME_KEY);

        int maxAge = DEFAULT_TIMEOUT;
        boolean allowCredentials = false;

        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new ServletException("Invalid configuration. Allowed Origins must be set.");
        }

        if (allowedMethods == null || allowedMethods.isEmpty()) {
            throw new ServletException("Invalid configuration. Allowed Methods must be set.");
        }

        final List<String> acceptedOrigins = csvHelper.csvStringToList(allowedOrigins, false);
        final List<String> acceptedMethods = csvHelper.csvStringToList(allowedMethods, false);

        if (!acceptedMethods.contains(CORSConstants.HTTP_OPTIONS)) {
            acceptedMethods.add(CORSConstants.HTTP_OPTIONS); //always includes OPTIONS for PRE-FLIGHT flow
        }

        final List<String> acceptedHeaders = csvHelper.csvStringToList(allowedHeaders, true);
        final List<String> exposedHeaders = csvHelper.csvStringToList(exposedHeaderStr, true);

        //defaults to 0, and isn't included
        if (filterConfig.getInitParameter(CORSConstants.MAX_AGE_KEY) != null) {
            try {
                maxAge = Integer.valueOf(filterConfig.getInitParameter(CORSConstants.MAX_AGE_KEY));
            } catch (NumberFormatException e) {
                throw new ServletException("Invalid configuration. Max-age must be an integer.", e);
            }
        }

        //defaults to false
        if (filterConfig.getInitParameter(CORSConstants.ALLOW_CREDENTIALS_KEY) != null) {
            allowCredentials = Boolean.valueOf(filterConfig.getInitParameter(CORSConstants.ALLOW_CREDENTIALS_KEY));
        }

        service = new CORSService(acceptedOrigins, acceptedMethods, acceptedHeaders,
                exposedHeaders, maxAge, allowCredentials, expectedHostname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        if (service.handleRequest(req, res)) {
            chain.doFilter(req, res);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        service = null;
    }
}
