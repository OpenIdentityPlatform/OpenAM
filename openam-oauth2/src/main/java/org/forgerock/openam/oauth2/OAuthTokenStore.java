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
 * Copyright 2012-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.query.QueryFilter;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
    private final OAuth2AuditLogger auditLogger;
    private final Debug logger;

    /**
     * Constructs a new OAuthTokenStore instance.
     *
     * @param cts An instance of the CTSPersistentStore.
     * @param tokenAdapter An instance of a TokenAdapter.
     * @param tokenIdFactory An instance of the TokenIdFactory.
     */
    @Inject
    public OAuthTokenStore(CTSPersistentStore cts, TokenIdFactory tokenIdFactory,
            @Named(OAuth2Constants.CoreTokenParams.OAUTH_TOKEN_ADAPTER) TokenAdapter<JsonValue> tokenAdapter,
                           OAuth2AuditLogger auditLogger, @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug logger) {
        this.cts = cts;
        this.tokenAdapter = tokenAdapter;
        this.tokenIdFactory = tokenIdFactory;
        this.auditLogger = auditLogger;
        this.logger = logger;
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
        Token token = cts.read(tokenIdFactory.generateTokenId(id));
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
        try {
            cts.delete(id);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"DELETED_TOKEN", id};
                auditLogger.logAccessMessage("DELETED_TOKEN", obs, null);
            }
        } catch (CoreTokenException e) {
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_DELETE_TOKEN", id};
                auditLogger.logErrorMessage("FAILED_DELETE_TOKEN", obs, null);
            }
            logger.error("Could not delete token " + e.getMessage());
            throw e;
        }
    }

    /**
     * Queries for OAuth2 tokens based on the specified query parameters.
     *
     * @param query The query parameters.
     * @return A JsonValue of the query results.
     * @throws CoreTokenException If there is a problem performing the query.
     */
    public JsonValue query(QueryFilter<CoreTokenField> query) throws CoreTokenException {
        Collection<Token> tokens = cts.query(new TokenFilterBuilder().withQuery(query).build());
        return convertResults(tokens);
    }

    /**
     * Internal conversion function to handle the CTSPersistentStore query result.
     *
     * @param tokens A non null, but possibly empty collection of tokens.
     * @return The JsonValue expected by the caller.
     */
    private JsonValue convertResults(Collection<Token> tokens) {
        List<Map<String, Object>> results = new ArrayList<>();

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
