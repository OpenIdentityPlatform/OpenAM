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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sso.providers.stateless;

import java.security.Principal;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.session.blacklist.SessionBlacklist;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOProvider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

public class StatelessSSOProvider implements SSOProvider {

    private final StatelessSessionFactory statelessSessionFactory;
    private final SessionBlacklist sessionBlacklist;
    private final StatelessAdminRestriction restriction;
    private final Debug debug;

    /**
     * Default constructor is required by interface.
     */
    @Inject
    public StatelessSSOProvider(StatelessSessionFactory statelessSessionFactory,
                                SessionBlacklist sessionBlacklist,
                                StatelessAdminRestriction restriction,
                                @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.statelessSessionFactory = statelessSessionFactory;
        this.sessionBlacklist = sessionBlacklist;
        this.restriction = restriction;
        this.debug = debug;
    }

    private SSOToken createSSOToken(SessionID sessionId) throws SSOException {
        StatelessSession session;

        try {
            session = statelessSessionFactory.generate(sessionId);
        } catch (SessionException e) {
            throw new SSOException(e);
        }

        final StatelessSSOToken ssoToken = new StatelessSSOToken(session);
        if (isValidToken(ssoToken, false)) {
            return ssoToken;
        } else {
            Principal principal = null;
            try {
                principal = ssoToken.getPrincipal();
            } catch (SSOException e) {
                debug.warning("Could not obtain token principal for invalid token: " + e.getMessage(), e);
            }
            throw new SSOException("Token for principal " + (principal != null ? principal.getName() : null) + " invalid.");
        }
    }

    @Override
    public SSOToken createSSOToken(HttpServletRequest request) throws UnsupportedOperationException, SSOException {
        SessionID sid = new SessionID(request);
        return createSSOToken(sid);
    }

    @Override
    public SSOToken createSSOToken(Principal user, String password) throws SSOException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Unsupported Create SSO Token With Principal and Password");
    }

    @Override
    public SSOToken createSSOToken(String sid) throws SSOException, UnsupportedOperationException {
        return createSSOToken(sid, false);
    }

    public SSOToken createSSOToken(String sid, boolean invokedByAuth) throws SSOException, UnsupportedOperationException {
        return createSSOToken(sid, invokedByAuth, true);
    }

    @Override
    public SSOToken createSSOToken(String sid, boolean invokedByAuth, boolean possiblyResetIdleTime) throws SSOException, UnsupportedOperationException {
        SessionID sessionId = new SessionID(sid);
        return createSSOToken(sessionId);
    }

    @Override
    public SSOToken createSSOToken(String sid, String clientIP) throws SSOException, UnsupportedOperationException {
        SessionID sessionId = new SessionID(sid);
        return createSSOToken(sessionId);
    }

    @Override
    public void destroyToken(SSOToken token) throws SSOException {
        logout(token);
    }

    @Override
    public void logout(final SSOToken token) throws SSOException {
        try {
            extractStatelessSession(token).logout();
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public boolean isValidToken(SSOToken token) {
        return isValidToken(token, true);
    }

    @Override
    public boolean isValidToken(SSOToken token, boolean refresh) {
        final StatelessSSOToken statelessSSOToken = (StatelessSSOToken) token;
        final StatelessSession session = statelessSSOToken.getSession();

        // Stateless Sessions are not allowed for super users.
        try {
            if (restriction.isRestricted(token)) {
                return false;
            }
        } catch (SessionException e) {
            debug.message("Unable to verify if the SSOToken represents a super user", e);
            return false;
        }

        try {
            return statelessSSOToken.isValid(refresh) && !sessionBlacklist.isBlacklisted(session);
        } catch (SessionException e) {
            debug.error("Unable to check session blacklist: {}", e);
            return false;
        }
    }

    @Override
    public void validateToken(SSOToken token) throws SSOException {
        if (!isValidToken(token, false)) {
            throw new SSOException("Failed verification of JWT contents.");
        }
    }

    @Override
    public void refreshSession(SSOToken token) throws SSOException {
        refreshSession(token, true);
    }

    @Override
    public void refreshSession(SSOToken token, boolean resetIdle) throws SSOException {
        try {
            extractStatelessSession(token).refresh(resetIdle);
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public void destroyToken(SSOToken destroyer, SSOToken destroyed) throws SSOException {
        try {
            extractStatelessSession(destroyed).destroySession(extractStatelessSession(destroyed));
        } catch (SessionException e) {
            throw new SSOException(e);
        }
    }

    @Override
    public Set getValidSessions(SSOToken requester, String server) throws SSOException {
        return null;
    }

    /**
     * Static helper to get the stateless session from an SSO token.
     *
     * @throws SSOException if the token is not a stateless SSO token.
     */
    private static StatelessSession extractStatelessSession(SSOToken token) throws SSOException {
        if (token instanceof StatelessSSOToken) {
            return ((StatelessSSOToken) token).getSession();
        } else {
            throw new SSOException("Not a stateless SSOToken");
        }
    }
}
