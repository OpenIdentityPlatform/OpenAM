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

import static org.forgerock.openam.audit.AuditConstants.EventName.*;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.service.persistence.SessionPersistenceManager;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.ForeignSessionHandler;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionCache;
import com.iplanet.dpro.session.service.MonitoringOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionInfo;
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
    private final TokenIdFactory tokenIdFactory;
    private final CTSPersistentStore coreTokenService;
    private final SessionAdapter tokenAdapter;

    private final SessionNotificationSender sessionNotificationSender;
    private final SessionLogging sessionLogging;
    private final SessionAuditor sessionAuditor;
    private final MonitoringOperations monitoringOperations; // Note: there should be an increment and a decrement in this class for this to make sense

    @VisibleForTesting
    @Inject
    SessionAccessManager(@Named(SessionConstants.SESSION_DEBUG) final Debug debug,
                                 final ForeignSessionHandler foreignSessionHandler,
                                 final SessionCache sessionCache,
                                 final InternalSessionCache internalSessionCache,
                                 final TokenIdFactory tokenIdFactory,
                                 final CTSPersistentStore coreTokenService,
                                 final SessionAdapter tokenAdapter,
                                 final SessionNotificationSender sessionNotificationSender,
                                 final SessionLogging sessionLogging,
                                 final SessionAuditor sessionAuditor,
                                 final MonitoringOperations monitoringOperations) {
        this.debug = debug;
        this.foreignSessionHandler = foreignSessionHandler;
        this.sessionCache = sessionCache;
        this.internalSessionCache = internalSessionCache;
        this.tokenIdFactory = tokenIdFactory;
        this.coreTokenService = coreTokenService;
        this.tokenAdapter = tokenAdapter;
        this.sessionNotificationSender = sessionNotificationSender;
        this.sessionLogging = sessionLogging;
        this.sessionAuditor = sessionAuditor;
        this.monitoringOperations = monitoringOperations;
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
            return recoverSession(sessionId);
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
            return recoverSessionByHandle(sessionHandle);
        }
        return internalSession;
    }

    private InternalSession recoverSessionByHandle(String sessionHandle) {

        final TokenFilter tokenFilter = new TokenFilterBuilder()
                .withAttribute(SessionTokenField.SESSION_HANDLE.getField(), sessionHandle)
                .build();

        Token token = null;

        try {
            final Collection<Token> results = coreTokenService.query(tokenFilter);
            if (results.isEmpty()) {
                return null;
            }
            if (results.size() != 1) {
                debug.error("Duplicate session handle found in Core Token Service");
                return null;
            }
            token = CollectionUtils.getFirstItem(results);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }
        return getInternalSessionFromToken(token);


    }

    private InternalSession getInternalSessionFromToken(Token token) {

        /*
         * As a side effect of deserialising an InternalSession, we must trigger
         * the InternalSession to reschedule its timing task to ensure it
         * maintains the session expiry function.
         */
        InternalSession session = tokenAdapter.fromToken(token);
        session.setSessionServiceDependencies(InjectorHolder.getInstance(SessionService.class),
                InjectorHolder.getInstance(SessionServiceConfig.class),
                InjectorHolder.getInstance(SessionLogging.class),
                InjectorHolder.getInstance(SessionAuditor.class),
                debug);
        session.scheduleExpiry();

        boolean destroyed = destroySessionIfNecessary(session);
        if (!destroyed) {
            putInternalSessionIntoInternalSessionCache(session);
            foreignSessionHandler.updateSessionMaps(session);
        }

        return session;
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
        return internalSessionCache.getByRestrictedID(sessionID);
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
     * Remove a session from the internal session cache and make it as no longer local.  The Session remains in the
     * (non-internal) session cache.
     * @param sessionId the id of the session to be released
     * @return the internal session that has been released
     */
    public InternalSession releaseSession(SessionID sessionId) {
        if (sessionCache.hasSession(sessionId)) {
            sessionCache.readSession(sessionId).setSessionIsLocal(false);
        }

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
        internalSession.setPersistenceManager(null);
        return removeInternalSession(internalSession);
    }

    /**
     * This will recover the specified session from the repository, and add it to the cache.
     * Returns null if no session was recovered.
     * @param sessionID Session ID
     */
    private InternalSession recoverSession(SessionID sessionID) {

        String tokenId = tokenIdFactory.toSessionTokenId(sessionID);
        Token token = null;

        try {
            token = coreTokenService.read(tokenId);
        } catch (CoreTokenException e) {
            debug.error("Failed to retrieve session by its handle", e);
        }
        if (token == null) {
            return null;
        }

        return getInternalSessionFromToken(token);
    }

    /**
     * Utility method to check if session has to be destroyed and to remove it
     * if so.
     *
     * @param sess session object
     * @return true if session should (and has !) been destroyed
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
            case PURGE:
                SessionInfo sessionInfo = session.toSessionInfo();
                sessionLogging.logEvent(sessionInfo, SessionEvent.DESTROY);
                sessionAuditor.auditActivity(sessionInfo, AM_SESSION_DESTROYED);
                // intentional fall through to destroy
            case DESTROY:
                delete(session);
                session.changeStateWithoutNotify(SessionState.DESTROYED);
                sessionNotificationSender.sendEvent(session, SessionEvent.DESTROY);
                return true;
            case MAX_TIMEOUT:
                session.changeStateAndNotify(SessionEvent.MAX_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_MAX_TIMED_OUT);
                return false;
            case IDLE_TIMEOUT:
                session.changeStateAndNotify(SessionEvent.IDLE_TIMEOUT);
                sessionAuditor.auditActivity(session.toSessionInfo(), AM_SESSION_IDLE_TIMED_OUT);
                return false;
            default:
                return false;
        }
    }

    private InternalSession removeInternalSession(final InternalSession session) {

        if (null == session) {
            return null;
        }

        foreignSessionHandler.remove(session.getID());
        session.cancel();
        // Session Constraint
        if (session.getState() == SessionState.VALID) {
            monitoringOperations.decrementActiveSessions();
        }

        if (session.isStored()) {
            delete(session);
        }
        return session;
    }

    /**
     * Called to notify the session access manager that an InternalSession has been updated.
     * @param session The session that was updated.
     */
    private void update(InternalSession session) {
        if (session.isStored()) {
            if (session.getState() != SessionState.VALID) {
                delete(session);
            } else if (!session.isTimedOut() || session.getTimeLeftBeforePurge() > 0) {
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
            coreTokenService.update(tokenAdapter.toToken(session));
        } catch (Exception e) {
            debug.error("SessionService.save: " + "exception encountered", e);
        }
    }

    private void delete(InternalSession session) {
        session.setStored(false);
        try {
            String tokenId = tokenIdFactory.toSessionTokenId(session.getID());
            coreTokenService.delete(tokenId);
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
