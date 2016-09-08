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

package org.forgerock.openam.sm.datalayer.impl;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Closeable;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class SimpleTaskExecutorTest {

    private SimpleTaskExecutor<Closeable> executor;
    private TokenStorageAdapter<Closeable> adapter;
    private ConnectionFactory<Closeable> connectionFactory;

    @BeforeMethod
    public void setup() throws Exception {
        connectionFactory = mock(ConnectionFactory.class);
        adapter = mock(TokenStorageAdapter.class);
        executor = new SimpleTaskExecutor<Closeable>(connectionFactory, mock(Debug.class), adapter);
    }

    @Test
    public void testExecute() throws Exception {
        // given
        Task task = mock(Task.class);
        Closeable connection = mock(Closeable.class);
        when(connectionFactory.create()).thenReturn(connection);
        when(connectionFactory.isValid(connection)).thenReturn(true);
        executor.start();

        // when
        executor.execute(null, task);

        // then
        verify(task).execute(connection, adapter);
    }

    @Test
    public void shouldCloseConnection() throws Throwable {
        // given
        Closeable connection = mock(Closeable.class);
        when(connectionFactory.create()).thenReturn(connection);
        executor.start();

        // When
        executor.finalize();

        // Then
        verify(connection).close();
    }

    @Test
    public void shouldCloseConnectionForEachTask() throws Exception {
        // Given
        Closeable connection = mock(Closeable.class);
        given(connectionFactory.create()).willReturn(connection);
        given(connectionFactory.isValid(connection)).willReturn(true, false);
        executor.start();

        // When
        executor.execute(null, mock(Task.class));
        executor.execute(null, mock(Task.class));

        // Then
        verify(connectionFactory, times(2)).create();
        verify(connection, times(1)).close();

    }

}