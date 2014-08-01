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

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.query.PartialToken;

import java.util.Collection;
import java.util.Map;

/**
 * Core Token Service Persistent Store is responsible for the storage and retrieval of Tokens
 * from a persistent store.
 *
 * The Core Token Service is exposed through a series of CRUDL operations which require the
 * use of TokenAdapters to convert from objects to be stored into the expected Core Token
 * Service format. This allows the Tokens to be stored in a generic storage.
 *
 * The current main use cases for the CTS are:
 *
 * - Session fail-over
 * - OAuth token storage
 * - SAML Federation token storage
 * - Forgotten Password
 *
 * The implementation of this interface must be thread safe as this service will often
 * be on a direct call path from the front end of the Server hosting OpenAM, and so
 * multiple calls to the service can occur at the same time from different calling threads.
 *
 * @see org.forgerock.openam.cts.adapters.TokenAdapter
 * @see Token
 */
public interface CTSPersistentStore {

    /**
     * Create a Token in the persistent store. If the Token already exists then this create
     * may be ignored, or an error thrown to indicate this failure.
     *
     * @see CTSPersistentStore#update(org.forgerock.openam.cts.api.tokens.Token)
     *
     * @param token Non null Token to create.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    void create(Token token) throws CoreTokenException;

    /**
     * Read a Token from the persistent store.
     *
     * @param tokenId The non null Token Id that the Token was created with.
     * @return Null if there was no matching Token. Otherwise a fully populated Token will be returned.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    Token read(String tokenId) throws CoreTokenException;

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
    void update(Token token) throws CoreTokenException;

    /**
     * Delete the Token from the store.
     *
     * @param token Non null Token to be deleted from the store.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    void delete(Token token) throws CoreTokenException;

    /**
     * Delete the Token from the store based on its id.
     *
     * Note: It is often more efficient to delete the token based on the Id if you already
     * have this information, rather than reading the Token first before removing it.
     *
     * @param tokenId The non null Token Id of the token to remove.
     * @throws CoreTokenException If there was a non-recoverable error during the operation.
     */
    void delete(String tokenId) throws CoreTokenException;

    /**
     * Delete a collection of Tokens from the Token Store using a filter to narrow down the
     * Tokens to be deleted.
     *
     * @param query Non null filters which will be combined logically using AND.
     *
     * @return total number of tokens deleted by query.
     *
     * @throws DeleteFailedException If the delete failed for any reason.
     */
    int delete(Map<CoreTokenField, Object> query) throws CoreTokenException;

    /**
     * Performs a query against the persistent store using the provided TokenFilter.
     *
     * The filter is assembled by the TokenFilterBuilder which provides the options on how
     * to turn the query being performed.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilter
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     *
     * @param filter Non null filter.
     * @return Non null, but maybe empty.
     * @throws CoreTokenException If there was any error whilst performing the query.
     */
    Collection<Token> query(TokenFilter filter) throws CoreTokenException;

    /**
     * Performs a specialised Query whereby PartialTokens are returned. This is an optimisation
     * which allows the caller to specify just the fields that are being returned.
     *
     * As stated in the PartialToken class, this is not a full Token and should not be treated
     * as one. Instead it is a way of getting specific access to parts of the Token data.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilter#addReturnAttribute(org.forgerock.openam.cts.api.fields.CoreTokenField)
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     * @see org.forgerock.openam.cts.impl.query.PartialToken
     *
     * @param tokenFilter Non null TokenFilter, with the return attributes defined.
     * @throws CoreTokenException If there was any error whilst performing the query.
     * @return Non null, but maybe empty.
     */
    Collection<PartialToken> attributeQuery(TokenFilter tokenFilter) throws CoreTokenException;

    /**
     * Performs a query against the persistent store using the provided TokenFilter and then deletes the matching
     * tokens from the store.
     *
     * @see org.forgerock.openam.cts.api.filter.TokenFilterBuilder
     * @param tokenFilter Non null filter.
     * @throws CoreTokenException If there was a problem queuing the task to be performed.
     */
    void deleteOnQuery(TokenFilter tokenFilter) throws CoreTokenException;
}
