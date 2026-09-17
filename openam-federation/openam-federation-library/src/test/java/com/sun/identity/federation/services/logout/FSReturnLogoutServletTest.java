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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.federation.services.logout;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Drives {@code /ReturnLogout/*} through its second alias source: the servlet is
 * prefix-mapped, so with no {@code metaAlias} parameter the alias is cut out of
 * the raw, still-encoded {@code getRequestURI()}.
 */
public class FSReturnLogoutServletTest {

    private FSReturnLogoutServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeMethod
    public void setUp() throws Exception {
        servletContext = mock(ServletContext.class);
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(servletContext);
        servlet = new FSReturnLogoutServlet();
        servlet.init(config);
        request = mock(HttpServletRequest.class);
        when(request.getParameter("metaAlias")).thenReturn(null);
        response = mock(HttpServletResponse.class);
    }

    @Test
    public void anOrdinaryAliasInTheRequestUriPassesTheGuard() throws Exception {
        when(request.getRequestURI()).thenReturn("/openam/ReturnLogout/metaAlias/idp");

        servlet.doGet(request, response);

        // No session in a unit test: the servlet fails later, but not on the alias.
        verify(response, never()).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(response).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @DataProvider
    public Object[][] traversingRequestUris() {
        return new Object[][] {
            {"/openam/ReturnLogout/metaAlias/../../WEB-INF/web.xml"},
            {"/openam/ReturnLogout/metaAlias/idp/..%2f..%2fWEB-INF/web.xml"},
            {"/openam/ReturnLogout/metaAlias/%2e%2e/%2e%2e/WEB-INF/web.xml"},
            {"/openam/ReturnLogout/metaAlias/%252e%252e/%252e%252e/WEB-INF/web.xml"},
        };
    }

    @Test(dataProvider = "traversingRequestUris")
    public void answers400WithoutDispatchingForATraversingAliasInTheRequestUri(String uri) throws Exception {
        when(request.getRequestURI()).thenReturn(uri);

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
        verify(servletContext, never()).getRequestDispatcher(anyString());
        verify(request, never()).getRequestDispatcher(anyString());
        verify(response, never()).sendError(anyInt());
    }
}
