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
 * Copyright 2013-2014 ForgeRock AS
 */
package org.forgerock.openam.cts.impl;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.TokenType;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.PartialToken;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;
import org.forgerock.openam.cts.reaper.CTSReaperInit;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

public class CoreTokenAdapterTest {
    protected CoreTokenAdapter adapter;

    private TaskDispatcher mockTaskDispatcher;
    private ResultHandlerFactory mockResultHandlerFactory;
    private Debug mockDebug;
    private CTSReaperInit mockReaperInit;

    @BeforeMethod
    public void setup() {
        mockTaskDispatcher = mock(TaskDispatcher.class);
        mockResultHandlerFactory = mock(ResultHandlerFactory.class);
        mockReaperInit = mock(CTSReaperInit.class);
        mockDebug = mock(Debug.class);

        adapter = new CoreTokenAdapter(
                mockTaskDispatcher,
                mockResultHandlerFactory,
                mockReaperInit,
                mockDebug);
    }

    @Test
    public void shouldCreateToken() throws ErrorResultException, CoreTokenException {
        // Given
        Token token = new Token("badger", TokenType.SESSION);

        // When
        adapter.create(token);

        // Then
        verify(mockTaskDispatcher).create(eq(token), any(ResultHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadToken() throws CoreTokenException, ErrorResultException {
        // Given
        String tokenId = "badger";
        Token token = new Token(tokenId, TokenType.SESSION);

        ResultHandler<Token> mockResultHandler = mock(ResultHandler.class);
        given(mockResultHandler.getResults()).willReturn(token);
        given(mockResultHandlerFactory.getReadHandler()).willReturn(mockResultHandler);

        // When
        Token result = adapter.read(tokenId);

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

        ResultHandler<Collection<Token>> mockResultHandler = mock(ResultHandler.class);
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

        ResultHandler<Collection<PartialToken>> mockResultHandler = mock(ResultHandler.class);
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
        adapter.updateOrCreate(token);

        // Then
        verify(mockTaskDispatcher).update(eq(token), any(ResultHandler.class));
    }

    @Test
    public void shouldPerformDelete() throws CoreTokenException {
        // Given
        String tokenId = "badger";

        // When
        adapter.delete(tokenId);

        // Then
        verify(mockTaskDispatcher).delete(eq(tokenId), any(ResultHandler.class));
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
