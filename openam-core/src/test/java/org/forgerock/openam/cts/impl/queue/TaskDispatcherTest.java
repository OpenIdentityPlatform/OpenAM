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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.queue;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

import java.util.Collection;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.util.Options;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TaskDispatcherTest {

    private TaskDispatcher queue;
    private TaskFactory mockTaskFactory;
    private Token mockToken;
    private Options options;
    private ResultHandler mockHandler;
    private SeriesTaskExecutor mockExecutor;

    @BeforeMethod
    public void setup() {
        mockTaskFactory = mock(TaskFactory.class);
        mockExecutor = mock(SeriesTaskExecutor.class);
        mockHandler = mock(ResultHandler.class);

        mockToken = mock(Token.class);
        given(mockToken.getTokenId()).willReturn("badger");
        options = Options.defaultOptions();

        queue = new TaskDispatcher(
                mockTaskFactory,
                mockExecutor);
    }

    @Test
    public void shouldUseConfigurationToInitialise() throws CoreTokenException {
        queue.startDispatcher();
        verify(mockExecutor).start();
    }

    @DataProvider
    private Object[][] rejectNullsOnCreateOrUpdate() {
        return new Object[][]{
            {null, options, mockHandler},
            {mockToken, null, mockHandler},
            {mockToken, options, null},
        };
    }

    @DataProvider
    private Object[][] rejectNullsOnDelete() {
        return new Object[][]{
                {null, mockHandler},
                {"tokenId", null},
        };
    }

    @DataProvider
    private Object[][] rejectNullsOnRead() {
        return new Object[][]{
                {null, options, mockHandler},
                {"tokenId", null, mockHandler},
                {"tokenId", options, null},
        };
    }

    @DataProvider
    private Object[][] rejectNullsOnQuery() {
        TokenFilter filter = mock(TokenFilter.class);
        return new Object[][]{
                {null, mockHandler},
                {filter, null},
        };
    }

    @Test (dataProvider = "rejectNullsOnCreateOrUpdate", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnCreate(Token token, Options options, ResultHandler<Token, ?> handler)
            throws CoreTokenException {
        queue.create(token, options, handler);
    }

    @Test (dataProvider = "rejectNullsOnCreateOrUpdate", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnUpdate(Token token, Options options, ResultHandler<Token, ?> handler)
            throws CoreTokenException {
        queue.update(token, options, handler);
    }

    @Test (dataProvider = "rejectNullsOnDelete", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullStringOnDelete(String tokenId, ResultHandler<PartialToken, ?> handler)
            throws CoreTokenException {
        queue.delete(tokenId, handler);
    }

    @Test (dataProvider = "rejectNullsOnRead", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnRead(String tokenId, Options options, ResultHandler<Token, ?> handler)
            throws CoreTokenException {
        queue.read(tokenId, options, handler);
    }

    @Test (dataProvider = "rejectNullsOnQuery", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnQuery(TokenFilter tokenFilter, ResultHandler<Collection<Token>, ?> handler)
            throws CoreTokenException {
        queue.query(tokenFilter, handler);
    }

    @Test (dataProvider = "rejectNullsOnQuery", expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnPartialQuery(TokenFilter tokenFilter,
            ResultHandler<Collection<PartialToken>, ?> handler) throws CoreTokenException {
        queue.partialQuery(tokenFilter, handler);
    }

    @Test
    public void shouldCreate() throws Exception {
        // Given
        Token token = mock(Token.class);
        given(token.getTokenId()).willReturn("123");
        Task task = mock(Task.class);
        given(mockTaskFactory.create(token, options, mockHandler)).willReturn(task);

        // When
        queue.create(token, options, mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldUpdate() throws Exception {
        // Given
        Token token = mock(Token.class);
        given(token.getTokenId()).willReturn("123");
        Task task = mock(Task.class);
        given(mockTaskFactory.update(token, options, mockHandler)).willReturn(task);

        // When
        queue.update(token, options, mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldDelete() throws Exception {
        // Given
        Task task = mock(Task.class);
        given(mockTaskFactory.delete(eq("123"), any(Options.class), eq(mockHandler))).willReturn(task);

        // When
        queue.delete("123", mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldRead() throws Exception {
        // Given
        Task task = mock(Task.class);
        given(mockTaskFactory.read("123", options, mockHandler)).willReturn(task);

        // When
        queue.read("123", options, mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldQuery() throws Exception {
        // Given
        TokenFilter filter = mock(TokenFilter.class);
        Task task = mock(Task.class);
        given(mockTaskFactory.query(filter, mockHandler)).willReturn(task);

        // When
        queue.query(filter, mockHandler);

        // Then
        verify(mockExecutor).execute(null, task);
    }

    @Test
    public void shouldPartialQuery() throws Exception {
        // Given
        TokenFilter filter = mock(TokenFilter.class);
        Task task = mock(Task.class);
        given(mockTaskFactory.partialQuery(filter, mockHandler)).willReturn(task);

        // When
        queue.partialQuery(filter, mockHandler);

        // Then
        verify(mockExecutor).execute(null, task);
    }

}