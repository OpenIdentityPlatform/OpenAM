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

package org.forgerock.openam.session.service.access.persistence.caching;

import javax.inject.Inject;

import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStoreStep;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceException;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;


/**
 * This class provides a bridge between caching code, and the InternalSession persisting steps.
 */
public class InternalSessionCacheStep implements InternalSessionStoreStep {

    private InternalSessionCache sessionCache;

    @Inject
    public InternalSessionCacheStep(InternalSessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public InternalSession getBySessionID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        Reject.ifNull(sessionID);

        InternalSession internalSession = sessionCache.getBySessionID(sessionID);
        if (internalSession != null) {
            return internalSession;
        }
        return cacheInternalSession(next.getBySessionID(sessionID));
    }

    @Override
    public InternalSession getByHandle(String sessionHandle, InternalSessionStore next) throws SessionPersistenceException {
        Reject.ifNull(sessionHandle);

        InternalSession internalSession = sessionCache.getByHandle(sessionHandle);
        if (internalSession != null) {
            return internalSession;
        }
        return cacheInternalSession(next.getByHandle(sessionHandle));
    }

    @Override
    public InternalSession getByRestrictedID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        Reject.ifNull(sessionID);

        InternalSession internalSession = sessionCache.getByRestrictedID(sessionID);
        if (internalSession != null) {
            return internalSession;
        }
        return cacheInternalSession(next.getByRestrictedID(sessionID));
    }

    private InternalSession cacheInternalSession(InternalSession internalSession) {

        if (internalSession != null) {
            sessionCache.put(internalSession);
        }
        return internalSession;
    }

    @Override
    public void store(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        Reject.ifNull(session);
        sessionCache.put(session);
        next.store(session);
    }

    @Override
    public void remove(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        Reject.ifNull(session);
        sessionCache.remove(session.getSessionID());
        next.remove(session);
    }
}
