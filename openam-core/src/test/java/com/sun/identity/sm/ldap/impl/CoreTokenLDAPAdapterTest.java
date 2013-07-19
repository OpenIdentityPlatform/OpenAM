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

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.TokenType;
import com.sun.identity.sm.ldap.api.tokens.Token;
import com.sun.identity.sm.ldap.exceptions.CoreTokenException;
import com.sun.identity.sm.ldap.exceptions.DeleteFailedException;
import com.sun.identity.sm.ldap.utils.LDAPDataConversion;
import com.sun.identity.sm.ldap.utils.TokenAttributeConversion;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LinkedHashMapEntry;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenLDAPAdapterTest {
    private Connection mockConnection;
    private DataLayerConnectionFactory mockFactory;
    private CoreTokenConstants constants;
    private LDAPDataConversion dataConversion;
    private TokenAttributeConversion attributeConversion;

    @BeforeMethod
    public void setup() {
        mockConnection = mock(Connection.class);
        mockFactory = mock(DataLayerConnectionFactory.class);
        try {
            given(mockFactory.getConnection()).willReturn(mockConnection);
        } catch (ErrorResultException e) {
            throw new IllegalStateException(e);
        }

        constants = new CoreTokenConstants("cn=test");
        dataConversion = new LDAPDataConversion();
        attributeConversion = new TokenAttributeConversion(constants, dataConversion);
    }


    @Test
    public void shouldCreateToken() throws CoreTokenException, ErrorResultException {
        // Given
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result result = successResult();
        given(mockConnection.add(any(Entry.class))).willReturn(result);

        // When
        adapter.create(token);

        // Then
        verify(mockConnection).add(any(Entry.class));
    }

    @Test
    public void shouldGenerateAnInstanceOfQueryBuilder() throws ErrorResultException {
        // Given
        QueryFactory factory = mock(QueryFactory.class);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                factory,
                mock(Debug.class));

        // When
        adapter.query();

        // Then
        verify(factory).createInstance(mockFactory, constants);
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldFailIfMultipleTokensFoundWhilstUpdating() throws CoreTokenException, ErrorResultException {
        // Given
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(2),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // When / Then
        adapter.update(token);

    }

    @Test
    public void shouldCreateTokenIfNotPresentWhilstUpdating() throws CoreTokenException, ErrorResultException {
        // Given
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(0),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result success = successResult();
        given(mockConnection.add(any(Entry.class))).willReturn(success);

        // When
        adapter.update(token);

        // Then
        verify(mockConnection).add(any(Entry.class));
    }

    @Test
    public void shouldPerformUpdate() throws ErrorResultException, CoreTokenException {
        // Given
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                mockQueryBuilderFactory(1),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the Connection add is a success
        Result success = successResult();
        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(success);

        // When
        adapter.update(token);

        // Then
        verify(mockConnection).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldDeleteToken() throws ErrorResultException, DeleteFailedException {
        // Given
        attributeConversion = mock(TokenAttributeConversion.class);

        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));

        Token token = new Token("badger", TokenType.SESSION);

        // Ensure the delete operation is a success
        Result success = successResult();
        given(mockConnection.delete(anyString())).willReturn(success);

        // Ensure conversion via TokenAttributeConversion is successful
        DN dn = DN.rootDN();
        given(attributeConversion.generateTokenDN(eq(token))).willReturn(dn);

        // When
        adapter.delete(token);

        // Then
        verify(mockConnection).delete(anyString());
    }

    @Test
    public void shouldReturnTokenWhenRead() throws CoreTokenException, ErrorResultException {
        // Given
        attributeConversion = mock(TokenAttributeConversion.class);

        String tokenId = "badger";
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));

        // Ensure the connection returns something useful
        SearchResultEntry mockResult = mock(SearchResultEntry.class);
        given(mockConnection.readEntry(any(DN.class))).willReturn(mockResult);

        // Ensure we generate a suitable token from the SearchResultEntry conversion.
        Token mockToken = mock(Token.class);
        given(mockToken.getTokenId()).willReturn(tokenId);
        given(attributeConversion.tokenFromEntry(mockResult)).willReturn(mockToken);

        // When
        Token result = adapter.read(tokenId);

        // Then
        assertEquals(tokenId, result.getTokenId());
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldFailReadIfOtherExceptionIsEncountered() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "badger";
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));
        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.OTHER, "test");
        given(mockConnection.readEntry(any(DN.class))).willThrow(exception);

        // When / Then
        adapter.read(tokenId);
    }

    @Test
    public void shouldReturnNullIfNoTokenFoundWhenRead() throws ErrorResultException, CoreTokenException {
        // Given
        String tokenId = "badger";
        CoreTokenLDAPAdapter adapter = new CoreTokenLDAPAdapter(
                mockFactory,
                dataConversion,
                attributeConversion,
                constants,
                new QueryFactory(),
                mock(Debug.class));
        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.NO_SUCH_OBJECT, "test");
        given(mockConnection.readEntry(any(DN.class))).willThrow(exception);

        // When
        Token result = adapter.read(tokenId);

        // Then
        assertNull(result);
    }

    private QueryFactory mockQueryBuilderFactory(int entriesToReturn) throws CoreTokenException {
        QueryFactory factory = mock(QueryFactory.class);
        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        given(factory.createInstance(
                any(DataLayerConnectionFactory.class),
                any(CoreTokenConstants.class)))
                .willReturn(queryBuilder);
        given(factory.createFilter()).willReturn(new QueryFilter(mock(LDAPDataConversion.class)));

        List<Entry> entries = new LinkedList<Entry>();
        for (int ii = 0; ii < entriesToReturn; ii++) {
            Entry e = new LinkedHashMapEntry();
            entries.add(e);
        }
        given(queryBuilder.withFilter(any(Filter.class))).willReturn(queryBuilder);
        given(queryBuilder.executeRawResults()).willReturn(entries);

        return factory;
    }

    private Result successResult() {
        Result result = mock(Result.class);
        ResultCode resultCode = ResultCode.SUCCESS;
        given(result.getResultCode()).willReturn(resultCode);
        return result;
    }
}
