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

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;

/**
 * Session activator that just destroys the old session and does not create any new session. Used for cases where
 * OpenAM is being used for authentication only and not session management.
 */
enum NoSessionActivator implements SessionActivator {
    INSTANCE;

    /**
     * Destroys the existing auth session, but does not activate any new session.
     *
     * @param loginState the login state used for authentication. Unused.
     * @param sessionService the session service.
     * @param authSession the session used for authentication.
     * @return {@code null} always
     */
    @Override
    public InternalSession activateSession(final LoginState loginState, final SessionService sessionService,
                                           final InternalSession authSession) {
        sessionService.destroyInternalSession(authSession.getID());
        return null;
    }
}
