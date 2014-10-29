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

import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

/**
 * Handler for a specific OAuth2 grant type, i.e. Authorization Code, Client Credentials, Password Credentials.
 *
 * @since 12.0.0
 */
public interface GrantTypeHandler {

    /**
     * Handles an access token request for a specific OAuth2 grant type. Will validate that the OAuth2 request is valid
     * and contains all the required parameters, check that the authorization code is valid and has not expired
     * and will then issue an access token.
     *
     * @param request The OAuth2Request. Must not be {@code null}.
     * @return An AccessToken.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws ClientAuthenticationFailedException If client authentication fails.
     * @throws InvalidGrantException If the requested grant on the request is not supported.
     * @throws InvalidCodeException If the authorization code on the request has expired.
     * @throws ServerException If any internal server error occurs.
     * @throws UnauthorizedClientException If the client's authorization fails.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     */
    AccessToken handle(OAuth2Request request) throws RedirectUriMismatchException, InvalidClientException,
            InvalidRequestException, ClientAuthenticationFailedException, InvalidGrantException, InvalidCodeException,
            ServerException, UnauthorizedClientException, InvalidScopeException;
}
