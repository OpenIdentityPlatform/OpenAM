
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

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilterVisitor;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LdapAdapterTest {

    private Connection mockConnection;
    private LdapQueryFactory mockQueryFactory;
    private LdapQueryFilterVisitor mockQueryVisitor;
    private LdapAdapter adapter;
    private LdapTokenAttributeConversion mockConversion;

    @BeforeMethod
    private void setup() throws Exception {
        mockConnection = mock(Connection.class);
        mockConversion = mock(LdapTokenAttributeConversion.class);
        mockQueryFactory = mock(LdapQueryFactory.class);
        mockQueryVisitor = mock(LdapQueryFilterVisitor.class);

        adapter = new LdapAdapter(mockConversion, mockQueryVisitor, mockQueryFactory);
    }

    @Test
    public void shouldUseConnectionForCreate() throws Exception {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        Result successResult = mockSuccessfulResult();
        given(mockConnection.add(any(Entry.class))).willReturn(successResult);

        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        // When
        adapter.create(mockConnection, token);

        // Then
        verify(mockConnection).add(any(Entry.class));
    }

    @Test
    public void shouldUseConnectionForRead() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.read(mockConnection, tokenId);

        // Then
        ArgumentCaptor<DN> captor = ArgumentCaptor.forClass(DN.class);
        verify(mockConnection).readEntry(captor.capture());
        assertEquals(testDN, captor.getValue());
    }

    @Test
    public void shouldReturnNullWhenObjectNotFound() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.readEntry(eq(testDN))).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        Token result = adapter.read(mockConnection, tokenId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldUseConnectionForDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.delete(anyString())).willReturn(successResult);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.delete(mockConnection, tokenId);

        // Then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockConnection).delete(captor.capture());
        assertEquals(String.valueOf(testDN), captor.getValue());
    }

    @Test
    public void shouldDoNothingIfObjectNotFoundDuringDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.delete(anyString())).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When / Then
        adapter.delete(mockConnection, tokenId);
    }

    @Test
    public void shouldThrowAllOtherExceptionsDuringDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        ErrorResultException exception = ErrorResultException.newErrorResult(ResultCode.OTHER);
        given(mockConnection.delete(anyString())).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When / Then
        try {
            adapter.delete(mockConnection, tokenId);
            fail();
        } catch (LdapOperationFailedException e) {}
    }

    @Test
    public void shouldNoNothingIfNoModificaitonsOnUpdate() throws Exception {
        // Given
        String tokenId = "badger";
        Token first = new Token(tokenId, TokenType.OAUTH);
        Token second = new Token(tokenId, TokenType.OAUTH);

        Connection mockConnection = mock(Connection.class);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        adapter = new LdapAdapter(conversion, mockQueryVisitor, mockQueryFactory);

        // When
        adapter.update(mockConnection, first, second);

        // Then
        verify(mockConnection, never()).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldPerformUpdate() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.OAUTH);
        Token second = new Token("badger", TokenType.OAUTH);

        Connection mockConnection = mock(Connection.class);
        Result successResult = mockSuccessfulResult();
        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null);

        // When
        adapter.update(mockConnection, first, second);

        // Then
        verify(mockConnection).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldQuery() throws Exception {
        // Given
        final QueryBuilder<Connection, Filter> mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willAnswer(new Answer<QueryBuilder<Connection, Filter>>() {
            @Override
            public QueryBuilder<Connection, Filter> answer(InvocationOnMock invocation) throws Throwable {
                return mockBuilder;
            }
        });
        given(mockBuilder.execute(any(Connection.class)))
                .willReturn(Arrays.asList((Collection<Token>) Arrays.asList(new Token("weasel", TokenType.OAUTH))).iterator());
        given(mockQueryFactory.createInstance()).willReturn(mockBuilder);
        QueryFilterVisitor<Filter, Void, CoreTokenField> visitor = mock(QueryFilterVisitor.class);
        given(mockQueryFactory.createFilterConverter()).willReturn(visitor);
        given(visitor.visitBooleanLiteralFilter(null, true)).willReturn(Filter.alwaysTrue());

        // When
        TokenFilter filter = new TokenFilterBuilder().withQuery(QueryFilter.<CoreTokenField>alwaysTrue()).build();
        Collection<Token> result = adapter.query(mockConnection, filter);

        // Then
        verify(mockBuilder).withFilter(any(Filter.class));
        verify(mockBuilder).execute(mockConnection);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next().getTokenId()).isEqualTo("weasel");
    }

    @Test
    public void shouldPartialQuery() throws Exception {
        // Given
        final QueryBuilder<Connection, Filter> mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willAnswer(new Answer<QueryBuilder<Connection, Filter>>() {
            @Override
            public QueryBuilder<Connection, Filter> answer(InvocationOnMock invocation) throws Throwable {
                return mockBuilder;
            }
        });
        given(mockBuilder.returnTheseAttributes(anySetOf(CoreTokenField.class))).willAnswer(new Answer<QueryBuilder<Connection, Filter>>() {
            @Override
            public QueryBuilder<Connection, Filter> answer(InvocationOnMock invocation) throws Throwable {
                return mockBuilder;
            }
        });

        PartialToken partialToken = new PartialToken(new HashMap<CoreTokenField, Object>());
        given(mockBuilder.executeAttributeQuery(any(Connection.class)))
                .willReturn(Arrays.asList((Collection<PartialToken>) Arrays.asList(partialToken)).iterator());
        given(mockQueryFactory.createInstance()).willReturn(mockBuilder);
        QueryFilterVisitor<Filter, Void, CoreTokenField> visitor = mock(QueryFilterVisitor.class);
        given(mockQueryFactory.createFilterConverter()).willReturn(visitor);
        given(visitor.visitBooleanLiteralFilter(null, true)).willReturn(Filter.alwaysTrue());

        // When
        TokenFilter filter = new TokenFilterBuilder()
                .withQuery(QueryFilter.<CoreTokenField>alwaysTrue())
                .returnAttribute(CoreTokenField.STRING_ONE)
                .build();
        Collection<PartialToken> result = adapter.partialQuery(mockConnection, filter);

        // Then
        verify(mockBuilder).withFilter(any(Filter.class));
        verify(mockBuilder).returnTheseAttributes(asSet(CoreTokenField.STRING_ONE));
        verify(mockBuilder).executeAttributeQuery(mockConnection);
        assertThat(result).containsOnly(partialToken);
    }

    private static Result mockSuccessfulResult() {
        Result result = mock(Result.class);
        ResultCode resultCode = ResultCode.SUCCESS;
        given(result.getResultCode()).willReturn(resultCode);
        return result;
    }
}