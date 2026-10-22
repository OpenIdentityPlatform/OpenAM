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

package com.sun.identity.federation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

import com.sun.identity.plugin.session.SessionListener;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.shared.debug.Debug;

/**
 * URL rewriting appends the session token to the target for cookie-less clients, so
 * {@code FSUtils.forwardRequest} must only rewrite a target served by this container.
 * Rewriting an off-host target would publish the token to that host through the
 * {@code sendRedirect} branch.
 * <p>
 * A recording {@link SessionProvider} is bound directly into {@code SessionManager}'s
 * resolved-provider field, which is the only seam that observes whether {@code rewriteURL}
 * was called at all. Going through the {@code com.sun.identity.plugin.session.class}
 * property instead would only take effect if this test happened to be the first in the JVM
 * to touch {@code SessionManager}'s static initialiser - and TestNG gives no cross-package
 * ordering guarantee, so on the other ordering these tests silently stopped testing
 * anything and the leaked property changed every later test in the module.
 */
public class FSUtilsForwardRewriteTest {

    private static final String SAME_CONTAINER =
        "http://host.example.com:8080/openam/wsfederation/jsp/mex.jsp";
    private static final String OFF_HOST = "https://evil.example/openam/landing";

    private String originalDeploymentURI;
    private SessionProvider originalProvider;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private RequestDispatcher dispatcher;
    private Debug originalDebug;
    private Debug debug;

    @BeforeMethod
    public void setUp() throws Exception {
        FederationStaticsLock.acquire();
        originalDebug = FSUtils.debug;
        debug = mock(Debug.class);
        FSUtils.debug = debug;
        originalProvider = (SessionProvider) providerField().get(null);
        providerField().set(null, new RecordingSessionProvider());

        originalDeploymentURI = FSUtils.deploymentURI;
        FSUtils.deploymentURI = "/openam";

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getServerName()).thenReturn("host.example.com");
        when(request.getServerPort()).thenReturn(8080);
        dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);

        RecordingSessionProvider.REWRITTEN.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        try {
            providerField().set(null, originalProvider);
            FSUtils.deploymentURI = originalDeploymentURI;
            FSUtils.debug = originalDebug;
            RecordingSessionProvider.REWRITTEN.clear();
        } finally {
            FederationStaticsLock.release();
        }
    }

    /** The provider {@code SessionManager.getProvider} hands out, resolved once at class load. */
    private static Field providerField() throws Exception {
        Field field = SessionManager.class.getDeclaredField("sp");
        field.setAccessible(true);
        return field;
    }

    @Test
    public void shouldNotRewriteOffHostTarget() throws Exception {
        FSUtils.forwardRequest(request, response, OFF_HOST);

        assertThat(RecordingSessionProvider.REWRITTEN)
            .as("session token must never be appended to an off-host target")
            .isEmpty();
    }

    @Test
    public void shouldStillRewriteSameContainerTarget() throws Exception {
        FSUtils.forwardRequest(request, response, SAME_CONTAINER);

        assertThat(RecordingSessionProvider.REWRITTEN)
            .as("rewriting must be preserved for targets this container serves")
            .containsExactly(SAME_CONTAINER);
    }

    /**
     * {@code forwardRequest} re-checks the container after rewriting, because a provider whose
     * {@code rewriteURL} does more than append a path parameter can move the target. When it
     * does move it off-host the rewritten string is the one carrying the session token, so it
     * is the one that must not be sent: the un-rewritten URL is redirected instead.
     */
    @Test
    public void shouldNotSendARewrittenUrlThatMovedOffHost() throws Exception {
        providerField().set(null, new MovingSessionProvider());

        FSUtils.forwardRequest(request, response, SAME_CONTAINER);

        verify(response).sendRedirect(SAME_CONTAINER);
        verify(response, never()).sendRedirect(MovingSessionProvider.MOVED);
    }

    @Test
    public void shouldBoundTheLoggedLengthOfATargetThatRewritingMovedOffHost() throws Exception {
        providerField().set(null, new MovingSessionProvider());

        FSUtils.forwardRequest(request, response, SAME_CONTAINER + "?x=" + "a".repeat(300));

        verify(debug).error("FSUtils.forwardRequest: URL rewriting moved the target off this "
            + "container; redirecting the un-rewritten URL:" + SAME_CONTAINER + "?x="
            + "a".repeat(193) + "...[truncated, 363 characters]");
    }

    @Test
    public void shouldRedirectTheArrivedUrlWhenTheRewrittenOneDoesNotParse() throws Exception {
        providerField().set(null, new UnparseableSessionProvider());

        FSUtils.forwardRequest(request, response, SAME_CONTAINER);

        verify(response).sendRedirect(SAME_CONTAINER);
        verify(response, never()).sendError(anyInt());
    }

    /**
     * The forward/redirect decision reads the port as the dispatched string writes it, and
     * that string is the rewritten one.
     */
    @Test
    public void shouldDecideOnTheRewrittenUrlsWrittenPort() throws Exception {
        providerField().set(null, new PortDroppingSessionProvider());
        when(request.getServerPort()).thenReturn(80);

        FSUtils.forwardRequest(request, response,
            "http://host.example.com:80/openam/wsfederation/jsp/mex.jsp");

        verify(response).sendRedirect(PortDroppingSessionProvider.REWRITTEN_URL);
        verify(dispatcher, never()).forward(any(), any());
    }

    /** Rewrites into something that is not a URL at all. */
    public static class UnparseableSessionProvider extends RecordingSessionProvider {

        @Override
        public String rewriteURL(Object session, String url) {
            super.rewriteURL(session, url);
            return "not a url;AMAuthCookie=token";
        }
    }

    /** Keeps the target on this container but stops writing its port. */
    public static class PortDroppingSessionProvider extends RecordingSessionProvider {

        static final String REWRITTEN_URL =
            "http://host.example.com/openam/wsfederation/jsp/mex.jsp;AMAuthCookie=token";

        @Override
        public String rewriteURL(Object session, String url) {
            super.rewriteURL(session, url);
            return REWRITTEN_URL;
        }
    }

    /** Rewrites onto another host, the case the post-rewrite container re-check exists for. */
    public static class MovingSessionProvider extends RecordingSessionProvider {

        static final String MOVED = "https://evil.example/openam/landing;AMAuthCookie=token";

        @Override
        public String rewriteURL(Object session, String url) {
            super.rewriteURL(session, url);
            return MOVED;
        }
    }

    /** Records every URL it is asked to rewrite. Must be public with a no-arg constructor. */
    public static class RecordingSessionProvider implements SessionProvider {

        static final List<String> REWRITTEN = new ArrayList<String>();

        private static final Object TOKEN = new Object();

        public String rewriteURL(Object session, String url) {
            REWRITTEN.add(url);
            return url + ";AMAuthCookie=token";
        }

        public Object getSession(HttpServletRequest request) {
            return TOKEN;
        }

        public boolean isValid(Object session) {
            return true;
        }

        public Object createSession(java.util.Map info, HttpServletRequest request,
                HttpServletResponse response, StringBuffer targetApplication) {
            throw new UnsupportedOperationException();
        }

        public Object getSession(String sessionID) {
            throw new UnsupportedOperationException();
        }

        public void invalidateSession(Object session, HttpServletRequest request,
                HttpServletResponse response) {
            throw new UnsupportedOperationException();
        }

        public String getSessionID(Object session) {
            return "sessionID";
        }

        public String getPrincipalName(Object session) {
            return "principal";
        }

        public void setProperty(Object session, String name, String[] values) {
            throw new UnsupportedOperationException();
        }

        public String[] getProperty(Object session, String name) {
            throw new UnsupportedOperationException();
        }

        public void addListener(Object session, SessionListener listener) {
            throw new UnsupportedOperationException();
        }

        public void setLoadBalancerCookie(HttpServletRequest request,
                HttpServletResponse response) {
            throw new UnsupportedOperationException();
        }

        public long getTimeLeft(Object session) {
            throw new UnsupportedOperationException();
        }
    }
}
