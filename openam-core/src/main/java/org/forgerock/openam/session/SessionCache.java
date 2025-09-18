/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.session;

import static org.forgerock.openam.utils.Time.currentTimeMillis;

import jakarta.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.inject.name.Named;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.share.SessionBundle;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Responsible for providing a single point of contact for all Sessions stored in memory.
 */
@Singleton
public class SessionCache {

    /**
     * Maps stateful sessions, allowing their sessionId to be used as a lookup to the Session object.
     */
    private final ConcurrentHashMap<SessionID, Session> sessionTable = new ConcurrentHashMap<SessionID, Session>();

    private final ConcurrentHashMap<SessionID, SessionCuller> sessionCullerTable = new ConcurrentHashMap<>();

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
     * Retrieve the culler for the provided session.
     * @param sessionID Non null sessionID of the Session to lookup.
     * @return Null indicates no Session present, otherwise non null.
     */
    private SessionCuller getSessionCuller(SessionID sessionID) {
        Reject.ifNull(sessionID);
        return sessionCullerTable.get(sessionID);
    }

    /**
     * Store a Session in the table.
     *
     * @param session The Session to store. Non null.
     */
    @VisibleForTesting
    void writeSession(Session session) {
        Reject.ifNull(session);
        Reject.ifNull(session.getID());
        sessionCullerTable.put(session.getID(), new SessionCuller(session));
        sessionTable.put(session.getID(), session);
    }

    /**
     * Delete the Session from the table.
     * @param sessionID The SessionID of the Session to delete. Non null.
     * @return A possibly null Session if none was matched, otherwise non null.
     */
    private Session deleteSession(SessionID sessionID) {
        Reject.ifNull(sessionID);

        SessionCuller sessionCuller = sessionCullerTable.remove(sessionID);
        if (sessionCuller != null) {
            sessionCuller.cancel();
        }
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
            SessionCuller sessionCuller = getSessionCuller(sid);
            Reject.ifNull(sessionCuller);
            deleteSession(sid);

            // ensure session has destroyed state and observers are notified (exactly once)
            if (session.getRemoved().compareAndSet(false, true)) {
                session.setState(SessionState.DESTROYED);
                SessionEvent event = new SessionEvent(session, SessionEventType.DESTROY, eventTime);
                Session.invokeListeners(event);
            }
        }
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

        if (StringUtils.isEmpty(sessionID.toString())) {
            throw new InvalidSessionIdException();
        }

        Session session = readSession(sessionID);
        if (session != null) {
            
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
            } else  if (!allowInvalidSessions && possiblyResetIdleTime) {
                session.refresh(true);
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
     * Used to notify the cache that a session has been updated, and that it should reschedule the culler if necessary.
     * @param session The session that was refreshed.
     * @param oldMaxCachingTime The previous maxCachingTime.
     * @param oldMaxIdleTime The previous maxIdleTime.
     * @param oldMaxSessionTime The previous maxSessionTime.
     */
    public void notifySessionRefresh(Session session, long oldMaxCachingTime, long oldMaxIdleTime,
                                     long oldMaxSessionTime) {
        SessionCuller sessionCuller = getSessionCuller(session.getID());
        if (sessionCuller != null) {
            if ((!sessionCuller.isScheduled()) || (oldMaxCachingTime > session.getMaxCachingTime()) ||
                    (oldMaxIdleTime > session.getMaxIdleTime()) || (oldMaxSessionTime > session.getMaxSessionTime())) {
                sessionCuller.scheduleToTimerPool();
            }
        }
    }
    
    final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static {
    	if (!getInstance().sessionPollerPool.isPollingEnabled()) {
	    	scheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					final SessionCache cache=getInstance();
					cache.debug.message("session culler {}",cache.sessionCullerTable.size());
					cache.sessionCullerTable.forEachValue(Long.MAX_VALUE, sessionCuller-> {
						if (!sessionCuller.isScheduled() && sessionCuller.willExpire(sessionCuller.session.getMaxSessionTime())) {
							cache.debug.error("session culler phantom {}",sessionCuller.session);
							sessionCuller.run();
						}
					});
				}
			}, 60, 60, TimeUnit.MINUTES);
    	}
    }
}