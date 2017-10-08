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
 */

package org.forgerock.openam.session;

import java.util.List;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.sun.identity.shared.debug.Debug;

/**
 * Ex-Sun class, pulled out from Session.java.
 *
 * Used to poll a SessionService periodically. The information recovered is used to update a Session object, and if
 * necessary will result in it being removed from the local cache.
 *
 */
public class SessionPollerSender implements Runnable {

    private final SessionCuller sessionCuller;
    private SessionInfo info = null;
    private final Session session;
    private final SessionID sid;
    private final Debug debug = Debug.getInstance(SessionConstants.SESSION_DEBUG);
    private final SessionCache sessionCache = SessionCache.getInstance();
    private final SessionPLLSender pllSender = new SessionPLLSender(SessionCookies.getInstance());

    /**
     * Creates a new SessionPollerSender.
     * @param session The session to create the sender for.
     * @param sessionCuller The associated session culler.
     */
    public SessionPollerSender(Session session, SessionCuller sessionCuller) {
        this.sessionCuller = sessionCuller;
        this.session = session;
        this.sid = session.getID();
    }

    public void run() {
        try {
            SessionRequest sreq = new SessionRequest(SessionRequest.GetSession, sid.toString(), false);
            SessionResponse sres = pllSender.sendPLLRequest(session.getSessionServiceURL(), sreq);

            if (sres.getException() != null) {
                sessionCache.removeSID(sid);
                return;
            }

            List<SessionInfo> infos = sres.getSessionInfo();

            if (infos.size() == 1) {
                info = infos.get(0);
            }
        } catch (Exception ex) {
            sessionCache.removeSID(sid);
            if (debug.messageEnabled())
                debug.message("Could not connect to the session server"
                        + ex.getMessage());
        }

        if (info != null) {
            if (debug.messageEnabled()) {
                debug.message("Updating" + info.toXMLString());
            }
            try {
                if (info.getState().equals("invalid") || info.getState().equals("destroyed")) {
                    sessionCache.removeSID(sid);
                } else {
                    long oldMaxCachingTime = session.getMaxCachingTime();
                    long oldMaxIdleTime = session.getMaxIdleTime();
                    long oldMaxSessionTime = session.getMaxSessionTime();
                    session.update(info);
                    if ((!sessionCuller.isScheduled()) ||
                            (oldMaxCachingTime > session.getMaxCachingTime()) ||
                            (oldMaxIdleTime > session.getMaxIdleTime()) ||
                            (oldMaxSessionTime > session.getMaxSessionTime())) {
                        sessionCuller.scheduleToTimerPool();
                    }
                }
            } catch (SessionException se) {
                sessionCache.removeSID(sid);
                debug.error("Exception encountered while update in polling",
                        se);
            }
        } else {
            sessionCache.removeSID(sid);
        }
        sessionCuller.setIsPolling(false);
    }
}
