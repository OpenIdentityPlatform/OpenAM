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
package com.sun.identity.saml2.idpdiscovery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CookieWriterServletTest {

    private boolean savedHttpOnly;
    private String savedSameSite;

    @BeforeMethod
    public void routeTheCookieThroughTheContainer() {
        savedHttpOnly = CookieUtils.cookieHttpOnly;
        savedSameSite = CookieUtils.cookieSameSite;
        CookieUtils.cookieHttpOnly = false;
        CookieUtils.cookieSameSite = null;
    }

    @AfterMethod
    public void restoreTheCookieSettings() {
        CookieUtils.cookieHttpOnly = savedHttpOnly;
        CookieUtils.cookieSameSite = savedSameSite;
    }

    @Test
    public void goesOnToTheRelayStateWhenTheContainerRefusesTheCookie() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/idpdiscovery/saml2writer");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("cdc.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getParameter("_saml_idp")).thenReturn("https://idp.example.com");
        when(request.getParameter("RelayState")).thenReturn("/sp/return");
        HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(new IllegalArgumentException("An invalid domain [example.com:8443] was specified"))
                .when(response).addCookie(any(Cookie.class));

        new CookieWriterServlet().doGet(request, response);

        verify(response).sendRedirect("/sp/return");
    }
}
