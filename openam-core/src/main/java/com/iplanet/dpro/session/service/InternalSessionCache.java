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
package com.iplanet.dpro.session.service;

import com.iplanet.dpro.session.SessionID;

import org.forgerock.openam.utils.SingleValueMapper;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for caching and providing access to {@link com.iplanet.dpro.session.service.InternalSession} objects.
 *
 * InternalSessions are the representation of the users session which has been homed on a particular server.
 * These sessions are managed entirely by {@link com.iplanet.dpro.session.service.SessionService}.
 *
 * This cache will provide access to the sessions via a number of concepts:
 *
 * - SessionID of the InternalSession
 * - Session handle of the InternalSession
 * - Any of the restricted SessionIDs of the InternalSession
 *
 * This cache has been designed to remove previous references to Session handles and restricted tokens
 * when they are no longer referenced by the InternalSession.
 */
@Singleton
public class InternalSessionCache {
    private final ConcurrentHashMap<SessionID, InternalSession> cache;
    private final SingleValueMapper<String, InternalSession> handle = new SingleValueMapper<String, InternalSession>();
    private final SingleValueMapper<SessionID, InternalSession> restricted = new SingleValueMapper<SessionID, InternalSession>();

    /**
     * Construct an InternalSessionCache intended to provide Session caching for provided SessionService configuration.
     * @param config Non null configuration to base caching estimates from.
     */
    @Inject
    public InternalSessionCache(SessionServiceConfig config) {
        cache = new ConcurrentHashMap<SessionID, InternalSession>(config.getMaxSessions());
    }

    public InternalSession getBySessionID(SessionID sessionID) {
        return cache.get(sessionID);
    }

    public InternalSession getByHandle(String sessionHandle) {
        return handle.get(sessionHandle);
    }

    public InternalSession getByRestrictedID(SessionID sessionID) {
        return restricted.get(sessionID);
    }

    /**
     * Stores the InternalSession in the cache. This will also store any associated references
     * which have been stored on the Session:
     *
     * - Session Handle
     * - Restricted Tokens
     *
     * Synchronized: makes updates to multiple data structures atomic.
     *
     * @param session Non null InternalSession to store.
     */
    public synchronized void put(InternalSession session) {
        Reject.ifNull(session);
        cache.put(session.getID(), session);

        // Session Handle
        if (session.getSessionHandle() != null) {
            handle.put(session.getSessionHandle(), session);
        }

        // Restricted Sessions
        for (SessionID restrictedID : session.getRestrictedTokens()) {
            restricted.put(restrictedID, session);
        }
    }

    /**
     * Remove the Session from the cache.
     *
     * Synchronized: makes updates to multiple data structures atomic.
     *
     * @param sessionID Non null SessionID.
     *
     * @return The InternalSession that was removed from the cache.
     */
    public synchronized InternalSession remove(SessionID sessionID) {
        InternalSession remove = cache.remove(sessionID);

        if (remove == null) {
            return null;
        }

        // Clear Session Handle
        if (remove.getSessionHandle() != null) {
            handle.remove(remove.getSessionHandle());
        }

        // Clear Restricted Tokens
        for (SessionID restrictedID : remove.getRestrictedTokens()) {
            restricted.remove(restrictedID);
        }

        return remove;
    }

    /**
     * @param session The InternalSession to remove.
     * @return Non null InternalSession removed from the cache.
     */
    public InternalSession remove(InternalSession session) {
        return remove(session.getID());
    }

    /**
     * @return Current number of sessions stored in the cache.
     */
    public int size() {
        return cache.size();
    }

    /**
     * @return <tt>true</tt> if this cache is empty.
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * @return Unmodifiable collection of all Sessions that are stored in the cache.
     */
    public Collection<InternalSession> getAllSessions() {
        return Collections.unmodifiableCollection(cache.values());
    }
}
