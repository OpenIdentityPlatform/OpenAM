/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock, Inc. All Rights Reserved
 *
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
 * information:
 *
 * "Portions copyright [year] [name of copyright owner]".
 *
 */
package org.forgerock.openam.cts;

import com.google.inject.name.Named;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.fields.CoreTokenFieldTypes;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.CoreTokenAdapter;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.openam.cts.reaper.CTSReaper;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.blob.TokenBlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Core Token Service Persistent Store is responsible for the storage and retrieval of
 * Tokens from the persistent store.
 *
 * The Core Token Service is exposed through a series of CRUDL operations which use TokenAdapters
 * to convert from objects to be stored in the Core Token Service, to the Token format required
 * for generic storage.
 *
 * The Core Token Service is responsible for the storage mechanism behind the Session fail-over
 * feature.
 *
 * Persistence is currently provided by LDAP.
 *
 * @see org.forgerock.openam.cts.adapters.TokenAdapter
 * @see Token
 *
 * @author steve
 * @author jeff.schenk@forgerock.com
 * @author jason.lemay@forgerock.com
 * @author robert.wapshott@forgerock.com
 */
public class CTSPersistentStore {

    private Debug debug;

    // Injected
    private final LDAPDataConversion dataConversion;
    private final TokenBlobStrategy strategy;
    private final CoreTokenAdapter adapter;

    /**
     * Private restricted to preserve Singleton Instantiation.
     */
    @Inject
    public CTSPersistentStore(LDAPDataConversion dataConversion, TokenBlobStrategy strategy,
                              CoreTokenAdapter adapter, final CTSReaper reaper,
                              @Named(CTSReaper.CTS_SCHEDULED_SERVICE) ScheduledExecutorService executorService,
                              CoreGuiceModule.ShutdownManagerWrapper wrapper,
                              @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {

        this.dataConversion = dataConversion;
        this.strategy = strategy;
        this.adapter = adapter;
        this.debug = debug;

        // Start the CTS Reaper and schedule shutdown for when system shutdown is signalled.
        reaper.startup(executorService);
        wrapper.addShutdownListener(new ShutdownListener() {
            public void shutdown() {
                reaper.shutdown();
            }
        });
    }

    /**
     * Create a Token in the persistent store. If the Token already exists in the store then this
     * function will throw a CoreTokenException. Instead it is recommended to use the update function.
     *
     * @see CTSPersistentStore#update(org.forgerock.openam.cts.api.tokens.Token)
     *
     * @param token Non null Token to create.
     * @throws CoreTokenException If there was a non-recoverable error during the operation or if
     * the Token already exists in the store.
     */
    public void create(Token token) throws CoreTokenException {
        try {
            strategy.perfom(token);
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to perform Token Blob strategy.", e);
        }
        adapter.create(token);
    }

    /**
     * Read a Token from the persistent store.
     *
     * @param tokenId The non null Token Id that the Token was created with.
     * @return Null if there was no matching Token. Otherwise a fully populated Token will be returned.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public Token read(String tokenId) throws CoreTokenException {
        Token token = adapter.read(tokenId);
        try {
            strategy.reverse(token);
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to reverse Token Blob strategy.", e);
        }

        return token;
    }

    /**
     * Update an existing Token in the store. If the Token does not exist in the store then a
     * Token is created. If the Token did exist in the store then it is updated.
     *
     * Not all fields on the Token can be updated, see the Token class for more details.
     *
     * @see Token
     *
     * @param token Non null Token to update.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void update(Token token) throws CoreTokenException {
        try {
            strategy.perfom(token);
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to perform Token Blob strategy.", e);
        }

        adapter.updateOrCreate(token);

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Update: {0} updated",
                    token.getTokenId()));
        }
    }

    /**
     * Delete the Token from the store.
     *
     * @param token Non null Token to be deleted from the store.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void delete(Token token) throws CoreTokenException {
        delete(token.getTokenId());
    }

    /**
     * Delete the Token from the store based on its id.
     *
     * Note: It is often more efficient to delete the token based on the Id if you already
     * have this information, rather than reading the Token first before removing it.
     *
     * @param tokenId The non null Token Id of the token to remove.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public void delete(String tokenId) throws DeleteFailedException {
        adapter.delete(tokenId);
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Delete: {0} deleted",
                    tokenId));
        }
    }

    /**
     * Delete a collection of Tokens from the Token Store using a filter to narrow down the
     * Tokens to be deleted.
     *
     * Note: This operation is linear in its execution time so the more Tokens being deleted, the
     * longer it will take.
     *
     * @param query Non null filters which will be combined logically using AND.
     *
     * @return total number of tokens deleted by query.
     *
     * @throws DeleteFailedException If the delete failed for any reason.
     */
    public int delete(Map<CoreTokenField, Object> query) throws DeleteFailedException {
        QueryFilter.QueryFilterBuilder queryFilter = adapter.buildFilter().and();
        for (Map.Entry<CoreTokenField, Object> entry : query.entrySet()) {
            CoreTokenField key = entry.getKey();
            Object value = entry.getValue();
            queryFilter = queryFilter.attribute(key, value);
        }

        QueryBuilder builder = adapter
                .query()
                .withFilter(queryFilter.build())
                .returnTheseAttributes(CoreTokenField.TOKEN_ID);

        Collection<Entry> entries;
        try {
            entries = builder.executeRawResults();
            for (Entry entry : entries) {
                Attribute attribute = entry.getAttribute(CoreTokenField.TOKEN_ID.toString());
                String tokenId = attribute.firstValueAsString();
                adapter.delete(tokenId);
            }
            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Delete: {0} deleted",
                        entries.size()));
            }
        } catch (CoreTokenException e) {
            throw new DeleteFailedException(builder, e);
        }
        return entries.size();
    }

    /**
     * Perform a query based on a collection of queryable parameters.
     *
     * The query will be an AND query where each matching Token must match on all query parameters
     * provided.
     *
     * @param query A mapping of CoreTokenField keys to values.
     * @return A non null, but possibly empty collection of Tokens.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    public Collection<Token> list(Map<CoreTokenField, Object> query) throws CoreTokenException {
        // Verify all types are safe to cast.
        CoreTokenFieldTypes.validateTypes(query);

        QueryBuilder builder = adapter.query();
        QueryFilter.QueryFilterBuilder filterBuilder = adapter.buildFilter().and();

        for (Map.Entry<CoreTokenField, Object> entry : query.entrySet()) {
            CoreTokenField key = entry.getKey();
            Object value = entry.getValue();
            filterBuilder = filterBuilder.attribute(key, value);
        }

        Collection<Token> tokens = builder.withFilter(filterBuilder.build()).execute();
        decryptTokens(tokens);

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "List: {0} Tokens listed",
                    tokens.size()));
        }

        return tokens;
    }

    /**
     * Performs a list operation against the Core Token Service with a predefined filter. This
     * allows more complex filters to be constructed and is intended to be used with the
     * QueryFilter fluent class.
     *
     * @see QueryFilter
     *
     * @param filter A non null OpenDJ LDAP Filter to use to control the results returned.
     * @return A non null, but possible empty collection of Tokens.
     * @throws CoreTokenException If there was an unrecoverable error.
     */
    public Collection<Token> list(Filter filter) throws CoreTokenException {
        Collection<Token> tokens = adapter.query().withFilter(filter).execute();
        decryptTokens(tokens);
        return tokens;
    }

    /**
     * Handles the decrypting of tokens when needed.
     *
     * @param tokens A non null collection of Tokens.
     * @return A new collection of Tokens with their byte contents decrypted if necessary.
     */
    private void decryptTokens(Collection<Token> tokens) throws CoreTokenException {
        for (Token token : tokens) {
            try {
                strategy.reverse(token);
            } catch (TokenStrategyFailedException e) {
                throw new CoreTokenException("Failed to reverse Token Blob strategy.", e);
            }
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    public Map<String, Long> getTokensByUUID(String uuid) throws CoreTokenException {
        Collection<Entry> entries;
        Filter filter = adapter.buildFilter().and().userId(uuid).build();
        entries = adapter.query()
                .withFilter(filter)
                .returnTheseAttributes(CoreTokenField.TOKEN_ID, CoreTokenField.EXPIRY_DATE)
                .executeRawResults();

        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER +
                    "Querying Sessions by User Id. Found {0} Sessions.\n" +
                    "UUID: {1}",
                    entries.size(),
                    uuid));
        }

        Map<String, Long> sessions = new HashMap<String, Long>();
        for (Entry entry : entries) {
            String sessionId = entry.getAttribute(CoreTokenField.TOKEN_ID.toString()).firstValueAsString();
            String dateString = entry.getAttribute(CoreTokenField.EXPIRY_DATE.toString()).firstValueAsString();

            Calendar timestamp = dataConversion.fromLDAPDate(dateString);
            long epochedSeconds = dataConversion.toEpochedSeconds(timestamp);

            sessions.put(sessionId, epochedSeconds);
        }

        return sessions;
    }
}
