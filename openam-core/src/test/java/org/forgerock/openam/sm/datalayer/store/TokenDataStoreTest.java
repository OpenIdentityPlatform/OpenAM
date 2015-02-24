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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.store;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.PooledTaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.util.query.QueryFilter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class TokenDataStoreTest {

    private TokenDataStore<Object> store;
    private TaskExecutor taskExecutor;
    private TaskFactory taskFactory;
    private JavaBeanAdapter<Object> adapter;

    @BeforeMethod
    public void setup() throws Exception {
        adapter = mock(JavaBeanAdapter.class);
        taskExecutor = mock(PooledTaskExecutor.class);
        taskFactory = mock(TaskFactory.class);
        this.store = new TokenDataStore<Object>(adapter, taskExecutor, taskFactory, mock(Debug.class));
    }

    @Test
    public void testCreate() throws Exception {
        // Given
        Token token = new Token("123", TokenType.GENERIC);
        final Task task = mock(Task.class);
        when(adapter.toToken(anyObject())).thenReturn(token);
        when(taskFactory.create(any(Token.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(new Object());
                return task;
            }
        });

        // When
        store.create(new Object());

        // Then
        verify(taskFactory).create(eq(token), any(ResultHandler.class));
        verify(taskExecutor).execute("123", task);
    }

    @Test(expectedExceptions = ServerException.class)
    public void testCreateError() throws Exception {
        // Given
        Token token = new Token("123", TokenType.GENERIC);
        final Task task = mock(Task.class);
        when(adapter.toToken(anyObject())).thenReturn(token);
        when(taskFactory.create(any(Token.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processError(new Exception());
                return task;
            }
        });

        // When
        store.create(new Object());

        // Then - exception;
    }

    @Test(expectedExceptions = ServerException.class)
    public void testCreateExecutorError() throws Exception {
        // Given
        Token token = new Token("123", TokenType.GENERIC);
        final Task task = mock(Task.class);
        when(adapter.toToken(anyObject())).thenReturn(token);
        when(taskFactory.create(any(Token.class), any(ResultHandler.class))).thenReturn(task);
        doThrow(DataLayerException.class).when(taskExecutor).execute("123", task);

        // When
        store.create(new Object());

        // Then - exception;
    }

    @Test
    public void testRead() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task task = mock(Task.class);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Task>() {
            @Override
            public Task answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return task;
            }
        });

        // When
        Object result = store.read("123");

        // Then
        verify(taskFactory).read(eq("123"), any(ResultHandler.class));
        verify(taskExecutor).execute("123", task);
        assertThat(result).isSameAs(returned);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testReadNotExisting() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task task = mock(Task.class);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Task>() {
            @Override
            public Task answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(null);
                return task;
            }
        });

        // When
        store.read("123");
    }

    @Test(expectedExceptions = ServerException.class)
    public void testReadError() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task task = mock(Task.class);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Task>() {
            @Override
            public Task answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processError(new Exception());
                return task;
            }
        });

        // When
        store.read("123");
    }

    @Test(expectedExceptions = ServerException.class)
    public void testReadExecutorError() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task task = mock(Task.class);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Task>() {
            @Override
            public Task answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return task;
            }
        });
        doThrow(DataLayerException.class).when(taskExecutor).execute(anyString(), any(Task.class));

        // When
        store.read("123");
    }

    @Test
    public void testUpdate() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        final Task readTask = mock(Task.class);
        final Task updateTask = mock(Task.class);
        when(adapter.toToken(anyObject())).thenReturn(token);
        when(taskFactory.read(eq("123"), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return readTask;
            }
        });
        when(taskFactory.update(any(Token.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return updateTask;
            }
        });

        // When
        store.update(new Object());

        // Then
        verify(taskFactory).update(eq(token), any(ResultHandler.class));
        verify(taskExecutor).execute("123", readTask);
        verify(taskExecutor).execute("123", updateTask);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testUpdateNotExisting() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        final Task readTask = mock(Task.class);
        when(adapter.toToken(anyObject())).thenReturn(token);
        when(taskFactory.read(eq("123"), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(null);
                return readTask;
            }
        });

        // When
        store.update(new Object());

        // Then - exception
    }

    @Test(expectedExceptions = ServerException.class)
    public void testUpdateError() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task readTask = mock(Task.class);
        final Task updateTask = mock(Task.class);
        when(adapter.toToken(any())).thenReturn(token);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(eq("123"), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return readTask;
            }
        });
        when(taskFactory.update(any(Token.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processError(new Exception());
                return updateTask;
            }
        });

        // When
        store.update(new Object());

        // Then - exception
    }

    @Test(expectedExceptions = ServerException.class)
    public void testUpdateExecutorError() throws Exception {
        // Given
        final Token token = new Token("123", TokenType.GENERIC);
        Object returned = new Object();
        final Task readTask = mock(Task.class);
        final Task updateTask = mock(Task.class);
        when(adapter.toToken(any())).thenReturn(token);
        when(adapter.fromToken(token)).thenReturn(returned);
        when(taskFactory.read(eq("123"), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return readTask;
            }
        });
        when(taskFactory.update(any(Token.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(token);
                return updateTask;
            }
        });
        doThrow(DataLayerException.class).when(taskExecutor).execute(anyString(), eq(updateTask));

        // When
        store.update(new Object());

        // Then - exception
    }

    @Test
    public void testDelete() throws Exception {
        // Given
        final Task task = mock(Task.class);
        when(taskFactory.delete(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(new Object());
                return task;
            }
        });

        // When
        store.delete("123");

        // Then
        verify(taskFactory).delete(eq("123"), any(ResultHandler.class));
        verify(taskExecutor).execute("123", task);
    }

    @Test(expectedExceptions = ServerException.class)
    public void testDeleteError() throws Exception {
        // Given
        final Task task = mock(Task.class);
        when(taskFactory.delete(anyString(), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processError(new Exception());
                return task;
            }
        });

        // When
        store.delete("123");

        // Then - exception
    }

    @Test(expectedExceptions = ServerException.class)
    public void testDeleteExecutorError() throws Exception {
        // Given
        final Task task = mock(Task.class);
        when(taskFactory.delete(anyString(), any(ResultHandler.class))).thenReturn(task);
        doThrow(DataLayerException.class).when(taskExecutor).execute("123", task);

        // When
        store.delete("123");

        // Then - exception
    }

    @Test
    public void testQuery() throws Exception {
        // Given
        QueryFilter<String> query = QueryFilter.alwaysTrue();
        TokenFilter tokenFilter = mock(TokenFilter.class);
        when(adapter.toTokenQuery(query)).thenReturn(tokenFilter);
        final Token token1 = new Token("123", TokenType.GENERIC);
        final Token token2 = new Token("456", TokenType.GENERIC);
        Object o1 = new Object();
        Object o2 = new Object();
        when(adapter.fromToken(token1)).thenReturn(o1);
        when(adapter.fromToken(token2)).thenReturn(o2);
        final Task task = mock(Task.class);
        when(taskFactory.query(any(TokenFilter.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processResults(asSet(token1, token2));
                return task;
            }
        });

        // When
        Set<Object> result = store.query(query);

        // Then
        verify(taskFactory).query(eq(tokenFilter), any(ResultHandler.class));
        verify(taskExecutor).execute(null, task);
        assertThat(result).containsOnly(o1, o2);
    }

    @Test(expectedExceptions = ServerException.class)
    public void testQueryError() throws Exception {
        // Given
        QueryFilter<String> query = QueryFilter.alwaysTrue();
        TokenFilter tokenFilter = mock(TokenFilter.class);
        when(adapter.toTokenQuery(query)).thenReturn(tokenFilter);
        final Token token1 = new Token("123", TokenType.GENERIC);
        final Token token2 = new Token("456", TokenType.GENERIC);
        Object o1 = new Object();
        Object o2 = new Object();
        when(adapter.fromToken(token1)).thenReturn(o1);
        when(adapter.fromToken(token2)).thenReturn(o2);
        final Task task = mock(Task.class);
        when(taskFactory.query(any(TokenFilter.class), any(ResultHandler.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ResultHandler) invocation.getArguments()[1]).processError(new Exception());
                return task;
            }
        });

        // When
        store.query(query);

        // Then - exception;
    }

    @Test(expectedExceptions = ServerException.class)
    public void testQueryExecutorError() throws Exception {
        // Given
        QueryFilter<String> query = QueryFilter.alwaysTrue();
        TokenFilter tokenFilter = mock(TokenFilter.class);
        when(adapter.toTokenQuery(query)).thenReturn(tokenFilter);
        final Token token1 = new Token("123", TokenType.GENERIC);
        final Token token2 = new Token("456", TokenType.GENERIC);
        Object o1 = new Object();
        Object o2 = new Object();
        when(adapter.fromToken(token1)).thenReturn(o1);
        when(adapter.fromToken(token2)).thenReturn(o2);
        final Task task = mock(Task.class);
        when(taskFactory.query(any(TokenFilter.class), any(ResultHandler.class))).thenReturn(task);
        doThrow(DataLayerException.class).when(taskExecutor).execute(null, task);

        // When
        store.query(query);

        // Then - exception;
    }
}