/**
 * Copyright 2013 ForgeRock, Inc.
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
package com.sun.identity.sm.ldap.impl;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.CreateFailedException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import com.sun.identity.sm.ldap.exceptions.OperationFailedException;
import com.sun.identity.sm.ldap.exceptions.QueryFailedException;
import com.sun.identity.sm.ldap.exceptions.SetFailedException;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Primary interface to LDAP which provides a CRUDL style interface.
 *
 * Note: This class uses Token as its main means of adding data to and from LDAP and therefore is intended
 * for use by the Core Token Service.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenLDAPAdapter {
    // Injected
    private final DataLayerConnectionFactory connectionFactory;
    private final LDAPDataConversion conversion;
    private final CoreTokenConstants constants;
    private final QueryFactory factory;
    private final Debug debug;
    private final TokenAttributeConversion attributeConversion;

    /**
     * Create a new instance of the CoreTokenLDAPAdapter with dependencies.
     * @param connectionFactory Required to access LDAP.
     * @param conversion Required to manage data conversions.
     * @param constants Required for resolution of LDAP DN.
     */
    public CoreTokenLDAPAdapter(DataLayerConnectionFactory connectionFactory, LDAPDataConversion conversion,
                                CoreTokenConstants constants) {
        this(connectionFactory,
             conversion,
             new TokenAttributeConversion(constants, conversion),
             constants,
             new QueryFactory(),
             SessionService.sessionDebug);
    }

    /**
     * Create a new instance of the CoreTokenLDAPAdapter with all dependencies exposed.
     * @param connectionFactory Required to access LDAP.
     * @param conversion Required to manage data conversions.
     * @param attributeConversionConversion Required to process results.
     * @param constants Required for resolution of LDAP DN.
     * @param debug Debugging instance to use.
     */
    public CoreTokenLDAPAdapter(DataLayerConnectionFactory connectionFactory, LDAPDataConversion conversion,
                                TokenAttributeConversion attributeConversionConversion,
                                CoreTokenConstants constants, QueryFactory factory,
                                Debug debug) {
        this.connectionFactory = connectionFactory;
        this.conversion = conversion;
        this.attributeConversion = attributeConversionConversion;
        this.constants = constants;
        this.factory = factory;
        this.debug = debug;
    }

    /**
     * Create a token in the persistent store.
     *
     * @param token Token to create.
     * @throws CoreTokenException If the Token exists already of there was
     * an error as a result of this operation.
     */
    public void create(Token token) throws CoreTokenException {
        Entry entry = attributeConversion.getEntry(token);

        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            Result result = connection.add(entry);
            processResult(result);

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Create: Created {0} Token {1}\n" +
                        "{2}",
                        token.getType(),
                        token.getTokenId(),
                        token));
            }

        } catch (ErrorResultException e) {
            throw new CreateFailedException(token, e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Start the query process.
     *
     * The API of the QueryBuilder will guide the caller through the available query options.
     *
     * @return A new QueryBuilder instance.
     */
    public QueryBuilder query() {
        return factory.createInstance(connectionFactory, constants);
    }

    public QueryFilter buildFilter() {
        return factory.createFilter();
    }

    /**
     * Update a Token in the LDAP store.
     *
     * This function will perform a read of the Token ID to determine if the Token has been
     * persisted already. If it has not been persisted, then delegates to the create function.
     *
     * Otherwise performs a set based on the difference between the Token in the store and the
     * Token being stored.
     *
     * If this difference has no changes, then there is nothing to be done.
     *
     * @param token Token to update or create.
     * @throws CoreTokenException If there was an error updating the token, or if there were
     * multiple tokens present in the store that have the same id.
     */
    public void update(Token token) throws CoreTokenException {
        // If the token does not exist create it. There shouldn't be more than one token.
        Collection<Entry> entries;
        try {
            Filter filter = buildFilter().and().tokenId(token.getTokenId()).build();
            entries = query().withFilter(filter).executeRawResults();

            if (entries.size() > 1) {
                throw new CoreTokenException("Multiple tokens detected for Token", token);
            }

            // Create the token instead of updating it.
            if (entries.size() == 0) {
                create(token);
                return;
            }
        } catch (QueryFailedException e) {
            throw new CoreTokenException("Error querying token", token, e);
        }

        /**
         * Generate the difference between the tokens.
         *
         * Note: The read operation above returned an Entry which has had the
         * object class stripped out of it already, so we must make sure to do the
         * same for current Entry.
         */
        Entry previous = entries.iterator().next();
        Entry current = attributeConversion.getEntry(token);
        TokenAttributeConversion.stripObjectClass(current);

        ModifyRequest request = Entries.diffEntries(previous, current);

        // Test to see if there are any modifications
        if (request.getModifications().isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Update: no modifications for Token {0}",
                        token.getTokenId()));
            }
            return;
        }

        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            processResult(connection.modify(request));

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                                "Update: Token {0} changed.\n" +
                                "Previous:\n" +
                                "{1}\n" +
                                "Current:\n" +
                                "{2}",
                        token.getTokenId(),
                        attributeConversion.tokenFromEntry(previous),
                        attributeConversion.tokenFromEntry(current)));
            }

        } catch (ErrorResultException e) {
            throw new SetFailedException(token, request, e);
        } catch (OperationFailedException e) {
            throw new SetFailedException(token, request, e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Deletes a token from the store based on its token id.
     * @param tokenId Non null token id.
     * @throws CoreTokenException If there was any problem deleting the token.
     */
    public void delete(String tokenId) throws DeleteFailedException {
        delete(attributeConversion.generateTokenDN(tokenId));
    }

    /**
     * Remove a given Token from the store.
     * @param token A non null Token.
     * @throws CoreTokenException If there was any problem deleting the token.
     */
    public void delete(Token token) throws DeleteFailedException {
        delete(attributeConversion.generateTokenDN(token));
    }

    /**
     * Internal remove function will perform the remove based on the DN of the token.
     * This function does not check the token exists before performing the delete.
     * @param dnToDelete DN to delete.
     * @throws CoreTokenException If there was any problem performing this operation.
     */
    private void delete(DN dnToDelete) throws DeleteFailedException {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            String dn = String.valueOf(dnToDelete);
            processResult(connection.delete(dn));

            if (debug.messageEnabled()) {
                debug.message(MessageFormat.format(
                        CoreTokenConstants.DEBUG_HEADER +
                        "Delete: Deleted DN {0}",
                        dn));
            }

        } catch (ErrorResultException e) {
            ResultCode resultCode = e.getResult().getResultCode();
            /**
             * If the object does not exist, that will also conclude this delete operation.
             */
            if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
                return;
            }

            throw new DeleteFailedException(dnToDelete, e);
        } catch (OperationFailedException e) {
            throw new DeleteFailedException(dnToDelete, e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Process the result and detect if there was an exceptional condition.
     * @param result Non null.
     * @throws OperationFailedException If the operation failed, the reason will be included in this exception.
     */
    private void processResult(Result result) throws OperationFailedException {
        ResultCode resultCode = result.getResultCode();
        if (resultCode.isExceptional()) {
            throw new OperationFailedException(result);
        }
    }
}