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

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;

import java.util.Map;
import java.util.Set;

/**
 * Handles the issuing of Tokens for a response type, i.e. code, token, id_token.
 *
 * @since 12.0.0
 * @supported.all.api
 */
public interface ResponseTypeHandler {

    /**
     * Handles the creating of a Token instance and storing the Token in the OAuth2 provider {@link TokenStore}.
     *
     * @param tokenType The type of the token.
     * @param scope The requested scope.
     * @param resourceOwnerId The resource owner's id.
     * @param clientId The client's id.
     * @param redirectUri The redirect uri.
     * @param nonce The nonce.
     * @param request The OAuth2 request.
     * @return A {@code Map.Entry} of the token name with the Token instance.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     */
    Map.Entry<String, Token> handle(String tokenType, Set<String> scope, String resourceOwnerId, String clientId,
            String redirectUri, String nonce, OAuth2Request request) throws ServerException, InvalidClientException;

    /**
     * Returns the location in which the token should be returned, {@link UrlLocation}.
     *
     * @return The UrlLocation.
     */
    UrlLocation getReturnLocation();
}
