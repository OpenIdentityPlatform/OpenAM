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
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.impl.SeriesTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TaskDispatcherTest {

    private TaskDispatcher queue;
    private TaskFactory mockTaskFactory;
    private Token mockToken;
    private ResultHandler mockHandler;
    private SeriesTaskExecutor mockExecutor;

    @BeforeMethod
    public void setup() {
        mockTaskFactory = mock(TaskFactory.class);
        mockExecutor = mock(SeriesTaskExecutor.class);
        mockHandler = mock(ResultHandler.class);

        mockToken = mock(Token.class);
        given(mockToken.getTokenId()).willReturn("badger");

        queue = new TaskDispatcher(
                mockTaskFactory,
                mockExecutor);
    }

    @Test
    public void shouldUseConfigurationToInitialise() throws CoreTokenException {
        queue.startDispatcher();
        verify(mockExecutor).start();
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnCreate() throws CoreTokenException {
        queue.create(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullTokenOnUpdate() throws CoreTokenException {
        queue.update(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullStringOnDelete() throws CoreTokenException {
        queue.delete(null, mockHandler);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnRead() throws CoreTokenException {
        queue.read(null, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnQuery() throws CoreTokenException {
        queue.query(null, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldRejectNullArgsOnPartialQuery() throws CoreTokenException {
        queue.partialQuery(null, null);
    }

    @Test
    public void shouldCreate() throws Exception {
        // Given
        Token token = mock(Token.class);
        given(token.getTokenId()).willReturn("123");
        Task task = mock(Task.class);
        given(mockTaskFactory.create(token, mockHandler)).willReturn(task);

        // When
        queue.create(token, mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldUpdate() throws Exception {
        // Given
        Token token = mock(Token.class);
        given(token.getTokenId()).willReturn("123");
        Task task = mock(Task.class);
        given(mockTaskFactory.update(token, mockHandler)).willReturn(task);

        // When
        queue.update(token, mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldDelete() throws Exception {
        // Given
        Task task = mock(Task.class);
        given(mockTaskFactory.delete("123", mockHandler)).willReturn(task);

        // When
        queue.delete("123", mockHandler);

        // Then
        verify(mockExecutor).execute("123", task);
    }

    @Test
    public void shouldRead() throws Exception {
        // Given
        Task task = mock(Task.class);
        given(mockTaskFactory.read("123", mockHandler)).willReturn(task);

        // When
        queue.read("123", mockHandler);

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