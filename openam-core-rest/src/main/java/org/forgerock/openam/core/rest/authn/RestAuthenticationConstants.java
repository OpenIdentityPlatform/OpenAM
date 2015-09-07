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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.core.rest.authn;

/**
 * Collection of constants related to REST authentication.
 *
 * @since 13.0.0
 */
public final class RestAuthenticationConstants {

    /**
     * Name in auth ID JWT for session ID
     */
    public static final String SESSION_ID = "sessionId";

    /**
     * Name in JSON response for auth ID
     */
    public static final String AUTH_ID = "authId";

    /**
     * Name in JSON response for token ID
     */
    public static final String TOKEN_ID = "tokenId";

    private RestAuthenticationConstants() {
        // Prevent instantiation
    }

}
