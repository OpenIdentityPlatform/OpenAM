/**
 * Copyright 2013 ForgeRock AS.
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
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.cts.impl;

import javax.inject.Inject;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.LDAPOperationFailedException;
import org.forgerock.openam.cts.utils.TokenAttributeConversion;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

/**
 * Responsible adapting the LDAP SDK Connection and its associated domain
 * values into Tokens. This class will manage the associated conversion tasks
 * and present a common interface to calling classes.
 *
 * It also helps us work around a number of final classes in the SDK which were
 * hindering unit testing.
 *
 * @author robert.wapshott@forgerock.com
 */
public class LDAPAdapter {
    private final TokenAttributeConversion conversion;

    /**
     * Create an instance of this adapter.
     * @param conversion Non null, required for Token conversion.
     */
    @Inject
    public LDAPAdapter(TokenAttributeConversion conversion) {
        this.conversion = conversion;
    }

    /**
     * Create the Token in LDAP.
     *
     * @param connection The non null connection to perform this call against.
     * @param token Non null Token to create.
     * @throws ErrorResultException Unexpected LDAP Exception.
     * @throws LDAPOperationFailedException If the operation failed for a known reason.
     */
    public void create(Connection connection, Token token) throws ErrorResultException, LDAPOperationFailedException {
        Entry entry = conversion.getEntry(token);
        processResult(connection.add(entry));
    }

    /**
     * Performs a read against the LDAP connection and converts the result into a Token.
     *
     * @param connection The non null connection to perform this call against.
     * @param tokenId The id of the Token to read.
     * @return Token if found, otherwise null.
     */
    public Token read(Connection connection,  String tokenId) throws ErrorResultException {
        DN dn = conversion.generateTokenDN(tokenId);
        SearchResultEntry resultEntry = connection.readEntry(dn);
        return conversion.tokenFromEntry(resultEntry);
    }

    /**
     * Update the Token based on whether there were any changes between the two.
     *
     * @param connection The non null connection to perform this call against.
     * @param previous The non null previous Token to check against.
     * @param updated The non null Token to update with.
     * @return True if the token was updated, or false if there were no changes detected.
     * @throws ErrorResultException Unexpected LDAP Exception.
     * @throws LDAPOperationFailedException If the operation failed for a known reason.
     */
    public boolean update(Connection connection, Token previous, Token updated) throws ErrorResultException, LDAPOperationFailedException {
        Entry currentEntry = conversion.getEntry(updated);
        TokenAttributeConversion.stripObjectClass(currentEntry);

        Entry previousEntry = conversion.getEntry(previous);
        TokenAttributeConversion.stripObjectClass(previousEntry);

        ModifyRequest request = Entries.diffEntries(previousEntry, currentEntry);

        // Test to see if there are any modifications
        if (request.getModifications().isEmpty()) {
            return false;
        }

        processResult(connection.modify(request));
        return true;
    }

    /**
     * Performs a delete against the Token ID provided.
     *
     * Note: Token ID's must be unique and thus only one token will be deleted if found.
     *
     * @param connection Non null connection to call.
     * @param tokenId The non null Token ID to delete.
     * @throws ErrorResultException If there was an unexpected LDAP error during the process.
     * @throws LDAPOperationFailedException If the operation failed, this exception will capture the reason.
     */
    public void delete(Connection connection, String tokenId) throws LDAPOperationFailedException, ErrorResultException {
        String dn = String.valueOf(conversion.generateTokenDN(tokenId));
        processResult(connection.delete(dn));
    }

    /**
     * Perform a delete against the Token ID provided asynchronously.
     *
     * This will place the request on a worker queue and the Handler will be notified when
     * the deletion has compelted, or failed.
     *
     * @param connection Non null.
     * @param tokenId The token ID to delete.
     * @param handler An interface which will be called when the operation is completed.
     */
    public void deleteAsync(Connection connection, String tokenId, ResultHandler<Result> handler) {
        String dn = String.valueOf(conversion.generateTokenDN(tokenId));
        connection.deleteAsync(Requests.newDeleteRequest(dn), null, handler);
    }

    /**
     * Verify if the result was successful.
     * @param result Non null.
     * @throws LDAPOperationFailedException Thrown if the result was not successful.
     */
    private void processResult(Result result) throws LDAPOperationFailedException {
        ResultCode resultCode = result.getResultCode();
        if (resultCode.isExceptional()) {
            throw new LDAPOperationFailedException(result);
        }
    }
}
