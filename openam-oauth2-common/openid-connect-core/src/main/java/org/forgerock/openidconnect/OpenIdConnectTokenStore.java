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
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Interface for a OpenId Connect Token Store which the OpenId Connect Provider will implement.
 * <br/>
 * The OpenId Connect Token Store will be where all OpenId Connect tokens will be stored and later retrieved.
 *
 * @since 12.0.0
 */
public interface OpenIdConnectTokenStore extends TokenStore {

    /**
     * Creates an OpenId Connect token and stores it in the OpenId Connect Provider's store.
     *
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param authorizationParty The authorization party.
     * @param nonce The nonce.
     * @param ops The ops.
     * @param request The OAuth2 request.
     * @return An OpenIdConnectToken.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     */
    OpenIdConnectToken createOpenIDToken(String resourceOwnerId, String clientId, String authorizationParty,
            String nonce, String ops, OAuth2Request request) throws ServerException, InvalidClientException;
}
