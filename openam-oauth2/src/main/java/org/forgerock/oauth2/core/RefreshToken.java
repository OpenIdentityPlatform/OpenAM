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

package org.forgerock.oauth2.core;

import org.forgerock.json.JsonValue;

import java.util.Map;

/**
 * Base type for StatefulRefreshToken and StatelessRefreshToken.
 */
public interface RefreshToken extends IntrospectableToken {

    /**
     * Gets the token's redirect uri.
     *
     * @return The token's redirect uri.
     */
    String getRedirectUri();

    @Override
    String getTokenId();

    /**
     * Gets the token's claims.
     *
     * @return The token's claims.
     */
    String getClaims();

    /**
     * Gets the token's Authentication Context Class reference.
     *
     * @return The token's Authentication Context Class reference.
     */
    String getAuthenticationContextClassReference();

    /**
     * Gets the token's type.
     *
     * @return The token's type.
     */
    String getTokenType();

    /**
     * Gets the token's Auth Modules.
     *
     * @return The token's Auth Modules.
     */
    String getAuthModules();

    @Override
    Map<String, Object> toMap();

    /**
     * Gets the token's audit tracking id.
     *
     * @return The token's audit tracking id.
     */
    String getAuditTrackingId();

    /**
     * Gets the token's Auth Grant id.
     *
     * @return The token's Auth Grant id.
     */
    String getAuthGrantId();
}
