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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Set;

/**
 * Represents an OAuth2 Refresh Token.
 *
 * @since 12.0.0
 */
public class RefreshToken {

    private final CoreToken token;

    /**
     * Constructs a new RefreshToken backed by the specified CoreToken.
     *
     * @param token The backing CoreToken.
     */
    public RefreshToken(final CoreToken token) {
        this.token = token;
    }

    /**
     * The token's identifier.
     *
     * @return The token's identifier.
     */
    public String getTokenId() {
        return token.getTokenID();
    }

    /**
     * The user's identifier.
     *
     * @return The user's identifier.
     */
    public String getUserId() {
        return token.getUserID();
    }

    /**
     * Whether the token has expired.
     *
     * @return {@code true} if the token has expired.
     */
    public boolean isExpired() {
        return token.isExpired();
    }

    /**
     * Gets the client's identifier.
     *
     * @return The client's identifier.
     */
    public String getClientId() {
        return token.getClientID();
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        return token.getScope();
    }
}
