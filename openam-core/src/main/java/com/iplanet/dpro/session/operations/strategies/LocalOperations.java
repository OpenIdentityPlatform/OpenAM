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
 * Copyright 2014 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations.strategies;

import com.google.inject.name.Named;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.operations.SessionOperations;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;

import javax.inject.Inject;
import java.text.MessageFormat;

/**
 * Responsible for applying Session operations on the local Server instance.
 *
 * This will be based on invoking the {@link SessionService} directly. This implementation
 * has been refactored out from {@link Session}.
 */
public class LocalOperations implements SessionOperations {

    private final SessionService service;
    private final Debug debug;

    /**
     * Guice initialised constructor.
     *
     * @param debug Non null.
     * @param service Non null.
     */
    @Inject
    public LocalOperations(@Named(SessionConstants.SESSION_DEBUG) Debug debug, SessionService service) {
        this.service = service;
        this.debug = debug;
    }

    /**
     * Fetches the SessionInfo from the SessionService.
     *
     * @param session The Session to update.
     * @param reset If true, then update the last modified timestamp of the Session.
     * @return Null if there was an error locating the Session, otherwise non null.
     */
    public SessionInfo refresh(Session session, boolean reset) throws SessionException {
        SessionID sessionID = session.getID();
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Local fetch SessionInfo for {0}\n" +
                            "Reset: {1}",
                    sessionID.toString(),
                    reset));
        }
        return service.getSessionInfo(sessionID, reset);
    }


    /**
     * Logs out the Session using the SessionService.
     *
     * @param session Session to logout.
     */
    public void logout(Session session) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Local logout for {0}",
                    session.getID().toString()));
        }
        service.logout(session.getID());
    }

    /**
     * Destroy the Session using the SessionService.
     *
     * @param requester {@inheritDoc}
     * @param session {@inheritDoc}
     * @throws SessionException {@inheritDoc}
     */
    @Override
    public void destroy(Session requester, Session session) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format("Local destroy for {0}", session.getID().toString()));
        }
        service.destroySession(requester, session.getID());
    }

    /**
     * Sets the property using the SessionService.
     * {@inheritDoc}
     */
    public void setProperty(Session session, String name, String value) throws SessionException {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    "Local setProperty for {0} {1}={2}",
                    session.getID().toString(),
                    name,
                    value));
        }
        service.setProperty(session.getID(), name, value);
    }
}
