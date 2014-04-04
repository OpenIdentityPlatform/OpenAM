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

/**
 * Represents a OAuth2 Grant Type, used by the OAuth2 token endpoint.
 *
 * @since 12.0.0
 */
public interface GrantType {

    /**
     * Enum for all of the Grant Types specified in the core OAuth2 specification.
     *
     * @since 12.0.0
     */
    enum DefaultGrantType implements GrantType {
        /**
         * OAuth2 Authorization Code Grant Type.
         */
        AUTHORIZATION_CODE,
        /**
         * OAuth2 Password Credentials Grant Type.
         */
        PASSWORD,
        /**
         * OAuth2 Client Credentials Grant Type.
         */
        CLIENT_CREDENTIALS,
        /**
         * OAuth2 Refresh Token.
         */
        REFRESH_TOKEN;
    }
}
