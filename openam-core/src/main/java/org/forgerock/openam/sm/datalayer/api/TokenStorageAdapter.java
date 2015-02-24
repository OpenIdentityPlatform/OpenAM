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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.api;

import java.util.Collection;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;

/**
 * Adapts the token to some activity against the connection type, T.
 * @param <T> The type of connection that token activity will be recorded using.
 */
public interface TokenStorageAdapter<T> {

    /**
     * Create the Token in the database.
     *
     * @param connection The non null connection to perform this call against.
     * @param token      Non null Token to create.
     * @throws org.forgerock.openam.sm.datalayer.api.DataLayerException If the operation failed for a known reason.
     */
    void create(T connection, Token token) throws DataLayerException;

    /**
     * Performs a read against the database connection and converts the result into a Token.
     *
     * @param connection The non null connection to perform this call against.
     * @param tokenId The id of the Token to read.
     * @return Token if found, otherwise null.
     */
    Token read(T connection,  String tokenId) throws DataLayerException;

    /**
     * Update the Token based on whether there were any changes between the two.
     *
     * @param connection The non null connection to perform this call against.
     * @param previous The non null previous Token to check against.
     * @param updated The non null Token to update with.
     * @return True if the token was updated, or false if there were no changes detected.
     * @throws DataLayerException If the operation failed for a known reason.
     */
    boolean update(T connection, Token previous, Token updated) throws DataLayerException;

    /**
     * Performs a delete against the Token ID provided.
     *
     * @param connection Non null connection to call.
     * @param tokenId The non null Token ID to delete.
     * @throws LdapOperationFailedException If the operation failed, this exception will capture the reason.
     */
    void delete(T connection, String tokenId) throws DataLayerException;

    /**
     * Performs a full-token query using the provided filter.
     *
     * @param connection Non null connection to call.
     * @param query The non null filter specification.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
    Collection<Token> query(T connection, TokenFilter query) throws DataLayerException;

    /**
     * Performs a partial query using the provided filter.
     *
     * @param connection Non null connection to call.
     * @param query The non null filter specification.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
    Collection<PartialToken> partialQuery(T connection, TokenFilter query) throws DataLayerException;

}
