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

package com.sun.identity.authentication.service;

import javax.security.auth.Subject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionFactory;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;

/**
 * Creates stateless sessions after authentication.
 */
class StatelessSessionActivator extends DefaultSessionActivator {
    static final StatelessSessionActivator INSTANCE = new StatelessSessionActivator();

    private volatile StatelessSessionFactory statelessSessionFactory;
    private StatelessSession oldSession;

    @VisibleForTesting
    StatelessSessionActivator(final StatelessSessionFactory statelessSessionFactory) {
        this.statelessSessionFactory = statelessSessionFactory;
    }

    private StatelessSessionActivator() {
    }

    @Override
    public boolean activateSession(final LoginState loginState, final SessionService sessionService,
                                   final InternalSession authSession, final Subject subject, final Object loginContext)
            throws AuthException {

        if (loginState.getForceFlag()) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Cannot force auth stateless sessions.");
            }
            throw new AuthException(AMAuthErrorCode.STATELESS_FORCE_FAILED, null);
        }

        if (loginState.isSessionUpgrade()) {
            //set our old session -- necessary as if the currently owned token is stateless this won't be set
            SessionID sid = new SessionID(loginState.getHttpServletRequest());
            try {
                SessionInfo info = getStatelessSessionFactory().getSessionInfo(sid);
                oldSession = getStatelessSessionFactory().generate(info);
                loginState.setOldStatelessSession(oldSession);
            } catch (SessionException e) {
                throw new AuthException(AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
            }
        }

        //create our new session - the loginState needs this session as it's the one we'll be passing back to the user
        final InternalSession session = createSession(sessionService, loginState);
        loginState.setSession(session);

        return updateSessions(session, loginState, session, authSession, sessionService, subject, loginContext);
    }

    @Override
    protected InternalSession createSession(SessionService sessionService, LoginState loginState) {
        return sessionService.newInternalSession(loginState.getOrgDN(), null, true);
    }

    @Override
    protected boolean activateSession(InternalSession session, LoginState loginState) throws SessionException {
        boolean activated = session.activate(loginState.getUserDN(), true);
        if (activated) {
            // Update the session id in the login state to reflect the activated session
            loginState.setSessionID(getStatelessSessionFactory().generate(session).getID());
        }
        // Make sure that session is never scheduled
        session.cancel();

        if (oldSession != null) {
            try {
                oldSession.logout(); //attempt to blacklist the old session
            } catch (SessionException e) {
                DEBUG.warning("Unable to blacklist old stateless session after session upgrade.");
            }
        }

        return activated;
    }

    private StatelessSessionFactory getStatelessSessionFactory() {
        if (statelessSessionFactory == null) {
            statelessSessionFactory = InjectorHolder.getInstance(StatelessSessionFactory.class);
        }
        return statelessSessionFactory;
    }
}
