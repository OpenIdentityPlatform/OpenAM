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

import javax.security.auth.Subject;

/**
 * Encapsulates logic for activating a session after successful authentication. Implements a strategy design pattern
 * to separate the details of different session activation strategies (e.g., session upgrade).
 */
public interface SessionActivator {
    /**
     * Activates the given session after successful authentication, returning the an indication of whether activation
     * was successful. The {@link LoginState} should be updated to reflect the activated session.
     *
     * @param loginState the login state used for authentication. May be updated by the activator.
     * @param sessionService the session service.
     * @param authSession the session used for authentication.
     * @param subject the authenticated subject.
     * @param loginContext the login context.
     * @return whether activation was successful.
     * @throws AuthException if an error occurs that prevents session activation.
     */
    boolean activateSession(LoginState loginState, SessionService sessionService,
                            InternalSession authSession, Subject subject, Object loginContext) throws AuthException;
}
