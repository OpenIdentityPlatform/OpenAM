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

package com.sun.identity.federation.login;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.shared.debug.Debug;

/**
 * {@code /preLogin} is reachable without authentication and reads
 * {@code gotoOnFedCookieNo} straight from the query string. It used to hand that value to
 * {@code sendRedirect} with no validation at all, which is the same open redirect the
 * RelayState allow-list closes at {@code /postLogin}.
 */
public class FSPreLoginTest {

    private static final String OFF_HOST = "https://evil.example/";
    private static final String ALLOWED = "https://trusted.example.com/landing";
    private static final String FED_COOKIE = "fedCookie";

    private String originalFedCookieName;
    private IDFFMetaManager originalMetaInstance;
    private Debug originalDebug;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        // resolved from a system property in a static initialiser, which is unset under test
        originalFedCookieName = (String) fedCookieNameField().get(null);
        fedCookieNameField().set(null, FED_COOKIE);

        // FSPreLogin.setMetaInfo throws its way to the error page without a meta manager, so
        // the branch under test is only reachable with a provider that resolves
        originalMetaInstance = FSUtils.metaInstance;
        originalDebug = FSUtils.debug;
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            fedCookieNameField().set(null, originalFedCookieName);
            FSUtils.metaInstance = originalMetaInstance;
            FSUtils.debug = originalDebug;
        } finally {
            FederationStaticsLock.release();
        }
    }

    @Test
    public void shouldNotRedirectToAnUncheckedGotoOnFedCookieNo() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSPreLogin().doPreLogin(preLoginRequest(OFF_HOST), response);

        verify(response, never()).sendRedirect(OFF_HOST);
    }

    /**
     * The refusal above passes just as well when the branch is never reached at all -
     * {@code doPreLogin} swallows everything in one try/catch, so a change to
     * {@code setMetaInfo}, {@code getBaseURL} or the cookie parsing would make it green
     * without the guard doing anything. This pins the same branch from the other side: a
     * {@code gotoOnFedCookieNo} the provider's allow-list names is still redirected to.
     */
    @Test
    public void shouldStillRedirectToAnAllowedGotoOnFedCookieNo() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSPreLogin().doPreLogin(preLoginRequest(ALLOWED), response);

        verify(response).sendRedirect(ALLOWED);
    }

    /** The refused goto is logged as the caller sent it, but on one line. */
    @Test
    public void refusedGotoOnFedCookieNoIsLoggedOnOneLine() throws Exception {
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        FSUtils.debug = debug;

        new FSPreLogin().doPreLogin(preLoginRequest("https://evil.example/\r\nX"),
            mock(HttpServletResponse.class));

        verify(debug).warning("FSPreLogin::doPreLogin gotoOnFedCookieNoURL "
            + "\"https://evil.example/__X\" is not permitted by the provider's RelayState "
            + "allow-list; using the login URL");
    }

    /** A /preLogin GET whose fedCookie says "no", which is the branch that reads the goto. */
    private static HttpServletRequest preLoginRequest(String gotoOnFedCookieNo) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeaderNames())
            .thenReturn(Collections.enumeration(Collections.<String>emptyList()));
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/preLogin"));
        when(request.getRequestURI()).thenReturn("/openam/preLogin");
        when(request.getServerName()).thenReturn("host.example.com");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getParameter(IFSConstants.GOTO_ON_FEDCOOKIE_NO_URL))
            .thenReturn(gotoOnFedCookieNo);
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie(FED_COOKIE, "no") });
        return request;
    }

    private static Field fedCookieNameField() throws Exception {
        Field field = FSPreLogin.class.getDeclaredField("fedCookieName");
        field.setAccessible(true);
        return field;
    }
}
