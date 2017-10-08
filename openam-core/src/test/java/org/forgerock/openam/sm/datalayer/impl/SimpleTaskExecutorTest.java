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

import static org.mockito.Mockito.*;

import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TokenStorageAdapter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class SimpleTaskExecutorTest {

    private SimpleTaskExecutor executor;
    private TokenStorageAdapter adapter;

    @BeforeMethod
    public void setup() throws Exception {
        adapter = mock(TokenStorageAdapter.class);
        executor = new SimpleTaskExecutor(mock(Debug.class), adapter);
    }

    @Test
    public void testExecute() throws Exception {
        // given
        Task task = mock(Task.class);
        executor.start();

        // when
        executor.execute(null, task);

        // then
        verify(task).execute(adapter);
    }

}