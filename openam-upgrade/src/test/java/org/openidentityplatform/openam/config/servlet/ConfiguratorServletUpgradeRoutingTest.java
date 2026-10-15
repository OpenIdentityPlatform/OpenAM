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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * openam-core's own {@code ConfiguratorServletTest} cannot exercise real routing to {@code Upgrade}
 * (see its class Javadoc): its test classpath never has {@code openam-upgrade} on it, so
 * {@code ServiceLoader.load(ConfiguratorPageProvider.class)} always finds zero providers there.
 * This module's test classpath has both {@code ConfiguratorServlet} (openam-core) and
 * {@code Upgrade}/{@code UpgradePageProvider} (openam-upgrade), so this is the one place that can
 * actually prove the {@code META-INF/services} self-registration wires up end-to-end.
 */
public class ConfiguratorServletUpgradeRoutingTest {

    private ConfiguratorServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

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
        when(request.getMethod()).thenReturn("GET");
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getParameter("locale")).thenReturn("en");

        StringWriter responseBody = new StringWriter();
        response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

    @Test
    public void upgradeActionLinkRoutesToServiceLoaderDiscoveredPage() throws Exception {
        when(request.getServletPath()).thenReturn("/config/upgrade/upgrade.htm");
        when(request.getParameter("actionLink")).thenReturn("doUpgrade");

        // No real OpenAM bootstrap in this test JVM, so Upgrade's constructor sets error = true and
        // doUpgrade() NPEs on the still-null `upgrade` field (see UpgradeTest) - that NPE, wrapped
        // by ConfiguratorServlet.invokeAction(), is itself the proof that routing reached the real
        // Upgrade.doUpgrade(), rather than a 404 (unknown action).
        assertThatThrownBy(() -> servlet.service(request, response))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }
}
