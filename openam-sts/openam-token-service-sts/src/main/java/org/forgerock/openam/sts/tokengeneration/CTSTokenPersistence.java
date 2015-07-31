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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration;

import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.user.invocation.STSIssuedTokenState;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.query.QueryFilter;

import java.util.List;

/**
 * An interface to encapsulate the concerns of persisting issued tokens to the CTS, and of retrieving persisted instances
 * (performed during token validate), and of removing persisted instances (performed during token cancellation).
 */
public interface CTSTokenPersistence {
    CoreTokenField CTS_TOKEN_FIELD_STS_ID = CoreTokenField.STRING_ONE;
    CoreTokenField CTS_TOKEN_FIELD_STS_TOKEN_TYPE = CoreTokenField.STRING_TWO;
    /**
     * @param stsId The id of the sts instance persisting the token. Will be used to query for issued tokens later.
     * @param tokenType The type of to-be-persisted token
     * @param tokenString The token to be persisted
     * @param subjectId The subject which this token asserts
     * @param issueInstantMillis millisecond time at which token is issued
     * @param tokenLifetimeSeconds token lifetime in seconds
     * @throws CTSTokenPersistenceException thrown if the underlying CTSPersistentStore throws a CoreTokenException during
     * the presistence operation
     */
    void persistToken(String stsId, TokenType tokenType, String tokenString, String subjectId, long issueInstantMillis,
                      long tokenLifetimeSeconds) throws CTSTokenPersistenceException;

    /**
     * Called during token validation
     * @param tokenId the id of the token to be retrieved
     * @return the String representation of the token if found, or null if the token is not found
     * @throws CTSTokenPersistenceException thrown if the underlying CTSPersistentStore throws a CoreTokenException during
     * the CTS lookup
     */
    String getToken(String tokenId) throws CTSTokenPersistenceException;

    /**
     * Called during token cancellation
     * @param tokenId the id of the token to be deleted
     * @throws CTSTokenPersistenceException thrown if the underlying CTSPersistentStore throws a CoreTokenException during
     * the deletion
     */
    void deleteToken(String tokenId) throws CTSTokenPersistenceException;

    /**
     * Returns a list of STSIssuedTokenState instances corresponding to the sts-issued tokens specified by the QueryFilter.
     * These queries are currently limited to a specific sts id, and an optional subject asserted by the token.
     * @param queryFilter the QueryFilter referencing the CoreTokenFields which will guide the CTS search
     * @return the non-null, possibly-empty list returned by the query.
     * @throws CTSTokenPersistenceException if the query could not be successfully performed.
     */
    List<STSIssuedTokenState> listTokens(QueryFilter<CoreTokenField> queryFilter) throws CTSTokenPersistenceException;
}
