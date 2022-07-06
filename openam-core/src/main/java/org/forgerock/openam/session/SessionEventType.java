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
 * Portions Copyrighted 2022 Open Identity Platform Community
 */

package org.forgerock.openam.session;

/**
 * Represents a session event.
 * <p>
 * Maps from {@link com.iplanet.dpro.session.SessionEvent} to a session event enum.
 *
 * @since 14.0.0
 */
public enum SessionEventType {

    /**
     * Session creation event
     */
    SESSION_CREATION(0),

    /**
     * Session idle time out event
     */
    IDLE_TIMEOUT(1),

    /**
     * Session maximum time out event
     */
    MAX_TIMEOUT(2),

    /**
     * Session logout event
     */
    LOGOUT(3),

    // code = 4 is skipped as it used to be mapped to REACTIVATION (an event which is no longer possible)

    /**
     * Session destroy event
     */
    DESTROY(5),

    /**
     * Session Property changed
     */
    PROPERTY_CHANGED(6),

    /**
     * Session quota exhausted
     */
    QUOTA_EXHAUSTED(7),

    /**
     * Session property protected against change
     */
    PROTECTED_PROPERTY(8),

    // code = 9 is skipped as it used to be mapped to SESSION_MAX_LIMIT_REACHED (an event which is no longer possible)

    /**
     * Session event url added
     */
    EVENT_URL_ADDED(10);

    private final int code;

    SessionEventType(int code) {
        this.code = code;
    }

    /**
     * An int code identifier for this session event type.
     *
     * @return int code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Given the session event Id will return the corresponding session event enum.
     *
     * @param code the session event type code.
     * @return the corresponding session event type.
     * @throws IllegalArgumentException if the session event code is not valid.
     */
    public static SessionEventType fromCode(int code) {
        for (SessionEventType event : values()) {
            if (event.code == code) {
                return event;
            }
        }

        throw new IllegalArgumentException("Unknown session event Id " + code);
    }

}
