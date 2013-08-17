/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.xui;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.when;

public class XUIFilterTest {

    @Captor
    private ArgumentCaptor<String> captor = new ArgumentCaptor<String>();
    private ArgumentCaptor<String> captorEndUser = new ArgumentCaptor<String>();

    @Test
    public void testLogin() throws Exception {
        String contextPath = "/context";
        String pathInfo = "UI/Login";
        String query = "locale=fr";
        String xuiLoginPath = "/XUI/#login/";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseLogin = mock(HttpServletResponse.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        FilterChain filterChain = mock(FilterChain.class);
        ServletContext servletContext = mock(ServletContext.class);

        XUIFilter xuifilterLogin = new XUIFilter(true, true);

        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getQueryString()).thenReturn(query);

        xuifilterLogin.init(filterConfig);
        xuifilterLogin.doFilter(request, responseLogin, filterChain);

        verify(responseLogin).sendRedirect(captor.capture());

        assertTrue(captor.getValue().equalsIgnoreCase(contextPath + xuiLoginPath + "&" + query));
    }

    @Test
    public void testLogout() throws Exception {
        String contextPath = "/context";
        String xuiLogoutPath = "/XUI/#logout/";
        String logoutPath = "UI/Logout";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseLogout = mock(HttpServletResponse.class);

        FilterConfig filterConfig = mock(FilterConfig.class);
        FilterChain filterChain = mock(FilterChain.class);
        ServletContext servletContext = mock(ServletContext.class);

        XUIFilter xuifilterLogout = new XUIFilter(true, true);

        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        when(request.getPathInfo()).thenReturn(logoutPath);
        when(request.getQueryString()).thenReturn(null);

        xuifilterLogout.init(filterConfig);
        xuifilterLogout.doFilter(request, responseLogout, filterChain);
        verify(responseLogout).sendRedirect(captor.capture());
        assertTrue(captor.getValue().equalsIgnoreCase(contextPath + xuiLogoutPath));
    }

    @Test
    public void testEndUserPage() throws Exception {
        String contextPath = "/context";
        String profilePage = "/XUI/#profile/";
        String endUserPath = "idm/EndUser";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseEndUser = mock(HttpServletResponse.class);
        XUIFilter xuifilterEndUser = new XUIFilter(true, true);

        FilterConfig filterConfig = mock(FilterConfig.class);
        FilterChain filterChain = mock(FilterChain.class);
        ServletContext servletContext = mock(ServletContext.class);

        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn(contextPath);
        when(request.getPathInfo()).thenReturn(endUserPath);
        when(request.getQueryString()).thenReturn(null);
        xuifilterEndUser.init(filterConfig);
        xuifilterEndUser.doFilter(request, responseEndUser, filterChain);
        verify(responseEndUser).sendRedirect(captorEndUser.capture());
        assertTrue(captorEndUser.getValue().equalsIgnoreCase(contextPath + profilePage));
    }


}
