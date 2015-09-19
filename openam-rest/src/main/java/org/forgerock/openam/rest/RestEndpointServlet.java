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

import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.ResponseException;
import org.forgerock.http.servlet.HttpFrameworkServlet;
import org.forgerock.openam.rest.service.OAuth2ServiceEndpointApplication;
import org.forgerock.openam.rest.service.RestletServiceServlet;
import org.forgerock.openam.rest.service.UMAServiceEndpointApplication;
import org.forgerock.openam.rest.service.XACMLServiceEndpointApplication;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Root Servlet for all REST endpoint requests, which are then passed onto the correct underlying servlet, either
 * CREST for Resource endpoints or Restlet for Service endpoints.
 *
 * @since 12.0.0
 */
public class RestEndpointServlet extends HttpServlet {

    private final RestletServiceServlet restletXACMLServiceServlet;
    private final RestletServiceServlet restletOAuth2ServiceServlet;
    private final RestletServiceServlet restletUMAServiceServlet;
    private final HttpServlet restletXACMLHttpServlet;
    private final Filter authenticationFilter;

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
        this.authenticationFilter =
                InjectorHolder.getInstance(Key.get(Filter.class, Names.named("AuthenticationFilter")));
        this.restletXACMLHttpServlet = new HttpServletWrapper(this,
                new HttpFrameworkServlet(new RestletAuthnHttpApplication()));
    }

    /**
     * Constructor for test use.
     *
     * @param restletXACMLServiceServlet An instance of a RestletServiceServlet.
     * @param restletUMAServiceServlet An instance of a RestletServiceServlet.
     */
    RestEndpointServlet(
            RestletServiceServlet restletXACMLServiceServlet,
            RestletServiceServlet restletOAuth2ServiceServlet,
            RestletServiceServlet restletUMAServiceServlet,
            HttpServlet restletXACMLHttpServlet,
            Filter authenticationFilter) {
        this.restletXACMLServiceServlet = restletXACMLServiceServlet;
        this.restletOAuth2ServiceServlet = restletOAuth2ServiceServlet;
        this.restletUMAServiceServlet = restletUMAServiceServlet;
        this.restletXACMLHttpServlet = restletXACMLHttpServlet;
        this.authenticationFilter = authenticationFilter;
    }

    /**
     * Initialises the CREST and Restlet Servlets.
     *
     * @throws ServletException If the CREST Servlet init() call fails.
     */
    @Override
    public void init() throws ServletException {
        restletXACMLHttpServlet.init();
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
            restletXACMLHttpServlet.service(request, response);
        } else if ("/oauth2".equals(request.getServletPath())) {
            restletOAuth2ServiceServlet.service(new HttpServletRequestWrapper(request), response);
        } else if ("/uma".equals(request.getServletPath())) {
            restletUMAServiceServlet.service(new HttpServletRequestWrapper(request), response);
        }
    }

    /**
     * To add CAF authentication protection to the /xacml endpoint we need to
     * enter into the CHF world but before entering the Restlet world....
     */
    private final class RestletAuthnHttpApplication implements HttpApplication {

        @Override
        public Handler start() throws HttpApplicationException {
            return Handlers.chainOf(new RestletHandler(), authenticationFilter);
        }

        @Override
        public Factory<Buffer> getBufferFactory() {
            return null;
        }

        @Override
        public void stop() {
        }
    }

    /**
     * This CHF handler will be routed through CAF authentication filter and
     * then will invoke the Restlet servlet for the /xacml endpoint.
     */
    private final class RestletHandler implements Handler {

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            Map<String, Object> attributes = new HashMap<>(context.asContext(AttributesContext.class).getAttributes());
            HttpServletRequest httpRequest = (HttpServletRequest) attributes.remove(HttpServletRequest.class.getName());
            HttpServletResponse httpResponse =
                    (HttpServletResponse) attributes.remove(HttpServletResponse.class.getName());
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                if (httpRequest.getAttribute(attribute.getKey()) == null) {
                    httpRequest.setAttribute(attribute.getKey(), attribute.getValue());
                }
            }
            try {
                restletXACMLServiceServlet.service(new HttpServletRequestWrapper(httpRequest), httpResponse);
            } catch (ServletException | IOException e) {
                return newResultPromise(new ResponseException(e.getMessage(), e).getResponse());
            }
            return null;
        }
    }

    /**
     * Destroys the CREST and Restlet servlets.
     */
    @Override
    public void destroy() {
        restletXACMLHttpServlet.destroy();
        restletXACMLServiceServlet.destroy();
        restletOAuth2ServiceServlet.destroy();
        restletUMAServiceServlet.destroy();
    }

    private static final class HttpServletWrapper extends HttpServlet {

        private final javax.servlet.http.HttpServlet realServlet;
        private final HttpFrameworkServlet frameworkServlet;

        private HttpServletWrapper(javax.servlet.http.HttpServlet realServlet, HttpFrameworkServlet frameworkServlet) {
            this.realServlet = realServlet;
            this.frameworkServlet = frameworkServlet;
        }

        @Override
        public void init(ServletConfig config) throws ServletException {
            frameworkServlet.init(config);
        }

        @Override
        public void init() throws ServletException {
            init(getServletConfig());
        }

        @Override
        public void destroy() {
            frameworkServlet.destroy();
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            frameworkServlet.service(req, resp);
        }

        @Override
        public String getInitParameter(String name) {
            return realServlet.getInitParameter(name);
        }

        @Override
        public Enumeration getInitParameterNames() {
            return realServlet.getInitParameterNames();
        }

        @Override
        public ServletConfig getServletConfig() {
            return realServlet.getServletConfig();
        }

        @Override
        public ServletContext getServletContext() {
            return realServlet.getServletContext();
        }

        @Override
        public String getServletInfo() {
            return realServlet.getServletInfo();
        }

        @Override
        public String getServletName() {
            return realServlet.getServletName();
        }

        @Override
        public void log(String msg) {
            realServlet.log(msg);
        }

        @Override
        public void log(String message, Throwable t) {
            realServlet.log(message, t);
        }
    }
}
