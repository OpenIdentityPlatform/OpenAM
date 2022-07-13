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
 * $Id: SessionNotificationHandler.java,v 1.5 2008/07/23 18:13:55 ww203982 Exp $
 *
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2022 Open Identity Platform Community
 */
package com.iplanet.dpro.session;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionNotification;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.share.Notification;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.session.service.SessionAccessManager;

import java.util.Vector;

/**
 * <code>SessionNotificationHandler</code> implements
 * <code>NotificationHandler</code> processes the notifications for a session
 * object
 */
public class SessionNotificationHandler implements NotificationHandler {

    public static SessionNotificationHandler handler = null;

    public static Debug sessionDebug = null;

    private final SessionCache sessionCache;

    SessionNotificationHandler(SessionCache sessionCache) {
        this.sessionCache = sessionCache;
    }

    static {
        sessionDebug = Debug.getInstance("amSession");
    }

    /**
     * Process the notification.
     *
     * @param notifications array of notifications to be processed.
     */
    public void process(Vector<Notification> notifications) {
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.elementAt(i);
            processNotification(SessionNotification.parseXML(notification.getContent()));
        }
    }

    /**
     * Process the notification.
     *
     * @param notification A SessionNotification object describing changes to the Session
     */
    public void processNotification(SessionNotification notification) {
        SessionInfo info = notification.getSessionInfo();

        sessionDebug.message("SESSION NOTIFICATION : " + info.toXMLString());

        if (!info.getState().equals("valid")) {
            SessionID sid = new SessionID(info.getSessionID());
            sessionCache.removeSID(sid);
            SessionAccessManager sessionAccessManager = InjectorHolder.getInstance(SessionAccessManager.class);
            InternalSession internalSession = sessionAccessManager.getInternalSession(sid);
            if(internalSession != null) { //if InternalSession session exists in current CTS cluster or cache, remove it
                sessionAccessManager.removeInternalSession(internalSession);
            }
            return;
        }

        SessionID sid = new SessionID(info.getSessionID());
        Session session = sessionCache.readSession(sid);
        try {
            if (session == null) {
                // a new session is created
                return;
            }
            session.update(info);
        } catch (Exception e) {
            sessionDebug.error(
                    "SessionNotificationHandler:processNotification : ", e);
            sessionCache.removeSID(sid);
            return;
        }
        SessionEventType sessionEventType = SessionEventType.fromCode(notification.getNotificationType());
        SessionEvent evt = new SessionEvent(session, sessionEventType, notification.getNotificationTime());
        Session.invokeListeners(evt);

        //remote session
        if(sessionEventType.equals(SessionEventType.EVENT_URL_ADDED) && session.getLocalSessionEventListeners().size() == 0) { //remote session
            SessionAccessManager sessionAccessManager = InjectorHolder.getInstance(SessionAccessManager.class);
            InternalSession internalSession = sessionAccessManager.getInternalSession(session.getSessionID());
            if(internalSession != null) {
                for (String url : info.getSessionEventUrls()) {
                    internalSession.addSessionEventURL(url, internalSession.getSessionID());
                }
            }
        }
    }

}
