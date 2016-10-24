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
package org.forgerock.openam.dpro.session;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionBundle;

/**
 * This exception describes the case when a session is considered invalid, most likely because the session ID cannot be
 * found in the deployment.
 */
public class InvalidSessionIdException extends SessionException {

    /**
     * Constructs a new exception object, no session ID will be provided in the error message.
     */
    public InvalidSessionIdException() {
        this("");
    }

    /**
     * Constructs a new exception object.
     *
     * @param sessionId The Session ID that is considered invalid. May be null.
     */
    public InvalidSessionIdException(SessionID sessionId) {
        this(sessionId != null ? sessionId.toString() : "");
    }

    /**
     * Constructs a new exception object.
     * @param sessionId The Session ID that is considered invalid. May not be null.
     */
    public InvalidSessionIdException(String sessionId) {
        super(SessionBundle.getString("invalidSessionID") + sessionId);
    }
}
