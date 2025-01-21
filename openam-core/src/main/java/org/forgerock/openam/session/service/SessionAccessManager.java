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
 * Portions Copyrighted 2024-2025 3A Systems LLC
 */
package org.forgerock.openam.session.service;

import java.util.concurrent.ScheduledExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceException;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;

/**
 * Class for managing access to Sessions. This class handles concepts such as persisting a session for the first time,
 * as well as updating a stored session.
 */
@Singleton
public class SessionAccessManager {

    private final SessionCache sessionCache;
    private final InternalSessionStore internalSessionStore;
    private final NonExpiringSessionManager nonExpiringSessionManager;
    private final ScheduledExecutorService scheduledExecutorService;
    
    @VisibleForTesting
    @Inject
    SessionAccessManager(final SessionCache sessionCache, final AMExecutorServiceFactory esf,
            final ThreadMonitor threadMonitor, final InternalSessionStore internalSessionStore) {
        this.sessionCache = sessionCache;
        this.scheduledExecutorService = esf.createScheduledService(1, "NonExpiringSessionManager");
        this.nonExpiringSessionManager = new NonExpiringSessionManager(this, scheduledExecutorService, threadMonitor);
        this.internalSessionStore = internalSessionStore;
    }

    /**
     * Get the Session based on the SessionId.
     *
     * @param sessionId The session ID to recover the Session for.
     * @return The Session from the SessionCache.
     * @throws SessionException If anything goes wrong.
     */
    public Session getSession(SessionID sessionId) throws SessionException {
        return sessionCache.getSession(sessionId);
    }

    /**
     * Removes a session from the session cache.
     *
     * @param sessionID the ID of the session to remove from the cache
     */
    public void removeSessionId(SessionID sessionID) {
        sessionCache.removeSID(sessionID);
    }

    /**
     * Get the InternalSession based on the SessionId.
     *
     * @param sessionId The session ID to recover the InternalSession for.
     * @return The InternalSession from the InternalSessionCache or null if the internal session could not be retrieved
     * or recovered.
     * @throws SessionException If anything goes wrong.
     */
    public InternalSession getInternalSession(SessionID sessionId) {
        if (sessionId == null || StringUtils.isEmpty(sessionId.toString()) || sessionId.isSessionHandle()) {
            return null;
        }
        try {
            return internalSessionStore.getBySessionID(sessionId);
        } catch (SessionPersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Internal Session corresponding to a session handle.
     *
     * @param sessionHandle Session handle
     * @return Internal Session corresponding to a session handle
     */
    public InternalSession getInternalSessionByHandle(String sessionHandle) {
        if (StringUtils.isBlank(sessionHandle)) {
            return null;
        }
        try {
            return internalSessionStore.getByHandle(sessionHandle);
        } catch (SessionPersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a restricted session from a given SessionID.
     * @param sessionID the ID of the restricted session to retrieve
     * @return a restricted Internal Session
     */
    public InternalSession getByRestrictedID(SessionID sessionID) {
        try {
            return internalSessionStore.getByRestrictedID(sessionID);
        } catch (SessionPersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Persist the provided InternalSession to the backend.
     * @param session The session to persist.
     */
    public void persistInternalSession(InternalSession session) {

        try {
            if(session.isStored()) {
                internalSessionStore.update(session);
            }
            else  {
                internalSessionStore.create(session);
            }
        } catch (SessionPersistenceException e) {
            throw new RuntimeException(e);
        }

        if (!session.willExpire()) {
            nonExpiringSessionManager.addNonExpiringSession(session);
        }
    }

    /**
     * Get the size of the underlying InternalSessionCache.
     * @return The number of InternalSessions in the InternalSessionCache.
     */
    public int getInternalSessionLimit() {
        return 0;
    }

    /**
     * Get the number of internal sessions in the cache.
     * @return the number of sessions in the cache.
     */
    public int getInternalSessionCount() {
        return 0;
    }

    /**
     * Remove an internal session from the internal session cache.
     * @param internalSession the session to remove
     * @return the removed internal session
     */
    public void removeInternalSession(InternalSession internalSession) {
        if (null == internalSession) {
            return;
        }

        try {
            internalSessionStore.remove(internalSession);
        } catch (SessionPersistenceException e) {
            throw new RuntimeException(e);
        }
    }
}
