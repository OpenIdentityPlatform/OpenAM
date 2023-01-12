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

import java.util.Iterator;
import java.util.List;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * A series of storage steps, which allow for InternalSession access.
 */
public class InternalSessionStoreChain implements InternalSessionStore {

    private InternalSessionStore store;
    private List<InternalSessionStoreStep> steps;

    public InternalSessionStoreChain(List<InternalSessionStoreStep> steps, InternalSessionStore store) {
        this.steps = steps;
        this.store = store;
    }

    @Override
    public InternalSession getBySessionID(SessionID sessionID) throws SessionPersistenceException {
        return new ChainIterator().getBySessionID(sessionID);
    }

    @Override
    public InternalSession getByHandle(String sessionHandle) throws SessionPersistenceException {
        return new ChainIterator().getByHandle(sessionHandle);
    }

    @Override
    public InternalSession getByRestrictedID(SessionID sessionID) throws SessionPersistenceException {
        return new ChainIterator().getByRestrictedID(sessionID);
    }

    @Override
    public void create(InternalSession session) throws SessionPersistenceException {
        new ChainIterator().create(session);
    }

    @Override
    public void update(InternalSession session) throws SessionPersistenceException {
        new ChainIterator().update(session);
    }

    @Override
    public void remove(InternalSession session) throws SessionPersistenceException {
        new ChainIterator().remove(session);
    }

    private class ChainIterator implements InternalSessionStore {
        private final Iterator<InternalSessionStoreStep> iterator;
        ChainIterator() { this.iterator = steps.iterator(); }

        @Override
        public InternalSession getBySessionID(SessionID sessionID) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                return iterator.next().getBySessionID(sessionID, this);
            } else {
                return store.getBySessionID(sessionID);
            }
        }

        @Override
        public InternalSession getByHandle(String sessionHandle) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                return iterator.next().getByHandle(sessionHandle, this);
            } else {
                return store.getByHandle(sessionHandle);
            }
        }

        @Override
        public InternalSession getByRestrictedID(SessionID sessionID) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                return iterator.next().getByRestrictedID(sessionID, this);
            } else {
                return store.getByRestrictedID(sessionID);
            }
        }

        @Override
        public void create(InternalSession session) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                iterator.next().create(session, this);
            } else {
                store.create(session);
            }
        }

        @Override
        public void update(InternalSession session) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                iterator.next().update(session, this);
            } else {
                store.update(session);
            }
        }

        @Override
        public void remove(InternalSession session) throws SessionPersistenceException {
            if (iterator.hasNext()) {
                iterator.next().remove(session, this);
            } else {
                store.remove(session);
            }
        }
    }
}
