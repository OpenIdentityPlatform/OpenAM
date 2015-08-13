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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.forgerock.openam.rest.service.OAuth2ServiceEndpointApplication;
import org.forgerock.openam.rest.service.RestletServiceServlet;
import org.forgerock.openam.rest.service.UMAServiceEndpointApplication;
import org.forgerock.openam.rest.service.XACMLServiceEndpointApplication;

/**
 * Root Servlet for all REST endpoint requests, which are then passed onto the correct underlying servlet, either
 * CREST for Resource endpoints or Restlet for Service endpoints.
 *
 * @since 12.0.0
 */
public class RestEndpointServlet extends HttpServlet {

    public static final String CREST_CONNECTION_FACTORY_NAME = "CrestConnectionFactory";

    private final RestletServiceServlet restletXACMLServiceServlet;
    private final RestletServiceServlet restletOAuth2ServiceServlet;
    private final RestletServiceServlet restletUMAServiceServlet;

    /**
     * Constructs a new RestEndpointServlet.
     */
    public RestEndpointServlet() {
        this.restletXACMLServiceServlet = new RestletServiceServlet(this, XACMLServiceEndpointApplication.class,
                "xacmlRestletServiceServlet");
        this.restletOAuth2ServiceServlet = new RestletServiceServlet(this, OAuth2ServiceEndpointApplication.class,
                "oauth2RestletServiceServlet");
        this.restletUMAServiceServlet = new RestletServiceServlet(this, UMAServiceEndpointApplication.class,
                "umaRestletServiceServlet");
    }

    /**
     * Constructor for test use.
     *
     * @param restletXACMLServiceServlet An instance of a RestletServiceServlet.
     * @param restletUMAServiceServlet An instance of a RestletServiceServlet.
     */
    RestEndpointServlet(
            final RestletServiceServlet restletXACMLServiceServlet,
            final RestletServiceServlet restletOAuth2ServiceServlet,
            final RestletServiceServlet restletUMAServiceServlet) {
        this.restletXACMLServiceServlet = restletXACMLServiceServlet;
        this.restletOAuth2ServiceServlet = restletOAuth2ServiceServlet;
        this.restletUMAServiceServlet = restletUMAServiceServlet;
    }

    /**
     * Initialises the CREST and Restlet Servlets.
     *
     * @throws ServletException If the CREST Servlet init() call fails.
     */
    @Override
    public void init() throws ServletException {
        // Don't need to call init() as starts Restlet which is not needed as is not created by
        // Servlet Container.
    }

    /**
     * Delegates the request to either the CREST or Restlet servlet based on whether the request is for a resource
     * or service REST endpoint.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @throws ServletException If the CREST or Restlet Servlet service() calls fail.
     * @throws IOException If the CREST or Restlet Servlet service() calls fail.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if ("/xacml".equals(request.getServletPath())) {
            restletXACMLServiceServlet.service(new HttpServletRequestWrapper(request), response);
        } else if ("/oauth2".equals(request.getServletPath())) {
            restletOAuth2ServiceServlet.service(new HttpServletRequestWrapper(request), response);
        } else if ("/uma".equals(request.getServletPath())) {
            restletUMAServiceServlet.service(new HttpServletRequestWrapper(request), response);
        }
    }

    /**
     * Destroys the CREST and Restlet servlets.
     */
    @Override
    public void destroy() {
        restletXACMLServiceServlet.destroy();
        restletOAuth2ServiceServlet.destroy();
        restletUMAServiceServlet.destroy();
    }
}
