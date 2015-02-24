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
 * Copyright 2013-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl;

import java.util.Collection;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilterVisitor;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

import javax.inject.Inject;

/**
 * Responsible adapting the LDAP SDK Connection and its associated domain
 * values into Tokens. This class will manage the associated conversion tasks
 * and present a common interface to calling classes.
 *
 * It also helps us work around a number of final classes in the SDK which were
 * hindering unit testing.
 */
public class LdapAdapter implements TokenStorageAdapter<Connection> {
    private final LdapTokenAttributeConversion conversion;
    private final LdapQueryFilterVisitor queryConverter;
    private final LdapQueryFactory queryFactory;

    /**
     * Create an instance of this adapter.
     * @param conversion Non null, required for Token conversion.
     */
    @Inject
    public LdapAdapter(LdapTokenAttributeConversion conversion, LdapQueryFilterVisitor queryConverter,
            LdapQueryFactory queryFactory) {
        this.conversion = conversion;
        this.queryConverter = queryConverter;
        this.queryFactory = queryFactory;
    }

    /**
     * Create the Token in LDAP.
     *
     * @param connection The non null connection to perform this call against.
     * @param token Non null Token to create.
     * @throws ErrorResultException Unexpected LDAP Exception.
     * @throws org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException If the operation failed for a known reason.
     */
    public void create(Connection connection, Token token) throws LdapOperationFailedException {
        Entry entry = conversion.getEntry(token);
        try {
            processResult(connection.add(entry));
        } catch (ErrorResultException e) {
            throw new LdapOperationFailedException(e.getResult());
        }
    }

    /**
     * Performs a read against the LDAP connection and converts the result into a Token.
     *
     * @param connection The non null connection to perform this call against.
     * @param tokenId The id of the Token to read.
     * @return Token if found, otherwise null.
     */
    public Token read(Connection connection,  String tokenId) throws DataLayerException {
        DN dn = conversion.generateTokenDN(tokenId);
        try {
            SearchResultEntry resultEntry = connection.readEntry(dn);
            return conversion.tokenFromEntry(resultEntry);
        } catch (ErrorResultException e) {
            Result result = e.getResult();
            // Check for NO_SUCH_OBJECT
            if (result != null && ResultCode.NO_SUCH_OBJECT.equals(result.getResultCode())) {
                return null;
            }
            throw new LdapOperationFailedException(result);
        }
    }

    /**
     * Update the Token based on whether there were any changes between the two.
     *
     * @param connection The non null connection to perform this call against.
     * @param previous The non null previous Token to check against.
     * @param updated The non null Token to update with.
     * @return True if the token was updated, or false if there were no changes detected.
     * @throws org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException If the operation failed for a known reason.
     */
    public boolean update(Connection connection, Token previous, Token updated) throws LdapOperationFailedException {
        Entry currentEntry = conversion.getEntry(updated);
        LdapTokenAttributeConversion.stripObjectClass(currentEntry);

        Entry previousEntry = conversion.getEntry(previous);
        LdapTokenAttributeConversion.stripObjectClass(previousEntry);

        ModifyRequest request = Entries.diffEntries(previousEntry, currentEntry);

        // Test to see if there are any modifications
        if (request.getModifications().isEmpty()) {
            return false;
        }

        try {
            processResult(connection.modify(request));
        } catch (ErrorResultException e) {
            throw new LdapOperationFailedException(e.getResult());
        }
        return true;
    }

    /**
     * Performs a delete against the Token ID provided.
     *
     * @param connection Non null connection to call.
     * @param tokenId The non null Token ID to delete.
     * @throws ErrorResultException If there was an unexpected LDAP error during the process.
     * @throws org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException If the operation failed, this exception will capture the reason.
     */
    public void delete(Connection connection, String tokenId) throws LdapOperationFailedException {
        String dn = String.valueOf(conversion.generateTokenDN(tokenId));
        try {
            processResult(connection.delete(dn));
        } catch (ErrorResultException e) {
            Result result = e.getResult();
            // Check for NO_SUCH_OBJECT
            if (e.getResult() != null && ResultCode.NO_SUCH_OBJECT.equals(result.getResultCode())) {
                return;
            }
            throw new LdapOperationFailedException(e.getResult());
        }
    }

    @Override
    public Collection<Token> query(Connection connection, TokenFilter query) throws DataLayerException {
        try {
            return queryFactory.createInstance()
                    .withFilter(query.getQuery().accept(queryConverter, null))
                    .execute(connection).next();
        } catch (DataLayerRuntimeException e) {
            throw new DataLayerException("Error during partial query", e);
        }
    }

    @Override
    public Collection<PartialToken> partialQuery(Connection connection, TokenFilter query) throws DataLayerException {
        try {
            return queryFactory.createInstance()
                    .returnTheseAttributes(query.getReturnFields())
                    .withFilter(query.getQuery().accept(queryConverter, null))
                    .executeAttributeQuery(connection).next();
        } catch (DataLayerRuntimeException e) {
            throw new DataLayerException("Error during partial query", e);
        }
    }

    /**
     * Verify if the result was successful.
     * @param result Non null.
     * @throws org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException Thrown if the result was not successful.
     */
    private void processResult(Result result) throws LdapOperationFailedException {
        ResultCode resultCode = result.getResultCode();
        if (resultCode.isExceptional()) {
            throw new LdapOperationFailedException(result);
        }
    }
}
