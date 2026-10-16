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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems LLC.
 */

package com.sun.identity.shared.encode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.testng.annotations.Test;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

public class CookieUtilsTest {

    @Test
    public void getMatchingCookieDomains() {
    	SystemPropertiesManager.initializeProperties(Constants.SET_COOKIE_TO_ALL_DOMAINS, "false");
    	HttpServletRequest request=mock(HttpServletRequest.class);
    	when(request.getServerName()).thenReturn("openam.openshift.dev.domain.ru");
    	assertEquals(
	    	CookieUtils.getMatchingCookieDomains(request, Arrays.asList(new String[] {
	    			"localhost",
	    			".openshift.dev.domain.ru",
	    			".dev.domain.ru",
	    			".inside.domain.ru",
	    			".domain.ru"
	    	})), 
	    	(Set<String>)new HashSet<String>(Arrays.asList(new String[] {
	    		"domain.ru",
	    		"dev.domain.ru",
	    		"openshift.dev.domain.ru"
	    	}))
	    );
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
            assertTrue(CookieUtils.newCookie("iPlanetDirectoryPro", "AQIC").getSecure()));
        withCookieSettings(false, false, null, () ->
            assertFalse(CookieUtils.newCookie("iPlanetDirectoryPro", "AQIC").getSecure()));
    }

    /**
     * HttpOnly used to be applied in addCookieToResponse only, so a cookie handed straight to
     * response.addCookie went out without it; the cookie now carries it from creation.
     */
    @Test
    public void newCookieCarriesTheHttpOnlyFlagTheDeploymentConfigures() {
        withCookieSettings(false, true, null, () ->
            assertTrue(CookieUtils.newCookie("iPlanetDirectoryPro", "AQIC").isHttpOnly()));
        withCookieSettings(false, false, null, () ->
            assertFalse(CookieUtils.newCookie("iPlanetDirectoryPro", "AQIC").isHttpOnly()));
    }

    @Test
    public void addCookieToResponseSetsHttpOnlyOnTheServletCookie() {
        withCookieSettings(false, true, null, () -> {
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie cookie = new Cookie("iPlanetDirectoryPro", "AQIC");

            CookieUtils.addCookieToResponse(response, cookie);

            assertTrue(cookie.isHttpOnly());
            verify(response).addCookie(cookie);
            verify(response, never()).addHeader(anyString(), anyString());
        });
    }

    @Test
    public void addCookieToResponseLeavesHttpOnlyAloneWhenNotConfigured() {
        withCookieSettings(false, false, null, () -> {
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie cookie = new Cookie("iPlanetDirectoryPro", "AQIC");

            CookieUtils.addCookieToResponse(response, cookie);

            assertFalse(cookie.isHttpOnly());
            verify(response).addCookie(cookie);
        });
    }

    /** With SameSite configured the cookie goes out as a hand-built header, flags included. */
    @Test
    public void addCookieToResponseWritesTheHeaderItselfWhenSameSiteIsConfigured() {
        withCookieSettings(true, true, "Strict", () -> {
            HttpServletResponse response = mock(HttpServletResponse.class);
            Cookie cookie = new Cookie("iPlanetDirectoryPro", "AQIC");
            cookie.setPath("/");

            CookieUtils.addCookieToResponse(response, cookie);

            verify(response, never()).addCookie(any(Cookie.class));
            verify(response).addHeader(eq("Set-Cookie"),
                    eq("iPlanetDirectoryPro=AQIC;path=/;secure;httponly;SameSite=Strict"));
        });
    }
}
