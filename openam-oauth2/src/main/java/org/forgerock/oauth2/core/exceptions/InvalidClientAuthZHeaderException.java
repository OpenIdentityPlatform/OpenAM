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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core.exceptions;

/**
 * Thrown when client authentication fails.
 *
 * @since 13.0.0
 */
public class InvalidClientAuthZHeaderException extends InvalidClientException {

    private final String challengeScheme;
    private final String challengeRealm;

    /**
     * Constructs a new InvalidClientAuthZHeaderException with the specified message, header name and value.
     *
     * @param message The reason for the exception.
     * @param challengeScheme The name of the challenge type for the WWW-Authenticate header.
     * @param challengeRealm The name of the challenge realm for the WWW-Authenticate header.
     */
    InvalidClientAuthZHeaderException(final String message, final String challengeScheme,
                                             final String challengeRealm) {
        super(401, "invalid_client", message);
        this.challengeScheme = challengeScheme;
        this.challengeRealm = challengeRealm;
    }

    /**
     * Gets the challenge type for the WWW-Authenticate header.
     *
     * @return The challenge type.
     */
    public String getChallengeScheme() {
        return challengeScheme;
    }

    /**
     * Gets the challenge realm for the WWW-Authenticate header.
     *
     * @return The challenge realm.
     */
    public String getChallengeRealm() {
        return challengeRealm;
    }
}
