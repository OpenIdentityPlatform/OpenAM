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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the OAuthTokenStore interface that uses the CoreTokenService for storing the tokens as JSON
 * objects.
 *
 * @since 11.0.0
 */
@Singleton
public class OAuthTokenStore {

    private final CTSPersistentStore cts;
    private final TokenAdapter<JsonValue> tokenAdapter;
    private final TokenIdFactory tokenIdFactory;

    /**
     * Constructs a new OAuthTokenStore instance.
     *
     * @param cts An instance of the CTSPersistentStore.
     * @param tokenAdapter An instance of a TokenAdapter.
     * @param tokenIdFactory An instance of the TokenIdFactory.
     */
    @Inject
    public OAuthTokenStore(CTSPersistentStore cts, TokenAdapter<JsonValue> tokenAdapter,
            TokenIdFactory tokenIdFactory) {
        this.cts = cts;
        this.tokenAdapter = tokenAdapter;
        this.tokenIdFactory = tokenIdFactory;
    }

    /**
     * Creates a token entry in the CTS.
     *
     * @param token The token.
     * @throws CoreTokenException If there is a problem creating the token.
     */
    public void create(JsonValue token) throws CoreTokenException {
        cts.create(tokenAdapter.toToken(token));
    }

    /**
     * Reads a token, with the specified id, from the CTS.
     *
     * @param id The token's id.
     * @return A JsonValue of the token. May be {@code null} if the token is not found.
     * @throws CoreTokenException If there is a problem reading the token.
     */
    public JsonValue read(String id) throws CoreTokenException {
        Token token = cts.read(tokenIdFactory.getOAuthTokenId(id));
        //The CTS will not throw exception, but return null when read does not return a value
        if (token == null) {
            return null;
        }
        return tokenAdapter.fromToken(token);
    }

    /**
     * Updates a token with the specified new token data.
     *
     * @param token The new token data.
     * @throws CoreTokenException If there is a problem updating the token.
     */
    public void update(JsonValue token) throws CoreTokenException {
        cts.update(tokenAdapter.toToken(token));
    }

    /**
     * Deletes a token with the specified id.
     *
     * @param id The token's id.
     * @throws CoreTokenException If there is a problem deleting the token.
     */
    public void delete(String id) throws CoreTokenException {
        cts.delete(id);
    }

    /**
     * Queries for OAuth2 tokens based on the specified query parameters.
     *
     * @param queryParameters The query parameters.
     * @return A JsonValue of the query results.
     * @throws CoreTokenException If there is a problem performing the query.
     */
    public JsonValue query(Map<String, Object> queryParameters, TokenFilter.Type type) throws CoreTokenException {
        Collection<Token> tokens = cts.query(convertRequest(queryParameters, type));
        return convertResults(tokens);
    }

    /**
     * Converts the Map of filter parameters into an LDAP filter.
     *
     * @param filters A Map of filter parameters.
     * @param type The type of filter required (and/or).
     * @return A Mapping of CoreTokenField to Objects to query by.
     */
    private TokenFilter convertRequest(Map<String, Object> filters, TokenFilter.Type type) {
        TokenFilterBuilder.FilterAttributeBuilder builder = new TokenFilterBuilder().type(type);

        for (OAuthTokenField field : OAuthTokenField.values()) {
            if (filters.containsKey(field.getOAuthField())) {
                builder.withAttribute(field.getField(), filters.get(field.getOAuthField()));
            }
        }

        return builder.build();
    }

    /**
     * Internal conversion function to handle the CTSPersistentStore query result.
     *
     * @param tokens A non null, but possibly empty collection of tokens.
     * @return The JsonValue expected by the caller.
     */
    private JsonValue convertResults(Collection<Token> tokens) {
        Set<Map<String, Object>> results = new HashSet<Map<String, Object>>();

        for (Token token : tokens) {
            results.add(convertToken(token));
        }

        return new JsonValue(results);
    }

    /**
     * Internal conversion function.
     *
     * @param token The token to convert.
     * @return A Token in String to Set of Strings representation.
     */
    private Map<String, Object> convertToken(Token token) {
        if (token == null){
            return null;
        }
        return tokenAdapter.fromToken(token).asMap();
    }
}
