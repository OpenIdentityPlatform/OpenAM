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

import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * Verifies that a OAuth2 request that is made to one of the protected endpoints on the OAuth2 provider,
 * (i.e. tokeninfo, userinfo) contains a valid access token.
 *
 * @since 12.0.0
 */
public abstract class AccessTokenVerifier {

    public static final String HEADER = "header";
    /**
     * An {@link AccessTokenVerifier} that verifies the OAuth2 access token provided in a header without checking the
     * realm corresponding to the {@link OAuth2Request}.
     */
    public static final String REALM_AGNOSTIC_HEADER = "realm-agnostic-header";
    public static final String FORM_BODY = "form-body";
    /**
     * An {@link AccessTokenVerifier} that verifies the OAuth2 access token provided in the request payload without
     * checking the realm corresponding to the {@link OAuth2Request}.
     */
    public static final String REALM_AGNOSTIC_FORM_BODY = "realm-agnostic-form-body";
    public static final String QUERY_PARAM = "query-param";
    /**
     * An {@link AccessTokenVerifier} that verifies the OAuth2 access token provided in the query parameter without
     * checking the realm corresponding to the {@link OAuth2Request}.
     */
    public static final String REALM_AGNOSTIC_QUERY_PARAM = "realm-agnostic-query-param";
    protected final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private static final TokenState INVALID_TOKEN = new TokenState(null);

    private final TokenStore tokenStore;

    protected AccessTokenVerifier(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * Verifies that the specified OAuth2 request contains a valid access token which has not expired.
     *
     * @param request The OAuth2 request. Must not be {@code null}.
     * @return {@code true} if the request contains an access token which is valid and has not expired.
     */
    public TokenState verify(OAuth2Request request) {
        final String tokenId = obtainTokenId(request);

        if (tokenId == null) {
            logger.debug("Request does not contain token id.");
            return INVALID_TOKEN;
        }

        try {
            //verify token
            final AccessToken accessToken = tokenStore.readAccessToken(request, tokenId);
            //is token expired?
            if (accessToken != null) {
                return accessToken.isExpired() ? INVALID_TOKEN : new TokenState(tokenId);
            }
        } catch (ServerException e) {
            logger.debug(e.getMessage());
        } catch (InvalidGrantException e) {
            logger.debug(e.getMessage());
        } catch (NotFoundException e) { 
            logger.debug(e.getMessage());
        }
        return INVALID_TOKEN;
    }

    /**
     * Obtain the token ID from the request.
     * @param request The OAuth2 request. Must not be {@code null}.
     * @return The String access token ID.
     */
    protected abstract String obtainTokenId(OAuth2Request request);

    /**
     * Represents the state of the token on the request.
     */
    public static class TokenState {

        private final String tokenId;

        protected TokenState(String tokenId) {
            this.tokenId = tokenId;
        }

        public boolean isValid() {
            return tokenId != null;
        }

        public String getTokenId() {
            return tokenId;
        }
    }

}
