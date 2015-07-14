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

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

import java.util.Collection;
import java.util.Map;

/**
 * Core Token Service Persistent Store is responsible for the storage and retrieval of Tokens from a persistent store.
 *
 * The Core Token Service is exposed through a series of CRUDL operations which require the use of TokenAdapters to
 * convert from objects to be stored into the expected Core Token Service format. This allows the Tokens to be stored in
 * a generic storage.
 *
 * The current main use cases for the CTS are:
 *
 * - Session fail-over
 * - OAuth token storage
 * - SAML Federation token storage
 * - Forgotten Password
 *
 * The implementation of this interface must be thread safe as this service will often be on a direct call path from the
 * front end of the Server hosting OpenAM, and so multiple calls to the service can occur at the same time from
 * different calling threads.
 *
 * @see org.forgerock.openam.cts.adapters.TokenAdapter
 * @see Token
 */
public interface CTSPersistentStore {

    /**
     * Creates a Token in the persistent store synchronously.
     *
     * @param token Non null Token to create.
     * @throws CoreTokenException If there was an error while performing the different
     * {@link org.forgerock.openam.cts.utils.blob.TokenBlobStrategy}s on the provided Token, or if the operation itself
     * has failed.
     * scheduled for execution.
     */
    void create(Token token) throws CoreTokenException;

    /**
     * Creates a Token in the persistent store asynchronously. The result of the operation (success/failure) is ignored.
     *
     * @param token Non null Token to create.
     * @throws CoreTokenException If there was an error while performing the different
     * {@link org.forgerock.openam.cts.utils.blob.TokenBlobStrategy}s on the provided Token.
     * scheduled for execution.
     */
    void createAsync(Token token) throws CoreTokenException;

    /**
     * Read a Token from the persistent store synchronously.
     *
     * @param tokenId The non null Token Id that the Token was created with.
     * @return Null if there was no matching Token. Otherwise a fully populated Token will be returned.
     * @throws CoreTokenException If there was an error while performing the different
     * {@link org.forgerock.openam.cts.utils.blob.TokenBlobStrategy}s on the returned Token, or if the operation itself
     * has failed.
     * scheduled for execution.
     */
    Token read(String tokenId) throws CoreTokenException;

    /**
     * Updates an existing Token in the store synchronously. If the Token does not exist in the store then a Token is
     * created. If the Token did exist in the store then it is updated.
     *
     * Not all fields of the Token can be updated, see the Token class for more details.
     *
     * @see Token#isFieldReadOnly(org.forgerock.openam.tokens.CoreTokenField)
     *
     * @param token Non null Token to update.
     * @throws CoreTokenException If there was an error while performing the different
     * {@link org.forgerock.openam.cts.utils.blob.TokenBlobStrategy}s on the provided Token, or if the operation itself
     * has failed.
     * scheduled for later execution.
     */
    void update(Token token) throws CoreTokenException;

    /**
     * Updates an existing Token in the store asynchronously. If the Token does not exist in the store then a Token is
     * created. If the Token did exist in the store then it is updated.
     *
     * Not all fields of the Token can be updated, see the Token class for more details.
     *
     * @see Token#isFieldReadOnly(org.forgerock.openam.tokens.CoreTokenField)
     *
     * @param token Non null Token to update.
     * @throws CoreTokenException If there was an error while performing the different
     * {@link org.forgerock.openam.cts.utils.blob.TokenBlobStrategy}s on the provided Token.
     * scheduled for execution.
     */
    void updateAsync(Token token) throws CoreTokenException;

    /**
     * Delete the Token from the store synchronously.
     *
     * @param token Non null Token to be deleted from the store.
     * @throws CoreTokenException If the delete operation has failed.
     * scheduled for later execution.
     */
    void delete(Token token) throws CoreTokenException;

    /**
     * Deletes the Token from the store asynchronously.
     *
     * @param token Non null Token to be deleted from the store.
     * scheduled for later execution.
     */
    void deleteAsync(Token token) throws CoreTokenException;

    /**
     * Delete the Token from the store synchronously based on its id.
     *
     * Note: It is often more efficient to delete the token based on the Id if you already have this information,
     * rather than reading the Token first before removing it.
     *
     * @param tokenId The non null Token Id of the token to remove.
     * @throws CoreTokenException If the delete operation has failed.
     * scheduled for later execution.
     */
    void delete(String tokenId) throws CoreTokenException;

    /**
     * Delete the Token from the store asynchronously based on its id.
     *
     * Note: It is often more efficient to delete the token based on the Id if you already
     * have this information, rather than reading the Token first before removing it.
     *
     * @param tokenId The non null Token Id of the token to remove.
     * scheduled for later execution.
     */
    void deleteAsync(String tokenId) throws CoreTokenException;

    /**
     * Delete a collection of Tokens from the Token Store asynchronously using a filter to narrow down the Tokens to be
     * deleted.
     *
     * @param query Non null filters which will be combined logically using AND.
     * @return total number of tokens deleted by query.
     * @throws CoreTokenException If there was an error while looking up the deletable Tokens.
     * scheduled for later execution.
     */
    int delete(Map<CoreTokenField, Object> query) throws CoreTokenException;

    /**
     * Performs a synchronous query against the persistent store using the provided TokenFilter.
     *
     * The filter is assembled by the TokenFilterBuilder which provides the options on how to turn the query being
     * performed.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilter
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     *
     * @param filter Non null filter.
     * @return Non null, but maybe empty.
     * @throws CoreTokenException If there was any error whilst performing the query.
     * scheduled for later execution.
     */
    Collection<Token> query(TokenFilter filter) throws CoreTokenException;

    /**
     * Performs a partial Token query against the store synchronously. That is, a query which is aimed at specific
     * attributes of a Token, rather than whole Tokens. The return result will consist of PartialTokens for this
     * purpose.
     *
     * This function is useful for example, finding all Token IDs that match a certain criteria.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilter#addReturnAttribute(org.forgerock.openam.tokens.CoreTokenField)
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     * @see org.forgerock.openam.sm.datalayer.api.query.PartialToken
     *
     * @param tokenFilter Non null TokenFilter, with the return attributes defined.
     * @return Non null, but maybe empty.
     * @throws CoreTokenException If there was any error whilst performing the query.
     * scheduled for later execution.
     */
    Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException;

    /**
     * Performs an asynchronous query against the persistent store using the provided TokenFilter and then deletes the
     * matching tokens from the store.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     * @param tokenFilter Non null filter.
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     * scheduled for later execution.
     */
    void deleteOnQueryAsync(TokenFilter tokenFilter) throws CoreTokenException;
}
