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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.forgerock.openam.session.service.access.SessionPersistenceManager;
import org.forgerock.util.Reject;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * Ensures that InternalSessions have a SessionPersistenceManager set correctly as they are loaded in and out of the CTS
 */
@Singleton
public class SessionPersistenceManagerStep implements InternalSessionStoreStep, SessionPersistenceManager {

    private Provider<InternalSessionStore> fullSessionStore;

    @Inject
    public SessionPersistenceManagerStep(Provider<InternalSessionStore> internalSessionStore) {
        this.fullSessionStore = internalSessionStore;
    }

    @Override
    public InternalSession getBySessionID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return setPersistenceManager(next.getBySessionID(sessionID));
    }

    @Override
    public InternalSession getByHandle(String sessionHandle, InternalSessionStore next) throws SessionPersistenceException {
        return setPersistenceManager(next.getByHandle(sessionHandle));
    }

    @Override
    public InternalSession getByRestrictedID(SessionID sessionID, InternalSessionStore next) throws SessionPersistenceException {
        return setPersistenceManager(next.getByRestrictedID(sessionID));
    }

    @Override
    public void create(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        setPersistenceManager(session);
        next.create(session);
    }

    @Override
    public void update(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        setPersistenceManager(session);
        next.update(session);
    }

    @Override
    public void remove(InternalSession session, InternalSessionStore next) throws SessionPersistenceException {
        session.setPersistenceManager(null);
        next.remove(session);
    }

    @Override
    public void notifyUpdate(InternalSession internalSession) {
        Reject.ifNull(internalSession);
        if (internalSession.isStored()) {
            try {
                fullSessionStore.get().update(internalSession);
            } catch (SessionPersistenceException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private InternalSession setPersistenceManager(InternalSession internalSession) {
        if (internalSession == null) {
            return null;
        }
        internalSession.setPersistenceManager(this);
        return internalSession;
    }
}
