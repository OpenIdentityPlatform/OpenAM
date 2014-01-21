/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock, Inc. All Rights Reserved
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

import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.DeleteFailedException;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.opendj.ldap.Filter;

import java.util.Collection;
import java.util.Map;

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
public interface CTSPersistentStore {

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
    void delete(String tokenId) throws DeleteFailedException;

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
    int delete(Map<CoreTokenField, Object> query) throws DeleteFailedException;

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
    Collection<Token> list(Map<CoreTokenField, Object> query) throws CoreTokenException;

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
    Collection<Token> list(Filter filter) throws CoreTokenException;

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     *
     * @param uuid User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception if there is any problem with accessing the session
     *                   repository.
     */
    Map<String, Long> getTokensByUUID(String uuid) throws CoreTokenException;
}
