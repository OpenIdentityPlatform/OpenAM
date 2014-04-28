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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Set;

/**
 * Invalidates tokens and all their associated tokens. i.e. an Access Token and the Refresh Tokens and Authorization
 * code used to issue or refresh it.
 *
 * @since 12.0.0
 */
public class TokenInvalidator {

    private final TokenStore tokenStore;

    /**
     * Constructs a new TokenInvalidator.
     *
     * @param tokenStore An instance of the TokenStore.
     */
    @Inject
    public TokenInvalidator(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * Invalidates the specified token and all associated tokens.
     *
     * @param tokenId The token identifier of the token to invalidate.
     */
    @SuppressWarnings("unchecked")
    public void invalidateTokens(String tokenId) throws InvalidRequestException, ServerException {

        JsonValue token = tokenStore.queryForToken(tokenId);

        Set<HashMap<String, Set<String>>> list = (Set<HashMap<String, Set<String>>>) token.getObject();

        if (list != null && !list.isEmpty()) {
            for (HashMap<String, Set<String>> entry : list) {
                Set<String> idSet = entry.get(OAuth2Constants.CoreTokenParams.ID);
                Set<String> tokenNameSet = entry.get(OAuth2Constants.CoreTokenParams.TOKEN_NAME);
                Set<String> refreshTokenSet = entry.get(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN);
                String refreshTokenID = null;
                if (idSet != null && !idSet.isEmpty() && tokenNameSet != null && !tokenNameSet.isEmpty()) {
                    String entryID = idSet.iterator().next();
                    String type = tokenNameSet.iterator().next();

                    //if access_token delete the refresh token if it exists
                    if (tokenNameSet.iterator().next().equalsIgnoreCase(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)
                            && refreshTokenSet != null && !refreshTokenSet.isEmpty()) {
                        refreshTokenID = refreshTokenSet.iterator().next();
                        deleteToken(OAuth2Constants.Token.OAUTH_REFRESH_TOKEN, refreshTokenID);
                    }
                    //delete the access_token
                    invalidateTokens(entryID);
                    deleteToken(type, entryID);
                }
            }
        }
    }

    /**
     * Deletes the token with the specified type and identifier.
     *
     * @param type The type of token.
     * @param id The token's identifier.
     */
    private void deleteToken(String type, String id) throws ServerException, InvalidRequestException {
        if (type.equalsIgnoreCase(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)) {
            tokenStore.deleteAccessToken(id);
        } else if (type.equalsIgnoreCase(OAuth2Constants.Token.OAUTH_REFRESH_TOKEN)) {
            tokenStore.deleteRefreshToken(id);
        } else if (type.equalsIgnoreCase(OAuth2Constants.Params.CODE)) {
            tokenStore.deleteAuthorizationCode(id);
        } else {
            //shouldn't ever happen
        }
    }
}
