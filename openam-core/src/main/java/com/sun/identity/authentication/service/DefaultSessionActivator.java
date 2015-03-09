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

package com.sun.identity.authentication.service;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

import java.util.Enumeration;

/**
 * The default session activator
 */
class DefaultSessionActivator implements SessionActivator {
    static final DefaultSessionActivator INSTANCE = new DefaultSessionActivator();

    private static final Debug DEBUG = AuthD.debug;

    @Override
    public InternalSession activateSession(final LoginState loginState, final SessionService sessionService,
                                           final InternalSession authSession) throws AuthException {

        final SessionID oldSessId = authSession.getID();

        final InternalSession session = createSession(sessionService, loginState.getOrgDN());

        session.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
        loginState.setSessionProperties(session);

        //copying over the session properties that were set on the authentication session onto the new session
        // TODO: move forceAuth (and session upgrade) into a different session activator
        final InternalSession sessionToUpdate = loginState.getForceFlag() ? loginState.getOldSession() : session;
        Enumeration<String> authSessionProperties = authSession.getPropertyNames();
        while (authSessionProperties.hasMoreElements()) {
            String key = authSessionProperties.nextElement();
            String value = authSession.getProperty(key);
            sessionToUpdate.putProperty(key, value);
        }

        //destroying the authentication session
        sessionService.destroyInternalSession(oldSessId);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Activating session: " + session);
        }

        return session;
    }


    protected InternalSession createSession(SessionService sessionService, String domain) {
        return sessionService.newInternalSession(domain, null);
    }

}
