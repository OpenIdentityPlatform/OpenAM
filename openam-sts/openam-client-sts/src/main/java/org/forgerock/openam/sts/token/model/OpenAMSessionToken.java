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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.model;

import org.forgerock.util.Reject;

/**
 * This class represents an OpenAM session token, built around the OpenAM session id. It will be used as the token type
 * for TokenAuthenticationRequestDispatcher<T> and AuthenticationHandler<T> implementations.
 */
public class OpenAMSessionToken {
    private final String sessionId;

    public OpenAMSessionToken(String sessionId) {
        this.sessionId = sessionId;
        Reject.ifNull(this.sessionId, "Session id cannot be null!");
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return sessionId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OpenAMSessionToken) {
            OpenAMSessionToken otherToken = (OpenAMSessionToken)other;
            return sessionId.equals(otherToken.getSessionId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode();
    }
}
