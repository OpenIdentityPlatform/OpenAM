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

package org.forgerock.openam.session;

import static java.security.AccessController.*;

import java.util.Date;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.api.CoreTokenConstants;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.am.util.ThreadPoolException;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import com.sun.identity.shared.debug.Debug;

/**
 * This class is responsible for periodically checking to see whether a given Session should remain in the SessionCache,
 * or whether it should be removed. Note that as such, it should not be used on stateless sessions.
 *
 * Uses to SessionPollerSender to poll when in Client mode and polling is enabled.
 *
 */
public class SessionCuller extends GeneralTaskRunnable {
    private final SessionCache sessionCache;
    private SessionPollerSender sender = null;
    private SessionPollerPool sessionPollerPool;
    private Session session;
    private static Debug sessionDebug = Debug.getInstance(SessionConstants.SESSION_DEBUG);
    /**
     * This is the time value (computed as System.currentTimeMillis()) when a DESTROYED
     * session should be removed from the {@link SessionCache#sessionTable}.
     *
     * It will be set to {@link com.iplanet.dpro.session.service.SessionService#getReducedCrosstalkPurgeDelay() }
     * minutes after the time {@link org.forgerock.openam.session.SessionCache#removeRemoteSID } is called.
     *
     * Value zero means the session has not been destroyed or cross-talk is not being reduced.
     */
    private volatile long purgeAt = 0;

    /**
     * This is used only in polling mode to find the polling state of this
     * session.
     */
    private volatile boolean isPolling = false;

    SessionCuller(Session session) {
        this.session = session;
        if (SystemProperties.isServerMode()) {
            sessionCache = InjectorHolder.getInstance(SessionCache.class);
            sessionPollerPool = InjectorHolder.getInstance(SessionPollerPool.class);
        } else {
            sessionPollerPool = SessionPollerPool.getInstance();
            sessionCache = SessionCache.getInstance();
        }
    }

    @Override
    public boolean addElement(Object obj) {
        return false;
    }

    @Override
    public boolean removeElement(Object obj) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * @return -1 since it is not a periodic task.
     */
    @Override
    public long getRunPeriod() {
        return -1;
    }

    /**
     * Schedule this culler to be run, based on the state of the session.
     */
    void scheduleToTimerPool() {
        if (sessionPollerPool.isPollingEnabled()) {
            long timeoutTime = (session.getLatestRefreshTime() + (session.getMaxIdleTime() * 60)) * 1000;
            if (sessionPollerPool.getCacheBasedPolling()) {
                timeoutTime = Math.min((session.getLatestRefreshTime() + (session.getMaxCachingTime() * 60)) * 1000, timeoutTime);
            }
            rescheduleIfWillTimeOutBeforeExecution(timeoutTime);
        } else {
            if ((sessionPollerPool.isSessionCleanupEnabled()) && willExpire(session.getMaxSessionTime())) {
                long timeoutTime = (session.getLatestRefreshTime() + (session.getMaxSessionTime() * 60)) * 1000;
                rescheduleIfWillTimeOutBeforeExecution(timeoutTime);
            }
        }
    }

    private void rescheduleIfWillTimeOutBeforeExecution(long timeoutTime) {
        if (scheduledExecutionTime() > timeoutTime) {
            cancel();
        }
        if (!isScheduled()) {
            SystemTimerPool.getTimerPool().schedule(this, new Date(timeoutTime));
        }
    }

    /**
     * Returns true if the provided time is less than Long.MAX_VALUE seconds.
     */
    private boolean willExpire(long minutes) {
        return minutes < Long.MAX_VALUE / 60;
    }

    /**
     * Enables the Session Polling
     * @param b if <code>true</code> polling is enabled, disabled otherwise
     */
    void setIsPolling(boolean b) {
        isPolling = b;
    }

    /**
     * Checks if Polling is enabled
     * @return <code> true if polling is enabled , <code>false<code> otherwise
     */
    private boolean getIsPolling() {
        return isPolling;
    }

    /**
     * Return the time that this session will be purged at (in millis).
     * @return The time that this session will be purged at (in millis).
     */
    long getPurgeAt() {
        return purgeAt;
    }

    /**
     * Used to update the purge time, and reschedule if appropriate.
     * @param purgeAt The time to purge at in milliseconds.
     */
    void rescheduleForPurge(long purgeAt) {
        this.purgeAt = purgeAt;

        cancel();
        if (!isScheduled()) {
            SystemTimerPool.getTimerPool().schedule(this, new Date(purgeAt));
        } else {
            sessionDebug.error("Unable to schedule destroyed session for purging");
        }
    }

    @Override
    public void run() {
        if (sessionPollerPool.isPollingEnabled()) {
            try {
                if (!getIsPolling()) {
                    long expectedTime;
                    if (willExpire(session.getMaxIdleTime())) {
                        expectedTime = (session.getLatestRefreshTime() + (session.getMaxIdleTime() * 60)) * 1000;
                        if (sessionPollerPool.getCacheBasedPolling()) {
                            expectedTime = Math.min(expectedTime, (session.getLatestRefreshTime() +
                                    (session.getMaxCachingTime() * 60)) * 1000);
                        }
                    } else {
                        expectedTime = (session.getLatestRefreshTime() + (SessionMeta.getAppSSOTokenRefreshTime() * 60)) * 1000;
                    }
                    if (expectedTime > scheduledExecutionTime()) {
                        // Get an instance as required otherwise it causes issues on container restart.
                        SystemTimerPool.getTimerPool().schedule(this, new Date(expectedTime));
                        return;
                    }
                    if (sender == null) {
                        sender = new SessionPollerSender(session, this);
                    }
                    RestrictedTokenContext.doUsing(getContext(),
                            new RestrictedTokenAction() {
                                public Object run() throws Exception {
                                    try {
                                        setIsPolling(true);
                                        sessionPollerPool.getThreadPool().run(sender);
                                    } catch (ThreadPoolException e) {
                                        setIsPolling(false);
                                        sessionDebug.error("Send Polling Error: ", e);
                                    }
                                    return null;
                                }
                            });
                }
            } catch (SessionException se) {
                sessionCache.removeSID(session.getSessionID());
                sessionDebug.message("session is not in timeout state so clean it", se);
            } catch (Exception ex) {
                sessionDebug.error("Exception encountered while polling", ex);
            }
        } else {

            String sessionRemovalDebugMessage;
            if (purgeAt > 0) {
                /**
                 * Reduced crosstalk protection.
                 *
                 * In order to prevent sessions from being (re)created from CTS on remote servers before
                 * the destroyed state has been propagated, remote sessions are kept in memory for a configurable
                 * amount of time {@link CoreTokenConstants.REDUCED_CROSSTALK_PURGE_DELAY }.
                 *
                 * This delay introduced to cover the CTS replication lag is only required when running as an
                 * OpenAM server with a 'remote' copy of a session; therefore, this feature is not required
                 * when polling is enabled - since polling is only ever used by non-OpenAM clients.
                 */
                // destroyed session scheduled for purge
                if (purgeAt > scheduledExecutionTime()) {
                    SystemTimerPool.getTimerPool().schedule(this, new Date(purgeAt));
                    return;
                }
                sessionRemovalDebugMessage = "Session Removed, Reduced Crosstalk Purge Time complete";
            } else {
                // schedule at the max session time
                long expectedTime = -1;
                if (willExpire(session.getMaxSessionTime())) {
                    expectedTime = (session.getLatestRefreshTime() + (session.getMaxSessionTime() * 60)) * 1000;
                }
                if (expectedTime > scheduledExecutionTime()) {
                    SystemTimerPool.getTimerPool().schedule(this, new Date(expectedTime));
                    return;
                }
                sessionRemovalDebugMessage = "Session Destroyed, Caching time exceeded the Max Session Time";
            }

            try {
                sessionCache.removeSID(session.getSessionID());
                if (sessionDebug.messageEnabled()) {
                    sessionDebug.message(sessionRemovalDebugMessage);
                }
            } catch (Exception ex) {
                sessionDebug.error("Exception occured while cleaning up Session Cache", ex);
            }
        }
    }
}
