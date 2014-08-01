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
package org.forgerock.openam.cts;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.CoreTokenAdapter;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.utils.blob.TokenBlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Implementation of the CTS (Core Token Store) persistence layer.
 *
 * This implementation uses the an asynchronous approach to decouple callers from the processing
 * of the task. This is detailed in the {@link CoreTokenAdapter} in more detail.
 *
 * Responsible for managing the detail about the binary object part of a Token.
 *
 * @see Token
 * @see CoreTokenAdapter
 */
@Singleton
public class CTSPersistentStoreImpl implements CTSPersistentStore {

    private final Debug debug;

    // Injected
    private final TokenBlobStrategy strategy;
    private final CoreTokenAdapter adapter;

    /**
     * Creates a default implementation of the CTSPersistentStoreImpl.
     * @param adapter Required for CTS operations.
     * @param strategy Required for binary object transformations.
     * @param debug Required for debugging.
     */
    @Inject
    public CTSPersistentStoreImpl(TokenBlobStrategy strategy,
                                  CoreTokenAdapter adapter,
                                  @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {

        this.strategy = strategy;
        this.adapter = adapter;
        this.debug = debug;
    }

    /**
     * Create a Token in the persistent store. This operation assumes that the
     * Token does not exist in the store. It is generally advisable to use update
     * instead.
     *
     * @see CTSPersistentStore#update(Token)
     *
     * @param token Non null Token to create.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public void create(Token token) throws CoreTokenException {
        try {
            token.setBlob(strategy.perform(token.getBlob()));
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to perform Token Blob strategy.", e);
        }

        adapter.create(token);
    }

    /**
     * Read a Token from the persistent store. The Token will be located based on its Token ID.
     *
     * Note: This operation will block until the read has returned.
     *
     * @param tokenId The non null Token Id that the Token was created with.
     * @return Null if there was no matching Token. Otherwise a fully populated Token will be returned.
     *
     * * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public Token read(String tokenId) throws CoreTokenException {
        Token token = adapter.read(tokenId);
        if (token == null) {
            debug("Read: {0} did not exist", tokenId);
            return null;
        }

        try {
            token.setBlob(strategy.reverse(token.getBlob()));
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to reverse Token Blob strategy.", e);
        }

        debug("Read: {0} read", tokenId);
        return token;
    }

    /**
     * Update an existing Token in the store. If the Token does not exist in the store then a
     * Token is created. If the Token did exist in the store then it is updated.
     *
     * Not all fields on the Token can be updated, see the Token class for more details.
     *
     * @see Token#isFieldReadOnly(org.forgerock.openam.cts.api.fields.CoreTokenField)
     *
     * @param token Non null Token to update.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public void update(Token token) throws CoreTokenException {
        try {
            token.setBlob(strategy.perform(token.getBlob()));
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to perform Token Blob strategy.", e);
        }

        adapter.updateOrCreate(token);
        debug("Update: {0} updated", token.getTokenId());
    }

    /**
     * Delete the Token from the store.
     *
     * @param token Non null Token to be deleted from the store.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public void delete(Token token) throws CoreTokenException {
        delete(token.getTokenId());
    }

    /**
     * Delete the Token from the store based on its Token ID.
     *
     * @param tokenId The non null Token Id of the token to remove.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public void delete(String tokenId) throws CoreTokenException {
        adapter.delete(tokenId);
        debug("Delete: {0} deleted", tokenId);
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
     * @return total number of tokens deleted by query, excluding any deletion requests that could not be queued.
     *
     * @throws DeleteFailedException If there was a problem queuing the tasks to be performed.
     */
    @Override
    public int delete(Map<CoreTokenField, Object> query) throws DeleteFailedException {
        TokenFilterBuilder.FilterAttributeBuilder builder = new TokenFilterBuilder()
                .returnAttribute(CoreTokenField.TOKEN_ID)
                .and();
        for (Map.Entry<CoreTokenField, Object> entry : query.entrySet()) {
            CoreTokenField key = entry.getKey();
            Object value = entry.getValue();
            builder = builder.withAttribute(key, value);
        }

        TokenFilter filter = builder.build();
        Collection<String> failures = new ArrayList<String>();

        try {
            Collection<PartialToken> partialTokens = attributeQuery(filter);
            debug("Delete: queried {0} partial Tokens", Integer.toString(partialTokens.size()));

            for (PartialToken token : partialTokens) {
                String tokenId = token.getValue(CoreTokenField.TOKEN_ID);
                try {
                    delete(tokenId);
                } catch (CoreTokenException e) {
                    failures.add(tokenId);
                }
            }

            if (!failures.isEmpty()) {
                error("Failed to delete {0} tokens.\n{1}",
                        Integer.toString(failures.size()),
                        StringUtils.join(failures, ","));
            }
            return partialTokens.size() - failures.size();
        } catch (CoreTokenException e) {
            throw new DeleteFailedException("Failed to delete Tokens", e);
        }
    }

    /**
     * Queries the persistent store for all Tokens that match the given TokenFilter.
     *
     * The Tokens returned will be contained within a collection. Care should be taken to ensure
     * that the number of Tokens requested will not exhaust the given heap.
     *
     * The returned collection contains fully initialised Token instances from the persistence
     * store. If only partial details are required, see {@link #attributeQuery(TokenFilter)}
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     *
     * @param tokenFilter No null TokenFilter to use for the query.
     * @return Non null but possibly empty collection of Tokens.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public Collection<Token> query(TokenFilter tokenFilter) throws CoreTokenException {
        Collection<Token> tokens = adapter.query(tokenFilter);
        decryptTokens(tokens);
        return tokens;
    }

    /**
     * Performs a partial Token query against the store. That is, a query which is aimed at
     * specific attributes of a Token, rather than whole Tokens. The return result will consist
     * of PartialTokens for this purpose.
     *
     * This function is useful for example, finding all Token IDs that match a certain criteria.
     *
     * @see org.forgerock.openam.cts.impl.query.PartialToken
     *
     * @param tokenFilter Non null TokenFilter, with the return attributes defined.
     * @return A non null, but possibly empty collection.
     *
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    @Override
    public Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException {
        Collection<PartialToken> partialTokens = adapter.attributeQuery(tokenFilter);
        Collection<PartialToken> results = new ArrayList<PartialToken>();
        for (PartialToken p : partialTokens) {
            if (p.getFields().contains(CoreTokenField.BLOB)) {
                try {
                    byte[] value = p.getValue(CoreTokenField.BLOB);
                    results.add(new PartialToken(p, CoreTokenField.BLOB, strategy.reverse(value)));
                } catch (TokenStrategyFailedException e) {
                    throw new CoreTokenException("Failed to reverse Blob strategy", e);
                }
            } else {
                results.add(p);
            }
        }
        return results;
    }

    @Override
    public void deleteOnQuery(TokenFilter tokenFilter) throws CoreTokenException {
        adapter.deleteOnQuery(tokenFilter);
        debug("DeleteOnQuery: with query {0}", tokenFilter.toString());
    }

    /**
     * Handles the decrypting of tokens when needed.
     *
     * @param tokens A non null collection of Tokens which will be modified by this call.
     */
    private void decryptTokens(Collection<Token> tokens) throws CoreTokenException {
        for (Token token : tokens) {
            try {
                token.setBlob(strategy.reverse(token.getBlob()));
            } catch (TokenStrategyFailedException e) {
                throw new CoreTokenException("Failed to reverse Token Blob strategy.", e);
            }
        }
    }

    private void debug(String format, String... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_HEADER + format, args));
        }
    }

    private void error(String format, String... args) {
        if (debug.errorEnabled()) {
            debug.error(MessageFormat.format(CoreTokenConstants.DEBUG_HEADER + format, args));
        }
    }
}
