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
package org.forgerock.openam.session.service;

import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_IDLE_TIMED_OUT;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_MAX_TIMED_OUT;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.dpro.session.PartialSession;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.service.caching.InternalSessionCache;
import org.forgerock.openam.session.service.persistence.SessionPersistenceManager;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.ForeignSessionHandler;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.MonitoringOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionState;
import com.sun.identity.shared.debug.Debug;

/**
 * Class for managing access to Sessions.  This class is responsible for any session caching that is required
 * to optimise performance.
 */
@Singleton
public class SessionAccessManager implements SessionPersistenceManager {

    private final Debug debug;
    private final ForeignSessionHandler foreignSessionHandler;

    private final InternalSessionCache internalSessionCache;
    private final SessionCache sessionCache;

    private final SessionPersistentStore sessionPersistentStore;

    private final SessionNotificationSender sessionNotificationSender;
    private final SessionAuditor sessionAuditor;
    private final MonitoringOperations monitoringOperations; // Note: there should be an increment and a decrement in this class for this to make sense

    @VisibleForTesting
    @Inject
    SessionAccessManager(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                         final ForeignSessionHandler foreignSessionHandler,
                         final SessionCache sessionCache,
                         final InternalSessionCache internalSessionCache,
                         final SessionNotificationSender sessionNotificationSender,
                         final SessionAuditor sessionAuditor,
                         final MonitoringOperations monitoringOperations,
                         final SessionPersistentStore sessionPersistentStore) {
        this.debug = debug;
        this.foreignSessionHandler = foreignSessionHandler;
        this.sessionCache = sessionCache;
        this.internalSessionCache = internalSessionCache;
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionAuditor = sessionAuditor;
        this.monitoringOperations = monitoringOperations;
        this.sessionPersistentStore = sessionPersistentStore;
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
        InternalSession internalSession = internalSessionCache.getBySessionID(sessionId);
        if (internalSession == null) {
            return cacheSession(sessionPersistentStore.recoverSession(sessionId));
        }
        return internalSession;
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
        InternalSession internalSession = internalSessionCache.getByHandle(sessionHandle);
        if (internalSession == null) {
            return cacheSession(sessionPersistentStore.recoverSessionByHandle(sessionHandle));
        }
        return internalSession;
    }

    private InternalSession cacheSession(InternalSession session) {
        if (session == null) {
            return null;
        }
        
        boolean destroyed = destroySessionIfNecessary(session);
        if (!destroyed) {
            putInternalSessionIntoInternalSessionCache(session);
            foreignSessionHandler.updateSessionMaps(session);
        }

        return session;
    }
    /**
     * Checks if session has to be destroyed and to remove it
     * if so.
     *
     * @param sess session object
     * @return true if session has been destroyed
     */
    private boolean destroySessionIfNecessary(InternalSession sess) {
        boolean wasDestroyed = false;
        try {
            wasDestroyed = performSessionDestroyIfNecessary(sess);
        } catch (Exception ex) {
            debug.error("Exception in session destroyIfNecessary() : ", ex);
            wasDestroyed = true;
        }

        if (wasDestroyed) {
            try {
                removeInternalSession(sess.getID());
            } catch (Exception ex) {
                debug.error("Exception while removing session : ", ex);
            }
        }
        return wasDestroyed;
    }

    /**
     * Checks whether the session should be destroyed or not, and if so performs the operation.
     */
    private boolean performSessionDestroyIfNecessary(InternalSession session) {
        switch (session.checkSessionUpdate()) {
            case NO_CHANGE:
                return false;
            case DESTROY:
                delete(session);
                session.changeStateWithoutNotify(SessionState.DESTROYED);
                sessionNotificationSender.sendEvent(session, SessionEventType.DESTROY);
                return true;
            case MAX_TIMEOUT:
                session.changeStateAndNotify(SessionEventType.MAX_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                return false;
            case IDLE_TIMEOUT:
                session.changeStateAndNotify(SessionEventType.IDLE_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
                return false;
            default:
                return false;
        }
    }

    private void putInternalSessionIntoInternalSessionCache(InternalSession session) {
        session.setPersistenceManager(this);
        internalSessionCache.put(session);
        update(session);
    }


    /**
     * Get a restricted session from a given SessionID.
     * @param sessionID the ID of the restricted session to retrieve
     * @return a restricted Internal Session
     */
    public InternalSession getByRestrictedID(SessionID sessionID) {
        return sessionPersistentStore.getByRestrictedID(sessionID);
    }

    /**
     * Persist the provided InternalSession to the backend.
     * @param session The session to persist.
     */
    public void persistInternalSession(InternalSession session) {
        session.setStored(true);
        putInternalSessionIntoInternalSessionCache(session);
        update(session);
    }

    /**
     * Method responsible for keeping the local references to a session up to date. Call if the Session handle or
     * restricted ids change.
     * @param session The session to reload.
     */
    public void reloadSessionHandleAndRestrictedIds(InternalSession session) {
        if (internalSessionCache.getBySessionID(session.getSessionID()) == null) {
            throw new IllegalStateException("Tried to reload metadata for a session that was not stored.");
        }
        internalSessionCache.put(session); // called for side effects of reloading cache at this point in time
    }

    /**
     * Get the size of the underlying InternalSessionCache.
     * @return The number of InternalSessions in the InternalSessionCache.
     */
    public int getInternalSessionLimit() {
        return internalSessionCache.size();
    }

    /**
     * Get the number of internal sessions in the cache.
     * @return the number of sessions in the cache.
     */
    public int getInternalSessionCount() {
        return internalSessionCache.size();
    }

    /**
     * Get all sessions in the internal session cache.
     * @return a collection of all internal sessions in the internal session cache.
     */
    public Collection<InternalSession> getAllInternalSessions() {
        return internalSessionCache.getAllSessions();
    }

    /**
     * Return partial sessions matching the provided CREST query filter from the CTS servers.
     *
     * @param crestQuery The CREST query based on which we should look for matching sessions.
     * @return The collection of matching partial sessions.
     * @throws SessionException  If the request fails.
     */
    public Collection<PartialSession> getMatchingValidSessions(CrestQuery crestQuery) throws SessionException {
        try {
            return sessionPersistentStore.searchPartialSessions(crestQuery);
        } catch (CoreTokenException cte) {
            debug.error("An error occurred whilst querying CTS for matching sessions", cte);
            throw new SessionException(cte);
        }
    }

    /**
     * Remove a session from the internal session cache and make it as no longer local.  The Session remains in the
     * (non-internal) session cache.
     * @param sessionId the id of the session to be released
     * @return the internal session that has been released
     */
    public InternalSession releaseSession(SessionID sessionId) {
        InternalSession internalSession = internalSessionCache.remove(sessionId);
        internalSession.setPersistenceManager(null);
        return internalSession;
    }

    /**
     * Remove an internal session from the internal session cache.
     * @param sessionId the session id to remove
     * @return the removed internal session
     */
    public InternalSession removeInternalSession(SessionID sessionId) {
        if (null == sessionId) {
            return null;
        }

        InternalSession internalSession = internalSessionCache.remove(sessionId);

        if (internalSession == null) {
            return null;
        }

        internalSession.setPersistenceManager(null);
        foreignSessionHandler.remove(internalSession.getID());

        // Session Constraint
        if (internalSession.getState() == SessionState.VALID) {
            monitoringOperations.decrementActiveSessions();
        }

        if (internalSession.isStored()) {
            delete(internalSession);
        }
        return internalSession;
    }

    /**
     * Called to notify the session access manager that an InternalSession has been updated.
     * @param session The session that was updated.
     */
    private void update(InternalSession session) {
        // TODO: Simplify this all this logic by replacing it with the implementation of save(session)
        if (session.isStored()) {
            if (session.getState() != SessionState.VALID) {
                delete(session);
            } else if (!session.isTimedOut()) {
                // Only save if we are not about to delete the session anyway.
                save(session);
            }
        }
    }

    private void save(InternalSession session) {
        // do not save sessions which never expire, or which are not marked for persistence
        if (!session.willExpire() || !session.isStored()) {
            return;
        }
        try {
            sessionPersistentStore.save(session);
        } catch (Exception e) {
            debug.error("SessionService.save: " + "exception encountered", e);
        }
    }

    private void delete(InternalSession session) {
        session.setStored(false);
        try {
            sessionPersistentStore.delete(session);
        } catch (Exception e) {
            debug.error("SessionService : failed deleting session ", e);
        }
    }

    @Override
    public void notifyUpdate(SessionID sessionID) {
        InternalSession internalSession = getInternalSession(sessionID);
        if (internalSession == null) {
            throw new IllegalStateException(
                    "SessionAccessManager notified of event for InternalSession it does not contain");
        }
        update(internalSession);
    }
}
