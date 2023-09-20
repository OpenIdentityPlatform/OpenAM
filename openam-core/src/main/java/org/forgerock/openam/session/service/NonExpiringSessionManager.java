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

import static org.forgerock.openam.session.SessionConstants.NON_EXPIRING_SESSION_LENGTH_MINUTES;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.shared.debug.Debug;

/**
 * This class tracks sessions created by this server which are not set to expire. It achieves this by periodically
 * setting the max session time to a large number, and the idle time to double the refresh period. Then, every refresh,
 * it updates the latest access time of the session.
 * This means that if a server goes offline, the sessions created for it will eventually be removed.
 */
class NonExpiringSessionManager {

    private static final long refreshPeriodInMinutes = 5;

    private final Set<SessionID> nonExpiringSessions = new HashSet<>();

    private final SessionAccessManager sessionAccessManager;

    /**
     * Create a new manager for non expiring sessions. Should only be used by the SessionAccessManager.
     * @param sessionAccessManager The session access manager this uses to recover sessions.
     * @param threadMonitor The thread monitor used to restart the underlying thread on failure.
     */
    NonExpiringSessionManager(SessionAccessManager sessionAccessManager,
                              ScheduledExecutorService scheduler,
                              ThreadMonitor threadMonitor) {
        this.sessionAccessManager = sessionAccessManager;

        NonExpiringSessionUpdater sessionUpdater = new NonExpiringSessionUpdater();
        threadMonitor.watchScheduledThread(scheduler, sessionUpdater, 0, refreshPeriodInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Adds a session to be managed. This operation can not be undone.
     * @param session The non expiring session to manage.
     */
    void addNonExpiringSession(InternalSession session) {
        if (session.willExpire()) {
            throw new IllegalStateException("Tried to add session which would expire to NonExpiringSessionManager");
        }
        session.setMaxSessionTime(NON_EXPIRING_SESSION_LENGTH_MINUTES);
        session.setMaxIdleTime(refreshPeriodInMinutes * 10);
        updateSession(session);
        nonExpiringSessions.add(session.getID());
    }

    private void updateSession(InternalSession session) {
        if (session == null) {
            return;
        }
        session.setLatestAccessTime();
    }

    private class NonExpiringSessionUpdater implements Runnable {

        @Override
        public void run() {
            for (SessionID sessionID : nonExpiringSessions) 
	            try{
	                updateSession(sessionAccessManager.getInternalSession(sessionID));
	            }catch (Throwable e) {
	            	Debug.getInstance(SessionConstants.SESSION_DEBUG).warning("error update "+sessionID.toString(), e);
				}
        }
    }
}
