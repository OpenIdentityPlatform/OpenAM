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

package org.forgerock.openam.rest.service;

import org.restlet.ext.servlet.ServerServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to create an instance of the Restlet ServerServlet programmatically outside of the Servlet
 * Container.
 * <br/>
 * This enables the creation of an instance as a delegate that can be passed requests from another Java HttpServlet
 * that is created by the Servlet Container.
 *
 * @since 12.0.0
 */
public class RestletServiceServlet extends ServerServlet {

    private static final String RESTLET_APPLICATION_INIT_PARAM = "org.restlet.application";

    private final HttpServlet servlet;
    private final Class<? extends ServiceEndpointApplication> application;
    private final String name;

    /**
     * Constructs an instance of the RestletServiceServlet.
     *
     * @param servlet The underlying container created Java HttpServlet.
     */
    public RestletServiceServlet(final HttpServlet servlet, Class<? extends ServiceEndpointApplication> application,
            String name) {
        this.servlet = servlet;
        this.application = application;
        this.name = name;
    }

    /**
     * Overridden to return the ServiceEndpointApplication class name for the "org.restlet.application" init parameter.
     * <br/>
     * All other parameter requests get passed onto the underlying Java HttpServlet.
     *
     * @param name {@inheritDoc}
     * @param defaultValue {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public String getInitParameter(String name, String defaultValue) {
        if (RESTLET_APPLICATION_INIT_PARAM.equals(name)) {
            return application.getName();
        }
        final String value = servlet.getInitParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitParameter(String name) {
        return servlet.getInitParameter(name);
    }

    /**
     * Overridden to include the "org.restlet.application" init paramter name.
     *
     * @return {@code}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getInitParameterNames() {
        Set<String> names = new HashSet<String>();
        Enumeration<String> parameterNames = servlet.getInitParameterNames();
        while (parameterNames.hasMoreElements()) {
            names.add(parameterNames.nextElement());
        }
        names.add(RESTLET_APPLICATION_INIT_PARAM);
        return Collections.enumeration(names);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        return servlet.getServletContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletInfo() {
        return servlet.getServletInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(String msg) {
        servlet.log(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void log(String message, Throwable t) {
        servlet.log(message, t);
    }
}
