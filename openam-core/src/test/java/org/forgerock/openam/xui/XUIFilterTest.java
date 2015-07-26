/*
 * Copyright 2013-2015 ForgeRock AS.
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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import static org.fest.assertions.Assertions.*;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.owasp.esapi.ESAPI;

import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.when;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.Constants;

public class XUIFilterTest {

    private static final String CONTEXT = "/context";
    private XUIFilter filter;

    @BeforeMethod
    public void setUp() {
        XUIState xuiState = mock(XUIState.class);
        when(xuiState.isXUIEnabled()).thenReturn(true);
        filter = new XUIFilter(xuiState);
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getContextPath()).thenReturn(CONTEXT);
        filter.init(filterConfig);
    }

    @Test
    public void loginRedirectsToXUIWithQuery() throws Exception {
        String pathInfo = "/UI/Login";
        String query = "locale=fr&realm=/";
        String xuiLoginPath = "/XUI/#login/";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseLogin = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn(pathInfo);
        when(request.getQueryString()).thenReturn(query);

        filter.doFilter(request, responseLogin, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(responseLogin).sendRedirect(captor.capture());

        assertThat(captor.getValue()).isEqualTo(CONTEXT + xuiLoginPath + "&" + query);
    }

    @Test
    public void loginRedirectsToXUIWithCompositeAdvice() throws Exception {
        String pathInfo = "/UI/Login";
        String query = "locale=fr&realm=%2F";
        String compositeAdvice = "<Advices><AttributeValuePair><Attribute name=\"AuthLevelConditionAdvice\"/>"
                + "<Value>1</Value></AttributeValuePair></Advices>";
        String xuiLoginPath = "/XUI/#login/";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseLogin = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn(pathInfo);
        when(request.getQueryString()).thenReturn(query);
        when((request.getParameterMap())).thenAnswer(new Answer<Map>() {
            @Override
            public Map answer(InvocationOnMock invocation) throws Throwable {
                Map parameterMap = new LinkedHashMap<String,String[]>();
                parameterMap.put("locale", new String[]{"fr"});
                parameterMap.put("realm", new String[]{"/"});
                return parameterMap;
            }
        });
        when(request.getParameter(Constants.COMPOSITE_ADVICE)).thenReturn(compositeAdvice);

        filter.doFilter(request, responseLogin, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(responseLogin).sendRedirect(captor.capture());
        
        query += "&authIndexType=composite_advice&authIndexValue=" + ESAPI.encoder().encodeForURL(compositeAdvice);

        assertThat(captor.getValue()).isEqualTo(CONTEXT + xuiLoginPath + "&" + query);
    }

    @Test
    public void testLogout() throws Exception {
        String xuiLogoutPath = "/XUI/#logout/";
        String logoutPath = "/UI/Logout";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseLogout = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn(logoutPath);
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, responseLogout, filterChain);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(responseLogout).sendRedirect(captor.capture());
        assertThat(captor.getValue()).isEqualTo(CONTEXT + xuiLogoutPath);
    }

    @Test
    public void testEndUserPage() throws Exception {
        String profilePage = "/XUI/#profile/";
        String endUserPath = "/idm/EndUser";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse responseEndUser = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn(endUserPath);
        when(request.getQueryString()).thenReturn(null);

        filter.doFilter(request, responseEndUser, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(responseEndUser).sendRedirect(captor.capture());
        assertThat(captor.getValue()).isEqualTo(CONTEXT + profilePage);
    }
}
