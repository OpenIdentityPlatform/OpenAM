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

package org.forgerock.openam.rest;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.rest.resource.CrestHttpServlet;
import org.forgerock.openam.rest.router.RestEndpointManager;
import org.forgerock.openam.rest.service.JSONServiceEndpointApplication;
import org.forgerock.openam.rest.service.RestletServiceServlet;
import org.forgerock.openam.rest.service.XACMLServiceEndpointApplication;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Root Servlet for all REST endpoint requests, which are then passed onto the correct underlying servlet, either
 * CREST for Resource endpoints or Restlet for Service endpoints.
 *
 * @since 12.0.0
 */
public class RestEndpointServlet extends HttpServlet {

    public static final String CREST_CONNECTION_FACTORY_NAME = "CrestConnectionFactory";

    private final org.forgerock.json.resource.servlet.HttpServlet crestServlet;
    private final RestletServiceServlet restletJSONServiceServlet;
    private final RestletServiceServlet restletXACMLServiceServlet;
    private final RestEndpointManager endpointManager;

    /**
     * Constructs a new RestEndpointServlet.
     */
    public RestEndpointServlet() {
        this.crestServlet = new CrestHttpServlet(this, InjectorHolder.getInstance(Key.get(ConnectionFactory.class,
                Names.named(CREST_CONNECTION_FACTORY_NAME))));
        this.restletJSONServiceServlet = new RestletServiceServlet(this, JSONServiceEndpointApplication.class,
                "jsonRestletServiceServlet");
        this.restletXACMLServiceServlet = new RestletServiceServlet(this, XACMLServiceEndpointApplication.class,
                "xacmlRestletServiceServlet");
        this.endpointManager = InjectorHolder.getInstance(RestEndpointManager.class);
    }

    /**
     * Constructor for test use.
     *
     * @param crestServlet An instance of a CrestHttpServlet.
     * @param restletJSONServiceServlet An instance of a RestletServiceServlet.
     * @param restletXACMLServiceServlet An instance of a RestletServiceServlet.
     * @param endpointManager An instance of the RestEndpointManager.
     */
    RestEndpointServlet(final CrestHttpServlet crestServlet, final RestletServiceServlet restletJSONServiceServlet,
            final RestletServiceServlet restletXACMLServiceServlet, final RestEndpointManager endpointManager) {
        this.crestServlet = crestServlet;
        this.restletJSONServiceServlet = restletJSONServiceServlet;
        this.restletXACMLServiceServlet = restletXACMLServiceServlet;
        this.endpointManager = endpointManager;
    }

    /**
     * Initialises the CREST and Restlet Servlets.
     *
     * @throws ServletException If the CREST Servlet init() call fails.
     */
    @Override
    public void init() throws ServletException {
        crestServlet.init();
        // Don't need to call restServiceServlet.init() as starts Restlet which is not needed as is not created by
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
        if ("/json".equals(request.getServletPath())) {
            final String restRequest = getResourceName(request);

            final String endpoint = endpointManager.findEndpoint(restRequest);

            final RestEndpointManager.EndpointType endpointType = endpointManager.getEndpointType(endpoint);

            if (endpointType == null) {
                throw new ServletException("Endpoint Type could not be determined");
            }

            switch (endpointType) {
                case RESOURCE: {
                    crestServlet.service(request, response);
                    break;
                }
                case SERVICE: {
                    restletJSONServiceServlet.service(new HttpServletRequestWrapper(request), response);
                    break;
                }
            }
        } else if ("/xacml".equals(request.getServletPath())) {
            restletXACMLServiceServlet.service(new HttpServletRequestWrapper(request), response);
        }
    }

    /**
     * Gets the resource name (resource path) from the HttpServletRequest.
     *
     * @param req The HttpServletRequest.
     * @return The resource name (resource path).
     */
    private String getResourceName(final HttpServletRequest req) {
        // Treat null path info as root resource.
        String resourceName = req.getPathInfo();
        if (resourceName == null) {
            return "";
        }
        if (resourceName.endsWith("/")) {
            resourceName = resourceName.substring(0, resourceName.length() - 1);
        }
        return resourceName;
    }

    /**
     * Destroys the CREST and Restlet servlets.
     */
    @Override
    public void destroy() {
        crestServlet.destroy();
        restletXACMLServiceServlet.destroy();
        restletJSONServiceServlet.destroy();
    }
}
