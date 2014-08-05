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

import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequiredException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;

/**
 * Handles authorization requests from OAuth2 clients to the OAuth2 provider to grant authorization for a specific
 * client by a specific resource owner.
 *
 * @since 12.0.0
 */
public interface AuthorizationService {

    /**
     * Handles an initial authorization request from a OAuth2 client, validates the request is valid and contains
     * the required parameters, checks the resource owner has authenticated and given their consent for the client to
     * be authorized before issuing an AuthorizationToken.
     * <br/>
     * If the resource owner is not authenticated, then the user-agent is redirected to a login page for the
     * resource owner to authenticate. And if the resource owner has not given their consent (or have not requested
     * it to be saved from a previous authorization request) the user-agent is redirected to the user consent page.
     * The user-agent is then redirected back to the OAuth2 authorize endpoint.
     * <br/>
     * An AuthorizationToken is only ever issued by this method if the resource owner has previously given their
     * consent. In the case where the user-agent is redirected to the user consent page, when the user-agent is
     * redirected back to OAuth2 authorize endpoint the #authorize(OAuth2Request, boolean, boolean) method on this class
     * must be called.
     *
     * @param request The OAuth2Request for the client requesting authorization. Must not be {@code null}.
     * @return An AuthorizationToken containing the OAuth2 tokens granted as apart of the authorize call.
     * @throws ResourceOwnerAuthenticationRequired If the resource owner needs to authenticate before the authorize
     *          request can be allowed.
     * @throws ResourceOwnerConsentRequired If the resource owner's consent is required before the authorize request
     *          can be allowed.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws AccessDeniedException If resource owner authentication fails.
     * @throws ServerException If any internal server error occurs.
     * @throws LoginRequiredException If authenticating the resource owner fails.
     * @throws BadRequestException If the request is malformed.
     * @throws InteractionRequiredException If the OpenID Connect prompt parameter enforces that the resource owner
     *          is not asked to authenticate, but the resource owner does not have a current authenticated session.
     * @throws ResourceOwnerConsentRequiredException If the OpenID Connect prompt parameter enforces that the resource
     *          owner is not asked for consent, but the resource owners consent has not been previously stored.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    AuthorizationToken authorize(OAuth2Request request) throws ResourceOwnerAuthenticationRequired,
            ResourceOwnerConsentRequired, InvalidClientException, UnsupportedResponseTypeException,
            RedirectUriMismatchException, InvalidRequestException, AccessDeniedException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException,
            ResourceOwnerConsentRequiredException, InvalidScopeException;

    /**
     * Handles an authorization request from a OAuth2 client, validates the request is valid and contains the required
     * parameters, checks the resource owner has authenticated and given their consent for the client to be authorized
     * before issuing an AuthorizationToken.
     * <br/>
     * If the resource owner is not authenticated, then the user-agent is redirected to a login page for the
     * resource owner to authenticate.
     *
     * @param request The OAuth2Request for the client requesting authorization. Must not be {@code null}.
     * @param consentGiven {@code true} if the user has given their consent for the requesting client to be authorized.
     * @param saveConsent {@code true} if the user has requested that their consent be saved for future authorization
     *                    requests.
     * @return An AuthorizationToken containing the OAuth2 tokens granted as apart of the authorize call.
     * @throws AccessDeniedException If resource owner authentication fails or the resource owner does not grant
     *          authorization for the client.
     * @throws ResourceOwnerAuthenticationRequired If the resource owner needs to authenticate before the authorize
     *          request can be allowed.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws UnsupportedResponseTypeException If the requested response type is not supported by either the client
     *          or the OAuth2 provider.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws RedirectUriMismatchException If the redirect uri on the request does not match the redirect uri
     *          registered for the client.
     * @throws ServerException If any internal server error occurs.
     * @throws LoginRequiredException If authenticating the resource owner fails.
     * @throws BadRequestException If the request is malformed.
     * @throws InteractionRequiredException If the OpenID Connect prompt parameter enforces that the resource owner
     *          is not asked to authenticate, but the resource owner does not have a current authenticated session.
     * @throws IllegalArgumentException If the request is missing any required parameters.
     * @throws InvalidScopeException If the requested scope is invalid, unknown, or malformed.
     */
    AuthorizationToken authorize(OAuth2Request request, boolean consentGiven, boolean saveConsent)
            throws AccessDeniedException, ResourceOwnerAuthenticationRequired, InvalidClientException,
            UnsupportedResponseTypeException, InvalidRequestException, RedirectUriMismatchException, ServerException,
            LoginRequiredException, BadRequestException, InteractionRequiredException, InvalidScopeException;
}
