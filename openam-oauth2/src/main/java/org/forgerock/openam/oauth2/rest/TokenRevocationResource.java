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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2.rest;

import static java.util.Collections.singletonList;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.ACCESS_TOKEN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REFRESH_TOKEN;
import static org.forgerock.oauth2.core.Utils.isEmpty;
import static org.forgerock.oauth2.restlet.RestletConstants.SUPPORTED_RESTLET_CHALLENGE_SCHEMES;
import static org.forgerock.openam.cts.api.fields.OAuthTokenField.*;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.ClientAuthenticator;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientAuthZHeaderException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.OAuth2RealmResolver;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.query.QueryFilter;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests to the OAuth2 token endpoint for revoking tokens.
 *
 * @since 13.5.0
 */
public class TokenRevocationResource extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OAuth2RequestFactory requestFactory;
    private final ClientAuthenticator clientAuthenticator;
    private final TokenStore tokenStore;
    private final ExceptionHandler exceptionHandler;
    private final OAuth2RealmResolver realmResolver;

    /**
     * Constructs a new TokenRevocationResource.
     *
     * @param requestFactory An instance of the OAuth2RequestFactory.
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the OAuthTokenStore.
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param realmResolver An instance of the OAuth2RealmResolver
     */
    @Inject
    public TokenRevocationResource(OAuth2RequestFactory requestFactory,
            ClientAuthenticator clientAuthenticator,
            TokenStore tokenStore, ExceptionHandler exceptionHandler, OAuth2RealmResolver realmResolver) {
        this.clientAuthenticator = clientAuthenticator;
        this.requestFactory = requestFactory;
        this.tokenStore = tokenStore;
        this.exceptionHandler = exceptionHandler;
        this.realmResolver = realmResolver;
    }

    /**
     * Handles POST requests to the OAuth2 token revocation endpoint.
     *
     * @param entity The entity on the request.
     * @return The body to be sent in the response to the user agent.
     * @throws OAuth2RestletException If a OAuth2 error occurs whilst processing the revocation request.
     */
    @Post
    public Representation revoke(Representation entity) throws OAuth2RestletException {
        final OAuth2Request request = requestFactory.create(getRequest());
        final String realm = realmResolver.resolveFrom(request);
        final String tokenId = request.getParameter("token");
        try {
            if(isEmpty(tokenId)) {
                throw new InvalidRequestException("Missing parameter: token");
            }
            final ClientRegistration clientRegistration = clientAuthenticator.authenticate(request, null);
            final String clientId = clientRegistration.getClientId();
            final JsonValue token = getToken(clientId, tokenId);
            if (token != null) {
                final String tokenName = getAttributeValue(token, TOKEN_NAME);
                switch (tokenName) {
                    case ACCESS_TOKEN:
                        deleteAccessToken(realm, tokenId);
                        break;
                    case REFRESH_TOKEN:
                        deleteRefreshTokenAndAccessTokens(realm, token, clientId);
                        break;
                    default:
                        throw new InvalidRequestException("Invalid token name: " + tokenName);
                }
            }
            return new JsonRepresentation(new JSONObject());
        } catch (InvalidClientAuthZHeaderException e) {
            getResponse().setChallengeRequests(singletonList(
                    new ChallengeRequest(
                            ChallengeScheme.valueOf(SUPPORTED_RESTLET_CHALLENGE_SCHEMES.get(e.getChallengeScheme())),
                            e.getChallengeRealm())));
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (InvalidClientException e) {
            logger.error(e.getMessage(), e);
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (CoreTokenException e) {
            logger.error(e.getMessage(), e);
            throw new OAuth2RestletException(500, "Failed to find token with id :" + tokenId, e.getMessage(),
                    request.<String>getParameter("state"));
        } catch (OAuth2Exception e) {
            logger.error(e.getMessage(), e);
            throw new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(),
                    request.<String>getParameter("redirect_uri"), request.<String>getParameter("state"),
                    e.getParameterLocation());
        }

    }

    private void deleteAccessToken(String realm, String tokenId) throws ServerException, NotFoundException {
        try {
            tokenStore.delete(realm, tokenId);
        } catch (ServerException e) {
            logger.error("Failed to delete access token with id :" + tokenId, e);
            throw new ServerException("Failed to revoke access token");
        }
    }

    private void deleteRefreshTokenAndAccessTokens(String realm, JsonValue token,
            String clientId) throws ServerException, NotFoundException {
        final String userName = getAttributeValue(token, USER_NAME.getOAuthField());
        final String authGrantId = getAttributeValue(token, CoreTokenParams.AUTH_GRANT_ID);
        final JsonValue userTokens;
        int deletedTokens = 0;
        try {
            userTokens = getTokens(realm, clientId, userName, authGrantId);
            String tokenId = null;
            for (JsonValue userToken : userTokens) {
                try {
                    tokenId = getAttributeValue(userToken, ID.getOAuthField());
                    tokenStore.delete(realm, tokenId);
                    deletedTokens++;
                } catch (ServerException e) {
                    logger.error("Failed to delete token with id :" + tokenId, e);
                }
            }
            int allTokens = userTokens.size();
            if (deletedTokens < allTokens) {
                int notRevoked = allTokens - deletedTokens;
                logger.error("Failed to revoke " + notRevoked + " from " + allTokens + " tokens");
            }
        } catch (ServerException | NotFoundException e) {
            logger.error("Failed to fetch all the related tokens for the client :"
                    + clientId + "and user name :" + userName, e);
            throw new ServerException("Failed to revoke refresh and access tokens");
        }
    }

    private JsonValue getTokens(String realm, String clientId, String userName,
            String authGrantId) throws ServerException, NotFoundException {
        QueryFilter<CoreTokenField> allTokensQuery = and(equalTo(USER_NAME.getField(), userName),
                equalTo(CLIENT_ID.getField(), clientId), equalTo(AUTH_GRANT_ID.getField(), authGrantId));
        return tokenStore.queryForToken(realm, allTokensQuery);
    }

    private JsonValue getToken(String clientId, String tokenId)
            throws CoreTokenException, InvalidRequestException, InvalidGrantException,
            ServerException, NotFoundException {
        JsonValue token = tokenStore.read(tokenId);
        if(token != null) {
            String tokenClientId = getAttributeValue(token, CLIENT_ID.getOAuthField());
            if (!clientId.equals(tokenClientId)) {
                throw new InvalidGrantException("The provided token id : "
                        + tokenId + " belongs to different access grant.");
            }
        }
        return token;
    }

    private String getAttributeValue(JsonValue token, String attributeName) {
        String value = null;
        JsonValue jsonValue = token.get(attributeName);
        if (jsonValue.isString()) {
            value = jsonValue.asString();
        } else if (jsonValue.isCollection()) {
            value = CollectionUtils.getFirstItem(jsonValue.asCollection(String.class));
        }
        return value;
    }

    /**
     * Handles any exception that is thrown when processing a OAuth2 authorization request.
     *
     * @param throwable The throwable.
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}

