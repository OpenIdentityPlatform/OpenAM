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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.session.service.access.persistence;

import java.util.concurrent.TimeUnit;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * Checks that sessions which are being read are not timed out. Silently consumes them if they are not.
 * This should be run after any caching, so that invalid sessions are not reread every time.
 *
 * The filter step is not responsible for tidying up timed out sessions - the reaper will take care of that.
 */
public class TimeOutSessionFilterStep extends AbstractInternalSessionStoreStep {

    @Override
    public InternalSession getBySessionID(
            final SessionID sessionID,
            final InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getBySessionID(sessionID));
    }

    @Override
    public InternalSession getByHandle(
            final String sessionHandle,
            final InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getByHandle(sessionHandle));
    }

    @Override
    public InternalSession getByRestrictedID(
            final SessionID sessionID,
            final InternalSessionStore next) throws SessionPersistenceException {
        return processInternalSession(next.getByRestrictedID(sessionID));
    }

    private InternalSession processInternalSession(
            final InternalSession internalSession) throws SessionPersistenceException {
        if (internalSession == null || isSessionTimedOut(internalSession)) {
            // The session will not be returned if it has timed out
            // The filter step is not responsible for tidying up timed out sessions - the reaper will take care of that
            return null;
        }
        return internalSession;
    }

    private boolean isSessionTimedOut(final InternalSession internalSession) {
        // the session has already been timed out by a session monitor task
        if (internalSession.isTimedOut()) {
            return true;
        }
        // the session will not time out
        if (!internalSession.willExpire()) {
            return false;
        }
        // the session has passed its maximum session life
        if (0 >= internalSession.getTimeLeft()) {
            return true;
        }
        // the session has reached its maximum idle time
        if (internalSession.getIdleTime() >
                TimeUnit.SECONDS.convert(internalSession.getMaxIdleTime(), TimeUnit.MINUTES)) {
            return true;
        }
        // the session has not timed out
        return false;
    }
}
