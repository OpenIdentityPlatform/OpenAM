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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static org.forgerock.oauth2.core.AccessTokenVerifier.REALM_AGNOSTIC_HEADER;
import static org.forgerock.oauth2.core.AccessTokenVerifier.REALM_AGNOSTIC_QUERY_PARAM;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ExpiredTokenException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidTokenException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to return the full information of a OAuth2 token.
 *
 * @since 12.0.0
 */
@Singleton
public class TokenInfoService {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final AccessTokenVerifier headerTokenVerifier;
    private final AccessTokenVerifier queryTokenVerifier;
    private final ClientRegistrationStore clientRegistrationStore;

    /**
     * Constructs a new TokenInfoServiceImpl.
     *
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param headerTokenVerifier Basic HTTP access token verification.
     * @param queryTokenVerifier Query string access token verification.
     * @param clientRegistrationStore An instance of the ClientRegistrationStore.
     */
    @Inject
    public TokenInfoService(OAuth2ProviderSettingsFactory providerSettingsFactory,
            @Named(REALM_AGNOSTIC_HEADER) AccessTokenVerifier headerTokenVerifier,
            @Named(REALM_AGNOSTIC_QUERY_PARAM) AccessTokenVerifier queryTokenVerifier,
            ClientRegistrationStore clientRegistrationStore) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.headerTokenVerifier = headerTokenVerifier;
        this.queryTokenVerifier = queryTokenVerifier;
        this.clientRegistrationStore = clientRegistrationStore;
    }

    /**
     * Returns a Json representation of the token's information that is on the OAuth2 request.
     *
     * @param request The OAuth2 request.
     * @return The token's information.
     * @throws InvalidRequestException If the request is missing any required parameters or is otherwise malformed.
     * @throws ServerException If any internal server error occurs.
     * @throws BadRequestException If the request is malformed.
     * @throws InvalidGrantException If the given token is not an Access token.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    public JsonValue getTokenInfo(OAuth2Request request) throws InvalidTokenException, InvalidRequestException,
            ExpiredTokenException, ServerException, BadRequestException, InvalidGrantException, NotFoundException {

        final AccessTokenVerifier.TokenState headerToken = headerTokenVerifier.verify(request);
        final AccessTokenVerifier.TokenState queryToken = queryTokenVerifier.verify(request);

        ensureSingleTokenInRequest(headerToken, queryToken);
        assertTokenIsValid(headerToken, queryToken);
        final AccessToken accessToken = request.getToken(AccessToken.class);
        //since the token info request is realm agnostic the realm is read from the token and is set on the
        //request object to correctly check the client for token is created still exists and is active
        request.<Request>getRequest().getAttributes().put(OAuth2Constants.Custom.REALM, accessToken.getRealm());
        assertTokenClientExists(accessToken, request);

        logger.trace("In Validator resource - got token = " + accessToken);

        final Map<String, Object> response = new HashMap<String, Object>();
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final Map<String, Object> scopeEvaluation = providerSettings.evaluateScope(accessToken);
        response.putAll(accessToken.getTokenInfo());
        response.putAll(scopeEvaluation);

        return new JsonValue(response);
    }

    /**
     * Checks that the token for which the info is being requested is valid
     */
    private void assertTokenIsValid(AccessTokenVerifier.TokenState headerToken, AccessTokenVerifier.TokenState queryToken) throws InvalidTokenException {
        if (!headerToken.isValid() && !queryToken.isValid()) {
            logger.error("Access Token not valid");
            throw new InvalidTokenException();
        }
    }

    /**
     * Ensure there is only one token in the token info request
     */
    private void ensureSingleTokenInRequest(AccessTokenVerifier.TokenState headerToken, AccessTokenVerifier.TokenState queryToken) throws InvalidRequestException {
        if (headerToken.isValid() && queryToken.isValid()) {
            logger.error("Access Token provided in both query and header in request");
            throw new InvalidRequestException("Access Token cannot be provided in both query and header");
        }
    }

    /**
     * Checks if the client to which the access token is issued exists and is inactive
     */
    private void assertTokenClientExists(AccessToken accessToken, OAuth2Request request) throws InvalidTokenException {
        String clientId = accessToken.getClientId();
        try {
            clientRegistrationStore.get(clientId, request);
        } catch (InvalidClientException | NotFoundException e) {
            logger.error("The client identified by the id: " + clientId + " does not exist");
            throw new InvalidTokenException();
        }
    }
}
