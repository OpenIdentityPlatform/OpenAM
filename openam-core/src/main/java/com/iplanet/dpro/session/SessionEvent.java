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
 * $Id: SessionEvent.java,v 1.2 2008/06/25 05:41:29 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.iplanet.dpro.session;

import org.forgerock.openam.session.SessionEventType;
import org.forgerock.util.Reject;

/**
 * This class represents a session event.
 * <p>
 * If this is a new session, the event object contains all session information of this new
 * session; otherwise, only the changed information of the session is contained in the event.
 *
 * @see com.iplanet.dpro.session.Session
 * @see SessionEventType
 */
public class SessionEvent {

    private Session session;
    private SessionEventType eventType;
    private long eventTime;

    /**
     * Creates a new session event.
     *
     * @param session The session object which emitted this event.
     * @param eventType The event which has occurred.
     * @param eventTime The event time as UTC milliseconds from the epoch.
     */
    public SessionEvent(Session session, SessionEventType eventType, long eventTime) {
        this.session = session;
        this.eventType = eventType;
        this.eventTime = eventTime;
    }

    /**
     * Gets the session object which emitted this event.
     * 
     * @return The session object which emitted this event.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Gets the type of event that has occurred.
     * 
     * @return The type of this event.
     */
    public SessionEventType getType() {
        return eventType;
    }

    /**
     * Gets the time of this event.
     * 
     * @return The event time as UTC milliseconds from the epoch
     */
    public long getTime() {
        return eventTime;
    }

    /**
     * Invokes all listeners on the Session associated with the event.
     *
     * @param sessionEvent Non null Session Event.
     */
    public static void invokeListeners(SessionEvent sessionEvent) {
        Reject.ifNull(sessionEvent, sessionEvent.getSession());
        final Session session = sessionEvent.getSession();
        for (SessionListener listener : session.getLocalSessionEventListeners()) {
            listener.sessionChanged(sessionEvent);
        }
    }
}
