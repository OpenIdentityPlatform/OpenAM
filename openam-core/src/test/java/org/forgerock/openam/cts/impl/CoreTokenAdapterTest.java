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
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.Collection;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.utils.blob.TokenBlobStrategy;
import org.forgerock.openam.cts.worker.CTSWorkerManager;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.util.Options;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CoreTokenAdapterTest {
    protected CoreTokenAdapter adapter;

    private TokenBlobStrategy mockStrategy;
    private TaskDispatcher mockTaskDispatcher;
    private ResultHandlerFactory mockResultHandlerFactory;
    private Debug mockDebug;
    private CTSWorkerManager mockReaperInit;
    private Options options;

    @BeforeMethod
    public void setup() {
        mockStrategy = mock(TokenBlobStrategy.class);
        mockTaskDispatcher = mock(TaskDispatcher.class);
        mockResultHandlerFactory = mock(ResultHandlerFactory.class);
        mockReaperInit = mock(CTSWorkerManager.class);
        mockDebug = mock(Debug.class);
        options = Options.defaultOptions();

        adapter = new CoreTokenAdapter(mockStrategy, mockTaskDispatcher, mockResultHandlerFactory, mockReaperInit,
                mockDebug);
    }

    @Test
    public void shouldCreateToken() throws LdapException, CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        // When
        adapter.create(token, options);

        // Then
        verify(mockTaskDispatcher).create(eq(token), any(Options.class), any(ResultHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadToken() throws Exception {
        // Given
        String tokenId = "badger";
        Token token = new Token(tokenId, TokenType.SESSION);
        Options options = Options.defaultOptions();

        ResultHandler<Token, CoreTokenException> mockResultHandler = mock(ResultHandler.class);
        given(mockResultHandler.getResults()).willReturn(token);
        given(mockResultHandlerFactory.getReadHandler()).willReturn(mockResultHandler);

        // When
        Token result = adapter.read(tokenId, options);

        // Then
        assertThat(result.getTokenId()).isEqualTo(tokenId);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldPreventAttributeQueryWhenReturnAttributesAreNotDefined() throws CoreTokenException {
        adapter.attributeQuery(new TokenFilterBuilder().and().build());
    }

    @Test
    public void shouldUseTaskQueueForQuery() throws CoreTokenException {
        // Given
        Collection<Token> tokens = new ArrayList<Token>();

        ResultHandler<Collection<Token>, CoreTokenException> mockResultHandler = mock(ResultHandler.class);
        given(mockResultHandler.getResults()).willReturn(tokens);
        given(mockResultHandlerFactory.getQueryHandler()).willReturn(mockResultHandler);

        TokenFilter filter = new TokenFilterBuilder().and().build();

        // When
        adapter.query(filter);

        // Then
        verify(mockTaskDispatcher).query(eq(filter), eq(mockResultHandler));
    }

    @Test
    public void shouldUseTaskQueueForAttributeQuery() throws CoreTokenException {
        // Given
        Collection<PartialToken> partialTokens = new ArrayList<PartialToken>();

        ResultHandler<Collection<PartialToken>, CoreTokenException> mockResultHandler = mock(ResultHandler.class);
        given(mockResultHandler.getResults()).willReturn(partialTokens);
        given(mockResultHandlerFactory.getPartialQueryHandler()).willReturn(mockResultHandler);

        TokenFilter filter = new TokenFilterBuilder().returnAttribute(CoreTokenField.BLOB).build();

        // When
        adapter.attributeQuery(filter);

        // Then
        verify(mockTaskDispatcher).partialQuery(eq(filter), eq(mockResultHandler));
    }

    @Test
    public void shouldUseTaskQueueForUpdate() throws CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        // When
        adapter.updateOrCreate(token, options);

        // Then
        verify(mockTaskDispatcher).update(eq(token), any(Options.class), any(ResultHandler.class));
    }

    @Test
    public void shouldPerformDelete() throws CoreTokenException {
        // Given
        String tokenId = "badger";

        // When
        adapter.delete(tokenId, options);

        // Then
        verify(mockTaskDispatcher).delete(eq(tokenId), any(Options.class), any(ResultHandler.class));
    }

    @Test
    public void shouldAddTokenIDAsReturnFieldForDeleteOnQuery() throws CoreTokenException {
        // Given
        TokenFilter filter = new TokenFilterBuilder().build();

        // When
        adapter.deleteOnQuery(filter);

        // Then
        ArgumentCaptor<TokenFilter> captor = ArgumentCaptor.forClass(TokenFilter.class);
        verify(mockTaskDispatcher).partialQuery(captor.capture(), any(ResultHandler.class));
        TokenFilter capturedFilter = captor.getValue();
        assertThat(capturedFilter).isSameAs(filter);
        assertThat(capturedFilter.getReturnFields()).containsOnly(CoreTokenField.TOKEN_ID);
    }
}
