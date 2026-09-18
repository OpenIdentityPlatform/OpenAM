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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    /** Runs {@code body} with the deployment's Secure/HttpOnly/SameSite cookie settings pinned. */
    private static void withCookieSettings(boolean secure, boolean httpOnly, String sameSite, Runnable body) {
        boolean savedSecure = CookieUtils.secureCookie;
        boolean savedHttpOnly = CookieUtils.cookieHttpOnly;
        String savedSameSite = CookieUtils.cookieSameSite;
        try {
            CookieUtils.secureCookie = secure;
            CookieUtils.cookieHttpOnly = httpOnly;
            CookieUtils.cookieSameSite = sameSite;
            body.run();
        } finally {
            CookieUtils.secureCookie = savedSecure;
            CookieUtils.cookieHttpOnly = savedHttpOnly;
            CookieUtils.cookieSameSite = savedSameSite;
        }
    }

    @Test
    public void newCookieCarriesTheSecureFlagTheDeploymentConfigures() {
        withCookieSettings(true, false, null, () ->
            assertTrue(CookieUtils.newCookie("_saml_idp", "aWRw").getSecure()));
        withCookieSettings(false, false, null, () ->
            assertFalse(CookieUtils.newCookie("_saml_idp", "aWRw").getSecure()));
    }

    @Test
    public void newCookieCarriesTheHttpOnlyFlagTheDeploymentConfigures() {
        withCookieSettings(false, true, null, () ->
            assertTrue(CookieUtils.newCookie("_saml_idp", "aWRw").isHttpOnly()));
        withCookieSettings(false, false, null, () ->
            assertFalse(CookieUtils.newCookie("_saml_idp", "aWRw").isHttpOnly()));
    }

    /** Without SameSite the cookie goes through the servlet API, HttpOnly set on the cookie itself. */
    @Test
    public void addCookieToResponseSetsHttpOnlyOnTheServletCookie() {
        withCookieSettings(false, true, null, () -> {
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie cookie = new Cookie("_saml_idp", "aWRw");

            CookieUtils.addCookieToResponse(response, cookie);

            assertTrue(cookie.isHttpOnly());
            verify(response).addCookie(cookie);
            verify(response, never()).addHeader(anyString(), anyString());
        });
    }

    /** With SameSite configured the cookie goes out as a hand-built header, flags included. */
    @Test
    public void addCookieToResponseWritesTheHeaderItselfWhenSameSiteIsConfigured() {
        withCookieSettings(true, true, "Lax", () -> {
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie cookie = new Cookie("_saml_idp", "aWRw");
            cookie.setPath("/");

            CookieUtils.addCookieToResponse(response, cookie);

            verify(response, never()).addCookie(any(Cookie.class));
            verify(response).addHeader(eq("SET-COOKIE"), eq("_saml_idp=aWRw;path=/;secure;httponly;SameSite=Lax"));
        });
    }
}
