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

import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailedException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

/**
 * Handles access token requests from OAuth2 clients to the OAuth2 provider to grant access tokens for the requested
 * grant types.
 *
 * @since 12.0.0
 */
public interface AccessTokenService {

    /**
     * Handles a request for access token(s) by a OAuth2 client, validates that the request is valid and contains the
     * required parameters, checks that the authorization code on the request is valid and has not expired, or been
     * previously used.
     *
     * @param request The OAuth2Request for the client requesting an access token. Must not be {@code null}.
     * @return An AccessToken.
     * @throws InvalidGrantException If the requested grant on the request is not supported.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws ClientAuthenticationFailedException If client authentication fails.
     * @throws InvalidCodeException If the authorization code on the request has expired.
     * @throws ServerException If any internal server error occurs.
     * @throws UnauthorizedClientException If the client's authorization fails.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     */
    AccessToken requestAccessToken(OAuth2Request request) throws InvalidGrantException, RedirectUriMismatchException,
            InvalidClientException, InvalidRequestException, ClientAuthenticationFailedException, InvalidCodeException,
            ServerException, UnauthorizedClientException, InvalidScopeException;

    /**
     * Handles a request to refresh an already issued access token for a OAuth2 client, validates that the request is
     * valid and contains the  required parameters, checks that the refresh token on the request is valid and has not
     * expired, or been previously used to refresh an access token.
     *
     * @param request The OAuth2Request for the client requesting an refresh token. Must not be {@code null}.
     * @return An Access Token.
     * @throws ClientAuthenticationFailedException If client authentication fails.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws BadRequestException If the request is malformed.
     * @throws ServerException If any internal server error occurs.
     * @throws ExpiredTokenException If the access token or refresh token has expired.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     * @throws InvalidGrantException If the given token is not a refresh token.
     */
    AccessToken refreshToken(OAuth2Request request) throws ClientAuthenticationFailedException, InvalidClientException,
            InvalidRequestException, BadRequestException, ServerException, ExpiredTokenException, InvalidGrantException, InvalidScopeException;
}
