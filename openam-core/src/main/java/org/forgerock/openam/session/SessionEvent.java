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

/**
 * Represents a session event.
 * <p>
 * Maps from {@link com.iplanet.dpro.session.SessionEvent} to a session event enum.
 *
 * @since 14.0.0
 */
public enum SessionEvent {

    SESSION_CREATION(com.iplanet.dpro.session.SessionEvent.SESSION_CREATION),

    /**
     * Session idle time out event
     */
    IDLE_TIMEOUT(com.iplanet.dpro.session.SessionEvent.IDLE_TIMEOUT),

    /**
     * Session maximum time out event
     */
    MAX_TIMEOUT(com.iplanet.dpro.session.SessionEvent.MAX_TIMEOUT),

    /**
     * Session logout event
     */
    LOGOUT(com.iplanet.dpro.session.SessionEvent.LOGOUT),

    /**
     * Session reactivation event
     */
    REACTIVATION(com.iplanet.dpro.session.SessionEvent.REACTIVATION),

    /**
     * Session destroy event
     */
    DESTROY(com.iplanet.dpro.session.SessionEvent.DESTROY),

    /**
     * Session Property changed
     */
    PROPERTY_CHANGED(com.iplanet.dpro.session.SessionEvent.PROPERTY_CHANGED),

    /**
     * Session quota exhausted
     */
    QUOTA_EXHAUSTED(com.iplanet.dpro.session.SessionEvent.QUOTA_EXHAUSTED),

    /**
     * Session property protected against change
     */
    PROTECTED_PROPERTY(com.iplanet.dpro.session.SessionEvent.PROTECTED_PROPERTY);

    private final int sessionEventId;

    SessionEvent(int sessionEventId) {
        this.sessionEventId = sessionEventId;
    }

    /**
     * Given the session event Id will return the corresponding session event enum.
     *
     * @param sessionEventId the session event Id
     * @return the corresponding session event enum
     * @throws IllegalArgumentException if the session event Id is not valid
     */
    public static SessionEvent valueOf(int sessionEventId) {
        for (SessionEvent event : values()) {
            if (event.sessionEventId == sessionEventId) {
                return event;
            }
        }

        throw new IllegalArgumentException("Unknown session event Id " + sessionEventId);
    }

}
