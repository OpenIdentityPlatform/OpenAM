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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.service.activators;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.DefaultSessionActivator;
import com.sun.identity.authentication.service.LoginState;

import javax.security.auth.Subject;

/**
 * The force auth session activator: creates a new session, sets that as the current session
 * (setSessionProperties in loginState then replaces that with the  old session),
 * copies in properties from the auth session, deletes the auth session and returns.
 */
public final class ForceAuthSessionActivator extends DefaultSessionActivator {

    private static final ForceAuthSessionActivator INSTANCE = new ForceAuthSessionActivator();

    /**
     * Private constructor keeps things restricted to our singleton instance.
     */
    private ForceAuthSessionActivator() {
    }

    @Override
    public boolean activateSession(final LoginState loginState, final SessionService sessionService,
                                   final InternalSession authSession, final Subject subject, final Object loginContext)
            throws AuthException {

        if (!loginState.getForceFlag()) {
            throw new AuthException("Invalid session to activate.");
        }

        final InternalSession session = createSession(sessionService, loginState);

        return updateSessions(session, loginState, loginState.getOldSession(), authSession, sessionService, subject,
                loginContext);
    }

    public static ForceAuthSessionActivator getInstance() {
        return INSTANCE;
    }
}
