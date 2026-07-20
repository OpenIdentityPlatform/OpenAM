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
package com.sun.identity.jaxrpc;

import java.io.IOException;
import java.security.AccessController;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionType;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.debug.Debug;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet filter for the {@code /jaxrpc/*} endpoints.
 *
 * <p>The stock JAX-RPC servlet does not expose the incoming {@link HttpServletRequest}
 * to the service implementations. This filter captures the current request in a thread
 * local so that individual JAXRPC methods can read the caller's SSO token from the request
 * cookie when they need to authenticate the caller (e.g. the notification-URL registration
 * methods, to prevent unauthenticated stored SSRF - GHSA-w858-46wv-v45w).
 *
 * <p>The filter itself never rejects a request (the per-method {@code read/create/modify}
 * calls carry their token as an explicit argument and must keep working); authorization is
 * enforced per-method via {@link #isServerOrAgentAuthorized()}.
 */
public class JAXRPCRequestFilter implements Filter {

    /**
     * When set to {@code true}, the caller check performed by
     * {@link #isServerOrAgentAuthorized()} is skipped. Runtime toggle - lets legacy clients
     * that do not send an SSO cookie keep registering notification URLs without a rebuild.
     */
    public static final String SKIP_AUTH_CHECK =
            "org.openidentityplatform.notification.register.skip-auth-check";

    private static final Debug debug = Debug.getInstance("amJAXRPC");

    private static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST = new ThreadLocal<>();

    /**
     * @return the {@link HttpServletRequest} currently being processed on this thread by the
     *     {@code /jaxrpc/*} servlet, or {@code null} if there is none.
     */
    public static HttpServletRequest getCurrentRequest() {
        return CURRENT_REQUEST.get();
    }

    /**
     * Authorizes the current JAXRPC caller for notification-URL registration.
     *
     * <p>The caller is authorized when it presents (via the request cookie) a valid SSO token
     * that belongs to a <em>server</em> (an admin/application token) or an <em>agent</em> (an
     * {@link SessionType#APPLICATION} session). Regular user tokens and anonymous callers are
     * rejected. The check is skipped entirely when the {@link #SKIP_AUTH_CHECK} system property
     * is {@code true}.
     *
     * @return {@code true} if the caller is allowed to register/deregister notification URLs.
     */
    public static boolean isServerOrAgentAuthorized() {
        if (SystemProperties.getAsBoolean(SKIP_AUTH_CHECK, false)) {
            return true;
        }
        HttpServletRequest request = CURRENT_REQUEST.get();
        if (request == null) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCRequestFilter.isServerOrAgentAuthorized: "
                        + "no request bound to the current thread, rejecting");
            }
            return false;
        }
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken caller = manager.createSSOToken(request);
            if (caller == null || !manager.isValidToken(caller)) {
                return false;
            }
            // Server / admin token.
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            if (SessionUtils.isAdmin(adminToken, caller)) {
                return true;
            }
            // Agent: an application (agent/server) session that is still valid.
            Session session = new Session(new SessionID(caller.getTokenID().toString()));
            session.refresh(false);
            return !session.isTimedOut() && SessionType.APPLICATION.equals(session.getType());
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("JAXRPCRequestFilter.isServerOrAgentAuthorized: "
                        + "unable to authorize caller, rejecting", e);
            }
            return false;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to initialize
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean bound = false;
        if (request instanceof HttpServletRequest) {
            CURRENT_REQUEST.set((HttpServletRequest) request);
            bound = true;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (bound) {
                CURRENT_REQUEST.remove();
            }
        }
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }
}
