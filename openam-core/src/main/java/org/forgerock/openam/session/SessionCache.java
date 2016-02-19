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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.session;

import com.google.inject.name.Named;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.util.Reject;

import javax.inject.Singleton;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.forgerock.openam.session.SessionConstants.DESTROYED;
import static org.forgerock.openam.utils.Time.*;

/**
 * Responsible for providing a single point of contact for all Sessions stored in memory.
 */
@Singleton
public class SessionCache {

    /**
     * Maps stateful sessions, allowing their sessionId to be used as a lookup to the Session object.
     */
    private final ConcurrentMap<SessionID, Session> sessionTable = new ConcurrentHashMap<SessionID, Session>();

    private final SessionPollerPool sessionPollerPool;

    private final Debug debug;

    private static SessionCache instance;

    /**
     * ClientSDK: Usage without Guice, must maintain static initialisation.
     *
     * @return Provides accesses to a SessionCache singleton.
     */
    public synchronized static SessionCache getInstance() {
        if (instance == null) {
            instance = new SessionCache(SessionPollerPool.getInstance(), Debug.getInstance(SessionConstants.SESSION_DEBUG));
        }
        return instance;
    }

    // Hidden to enforce singleton.
    private SessionCache(SessionPollerPool sessionPollerPool,
                        @Named(SessionConstants.SESSION_DEBUG) Debug debug) {
        this.sessionPollerPool = sessionPollerPool;
        this.debug = debug;
    }

    /**
     * Checks for a Session in the Session cache.
     * @param sessionID Non null sessionID of the Session to lookup.
     * @return boolean true indicates Session is present.
     */
    public boolean hasSession(SessionID sessionID) {
        Reject.ifNull(sessionID);
        return sessionTable.containsKey(sessionID);
    }

    /**
     * Reads a Session from the Session table.
     * @param sessionID Non null sessionID of the Session to lookup.
     * @return Null indicates no Session present, otherwise non null.
     */
    public Session readSession(SessionID sessionID) {
        Reject.ifNull(sessionID);
        return sessionTable.get(sessionID);
    }

    /**
     * Store a Session in the table.
     *
     * @param session The Session to store. Non null.
     */
    public void writeSession(Session session) {
        Reject.ifNull(session);
        Reject.ifNull(session.getID());
        sessionTable.put(session.getID(), session);
    }

    /**
     * Delete the Session from the table.
     * @param sessionID The SessionID of the Session to delete. Non null.
     * @return A possibly null Session if none was matched, otherwise non null.
     */
    public Session deleteSession(SessionID sessionID) {
        Reject.ifNull(sessionID);
        return sessionTable.remove(sessionID);
    }

    /**
     * Removes the <code>SessionID</code> from session table.
     *
     * @param sid Session ID.
     */
    public void removeSID(SessionID sid) {
        if(sid == null || sid.isNull()){
            return;
        }
        Session session = readSession(sid);
        if (session != null) {

            long eventTime = currentTimeMillis();

            // remove from sessionTable if there is no purge delay or it has elapsed
            if (session.getPurgeAt() <= eventTime) {
                deleteSession(sid);
                session.cancel();
            }

            // ensure session has destroyed state and observers are notified (exactly once)
            if (session.getRemoved().compareAndSet(false, true)) {
                session.setState(DESTROYED);
                SessionEvent event = new SessionEvent(session, SessionEvent.DESTROY, eventTime);
                SessionEvent.invokeListeners(event);
            }
        }
    }

    /**
     * Wrapper method for {@link #removeSID} only to be called when receiving notification of session
     * destruction that has this server as its home server.
     *
     * @param info Current state of session
     */
    public void removeLocalSID(SessionInfo info) {
        SessionID sessionID = new SessionID(info.getSessionID());
        removeSID(sessionID);
    }

    /**
     * Wrapper method for {@link #removeSID} only to be called when receiving notification of session
     * destruction from the home server.
     *
     * This method should only be called when the identified session has another instance
     * of OpenAM as its home server.
     *
     * @param info Current state of session on home server
     */
    public void removeRemoteSID(SessionInfo info) {
        SessionID sessionID = new SessionID(info.getSessionID());

        long purgeDelay = getPurgeDelayForReducedCrosstalk();

        if (purgeDelay > 0) {

            Session session = readSession(sessionID);
            if (session == null) {
                /**
                 * Reduced crosstalk protection.
                 *
                 * As the indicated session has not yet been loaded, it will be created and added to the
                 * {@link #sessionTable} so that it can remain there in a DESTROYED state until it is purged.
                 */
                session = new Session(sessionID);
                try {
                    session.update(info);
                    writeSession(session);
                } catch (SessionException e) {
                    debug.error("Exception reading remote SessionInfo", e);
                }
            }

            session.setPurgeAt(currentTimeMillis() + (purgeDelay * 60 * 1000));
            session.cancel();
            if (!session.isScheduled()) {
                SystemTimerPool.getTimerPool().schedule(session, new Date(session.getPurgeAt()));
            } else {
                debug.error("Unable to schedule destroyed session for purging");
            }

        }

        removeSID(sessionID);
    }

    /**
     * Returns a session based on a Session ID object.
     *
     * @param sid Session ID.
     * @return A Session object.
     * @throws SessionException if the Session ID object does not contain a
     *         valid session string, or the session string was valid before
     *         but has been destroyed, or there was an error during
     *         communication with session service.
     */
    public Session getSession(SessionID sid) throws SessionException {
        return getSession(sid, false);
    }

    /**
     * Returns a Session based on a Session ID object.
     *
     * @param sessionID The Session Id.
     * @param allowInvalidSessions Whether to allow invalid Sessions to be returned.
     * @return A Session object.
     * @throws SessionException If the Session ID object does not contain a
     *         valid session string, or the session string was valid before
     *         but has been destroyed, or there was an error during
     *         communication with session service.
     */
    public Session getSession(SessionID sessionID, boolean allowInvalidSessions) throws SessionException {
        return getSession(sessionID, allowInvalidSessions, true);
    }

    /**
     * This function will get a session based on the session id.  It will allow invalid sessions to be returned,
     * and allow the caller to specify whether the session can be updated (and therefore have the idle time
     * refreshed).
     *
     * @param sessionID The Session id.
     * @param allowInvalidSessions If true, allow invalid Sessions to be returned.
     * @param possiblyResetIdleTime If true, the idle time of the session can be reset, if false, it is never reset.
     * @return A session object.
     * @throws SessionException If the Session ID object does not contain a
     *         valid session string, or the session string was valid before
     *         but has been destroyed, or there was an error during
     *         communication with session service.
     */
    public Session getSession(SessionID sessionID, boolean allowInvalidSessions, boolean possiblyResetIdleTime)
            throws SessionException {

        if (sessionID.toString() == null || sessionID.toString().length() == 0) {
            throw new SessionException(SessionBundle.rbName, "invalidSessionID", null);
        }

        Session session = readSession(sessionID);
        if (session != null) {

            /**
             * Reduced crosstalk protection.
             *
             * When a user logs out, or the Session is destroyed and crosstalk is reduced, it is possible
             * for a destroyed session to be recovered by accessing it on a remote server. Instead the
             * session will be left in the {@link #sessionTable} until it is purged. This check will
             * detect this condition and indicate to the caller their SessionID is invalid.
             */
            if (session.getState(false) == DESTROYED && getPurgeDelayForReducedCrosstalk() > 0) {
                throw new SessionException("Session is in a destroyed state");
            }

            TokenRestriction restriction = session.getRestriction();

            /*
             * In cookie hijacking mode...
             * After the server remove the agent token id from the
             * user token id. server needs to create the agent token
             * from this agent token id. Now, the restriction context
             * required for session creation is null, so we added it
             * to get the agent session created.
             */

            try {
                if (SystemProperties.isServerMode()) {
                    if ((restriction != null)  && !restriction.isSatisfied(RestrictedTokenContext.getCurrent())) {
                        throw new SessionException(SessionBundle.rbName, "restrictionViolation", null);
                    }
                }
            } catch (Exception e) {
                throw new SessionException(e);
            }
            if (!sessionPollerPool.getCacheBasedPolling() && session.maxCachingTimeReached()) {
                session.refresh(false);
            }
            return session;
        }

        session = new Session(sessionID);

        if (!allowInvalidSessions) {
            session.refresh(possiblyResetIdleTime);
        }

        session.setContext(RestrictedTokenContext.getCurrent());

        writeSession(session);
        if (!sessionPollerPool.isPollingEnabled()) {
            session.addInternalSessionListener();
        }
        return session;
    }

    /**
     * Determines the reduce crosstalk purge delay, or defaults to 0.
     *
     * @return purge delay
     */
    private long getPurgeDelayForReducedCrosstalk() {
        if (SystemProperties.isServerMode()) {
            SessionService ss = InjectorHolder.getInstance(SessionService.class);
            if (ss.isReducedCrossTalkEnabled()) {
                return ss.getReducedCrosstalkPurgeDelay();
            }
        }
        return 0;
    }
}
