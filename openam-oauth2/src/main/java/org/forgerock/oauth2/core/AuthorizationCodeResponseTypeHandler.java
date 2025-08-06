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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;

/**
 * Implementation of the ResponseTypeHandler for handling authorization code response types.
 *
 * @since 12.0.0
 */
@Singleton
public class AuthorizationCodeResponseTypeHandler implements ResponseTypeHandler {

    private final TokenStore tokenStore;

    /**
     * Constructs a new AuthorizationCodeResponseTypeHandler.
     *
     * @param tokenStore An instance of the TokenStore.
     */
    @Inject
    public AuthorizationCodeResponseTypeHandler(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * {@inheritDoc}
     */
    public Map.Entry<String, Token> handle(String tokenType, Set<String> scope,
                                           ResourceOwner resourceOwner, String clientId, String redirectUri,
                                           String nonce, OAuth2Request request, String codeChallenge,
                                           String codeChallengeMethod)
            throws ServerException, NotFoundException {

        final AuthorizationCode authorizationCode = tokenStore.createAuthorizationCode(scope, resourceOwner,
                clientId, redirectUri, nonce, request, codeChallenge, codeChallengeMethod);
        return new AbstractMap.SimpleEntry<String, Token>(OAuth2Constants.Params.CODE, authorizationCode);
    }

    /**
     * {@inheritDoc}
     */
    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.QUERY;
    }
}
