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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Drives the unauthenticated {@code /liberty-logout} endpoint end to end: the
 * request parameter goes in, the dispatcher path (or the error status) comes out.
 */
public class FSSingleLogoutServletTest {

    private FSSingleLogoutServlet servlet;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeMethod
    public void setUp() throws Exception {
        servletContext = mock(ServletContext.class);
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(servletContext);
        servlet = new FSSingleLogoutServlet();
        servlet.init(config);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    public void forwardsAnOrdinaryAliasToTheProcessLogoutHandler() throws Exception {
        when(request.getParameter("metaAlias")).thenReturn("/idp");
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(servletContext.getRequestDispatcher("/ProcessLogout/metaAlias/idp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @DataProvider
    public Object[][] traversingAliases() {
        return new Object[][] {
            {"/../../WEB-INF/web.xml"},
            // getParameter() has already decoded one layer; the container decodes
            // the dispatcher path once more before normalising it.
            {"/%2e%2e/%2e%2e/WEB-INF/web.xml"},
            {"/%252e%252e/%252e%252e/WEB-INF/web.xml"},
            {"/idp/..%2f..%2fWEB-INF/web.xml"},
        };
    }

    @Test(dataProvider = "traversingAliases")
    public void answers400WithoutDispatchingForATraversingAlias(String metaAlias) throws Exception {
        when(request.getParameter("metaAlias")).thenReturn(metaAlias);

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(servletContext, never()).getRequestDispatcher(anyString());
    }
}
