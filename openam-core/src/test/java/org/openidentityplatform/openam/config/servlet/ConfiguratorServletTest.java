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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Routing behavior of {@link ConfiguratorServlet} against the real migrated-page registry
 * (steps 1-7, the wizard shell, {@code Options}, {@code DefaultSummary} and {@code Upgrade} -
 * every real configurator page there is): a registered path dispatches {@code ?actionLink=}
 * requests to the matching {@code @ConfiguratorAction} method, and an unregistered path 404s.
 * Apache Click (and the named-dispatch fallback to it) is gone as of increment 8 - see
 * {@code docs/migration/click-to-freemarker/03-migration-plan.md}.
 *
 * <p>{@code upgrade.htm} is used below to prove the {@code ServiceLoader}-registered page path
 * still resolves - within {@code openam-core}'s own test classpath (which never has
 * {@code openam-upgrade} on it, so {@code ConfiguratorPageProvider} discovery finds zero
 * providers here), it exercises the genuinely-unregistered/404 branch instead. The real
 * end-to-end routing through the actual {@code Upgrade} page is covered by a dedicated test in
 * {@code openam-upgrade}, which - unlike this module - can see both classes on its classpath.
 */
public class ConfiguratorServletTest {

    private ConfiguratorServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseBody;

    @BeforeMethod
    public void setup() throws Exception {
        servlet = new ConfiguratorServlet();

        servletContext = mock(ServletContext.class);
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("configurator-servlet");
        servlet.init(servletConfig);

        HttpSession session = mock(HttpSession.class);
        request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    public void dispatchesRegisteredActionLinkToAnnotatedHandler() throws Exception {
        when(request.getServletPath()).thenReturn("/config/wizard/step1.htm");
        when(request.getParameter("actionLink")).thenReturn("checkAdminPassword");
        when(request.getParameter("admin")).thenReturn("longenough1");
        when(request.getParameter("adminConfirm")).thenReturn("longenough1");

        servlet.service(request, response);

        assertThat(responseBody.toString()).isEqualTo("{\"valid\":\"true\",\"body\":\"OK\"}");
    }

    @Test
    public void unknownActionLinkOnRegisteredPageIsRejected() throws Exception {
        when(request.getServletPath()).thenReturn("/config/wizard/step1.htm");
        when(request.getParameter("actionLink")).thenReturn("notARealAction");

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void unregisteredPathIsNotFound() throws Exception {
        // See the class Javadoc: this path is only "unregistered" from openam-core's own test
        // classpath, which never has the openam-upgrade ServiceLoader provider on it.
        when(request.getServletPath()).thenReturn("/config/upgrade/upgrade.htm");

        servlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
