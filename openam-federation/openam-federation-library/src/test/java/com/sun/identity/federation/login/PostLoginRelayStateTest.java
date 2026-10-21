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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.FederationStaticsLock;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.MetaManagerFailureFixture;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.debug.Debug;

/**
 * {@code /postLogin} sends the browser to the RelayState it names once the user has signed in.
 * The value is checked in {@code doPostLogin} itself; these cases go through that method, not
 * the check on its own, so the call is what they pin.
 */
public class PostLoginRelayStateTest {

    private static final String OFF_HOST = "https://evil.example/";
    private static final String ALLOWED = "https://trusted.example.com/landing";
    private static final String ERROR_PAGE =
        "http://host.example.com:8080/openam/config/federation/default/Error.jsp"
            + "?metaAlias=/idp";

    private static final Object SESSION = new Object();

    private IDFFMetaManager originalMetaInstance;
    private long originalFailedAt;
    private SessionProvider originalSessionProvider;
    private Debug originalDebug;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalMetaInstance = FSUtils.metaInstance;
        originalFailedAt = MetaManagerFailureFixture.failedAt();
        originalDebug = FSUtils.debug;
        originalSessionProvider = (SessionProvider) sessionProviderField().get(null);

        SessionProvider sessions = mock(SessionProvider.class);
        when(sessions.getSession(any(HttpServletRequest.class))).thenReturn(SESSION);
        when(sessions.getSessionID(SESSION)).thenReturn("session-id");
        when(sessions.getPrincipalName(SESSION)).thenReturn("uid=user,ou=people,dc=example,dc=com");
        sessionProviderField().set(null, sessions);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            FSUtils.metaInstance = originalMetaInstance;
            MetaManagerFailureFixture.setFailedAt(originalFailedAt);
            FSUtils.debug = originalDebug;
            sessionProviderField().set(null, originalSessionProvider);
        } finally {
            FederationStaticsLock.release();
        }
    }

    @DataProvider(name = "postLogin")
    public Object[][] postLogin() {
        return new Object[][] {
            { ALLOWED, ALLOWED },
            { OFF_HOST, HostedProviderFixture.HOME_URL }
        };
    }

    @Test(dataProvider = "postLogin")
    public void postLoginSendsTheUserOnlyToAnAllowedRelayState(String relayState, String target)
        throws Exception {

        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSPostLogin().doPostLogin(postLoginRequest(relayState), response);

        verify(response).sendRedirect(target);
        verify(response, times(1)).sendRedirect(anyString());
    }

    /**
     * A refused RelayState falls back to the provider's home page; with none configured there is
     * no safe page to send the user to, so the login ends on the error page.
     */
    @Test
    public void postLoginEndsOnTheErrorPageWhenARefusedRelayStateHasNoHomePageToFallBackTo()
        throws Exception {

        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowListAndNoHomePage();
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSPostLogin().doPostLogin(postLoginRequest(OFF_HOST), response);

        verify(response).sendRedirect(ERROR_PAGE);
        verify(response, never()).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Without a meta manager there is no error page either: the configuration that names it is
     * what could not be read. The login answers 500, once, rather than an empty 200.
     */
    @Test
    public void postLoginAnswersAnErrorStatusOnceWhenTheConfigurationCannotBeRead()
        throws Exception {

        FSUtils.metaInstance = null;
        HttpServletResponse response = mock(HttpServletResponse.class);

        new FSPostLogin().doPostLogin(postLoginRequest(OFF_HOST), response);

        verify(response, times(1)).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response, never()).sendRedirect(anyString());
    }

    /**
     * {@code FSPostLogin} captured the meta manager when the class loaded, so an SMS not up yet
     * at that moment sent every /postLogin to the error page until a restart. A refused
     * RelayState falls back to the home page of the provider configured now.
     */
    @Test
    public void postLoginRecoversWhenTheMetaManagerBecomesAvailableAfterClassLoad()
        throws Exception {

        FSUtils.metaInstance = null;
        // touches the class with no manager available - what an SMS outage at startup looks like
        new FSPostLogin().doPostLogin(postLoginRequest(OFF_HOST), mock(HttpServletResponse.class));

        FSUtils.metaInstance = HostedProviderFixture.secondFederatingProvider();
        HttpServletResponse response = mock(HttpServletResponse.class);
        new FSPostLogin().doPostLogin(postLoginRequest(OFF_HOST), response);

        verify(response).sendRedirect(HostedProviderFixture.SECOND_HOME_URL);
    }

    /**
     * The refused RelayState and the entity it was refused for are the caller's strings: a line
     * break in either must not start a forged entry in the debug file.
     */
    @Test
    public void refusedRelayStateIsLoggedOnOneLine() throws Exception {
        IDFFMetaManager metaManager = HostedProviderFixture.providerWithAllowList();
        when(metaManager.getEntityIDByMetaAlias(HostedProviderFixture.META_ALIAS))
            .thenReturn(HostedProviderFixture.ENTITY + "\r\nX");
        FSUtils.metaInstance = metaManager;
        Debug debug = installedDebug();

        new FSPostLogin().doPostLogin(postLoginRequest("https://evil.example/\r\nX"),
            mock(HttpServletResponse.class));

        verify(debug).warning("FSPostLogin::doPostLogin RelayState \"https://evil.example/__X\" "
            + "rejected for entity https://sp.example.com/idff__X; using provider home URL");
    }

    /**
     * The metaAlias is the caller's string too, and one that resolves to nothing leaves no home
     * page to fall back to.
     */
    @Test
    public void metaAliasWithNoReturnUrlIsLoggedOnOneLine() throws Exception {
        FSUtils.metaInstance = HostedProviderFixture.providerWithAllowList();
        Debug debug = installedDebug();
        HttpServletRequest request = postLoginRequest(OFF_HOST);
        when(request.getParameter(IFSConstants.META_ALIAS)).thenReturn("/idp\r\nX");

        new FSPostLogin().doPostLogin(request, mock(HttpServletResponse.class));

        verify(debug).warning("FSPostLogin::doPostLogin no valid return URL for metaAlias "
            + "\"/idp__X\"; sending error page");
    }

    /** A debug log with warnings on, installed as the one FSUtils writes to. */
    private static Debug installedDebug() {
        Debug debug = mock(Debug.class);
        when(debug.warningEnabled()).thenReturn(true);
        FSUtils.debug = debug;
        return debug;
    }

    /**
     * The browser's return to {@code /postLogin} after sign-in. Its server name is left unset,
     * so every target reads as another container's and is redirected to, which names it.
     */
    private static HttpServletRequest postLoginRequest(String relayState) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("host.example.com:8080");
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://host.example.com:8080/openam/postLogin"));
        when(request.getRequestURI()).thenReturn("/openam/postLogin");
        when(request.getParameter(IFSConstants.META_ALIAS))
            .thenReturn(HostedProviderFixture.META_ALIAS);
        when(request.getParameter(IFSConstants.LRURL)).thenReturn(relayState);
        return request;
    }

    /** The provider {@code SessionManager.getProvider} hands out, resolved once at class load. */
    private static Field sessionProviderField() throws Exception {
        Field field = SessionManager.class.getDeclaredField("sp");
        field.setAccessible(true);
        return field;
    }
}
