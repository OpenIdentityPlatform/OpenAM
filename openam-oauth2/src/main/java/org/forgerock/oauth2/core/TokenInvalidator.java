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

import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.ID;
import static org.forgerock.util.query.QueryFilter.and;
import static org.forgerock.util.query.QueryFilter.equalTo;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2RealmResolver;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invalidates tokens and all their associated tokens. i.e. an Access Token and the Refresh Tokens and Authorization
 * code used to issue or refresh it.
 *
 * @since 12.0.0
 */
public class TokenInvalidator {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    public static final CoreTokenField GRANT_ID_FIELD = CoreTokenField.STRING_FIFTEEN;
    public static final CoreTokenField CLIENT_ID_FIELD = CoreTokenField.STRING_NINE;
    public static final CoreTokenField USERNAME_FIELD = CoreTokenField.STRING_THREE;

    private final TokenStore tokenStore;
    private final OAuth2RealmResolver realmResolver;

    /**
     * Constructs a new TokenInvalidator.
     *
     * @param tokenStore An instance of the TokenStore.
     * @param realmResolver An instance of the OAuth2RealmResolver
     */
    @Inject
    public TokenInvalidator(TokenStore tokenStore, OAuth2RealmResolver realmResolver) {
        this.tokenStore = tokenStore;
        this.realmResolver = realmResolver;
    }


    /**
     * Invalidates all tokens associated with same auth grant, client and resource owner.
     *
     * @param request The request.
     * @param clientId The client id.
     * @param userName The username denoting the resource owner id
     * @param authGrantId The auth grant id.
     * @throws ServerException
     * @throws NotFoundException
     */
    public void invalidateTokens(OAuth2Request request, String clientId, String userName,
            String authGrantId) throws  ServerException, NotFoundException {

        String realm = realmResolver.resolveFrom(request);
        QueryFilter<CoreTokenField> allTokensQuery = and(equalTo(USERNAME_FIELD, userName),
                equalTo(CLIENT_ID_FIELD, clientId), equalTo(GRANT_ID_FIELD, authGrantId));
        JsonValue tokens = tokenStore.queryForToken(realm, allTokensQuery);
        for (JsonValue token : tokens) {
            tokenStore.delete(realm, getAttributeValue(token, ID));
        }
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
}
