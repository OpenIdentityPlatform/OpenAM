/**
 * Copyright 2014 ForgeRock AS.
 *
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
 */
package com.iplanet.dpro.session.utils;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.TokenRestrictionFactory;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;

import java.text.MessageFormat;

/**
 * Responsible for providing a collection of utility functions for
 * manipulating InternalSessions.
 */
public class SessionInfoFactory {

    private static final String ERROR_FORMAT = "{0} {1}";

    /**
     * Generates a SessionInfo which is a summary state of the Session used to
     * refresh remote instances of a Session.
     *
     * @param internalSession Non null InternalSession to summarise.
     * @param sessionID SessionID of the caller making the request.
     * @return Non null SessionInfo.
     *
     * @throws SessionException If there was a problem accessing the underlying Session.
     */
    public SessionInfo getSessionInfo(InternalSession internalSession, SessionID sessionID) throws SessionException {
        validateSession(internalSession, sessionID);
        return makeSessionInfo(internalSession, sessionID);
    }

    /**
     * Validates the state of an Internal Session against a Session ID.
     *
     * Performs two checks, firstly that the Session matches the SessionID
     * and secondly that the InternalSession is not timed out.
     *
     * @param internalSession InternalSession to check.
     * @param sid SessionID to check with the InternalSession.
     *
     * @throws SessionException If the InternalSession has timed out.
     *
     * @throws IllegalArgumentException If the SessionID of the InternalSession
     * and provided SessionID do not match.
     */
    public void validateSession(InternalSession internalSession, SessionID sid)
            throws SessionException {
        if (!sid.equals(internalSession.getID())
                && internalSession.getRestrictionForToken(sid) == null) {
            throw new IllegalArgumentException("Session id mismatch");
        }

        if (internalSession.getState() != Session.VALID) {
            if (internalSession.getTimeLeftBeforePurge() > 0) {
                throw new SessionTimedOutException(MessageFormat.format(ERROR_FORMAT,
                        SessionBundle.getString("sessionTimedOut"),
                        sid));
            } else {
                throw new SessionException(MessageFormat.format(ERROR_FORMAT,
                        SessionBundle.getString("invalidSessionState"),
                        sid));
            }
        }
    }

    /**
     * Generates a SessionInfo object from the given InternalSession.
     *
     * @param internalSession Non null InternalSession to use.
     * @param sid Session ID for the user performing the action.
     * @return A non null SessionInfo instance if valid.
     *
     * @throws SessionException If there was an error storing the TokenRestriction on the SessionInfo.
     *
     * @throws IllegalAccessException If this method has not been called in-conjunction with
     * SessionInfoFactory#validateSession
     */
    public SessionInfo makeSessionInfo(InternalSession internalSession, SessionID sid)
            throws SessionException {
        SessionInfo info = internalSession.toSessionInfo();
        TokenRestriction restriction = internalSession.getRestrictionForToken(sid);
        if (restriction != null) {
            try {
                info.properties.put(Session.TOKEN_RESTRICTION_PROP,
                        TokenRestrictionFactory.marshal(restriction));
            } catch (Exception e) {
                throw new SessionException(e);
            }
        } else if (!sid.equals(internalSession.getID())) {
            throw new IllegalArgumentException("Session id mismatch");
        }
        // replace master sid with the sid from the request (either master or
        // restricted) in order not to leak the master sid
        info.sid = sid.toString();
        return info;
    }
}
