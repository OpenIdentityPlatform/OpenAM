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

import static org.forgerock.openam.cts.api.CTSOptions.*;
import static org.forgerock.openam.tokens.CoreTokenField.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jakarta.inject.Inject;

import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.continuous.ContinuousQuery;
import org.forgerock.openam.cts.continuous.ContinuousQueryListener;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.DataLayerRuntimeException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.OptimisticConcurrencyCheckFailedException;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilterVisitor;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.AssertionFailureException;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.controls.PostReadRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadResponseControl;
import org.forgerock.opendj.ldap.controls.PreReadResponseControl;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Request;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.util.Option;
import org.forgerock.util.Options;

import com.forgerock.opendj.ldap.controls.TransactionIdControl;

/**
 * Responsible adapting the LDAP SDK Connection and its associated domain
 * values into Tokens. This class will manage the associated conversion tasks
 * and present a common interface to calling classes.
 *
 * It also helps us work around a number of final classes in the SDK which were
 * hindering unit testing.
 */
public class LdapAdapter implements TokenStorageAdapter {

    private final LdapTokenAttributeConversion conversion;
    private final LdapQueryFilterVisitor queryConverter;
    private final LdapQueryFactory queryFactory;
    private final ConnectionFactory<Connection> connectionFactory;
    private final Map<Option<?>, LdapOptionFunction> optionFunctionMap;

    private Connection connection;

    /**
     * Create an instance of this adapter.
     *
     * @param conversion Non null, required for Token conversion.
     * @param queryConverter For converting between CTS and LDAP.
     * @param queryFactory Produces queries.
     * @param connectionFactoryProvider Produces connection factories for connections to the datastore.
     */
    @Inject
    public LdapAdapter(LdapTokenAttributeConversion conversion, LdapQueryFilterVisitor queryConverter,
            LdapQueryFactory queryFactory, LdapConnectionFactoryProvider connectionFactoryProvider,
            Map<Option<?>, LdapOptionFunction> optionFunctionMap) {
        this.conversion = conversion;
        this.queryConverter = queryConverter;
        this.queryFactory = queryFactory;
        this.connectionFactory = connectionFactoryProvider.createFactory();
        this.optionFunctionMap = optionFunctionMap;
    }

    /**
     * Create the Token in LDAP.
     *
     * @param token Non null Token to create.
     * @param options Non null Options for the operations.
     * @return A copy of the created {@literal} {@code Token} with the ETag set.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
    public Token create(Token token, Options options) throws DataLayerException {
        Entry entry = conversion.getEntry(token);
        try {
            getConnection();
            AddRequest request = LDAPRequests.newAddRequest(entry)
                    .addControl(PostReadRequestControl.newControl(true, ETAG.toString()));
            request = applyOptions(request, options);
            Result result = connection.add(request);
            return tokenWithNewEtag(token, result);
        } catch (LdapException e) {
            throw new LdapOperationFailedException(e.getResult());
        }
    }

    /**
     * Performs a read against the LDAP connection and converts the result into a Token.
     *
     * @param tokenId The id of the Token to read.
     * @param options Non null Options for the operations.
     * @return Token if found, otherwise null.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     */
    public Token read(String tokenId, Options options) throws DataLayerException {
        DN dn = conversion.generateTokenDN(tokenId);

        try {
            getConnection();
            SearchRequest request = LDAPRequests.newSingleEntrySearchRequest(dn, "*", ETAG.toString());
            request = applyOptions(request, options);
            SearchResultEntry resultEntry = connection.searchSingleEntry(request);
            return conversion.tokenFromEntry(resultEntry);
        } catch (LdapException e) {
            Result result = e.getResult();
            if (result != null && ResultCode.NO_SUCH_OBJECT.equals(result.getResultCode())) {
                return null;
            }
            throw new LdapOperationFailedException(result);
        }
    }

    /**
     * Update the Token based on whether there were any changes between the two.
     *
     * It is up to the underlying ldap later to update the etag so the etag
     * so the etag is removed from the previous and updated token before the diff is
     * computed.
     *
     * <p>If the {@literal previous} {@code Token} contains a non-{@code null}
     * {@link CoreTokenField#ETAG} attribute value then the update will be performed with an
     * optimistic concurrency check. If it does not contain the attribute or it contains a
     * {@code null} value the update will be performed without any concurrency checks.</p>
     *
     * @param previous The non null previous Token to check against.
     * @param options The non null Options for the operation.
     * @throws OptimisticConcurrencyCheckFailedException If the operation failed due to an
     * assertion on the tokens ETag. Only possible if the {@link CoreTokenField#ETAG} attribute is
     * present on the {@literal previous} token.
     */
    public Token update(Token previous, Token updated, Options options) throws DataLayerException {
        Entry currentEntry = conversion.getEntry(updated);
        LdapTokenAttributeConversion.stripObjectClass(currentEntry);

        Entry previousEntry = conversion.getEntry(previous);
        LdapTokenAttributeConversion.stripObjectClass(previousEntry);

        //etag should not be modified and should not be part of the modification request
        //it is the up to the underlying ldap layer to generate etag
        previousEntry.removeAttribute(ETAG.toString());
        currentEntry.removeAttribute(ETAG.toString());
        ModifyRequest request = Entries.diffEntries(previousEntry, currentEntry,
            Entries.diffOptions().replaceSingleValuedAttributes());

        if (request.getModifications().isEmpty()) {
            return previous;
        }

        request.addControl(TransactionIdControl.newControl(AuditRequestContext.createSubTransactionIdValue()))
                .addControl(PostReadRequestControl.newControl(true, ETAG.toString()));

        request = applyOptions(request, options);

        try {
            getConnection();
            Result result = connection.modify(request);
            return tokenWithNewEtag(updated, result);
        } catch (AssertionFailureException e) {
            throw new OptimisticConcurrencyCheckFailedException(updated.getTokenId(),
                    options.get(OPTIMISTIC_CONCURRENCY_CHECK_OPTION), e);
        } catch (LdapException e) {
            throw new LdapOperationFailedException(e.getResult());
        }
    }

    /**
     * Performs a delete against the Token ID provided.
     *
     * <p>If the {@literal etag} parameter is a non-{@code null} value then the delete will be
     * performed with an optimistic concurrency check. If it is {@code null} then the delete will
     * be performed without any concurrency checks.</p>
     *
     * @param tokenId The non null Token ID to delete.
     * @param options The non null Options for the operation.
     * @return A {@link PartialToken} containing at least the {@link CoreTokenField#TOKEN_ID}.
     * @throws DataLayerException If the operation failed, this exception will capture the reason.
     * @throws OptimisticConcurrencyCheckFailedException If the operation failed due to an assertion on the tokens ETag.
     */
    public PartialToken delete(String tokenId, Options options) throws DataLayerException {
        String dn = String.valueOf(conversion.generateTokenDN(tokenId));
        try {
            getConnection();
            DeleteRequest request = LDAPRequests.newDeleteRequest(dn);
            request = applyOptions(request, options);
            Result result = connection.delete(request);
            verifySuccess(result);
            PreReadResponseControl control = result.getControl(PreReadResponseControl.DECODER, new DecodeOptions());
            if (control != null) {
                return conversion.tokenFromEntry(control.getEntry()).toPartialToken();
            } else {
                return new PartialToken(Collections.<CoreTokenField, Object>singletonMap(CoreTokenField.TOKEN_ID, tokenId));
            }
        } catch (AssertionFailureException e) {
            throw new OptimisticConcurrencyCheckFailedException(tokenId,
                    options.get(OPTIMISTIC_CONCURRENCY_CHECK_OPTION), e);
        } catch (LdapException e) {
            Result result = e.getResult();
            if (e.getResult() != null && ResultCode.NO_SUCH_OBJECT.equals(result.getResultCode())) {
                return new PartialToken(Collections.<CoreTokenField, Object>singletonMap(CoreTokenField.TOKEN_ID, tokenId));
            }
            throw new LdapOperationFailedException(e.getResult());
        } catch (DecodeException e) {
            throw new LdapOperationFailedException(e.getMessage());
        }
    }

    @Override
    public Collection<Token> query(TokenFilter query) throws DataLayerException {
        try {
            getConnection();
            return queryFactory.createInstance()
                    .limitResultsTo(query.getSizeLimit())
                    .within(query.getTimeLimit())
                    .withFilter(query.getQuery().accept(queryConverter, null))
                    .execute(connection).next();
        } catch (DataLayerRuntimeException e) {
            throw new DataLayerException("Error during partial query", e);
        }
    }

    @Override
    public Collection<PartialToken> partialQuery(TokenFilter query) throws DataLayerException {
        try {
            getConnection();
            return queryFactory.createInstance()
                    .returnTheseAttributes(query.getReturnFields())
                    .limitResultsTo(query.getSizeLimit())
                    .within(query.getTimeLimit())
                    .withFilter(query.getQuery().accept(queryConverter, null))
                    .executeAttributeQuery(connection).next();
        } catch (DataLayerRuntimeException e) {
            throw new DataLayerException("Error during partial query", e);
        }
    }

    @Override
    public ContinuousQuery startContinuousQuery(TokenFilter filter, ContinuousQueryListener listener)
            throws DataLayerException {
        return queryFactory.createInstance()
            .returnTheseAttributes(filter.getReturnFields())
            .withFilter(filter.getQuery().accept(queryConverter, null))
            .executeContinuousQuery(listener);
    }

    /**
     * Verify if the result was successful.
     *
     * @param result Non null.
     * @throws LdapOperationFailedException Thrown if the result was not successful.
     */
    private void verifySuccess(Result result) throws LdapOperationFailedException {
        ResultCode resultCode = result.getResultCode();
        if (resultCode.isExceptional()) {
            throw new LdapOperationFailedException(result);
        }
    }

    private Token tokenWithNewEtag(Token token, Result result) throws LdapOperationFailedException {
        verifySuccess(result);
        Token newToken = new Token(token);
        newToken.setAttribute(ETAG, extractEtag(result));
        return newToken;
    }

    private synchronized void getConnection() throws DataLayerException {
        if (!connectionFactory.isValid(connection)) {
            IOUtils.closeIfNotNull(connection);
            connection = connectionFactory.create();
        }
    }

    private String extractEtag(Result result) throws LdapOperationFailedException {
        try {
            PostReadResponseControl control =
                    result.getControl(PostReadResponseControl.DECODER, new DecodeOptions());
            Entry entry = control.getEntry();
            return entry.getAttribute(ETAG.toString()).firstValueAsString();
        } catch (final DecodeException e) {
            throw new LdapOperationFailedException("Failed to extract the e-tag", e);
        }
    }

    private <R extends Request> R applyOptions(R request, Options options) {
        R r = request;
        for (LdapOptionFunction optionFunction : optionFunctionMap.values()) {
            r = optionFunction.apply(r, options);
        }
        return r;
    }
}