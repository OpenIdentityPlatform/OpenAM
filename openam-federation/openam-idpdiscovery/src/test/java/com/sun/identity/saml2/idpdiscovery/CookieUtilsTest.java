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
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.saml2.idpdiscovery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies that {@link CookieUtils#isRedirectUrlValid} blocks the open redirect
 * described in GHSA-2pf8-52jh-5x3m while still allowing legitimate same-origin
 * and relative RelayState redirects.
 */
public class CookieUtilsTest {

    private HttpServletRequest request;

    @BeforeMethod
    public void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("idp.example.com");
        when(request.getServerPort()).thenReturn(443);
    }

    @Test
    public void allowsRelativeUrl() {
        assertTrue(CookieUtils.isRedirectUrlValid(request, "/sp/return?foo=bar"));
    }

    @Test
    public void allowsSameOriginAbsoluteUrl() {
        assertTrue(CookieUtils.isRedirectUrlValid(request,
                "https://idp.example.com:443/openam/back"));
    }

    @Test
    public void allowsSameOriginAbsoluteUrlWithoutExplicitPort() {
        assertTrue(CookieUtils.isRedirectUrlValid(request,
                "https://idp.example.com/openam/back"));
    }

    @Test
    public void rejectsExternalAbsoluteUrl() {
        assertFalse(CookieUtils.isRedirectUrlValid(request,
                "https://attacker.example/capture"));
    }

    @Test
    public void rejectsProtocolRelativeUrl() {
        assertFalse(CookieUtils.isRedirectUrlValid(request,
                "//attacker.example/capture"));
    }

    @Test
    public void rejectsBackslashTrick() {
        // Browsers normalise the backslash to "/", yielding "//attacker.example".
        assertFalse(CookieUtils.isRedirectUrlValid(request,
                "/\\attacker.example/capture"));
    }

    @Test
    public void rejectsNonHttpScheme() {
        assertFalse(CookieUtils.isRedirectUrlValid(request,
                "javascript:alert(document.domain)"));
    }

    @Test
    public void rejectsNullAndEmpty() {
        assertFalse(CookieUtils.isRedirectUrlValid(request, null));
        assertFalse(CookieUtils.isRedirectUrlValid(request, "   "));
    }
}
