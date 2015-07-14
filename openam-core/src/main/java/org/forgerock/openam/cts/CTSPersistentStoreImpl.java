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
 * Copyright 2012-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.CoreTokenAdapter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Implementation of the CTS (Core Token Store) persistence layer.
 *
 * This implementation offers synchronous and asynchronous approaches to decouple callers from the processing of token
 * related tasks.
 * This is detailed in the {@link CoreTokenAdapter} in more detail.
 *
 * @see Token
 * @see CoreTokenAdapter
 */
@Singleton
public class CTSPersistentStoreImpl implements CTSPersistentStore {

    // Injected
    private final CoreTokenAdapter adapter;
    private final Debug debug;

    /**
     * Creates a default implementation of the CTSPersistentStoreImpl.
     * @param adapter Required for CTS operations.
     * @param debug Required for debugging.
     */
    @Inject
    public CTSPersistentStoreImpl(CoreTokenAdapter adapter, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.adapter = adapter;
        this.debug = debug;
    }

    @Override
    public void create(Token token) throws CoreTokenException {
        final ResultHandler<Token, CoreTokenException> createHandler = adapter.create(token);
        //block until we get the results, and ignore non-exception results
        createHandler.getResults();
        debug("Token {0} created", token.getTokenId());
    }

    @Override
    public void createAsync(Token token) throws CoreTokenException {
        adapter.create(token);
        debug("Token {0} queued for creation", token.getTokenId());
    }

    @Override
    public Token read(String tokenId) throws CoreTokenException {
        Token token = adapter.read(tokenId);
        if (token == null) {
            debug("Token {0} did not exist", tokenId);
            return null;
        }

        debug("Token {0} read", tokenId);
        return token;
    }

    @Override
    public void update(Token token) throws CoreTokenException {
        final ResultHandler<Token, CoreTokenException> updateHandler = adapter.updateOrCreate(token);
        //block until we get the results, and ignore non-exception results
        updateHandler.getResults();
        debug("Token {0} updated", token.getTokenId());
    }

    @Override
    public void updateAsync(Token token) throws CoreTokenException {
        adapter.updateOrCreate(token);
        debug("Token {0} queued for update", token.getTokenId());
    }

    @Override
    public void delete(Token token) throws CoreTokenException {
        delete(token.getTokenId());
    }

    @Override
    public void deleteAsync(Token token) throws CoreTokenException {
        deleteAsync(token.getTokenId());
    }

    @Override
    public void delete(String tokenId) throws CoreTokenException {
        final ResultHandler<String, CoreTokenException> deleteHandler = adapter.delete(tokenId);
        //block until we get the results, and ignore non-exception results
        deleteHandler.getResults();
        debug("Token {0} deleted", tokenId);
    }

    @Override
    public void deleteAsync(String tokenId) throws CoreTokenException {
        adapter.delete(tokenId);
        debug("Token {0} queued for deletion", tokenId);
    }

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
            debug("Found {0} partial Tokens for deletion", Integer.toString(partialTokens.size()));

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

    @Override
    public Collection<Token> query(TokenFilter tokenFilter) throws CoreTokenException {
        debug("Query: {0}", tokenFilter.toString());
        return adapter.query(tokenFilter);
    }

    @Override
    public Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException {
        debug("AttributeQuery: {0}", tokenFilter.toString());
        return adapter.attributeQuery(tokenFilter);
    }

    @Override
    public void deleteOnQueryAsync(TokenFilter tokenFilter) throws CoreTokenException {
        debug("DeleteOnQuery: with query {0}", tokenFilter.toString());
        adapter.deleteOnQuery(tokenFilter);
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
