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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl;

import com.google.inject.name.Named;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.*;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.reaper.CTSReaperInit;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Collection;

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
    // Injected
    private final TaskDispatcher dispatcher;
    private final ResultHandlerFactory handlerFactory;
    private final Debug debug;

    /**
     * Create a new instance of the CoreTokenAdapter with dependencies.
     * @param dispatcher Non null TaskDispatcher to use for CTS operations.
     * @param handlerFactory Factory used to generate ResultHandlers for CTS operations.
     * @param reaperInit Required for starting the CTS Reaper.
     * @param debug Required for debug logging
     */
    @Inject
    public CoreTokenAdapter(TaskDispatcher dispatcher, ResultHandlerFactory handlerFactory,
                            CTSReaperInit reaperInit, @Named(CoreTokenConstants.CTS_DEBUG) Debug debug) {
        this.handlerFactory = handlerFactory;
        this.dispatcher = dispatcher;
        this.debug = debug;

        dispatcher.startDispatcher();
        reaperInit.startReaper();
    }

    /**
     * Create a token in the persistent store.
     *
     * @param token Token to create.
     * @throws CoreTokenException If the Token exists already or there was
     * an error as a result of this operation.
     */
    public void create(Token token) throws CoreTokenException {
        debug("Create: queued {0} Token {1}\n{2}", token.getType(), token.getTokenId(), token);
        dispatcher.create(token, handlerFactory.getCreateHandler());
    }

    /**
     * Read the Token based on its Token ID.
     *
     * @param tokenId The non null Token ID to read from the Token store.
     * @return Null if the Token could not be found, otherwise a non null Token.
     * @throws CoreTokenException If there was an unexpected problem with the request.
     */
    public Token read(String tokenId) throws CoreTokenException {
        debug("Read: queued {0}", tokenId);
        ResultHandler<Token> handler = handlerFactory.getReadHandler();
        dispatcher.read(tokenId, handler);

        try {
            Token token = handler.getResults();
            if (token == null) {
                debug("Read: no Token found for {0}", tokenId);
            } else {
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
     * @return True if the token was created or false if it already existed.
     * @throws CreateFailedException If an error occurs attempting to create the token.
     * @throws SetFailedException If an error occurs updating an existing token.
     */
    public boolean updateOrCreate(Token token) throws CoreTokenException {
        debug("UpdateOrCreate: queued {0} Token {1}\n{2}", token.getType(), token.getTokenId(), token);
        dispatcher.update(token, handlerFactory.getUpdateHandler());
        return false;
    }

    /**
     * Deletes a token from the store based on its token id.
     * @param tokenId Non null token id.
     * @throws CoreTokenException If there was an error while trying to remove the token with the given Id.
     */
    public void delete(String tokenId) throws CoreTokenException {
        debug("Delete: queued delete {0}", tokenId);
        dispatcher.delete(tokenId, handlerFactory.getDeleteHandler());
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
        ResultHandler<Collection<Token>> handler = handlerFactory.getQueryHandler();
        dispatcher.query(tokenFilter, handler);
        try {
            Collection<Token> tokens = handler.getResults();
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
     * @see TokenFilter#addReturnAttribute(org.forgerock.openam.cts.api.fields.CoreTokenField)
     * @see org.forgerock.openam.cts.api.fields.CoreTokenField
     *
     * @param filter Non null TokenFilter with return attributes defined.
     * @return Non null, but possibly empty results.
     * @throws CoreTokenException If there was an error performing the query.
     * @throws IllegalArgumentException If the filter did not define any Return Fields.
     */
    public Collection<PartialToken> attributeQuery(final TokenFilter filter)
            throws CoreTokenException, IllegalArgumentException {

        ResultHandler<Collection<PartialToken>> handler = handlerFactory.getPartialQueryHandler();
        try {
            attributeQueryWithHandler(filter, handler);
            Collection<PartialToken> partialTokens = handler.getResults();
            debug("AttributeQuery: returned {0} Partial Tokens: {1}", partialTokens.size(), filter);
            return partialTokens;
        } catch (CoreTokenException e) {
            throw new QueryFailedException(filter, e);
        }
    }

    /**
     * Queries the persistence layer using the given TokenFilter which must have the required
     * 'return attributes' defined within it. The results os this query then will be deleted from the store.
     *
     * @param filter Non null TokenFilter with return attributes defined.
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     * @throws IllegalArgumentException If the filter did not define any Return Fields.
     */
    public void deleteOnQuery(final TokenFilter filter) throws CoreTokenException, IllegalArgumentException {
        //Token ID should be always retrieved in order to be able to delete the returned tokens.
        filter.addReturnAttribute(CoreTokenField.TOKEN_ID);
        ResultHandler<Collection<PartialToken>> handler = handlerFactory.getDeleteOnQueryHandler();
        try {
            attributeQueryWithHandler(filter, handler);
        } catch (CoreTokenException e) {
            throw new QueryFailedException(filter, e);
        }
    }

    private void attributeQueryWithHandler(final TokenFilter filter,
            final ResultHandler<Collection<PartialToken>> handler) throws CoreTokenException {
        Reject.ifTrue(filter.getReturnFields().isEmpty(), "Must define return fields for attribute query.");

        debug("Attribute Query: queued with Filter: {0}", filter);
        dispatcher.partialQuery(filter, handler);
    }

    private void debug(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(
                    CoreTokenConstants.DEBUG_HEADER + format,
                    args));
        }
    }
}
