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

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.servlet.HttpServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;

/**
 * This class is used to create an instance of the CREST HttpServlet programatically outside of the Servlet Container.
 * <br/>
 * This enables the creation of an instance as a delegate that can be passed requests from another Java HttpServlet that
 * is created by the Servlet Container.
 *
 * @since 12.0.0
 */
public class CrestHttpServlet extends HttpServlet {

    private final javax.servlet.http.HttpServlet servlet;

    /**
     * Constructs a new CrestHttpServlet.
     *
     * @param servlet The underlying container created Java HttpServlet.
     * @param connectionFactory The CREST ConnectionFactory used by this CREST instance.
     */
    public CrestHttpServlet(javax.servlet.http.HttpServlet servlet, ConnectionFactory connectionFactory) {
        super(connectionFactory);
        this.servlet = servlet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitParameter(String name) {
        return servlet.getInitParameter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration getInitParameterNames() {
        return servlet.getInitParameterNames();
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
    public String getServletName() {
        return servlet.getServletName();
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
