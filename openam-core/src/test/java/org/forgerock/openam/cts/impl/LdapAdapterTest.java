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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.cts.api.CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION;
import static org.forgerock.openam.cts.api.CTSOptions.PRE_DELETE_READ_OPTION;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.opendj.ldap.controls.PostReadResponseControl.newControl;
import static org.mockito.BDDMockito.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import org.forgerock.opendj.ldap.controls.PostReadRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadResponseControl;
import org.forgerock.opendj.ldap.controls.PreReadRequestControl;
import org.forgerock.opendj.ldap.controls.PreReadResponseControl;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.util.Option;
import org.forgerock.util.Options;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;
import org.forgerock.util.time.Duration;
import org.mockito.ArgumentCaptor;
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
    private Map<Option<?>, LdapOptionFunction> optionFunctionMap;

    @BeforeMethod
    private void setup() throws Exception {
        mockConnection = mock(Connection.class);
        mockConversion = mock(LdapTokenAttributeConversion.class);
        mockQueryFactory = mock(LdapQueryFactory.class);
        mockQueryVisitor = mock(LdapQueryFilterVisitor.class);
        mockConnectionFactory = mock(ConnectionFactory.class);
        mockConnectionFactoryProvider = mock(LdapConnectionFactoryProvider.class);
        optionFunctionMap = new HashMap<>();

        given(mockConnectionFactoryProvider.createFactory()).willReturn(mockConnectionFactory);
        given(mockConnectionFactory.create()).willReturn(mockConnection);
        given(mockConnectionFactory.isValid(mockConnection)).willReturn(false);

        adapter = new LdapAdapter(mockConversion, mockQueryVisitor, mockQueryFactory, mockConnectionFactoryProvider,
                optionFunctionMap);
    }

    @Test
    public void shouldRenewConnection() throws Exception {
        //given
        Token token = new Token("badger", TokenType.SESSION);
        Options options = Options.defaultOptions();

        Connection mockConnection2 = mock(Connection.class);

        when(mockConnectionFactory.create()).thenReturn(mockConnection).thenReturn(mockConnection2);

        Result successResult = mockSuccessfulResult();
        given(mockConnection2.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        //when
        adapter.create(token, options); // first call creates first connection and uses
        adapter.create(token, options); // second call fails validation of first connection, creates second

        //then
        verify(mockConnection2, times(1)).add(any(AddRequest.class));
        verify(mockConnection, times(1)).add(any(AddRequest.class));
        verify(mockConnection, times(1)).close();
    }

    @Test
    public void shouldUseConnectionForCreate() throws Exception {
        // Given
        Token token = new Token("badger", TokenType.SESSION);
        Options options = Options.defaultOptions();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);

        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        // When
        adapter.create(token, options);

        // Then
        verify(mockConnection).add(any(AddRequest.class));
    }

    @Test
    public void shouldUseConnectionForRead() throws Exception {
        // Given
        String tokenId = "badger";
        Options options = Options.defaultOptions();
        DN testDN = DN.rootDN();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.read(tokenId, options);

        // Then
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(mockConnection).searchSingleEntry(captor.capture());
        assertEquals(testDN, captor.getValue().getName());
    }

    @Test
    public void shouldReturnNullWhenObjectNotFound() throws Exception {
        // Given
        String tokenId = "badger";
        Options options = Options.defaultOptions();
        DN testDN = DN.rootDN();
        SearchRequest request = LDAPRequests.newSingleEntrySearchRequest(testDN);

        LdapException exception = LdapException.newLdapException(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.searchSingleEntry(request)).willThrow(exception);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        Token result = adapter.read(tokenId, options);

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
        adapter.delete(tokenId, Options.defaultOptions());

        // Then
        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(mockConnection).delete(captor.capture());
        assertEquals(testDN, captor.getValue().getName());
    }

    @Test
    public void shouldAskForETagOnCreate() throws Exception {
        //Given
        Token token = new Token("badger", TokenType.SESSION);
        Options options = Options.defaultOptions();

        Result successResult = mockSuccessfulResult();
        given(mockConnection.add(any(AddRequest.class))).willReturn(successResult);
        given(mockConversion.getEntry(any(Token.class))).willReturn(mock(Entry.class));

        //When
        adapter.create(token, options);

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
        Options options = Options.defaultOptions();
        DN testDN = DN.rootDN();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);

        // When
        adapter.read(tokenId, options);

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
        Options options = Options.defaultOptions();

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider, optionFunctionMap);

        // When
        adapter.update(first, second, options);

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
        Options options = Options.defaultOptions()
                .set(OPTIMISTIC_CONCURRENCY_CHECK_OPTION, "ETAG");
        optionFunctionMap.put(OPTIMISTIC_CONCURRENCY_CHECK_OPTION, new ETagAssertionCTSOptionFunction());

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider, optionFunctionMap);

        // When
        adapter.update(first, second, options);

        // Then
        ArgumentCaptor<ModifyRequest> requestCaptor = ArgumentCaptor.forClass(ModifyRequest.class);
        verify(mockConnection).modify(requestCaptor.capture());

        AssertionRequestControl assertionRequestControl =
                requestCaptor.getValue().getControl(AssertionRequestControl.DECODER, new DecodeOptions());
        assertThat(assertionRequestControl).isNotNull();
        assertThat(assertionRequestControl.getFilter().toString()).isEqualTo("(etag=ETAG)");
    }

    @Test
    public void shouldAddPreReadRequestControlWhenRequested() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();
        CoreTokenField[] preReadAttributes = new CoreTokenField[]{CoreTokenField.STRING_ONE};

        Result successResult = mockSuccessfulResult();
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);
        optionFunctionMap.put(PRE_DELETE_READ_OPTION, new DeletePreReadOptionFunction());

        // When
        adapter.delete(tokenId, Options.defaultOptions().set(PRE_DELETE_READ_OPTION, preReadAttributes));

        // Then
        ArgumentCaptor<DeleteRequest> requestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(mockConnection).delete(requestCaptor.capture());

        PreReadRequestControl preReadRequestControl =
                requestCaptor.getValue().getControl(PreReadRequestControl.DECODER, new DecodeOptions());
        assertThat(preReadRequestControl).isNotNull();
        assertThat(preReadRequestControl.getAttributes()).containsOnly(CoreTokenField.TOKEN_ID.toString(),
                CoreTokenField.TOKEN_TYPE.toString(), CoreTokenField.STRING_ONE.toString());
    }

    @Test
    public void shouldPerformPreReadWhenRequested() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();
        Result successResult = mockSuccessfulResult();
        CoreTokenField[] preReadAttributes = new CoreTokenField[0];
        PreReadResponseControl preReadResponseControl = PreReadResponseControl.newControl(mock(Entry.class));
        Token preReadToken = new Token(tokenId, TokenType.SESSION);
        preReadToken.setAttribute(CoreTokenField.STRING_ONE, "STRING_ONE_VALUE");

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);
        optionFunctionMap.put(PRE_DELETE_READ_OPTION, new DeletePreReadOptionFunction());
        given(mockConversion.tokenFromEntry(any(Entry.class))).willReturn(preReadToken);

        given(successResult.getControl(eq(PreReadResponseControl.DECODER), any(DecodeOptions.class))).willReturn(preReadResponseControl);

        // When
        PartialToken partialToken = adapter.delete(tokenId, Options.defaultOptions().set(PRE_DELETE_READ_OPTION, preReadAttributes));

        // Then
        assertThat(partialToken).isNotNull();
        assertThat(partialToken.getFields()).containsOnly(CoreTokenField.TOKEN_ID, CoreTokenField.TOKEN_TYPE, CoreTokenField.STRING_ONE);
    }

    @Test
    public void shouldReturnPartialTokenWithTokenIdWhenNotPerformingPreRead() throws Exception {
        // Given
        String tokenId = "badger";
        DN testDN = DN.rootDN();
        Result successResult = mockSuccessfulResult();

        given(mockConversion.generateTokenDN(anyString())).willReturn(testDN);
        given(mockConnection.delete(any(DeleteRequest.class))).willReturn(successResult);

        // When
        PartialToken partialToken = adapter.delete(tokenId, Options.defaultOptions());

        // Then
        assertThat(partialToken).isNotNull();
        assertThat(partialToken.getFields()).containsOnly(CoreTokenField.TOKEN_ID);
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
        optionFunctionMap.put(OPTIMISTIC_CONCURRENCY_CHECK_OPTION, new ETagAssertionCTSOptionFunction());

        // When
        adapter.delete(tokenId, Options.defaultOptions().set(OPTIMISTIC_CONCURRENCY_CHECK_OPTION, etag));

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
        Options options = Options.defaultOptions();

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider, optionFunctionMap);

        // When
        adapter.update(first, second, options);

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
        adapter.delete(tokenId, Options.defaultOptions());

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
        adapter.delete(tokenId, Options.defaultOptions());
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
            adapter.delete(tokenId, Options.defaultOptions());
            fail();
        } catch (LdapOperationFailedException e) {}
    }

    @Test
    public void shouldDoNothingIfNoModificaitonsOnUpdate() throws Exception {
        // Given
        String tokenId = "badger";
        Token first = new Token(tokenId, TokenType.OAUTH);
        Token second = new Token(tokenId, TokenType.OAUTH);
        Options options = Options.defaultOptions();

        Connection mockConnection = mock(Connection.class);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        adapter = new LdapAdapter(conversion, mockQueryVisitor, mockQueryFactory, mockConnectionFactoryProvider,
                optionFunctionMap);

        // When
        adapter.update(first, second, options);

        // Then
        verify(mockConnection, never()).modify(any(ModifyRequest.class));
    }

    @Test
    public void shouldPerformUpdate() throws Exception {
        // Given
        Token first = new Token("weasel", TokenType.OAUTH);
        Token second = new Token("badger", TokenType.OAUTH);
        Options options = Options.defaultOptions();

        Result successResult = mockSuccessfulResult();

        given(mockConnection.modify(any(ModifyRequest.class))).willReturn(successResult);

        LdapDataLayerConfiguration config = mock(LdapDataLayerConfiguration.class);
        when(config.getTokenStoreRootSuffix()).thenReturn(DN.valueOf("ou=unit-test"));
        LDAPDataConversion dataConversion = new LDAPDataConversion();
        LdapTokenAttributeConversion conversion = new LdapTokenAttributeConversion(dataConversion, config);
        LdapAdapter adapter = new LdapAdapter(conversion, null, null, mockConnectionFactoryProvider,
                optionFunctionMap);

        // When
        adapter.update(first, second, options);

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
        PostReadResponseControl control = newControl(entry);
        Attribute attribute = mock(Attribute.class);
        ResultCode resultCode = ResultCode.SUCCESS;
        given(result.addControl(any(PostReadRequestControl.class))).willReturn(result);
        given(result.getResultCode()).willReturn(resultCode);
        given(result.getControl(eq(PostReadResponseControl.DECODER), any(DecodeOptions.class))).willReturn(control);
        given(entry.getAttribute(CoreTokenField.ETAG.toString())).willReturn(attribute);
        given(attribute.firstValueAsString()).willReturn(RandomStringUtils.random(4));
        return result;
    }
}