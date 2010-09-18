/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: HandlerServlet.java,v 1.7 2009/10/21 00:01:44 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.servlet;

import com.sun.identity.proxy.handler.Handler;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.http.Session;
import com.sun.identity.proxy.io.Streamer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Receives HTTP requests through the Servlet API, translates to proxy message
 * exchange and dispatches to a handler. This class is intended to be subclassed
 * to establish handler and base URI.
 *
 * @author Paul C. Bryan
 */
public class HandlerServlet extends HttpServlet
{
    /** The handler to dispatch exchanges to. */
    protected Handler handler = null;

    /** The base URI (scheme, host, port) of the remote server to relay requests to. */
    protected URI base = null;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        Exchange exchange = new Exchange();

        // ----- request ------------------------------------------------------

        exchange.request = new Request();

        exchange.request.method = request.getMethod();

        // build request URI manually to avoid inadvertent normalization
        StringBuilder sb = new StringBuilder();
        sb.append(base.getScheme()).append("://").append(base.getHost());
        int port = base.getPort();
        if (port != -1) {
            sb.append(':').append(Integer.toString(port));
        }
        sb.append(request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null) {
            sb.append('?').append(queryString);
        }
        exchange.request.uri = URI.create(sb.toString());
System.err.println("URI=" + exchange.request.uri);

        // request headers
        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            exchange.request.headers.add(name, Collections.list(request.getHeaders(name)));
        }

        String method = request.getMethod().toUpperCase();

        // include request entity if appears to be provided with request
        if ((request.getContentLength() > 0 || request.getHeader("Transfer-Encoding") != null)
        && !method.equals("GET") && !method.equals("HEAD") && !method.equals("TRACE")
        && !method.equals("DELETE")) {
            exchange.request.entity = request.getInputStream();
        }

        HttpSession httpSession = request.getSession();
        synchronized(httpSession) { // prevent race conditions
            Session session = (Session)httpSession.getAttribute(Session.class.getName());
            if (session == null) {
                session = new Session();
                httpSession.setAttribute(Session.class.getName(), session);
            }
            exchange.request.session = session;
        }

        exchange.request.principal = request.getUserPrincipal();

        // handy servlet-specific attributes, sure to be abused by downstream filters
        exchange.request.attributes.put("javax.servlet.http.HttpServletRequest", request);
        exchange.request.attributes.put("javax.servlet.http.HttpServletResponse", response);

        try {

        // ----- execute ------------------------------------------------------

            try {
                handler.handle(exchange);
            }
            catch (HandlerException he) {
                throw new ServletException(he);
            }

        // ----- response -----------------------------------------------------

            // response status-code (reason-phrase is deprecated in servlet api)
            response.setStatus(exchange.response.status);

            // response headers
            for (String name : exchange.response.headers.keySet()) {
                for (String value : exchange.response.headers.get(name)) {
                    if (value != null && value.length() > 0) {
                        response.addHeader(name, value);
                    }
                }
            }

            // response entity
            if (exchange.response.entity != null) {
                OutputStream out = response.getOutputStream();
                Streamer.stream(exchange.response.entity, out);
                out.flush();
            }
        }

        // ----- cleanup ------------------------------------------------------

        finally {

            if (exchange.response != null && exchange.response.entity != null) {
                try {
                    exchange.response.entity.close(); // important!
                }
                catch (IOException ioe) {
                }
            }
        }        
    }

    private String normalizeURI(String uri) {
        for (int n = 0, len = uri.length(); n < len; n++) {
            if (uri.charAt(n) != '/') {
                return (n <= 1 ? uri : uri.substring(n - 1));
            }
        }
        return "/";
    }
}

