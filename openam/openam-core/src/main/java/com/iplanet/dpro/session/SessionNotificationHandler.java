/**
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
 * Portions Copyrighted 2011-2014 ForgeRock AS.
 */
package com.iplanet.dpro.session;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionNotification;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.share.Notification;
import com.sun.identity.shared.debug.Debug;

import java.util.Vector;

/**
 * <code>SessionNotificationHandler</code> implements
 * <code>NotificationHandler</code> processes the notifications for a session
 * object
 */
public class SessionNotificationHandler implements NotificationHandler {

    public static SessionNotificationHandler handler = null;

    public static Debug sessionDebug = null;

    static {
        sessionDebug = Debug.getInstance("amSession");
    }


    /**
     * Process the notification.
     *
     * @param notifications array of notifications to be processed.
     */
    public void process(Vector notifications) {
        for (int i = 0; i < notifications.size(); i++) {
            processRemoteNotification((Notification) notifications.elementAt(i));
        }
    }

    /**
     * Process the notification.
     *
     * @param notification An XML Notification object describing changes to a remote Session
     */
    public void processRemoteNotification(Notification notification) {
        SessionNotification snot = SessionNotification.parseXML(notification.getContent());
        if (snot != null) {
            processNotification(snot, false);
        }
    }

    /**
     * Process the notification.
     *
     * @param sessionNotification A SessionNotification object describing changes to a local Session
     */
    public void processLocalNotification(SessionNotification sessionNotification) {
        processNotification(sessionNotification, true);
    }

    /**
     * Process the notification.
     *
     * @param snot Session Notification object.
     */
    private void processNotification(SessionNotification snot, boolean isLocal) {
        SessionInfo info = snot.getSessionInfo();

        sessionDebug.message("SESSION NOTIFICATION : " + info.toXMLString());

        if (!info.state.equals("valid")) {
            if (isLocal) {
                Session.removeLocalSID(info);
            } else {
                Session.removeRemoteSID(info);
            }
            return;
        }

        SessionID sid = new SessionID(info.sid);
        Session session = Session.readSession(sid);
        try {
            if (session == null) {
                // a new session is created
                if (Session.getAllSessionEventListeners().isEmpty()) {
                    return;
                }
                session = new Session(sid);
            }
            session.update(info);
        } catch (Exception e) {
            sessionDebug.error(
                    "SessionNotificationHandler:processNotification : ", e);
            Session.removeSID(sid);
            return;
        }
        SessionEvent evt = new SessionEvent(session, snot.getNotificationType(), snot.getNotificationTime());
        Session.invokeListeners(evt);
    }

}
