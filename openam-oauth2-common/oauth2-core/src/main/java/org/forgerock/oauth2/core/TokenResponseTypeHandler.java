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

import org.forgerock.oauth2.core.exceptions.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the ResponseTypeHandler for handling token response type.
 *
 * @since 12.0.0
 */
@Singleton
public class TokenResponseTypeHandler implements ResponseTypeHandler {

    private final TokenStore tokenStore;

    /**
     * Constructs a new TokenResponseTypeHandler.
     *
     * @param tokenStore An instance of the TokenStore.
     */
    @Inject
    public TokenResponseTypeHandler(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public Map.Entry<String, Token> handle(String tokenType, Set<String> scope,
                                           String resourceOwnerId, String clientId, String redirectUri, String nonce,
                                           OAuth2Request request) throws ServerException {

        final AccessToken generatedAccessToken = tokenStore.createAccessToken("token", tokenType, null, resourceOwnerId,
                clientId, redirectUri, scope, null, nonce, request);
        return new AbstractMap.SimpleEntry<String, Token>("access_token", generatedAccessToken);
    }

    /**
     * {@inheritDoc}
     */
    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.FRAGMENT;
    }
}
