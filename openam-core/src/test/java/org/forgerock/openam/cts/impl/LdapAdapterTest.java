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
 */

package org.forgerock.openam.cts.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang.RandomStringUtils;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.openam.cts.utils.LdapTokenAttributeConversion;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.api.query.QueryBuilder;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapDataLayerConfiguration;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFactory;
import org.forgerock.openam.sm.datalayer.impl.ldap.LdapQueryFilterVisitor;
import org.forgerock.openam.sm.datalayer.providers.LdapConnectionFactoryProvider;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.controls.AssertionRequestControl;
import org.forgerock.opendj.ldap.controls.ControlDecoder;
import org.forgerock.opendj.ldap.controls.PostReadRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadResponseControl;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.forgerock.util.time.Duration;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LdapAdapterTest {

    private Connection mockConnection;
    private LdapQueryFactory mockQueryFactory;
    private LdapQueryFilterVisitor mockQueryVisitor;
    private LdapAdapter adapter;
    private LdapTokenAttributeConversion mockConversion;
    private ConnectionFactory mockConnectionFactory;
    private LdapConnectionFactoryProvider mockConnectionFactoryProvider;

    @BeforeMethod
    private void setup() throws Exception {
        mockConnection = mock(Connection.class);
        mockConversion = mock(LdapTokenAttributeConversion.class);
        mockQueryFactory = mock(LdapQueryFactory.class);
        mockQueryVisitor = mock(LdapQueryFilterVisitor.class);
        mockConnectionFactory = mock(ConnectionFactory.class);
        mockConnectionFactoryProvider = mock(LdapConnectionFactoryProvider.class);

        given(mockConnectionFactoryProvider.createFactory()).willReturn(mockConnectionFactory);
        given(mockConnectionFactory.create()).willReturn(mockConnection);
        given(mockConnectionFactory.isValid(mockConnection)).willReturn(false);

        adapter = new LdapAdapter(mockConversion, mockQueryVisitor, mockQueryFactory, mockConnectionFactoryProvider);
    }

    @Test
    public void shouldRenewConnection() throws Exception {
        //given
        Token token = new Token("badger", TokenType.SESSION);

        Connection mockConnection2 = mock(Connection.class);

        when(mockConnectionFactory.create()).thenReturn(mockConnection).thenReturn(mockConnection2);

        Result successResult = mockSuccessfulResult();
        given(mockConnection2.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        //when
        adapter.create(token); // first call creates first connection and uses
        adapter.create(token); // second call fails validation of first connection, creates second

        //then
        verify(mockConnection2, times(1)).add(any(AddRequest.class));
        verify(mockConnection, times(1)).add(any(AddRequest.class));
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void shouldUseConnectionForCreate() throws Exception {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        Result successResult = mockSuccessfulResult();
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);

        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        // When
        adapter.create(token);

        // Then
        verify(mockConnection).add(any(AddRequest.class));
    }

    @Test
    public void shouldUseConnectionForRead() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.read(tokenId);

        // Then
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(mockConnection).searchSingleEntry(captor.capture());
        assertEquals(testDN, captor.getValue().getName());
    }

    @Test
    public void shouldReturnNullWhenObjectNotFound() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();
        SearchRequest request = LDAPRequests.newSingleEntrySearchRequest(testDN);

        LdapException exception = LdapException.newLdapException(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.searchSingleEntry(request)).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        Token result = adapter.read(tokenId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldUseConnectionForDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.delete(tokenId, null);

        // Then
        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(mockConnection).delete(captor.capture());
        assertEquals(testDN, captor.getValue().getName());
    }

    @Test
    public void shouldAskForETagOnCreate() throws Exception {
        //Given
        Token token = new Token("badger", TokenType.SESSION);

        Result successResult = mockSuccessfulResult();
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        //When
        adapter.create(token);

        //Then
        ArgumentCaptor<AddRequest> requestCaptor = ArgumentCaptor.forClass(AddRequest.class);
        verify(mockConnection).add(requestCaptor.capture());

        PostReadRequestControl postReadRequestControl =
                requestCaptor.getValue().getControl(PostReadRequestControl.DECODER, new DecodeOptions());
        assertThat(postReadRequestControl).isNotNull();
        assertThat(postReadRequestControl.getAttributes()).hasSize(1).containsExactly(CoreTokenField.ETAG.toString());
    }

    @Test
    public void shouldAskForETagOnRead() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.read(tokenId);

        //Then
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(mockConnection).searchSingleEntry(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAttributes())
                .hasSize(2)
                .containsExactly("*", CoreTokenField.ETAG.toString());
    }

    @Test
    public void shouldAskForETagOnUpdate() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.SESSION);
        Token second = new Token("badger", TokenType.SESSION);

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider);

        // When
        adapter.update(first, second);

        // Then
        ArgumentCaptor<ModifyRequest> requestCaptor = ArgumentCaptor.forClass(ModifyRequest.class);
        verify(mockConnection).modify(requestCaptor.capture());
        PostReadRequestControl postReadRequestControl =
                requestCaptor.getValue().getControl(PostReadRequestControl.DECODER, new DecodeOptions());
        assertThat(postReadRequestControl).isNotNull();
        assertThat(postReadRequestControl.getAttributes()).hasSize(1).containsExactly(CoreTokenField.ETAG.toString());
    }

    @Test
    public void shouldAddETagAssertionOnUpdate() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.SESSION);
        Token second = new Token("badger", TokenType.SESSION);
        first.setAttribute(CoreTokenField.ETAG, "ETAG");

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider);

        // When
        adapter.update(first, second);

        // Then
        ArgumentCaptor<ModifyRequest> requestCaptor = ArgumentCaptor.forClass(ModifyRequest.class);
        verify(mockConnection).modify(requestCaptor.capture());

        AssertionRequestControl assertionRequestControl =
                requestCaptor.getValue().getControl(AssertionRequestControl.DECODER, new DecodeOptions());
        assertThat(assertionRequestControl).isNotNull();
        assertThat(assertionRequestControl.getFilter().toString()).isEqualTo("(etag=ETAG)");
    }

    @Test
    public void shouldAddETagAssertionOnDelete() throws Exception {
        // Given
        String tokenId = "badger";
        String etag = "ETAG";
        DN testDN = DN.rootDN();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.delete(tokenId, etag);

        // Then
        ArgumentCaptor<DeleteRequest> requestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(mockConnection).delete(requestCaptor.capture());

        AssertionRequestControl assertionRequestControl =
                requestCaptor.getValue().getControl(AssertionRequestControl.DECODER, new DecodeOptions());
        assertThat(assertionRequestControl).isNotNull();
        assertThat(assertionRequestControl.getFilter().toString()).isEqualTo("(etag=ETAG)");
    }

    @Test
    public void shouldNotAddETagAssertionOnUpdateIfETagIsNull() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.SESSION);
        Token second = new Token("badger", TokenType.SESSION);

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider);

        // When
        adapter.update(first, second);

        // Then
        ArgumentCaptor<ModifyRequest> requestCaptor = ArgumentCaptor.forClass(ModifyRequest.class);
        verify(mockConnection).modify(requestCaptor.capture());

        AssertionRequestControl assertionRequestControl =
                requestCaptor.getValue().getControl(AssertionRequestControl.DECODER, new DecodeOptions());
        assertThat(assertionRequestControl).isNull();
    }

    @Test
    public void shouldNotAddETagAssertionOnDeleteIfETagIsNull() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.delete(tokenId, null);

        // Then
        ArgumentCaptor<DeleteRequest> requestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(mockConnection).delete(requestCaptor.capture());

        AssertionRequestControl assertionRequestControl =
                requestCaptor.getValue().getControl(AssertionRequestControl.DECODER, new DecodeOptions());
        assertThat(assertionRequestControl).isNull();
    }

    @Test
    public void shouldDoNothingIfObjectNotFoundDuringDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        LdapException exception = LdapException.newLdapException(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.delete(any(DeleteRequest.class))).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When / Then
        adapter.delete(tokenId, null);
    }

    @Test
    public void shouldThrowAllOtherExceptionsDuringDelete() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();

        LdapException exception = LdapException.newLdapException(ResultCode.OTHER);
        given(mockConnection.delete(any(DeleteRequest.class))).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When / Then
        try {
            adapter.delete(tokenId, null);
            fail();
        } catch (LdapOperationFailedException e) {}
    }

    @Test
    public void shouldDoNothingIfNoModificaitonsOnUpdate() throws Exception {
        // Given
        String tokenId = "badger";
        Token first = new Token(tokenId, TokenType.OAUTH);
        Token second = new Token(tokenId, TokenType.OAUTH);

        Connection mockConnection = mock(Connection.class);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        adapter = new LdapAdapter(conversion, mockQueryVisitor, mockQueryFactory, mockConnectionFactoryProvider);

        // When
        adapter.update(first, second);

        // Then
        verify(mockConnection, never()).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldPerformUpdate() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.OAUTH);
        Token second = new Token("badger", TokenType.OAUTH);

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider);

        // When
        adapter.update(first, second);

        // Then
        verify(mockConnection).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldQuery() throws Exception {
        // Given
        final QueryBuilder<Connection, Filter> mockBuilder = mock(QueryBuilder.class);
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(anySetOf(CoreTokenField.class))).willReturn(mockBuilder);
        given(mockBuilder.limitResultsTo(anyInt())).willReturn(mockBuilder);
        given(mockBuilder.within(any(Duration.class))).willReturn(mockBuilder);
        given(mockBuilder.execute(any(Connection.class)))
                .willReturn(Arrays.asList((Collection<Token>) Arrays.asList(new Token("weasel", TokenType.OAUTH))).iterator());
        given(mockQueryFactory.createInstance()).willReturn(mockBuilder);
        QueryFilterVisitor<Filter, Void, CoreTokenField> visitor = mock(QueryFilterVisitor.class);
        given(mockQueryFactory.createFilterConverter()).willReturn(visitor);
        given(visitor.visitBooleanLiteralFilter(null, true)).willReturn(Filter.alwaysTrue());

        // When
        TokenFilter filter = new TokenFilterBuilder().withQuery(QueryFilter.<CoreTokenField>alwaysTrue()).build();
        Collection<Token> result = adapter.query(filter);

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
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(anySetOf(CoreTokenField.class))).willReturn(mockBuilder);
        given(mockBuilder.limitResultsTo(anyInt())).willReturn(mockBuilder);
        given(mockBuilder.within(any(Duration.class))).willReturn(mockBuilder);

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
        Collection<PartialToken> result = adapter.partialQuery(filter);

        // Then
        verify(mockBuilder).withFilter(any(Filter.class));
        verify(mockBuilder).returnTheseAttributes(asSet(CoreTokenField.STRING_ONE));
        verify(mockBuilder).executeAttributeQuery(mockConnection);
        assertThat(result).containsOnly(partialToken);
    }

    private static Result mockSuccessfulResult() throws DecodeException {
        Result result = mock(Result.class);
        Entry entry = mock(Entry.class);
        PostReadResponseControl control = PostReadResponseControl.newControl(entry);
        Attribute attribute = mock(Attribute.class);
        ResultCode resultCode = ResultCode.SUCCESS;
        given(result.addControl(any(PostReadRequestControl.class))).willReturn(result);
        given(result.getResultCode()).willReturn(resultCode);
        given(result.getControl(Matchers.<ControlDecoder<PostReadResponseControl>>any(),
                any(DecodeOptions.class))).willReturn(control);
        given(entry.getAttribute(CoreTokenField.ETAG.toString())).willReturn(attribute);
        given(attribute.firstValueAsString()).willReturn(RandomStringUtils.random(4));
        return result;
    }
}