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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sso.providers.stateless;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOProvider;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import org.forgerock.openam.session.SessionCache;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Set;

public class StatelessSSOProvider implements SSOProvider {

    private final SessionCache sessionCache;
    private final StatelessSessionFactory statelessSessionFactory;

    /**
     * Default constructor is required by interface.
     */
    @Inject
    public StatelessSSOProvider(SessionCache sessionCache,
                                StatelessSessionFactory statelessSessionFactory) {
        this.sessionCache = sessionCache;
        this.statelessSessionFactory = statelessSessionFactory;
    }

    private SSOToken createSSOToken(SessionID sessionId) throws SSOException {
        StatelessSession session;

        try {
            session = statelessSessionFactory.generate(sessionId);
        } catch (SessionException e) {
            e.printStackTrace();
            throw new SSOException(e);
        }

        StatelessSSOToken ssoToken = new StatelessSSOToken(session);
        return isValidToken(ssoToken, false) ? ssoToken : null;
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
    }

    @Override
    public boolean isValidToken(SSOToken token) {
        return ((StatelessSSOToken) token).isValid();
    }

    @Override
    public boolean isValidToken(SSOToken token, boolean refresh) {
        return isValidToken(token);
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
            SSOTokenID tokenId = token.getTokenID();
            SessionID sid = new SessionID(tokenId.toString());
            Session session = sessionCache.getSession(sid);
            session.refresh(resetIdle);
        } catch (Exception e) {
            throw new SSOException(e);
        }
    }

    @Override
    public void destroyToken(SSOToken destroyer, SSOToken destroyed) throws SSOException {
        // TODO - check permissions etc via Session#destroy(Session) - see SSOProviderImpl

        // Ensure destroyed token is added to blacklist
        destroyToken(destroyed);
    }

    @Override
    public Set getValidSessions(SSOToken requester, String server) throws SSOException {
        return null;
    }
}
