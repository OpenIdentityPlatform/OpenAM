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
 * Copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.impl.tasks;

import static org.forgerock.openam.cts.api.CTSOptions.OPTIMISTIC_CONCURRENCY_CHECK_OPTION;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.util.Options;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateTaskTest {
    private UpdateTask task;
    private LdapAdapter mockAdapter;
    private Token mockPrevious;
    private Token mockUpdated;
    private Token mockReturned;
    private Options options;
    private ResultHandler<Token, ?> mockHandler;

    @BeforeMethod
    public void setup() throws Exception {
        mockUpdated = mock(Token.class);
        mockPrevious = mock(Token.class);
        mockReturned = mock(Token.class);
        mockAdapter = mock(LdapAdapter.class);
        options = Options.defaultOptions().set(OPTIMISTIC_CONCURRENCY_CHECK_OPTION, "ETAG");
        mockHandler = mock(ResultHandler.class);
        task = new UpdateTask(mockUpdated, options, mockHandler);

        given(mockAdapter.read(anyString(), eq(options))).willReturn(mockPrevious);
        given(mockAdapter.update(mockPrevious, mockUpdated, options)).willReturn(mockReturned);
    }

    @Test
    public void shouldUpdateWhenTokenPresent() throws Exception {
        task.execute(mockAdapter);
        verify(mockAdapter).update(eq(mockPrevious), eq(mockUpdated), eq(options));
    }

    @Test
    public void shouldCreateWhenNotPresent() throws Exception {
        given(mockAdapter.read(anyString(), eq(options))).willReturn(null);
        task.execute(mockAdapter);
        verify(mockAdapter, Mockito.times(0)).create(eq(mockUpdated), eq(options));
    }

    @Test (expectedExceptions = DataLayerException.class)
    public void shouldHandleException() throws Exception {
        doThrow(DataLayerException.class).when(mockAdapter).read(anyString(), eq(options));
        task.execute(mockAdapter);
        verify(mockHandler).processError(any(CoreTokenException.class));
    }

    @Test
    public void shouldCallHandlerOnSuccess() throws Exception {
        task.execute(mockAdapter);
        verify(mockHandler).processResults(eq(mockReturned));
    }
}