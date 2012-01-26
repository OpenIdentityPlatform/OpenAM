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
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionNotification;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.share.Notification;
import com.sun.identity.shared.debug.Debug;
import java.util.Hashtable;
import java.util.Vector;

/**
 * <code>SessionNotificationHandler</code> implements
 * <code>NotificationHandler</code> processes the notifications for a session 
 * object
 *
 */
public class SessionNotificationHandler implements NotificationHandler {

    private Hashtable sessionTable;

    public static SessionNotificationHandler handler = null;

    public static Debug sessionDebug = null;

    static {
        sessionDebug = Debug.getInstance("amSession");
    }
    
    /**
     * Constructs <code>SessionNotificationHandler</code>
     * @param table Session table
     */
    public SessionNotificationHandler(Hashtable table) {
        sessionTable = table;

    }

   /**
    * Process the notification.
    *
    * @param notifications array of notifications to be processed.
    */
   public void process(Vector notifications) {
        for (int i = 0; i < notifications.size(); i++) {
            Notification not = (Notification) notifications.elementAt(i);
            SessionNotification snot = SessionNotification.parseXML(not
                    .getContent());
            if (snot != null) {
                processNotification(snot);
            }
        }
    }

   /**
    * Process the notification.
    *
    * @param snot Session Notification object.
    */
   public void processNotification(SessionNotification snot) {
        SessionInfo info = snot.getSessionInfo();

        sessionDebug.message("SESSION NOTIFICATION : " + info.toXMLString());

        SessionID sid = new SessionID(info.sid);
        Session session = null;
        session = (Session) sessionTable.get(sid);
        if (session != null) {
            if (!info.state.equals("valid")) {
                Session.removeSID(sid);
                return;
            }
        }


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
        SessionEvent evt = new SessionEvent(session,
                snot.getNotificationType(), snot.getNotificationTime());
        Session.invokeListeners(evt);
    }

}
