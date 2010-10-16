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
 */

package com.iplanet.dpro.session;

/**
 * The <code>SessionEvent</code> class represents a session event. If this is
 * a new session, the session event contains all session information of this new
 * session, otherwise, only the changed information of the session is contained
 * in the session event.
 * </p>
 * The following are possible session event types:
 * <code>SESSION_CREATION</code>,
 * <code>IDLE_TIMEOUT</code>,
 * <code>MAX_TIMEOUT</code>,
 * <code>LOGOUT</code>,
 * <code>REACTIVATION</code>, and
 * <code>DESTROY</code>.
 * 
 * @see com.iplanet.dpro.session.Session
 */

public class SessionEvent {

    private Session session;

    private int eventType;

    private long eventTime;

    /** Session creation event */
    public static final int SESSION_CREATION = 0;

    /** Session idle time out event */
    public static final int IDLE_TIMEOUT = 1;

    /** Session maximum time out event */
    public static final int MAX_TIMEOUT = 2;

    /** Session logout event */
    public static final int LOGOUT = 3;

    /** Session reactivation event */
    public static final int REACTIVATION = 4;

    /** Session destroy event */
    public static final int DESTROY = 5;

    /** Session Property changed */
    public static final int PROPERTY_CHANGED = 6;

    /** Session quota exhausted */
    public static final int QUOTA_EXHAUSTED = 7;

    /**
     * No public constructor.
     */
    SessionEvent(Session sess, int type, long time) {
        session = sess;
        eventType = type;
        eventTime = time;
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
     * Gets the type of this event.
     * 
     * @return The type of this event. Possible types are :
     * 
     * <code>SESSION_CREATION</code>,
     * <code>IDLE_TIMEOUT</code>,
     * <code>MAX_TIMEOUT</code>,
     * <code>LOGOUT</code>,
     * <code>REACTIVATION</code>, and
     * <code>DESTROY</code>.
     */
    public int getType() {
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
}
