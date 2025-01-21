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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.cts.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.CreateFailedException;
import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.exceptions.ReadFailedException;
import org.forgerock.openam.cts.exceptions.SetFailedException;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.utils.blob.TokenBlobStrategy;
import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.forgerock.openam.cts.worker.CTSWorkerManager;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.Options;
import org.forgerock.util.Reject;

/**
 * CoreTokenAdapter is the final layer before persistence. In this case it uses the
 * LDAPAdapter for persistence which will in turn call the OpenDJ LDAP SDK.
 *
 * Exposes functionality via a similar CRUDQ style interface.
 *
 * Note: This class uses Token as its main means of adding data to and from LDAP and therefore is intended
 * for use by the Core Token Service.
 */
public class CoreTokenAdapter {

    private final TokenBlobStrategy strategy;
    private final TaskDispatcher dispatcher;
    private final ResultHandlerFactory handlerFactory;
    private final Debug debug;

    /**
     * Create a new instance of the CoreTokenAdapter with dependencies.
     * @param strategy Required for binary object transformations.
     * @param dispatcher Non null TaskDispatcher to use for CTS operations.
     * @param handlerFactory Factory used to generate ResultHandlers for CTS operations.
     * @param ctsWorkerManager Required for starting the CTS worker tasks.
     * @param debug Required for debug logging.
     */
    @Inject
    public CoreTokenAdapter(TokenBlobStrategy strategy, TaskDispatcher dispatcher, ResultHandlerFactory handlerFactory,
                            CTSWorkerManager ctsWorkerManager, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.strategy = strategy;
        this.handlerFactory = handlerFactory;
        this.dispatcher = dispatcher;
        this.debug = debug;

        dispatcher.startDispatcher();
        ctsWorkerManager.startTasks();
    }

    /**
     * Create a token in the persistent store.
     *
     * @param token Token to create.
     * @param options The Options for the operation.
     * @return The ResultHandler for the asynchronous operation.
     * @throws CoreTokenException If the Token exists already or there was
     * an error as a result of this operation.
     */
    public ResultHandler<Token, CoreTokenException> create(Token token, Options options) throws CoreTokenException {
        applyBlobStrategy(token);
        debug("Create: queued {0} Token {1}\n{2}", token.getType(), token.getTokenId(), token);
        final ResultHandler<Token, CoreTokenException> createHandler = handlerFactory.getCreateHandler();
        dispatcher.create(token, options, createHandler);
        return createHandler;
    }

    /**
     * Read the Token based on its Token ID.
     *
     * @param tokenId The non null Token ID to read from the Token store.
     * @param options The non null Options for the operation.
     * @return Null if the Token could not be found, otherwise a non null Token.
     * @throws CoreTokenException If there was an unexpected problem with the request.
     */
    public Token read(String tokenId, Options options) throws CoreTokenException {
        debug("Read: queued {0}", tokenId);
        ResultHandler<Token, CoreTokenException> handler = handlerFactory.getReadHandler();
        dispatcher.read(tokenId, options, handler);

        try {
            Token token = handler.getResults();
            if (token == null) {
                debug("Read: no Token found for {0}", tokenId);
            } else {
                reverseBlobStrategy(token);
                debug("Read: returned for {0}\n{1}", tokenId, token);
            }
            return token;
        } catch (CoreTokenException e) {
            throw new ReadFailedException(tokenId, e);
        }
    }

    /**
     * Update a Token in the LDAP store.
     *
     * This function will perform a read of the Token ID to determine if the Token has been
     * persisted already. If it has not been persisted, then delegates to the create function.
     *
     * Otherwise performs Modify operation based on the difference between the Token in
     * the store and the Token being stored.
     *
     * If this difference has no changes, then there is nothing to be done.
     *
     * @param token Token to update or create.
     * @param options The Options for the operation.
     * @return The ResultHandler for the asynchronous operation.
     * @throws CreateFailedException If an error occurs attempting to create the token.
     * @throws SetFailedException If an error occurs updating an existing token.
     */
    public ResultHandler<Token, CoreTokenException> updateOrCreate(Token token, Options options)
            throws CoreTokenException {
        applyBlobStrategy(token);
        debug("UpdateOrCreate: queued {0} Token {1}\n{2}", token.getType(), token.getTokenId(), token);
        final ResultHandler<Token, CoreTokenException> updateHandler = handlerFactory.getUpdateHandler();
        dispatcher.update(token, options, updateHandler);
        return updateHandler;
    }

    /**
     * Deletes a token from the store based on its token id.
     * @param tokenId Non null token id.
     * @param options Non null Options for the operation.
     * @return The ResultHandler for the asynchronous operation.
     * @throws CoreTokenException If there was an error while trying to remove the token with the given Id.
     */
    public ResultHandler<PartialToken, CoreTokenException> delete(String tokenId, Options options) throws CoreTokenException {
        debug("Delete: queued delete {0}", tokenId);
        final ResultHandler<PartialToken, CoreTokenException> deleteHandler = handlerFactory.getDeleteHandler();
        dispatcher.delete(tokenId, options, deleteHandler);
        return deleteHandler;
    }

    /**
     * Queries the persistence layer using the given TokenFilter to constrain the values.
     *
     * @param tokenFilter A non null TokenFilter.
     * @return A non null, possibly empty collection of Tokens that match the Filter.
     *
     * @throws CoreTokenException If there was a problem processing the error or if there
     * was a problem waiting for the response from the processor.
     */
    public Collection<Token> query(final TokenFilter tokenFilter) throws CoreTokenException {
        debug("Query: queued with Filter: {0}", tokenFilter);
        ResultHandler<Collection<Token>, CoreTokenException> handler = handlerFactory.getQueryHandler();
        dispatcher.query(tokenFilter, handler);
        try {
            Collection<Token> tokens = handler.getResults();
            for (Token token : tokens) {
                reverseBlobStrategy(token);
            }
            debug("Query: returned {0} Tokens with Filter: {1}", tokens.size(), tokenFilter);
            return tokens;
        } catch (CoreTokenException e) {
            throw new QueryFailedException(tokenFilter, e);
        }
    }

    /**
     * Queries the persistence layer using the given TokenFilter which must have the required
     * 'return attributes' defined within it. The results of this query will consist of PartialTokens
     * that match the requested CoreTokenFields.
     *
     * @see TokenFilter#addReturnAttribute(org.forgerock.openam.tokens.CoreTokenField)
     * @see org.forgerock.openam.tokens.CoreTokenField
     *
     * @param filter Non null TokenFilter with return attributes defined.
     * @return Non null, but possibly empty results.
     * @throws CoreTokenException If there was an error performing the query.
     * @throws IllegalArgumentException If the filter did not define any Return Fields.
     */
    public Collection<PartialToken> attributeQuery(final TokenFilter filter)
            throws CoreTokenException, IllegalArgumentException {

        ResultHandler<Collection<PartialToken>, CoreTokenException> handler = handlerFactory.getPartialQueryHandler();
        try {
            attributeQueryWithHandler(filter, handler);
            Collection<PartialToken> partialTokens = handler.getResults();
            Collection<PartialToken> results = new ArrayList<>();
            if (filter.getReturnFields().contains(CoreTokenField.BLOB)) {
                for (PartialToken p : partialTokens) {
                    try {
                        byte[] value = p.getValue(CoreTokenField.BLOB);
                        results.add(new PartialToken(p, CoreTokenField.BLOB, strategy.reverse(value)));
                    } catch (TokenStrategyFailedException e) {
                        throw new CoreTokenException("Failed to reverse Blob strategy", e);
                    }
                }
            } else {
                results = partialTokens;
            }
            debug("AttributeQuery: returned {0} Partial Tokens: {1}", results.size(), filter);
            return results;
        } catch (CoreTokenException e) {
            throw new QueryFailedException(filter, e);
        }
    }

    /**
     * Queries the persistence layer using the given TokenFilter which must have the required
     * 'return attributes' defined within it. The results of this query then will be deleted from the store.
     *
     * @param filter Non null TokenFilter with return attributes defined.
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     * @throws IllegalArgumentException If the filter did not define any Return Fields.
     */
    public void deleteOnQuery(final TokenFilter filter) throws CoreTokenException, IllegalArgumentException {
        //Token ID should be always retrieved in order to be able to delete the returned tokens.
        filter.addReturnAttribute(CoreTokenField.TOKEN_ID);
        ResultHandler<Collection<PartialToken>, CoreTokenException> handler = handlerFactory.getDeleteOnQueryHandler();
        try {
            attributeQueryWithHandler(filter, handler);
        } catch (CoreTokenException e) {
            throw new QueryFailedException(filter, e);
        }
    }

    /**
     * Sets up a continuous query on the persistence layer. The results of this query will be returned up
     * to the listener defined. If a continuous query already exists for the provided filter, the listener
     * will be added to the set of listeners for that filter.
     *
     * @param listener Receiving messages about alterations dependent on the settings of the supplied Filter.
     * @param filter Determining which messages the datalayer will inform us about.
     * @throws CoreTokenException In case of error starting the continuous query.
     */
    public void continuousQuery(ContinuousQueryListener listener, TokenFilter filter)
            throws CoreTokenException {
        dispatcher.continuousQuery(listener, filter);
    }

    /**
     * Removes a listener from a continuous query on the persistence layer.
     *
     * @param listener The listener which no longer requires updates supplied by the underlying query.
     * @param filter The filter which the underlying query is operating on.
     * @throws CoreTokenException In case of error.
     */
    public void removeContinuousQueryListener(ContinuousQueryListener listener, TokenFilter filter)
            throws CoreTokenException {
        dispatcher.removeContinuousQueryListener(listener, filter);
    }

    /**
     * Removes a listener from a continuous query on the persistence layer.
     *
     * @param filter The filter which the underlying query is operating on.
     */
    public void stopContinuousQuery(TokenFilter filter) {
        dispatcher.stopContinuousQuery(filter);
    }

    private void attributeQueryWithHandler(final TokenFilter filter,
            final ResultHandler<Collection<PartialToken>, CoreTokenException> handler) throws CoreTokenException {
        Reject.ifTrue(filter.getReturnFields().isEmpty(), "Must define return fields for attribute query.");

        debug("Attribute Query: queued with Filter: {0}", filter);
        dispatcher.partialQuery(filter, handler);
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(CoreTokenConstants.DEBUG_HEADER + format, args));
        }
    }

    private void applyBlobStrategy(Token token) throws CoreTokenException {
        try {
            token.setBlob(strategy.perform(token.getBlob()));
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to perform Token Blob strategy.", e);
        }
    }

    private void reverseBlobStrategy(Token token) throws CoreTokenException {
        try {
            token.setBlob(strategy.reverse(token.getBlob()));
        } catch (TokenStrategyFailedException e) {
            throw new CoreTokenException("Failed to reverse Token Blob strategy.", e);
        }
    }
}
