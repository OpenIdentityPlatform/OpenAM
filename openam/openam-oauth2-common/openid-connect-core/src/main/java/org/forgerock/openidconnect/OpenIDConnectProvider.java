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

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Provider for OpenId Connect for managing OpenId Connect sessions.
 *
 * @since 12.0.0
 */
public interface OpenIDConnectProvider {

    /**
     * Determines whether a user has a valid session.
     *
     * @param userId The user's id.
     * @param request The OAuth2 request.
     * @return {@code true} if the user is valid.
     */
    boolean isUserValid(String userId, OAuth2Request request);

    /**
     * Destroys a users session.
     *
     * @param kid The key id of the id token JWT
     * @throws ServerException If any internal server error occurs.
     */
    void destroySession(String kid) throws ServerException;
}
