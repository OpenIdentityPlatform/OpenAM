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

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.util.Options;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateTaskTest {

    private CreateTask task;
    private LdapAdapter mockAdapter;
    private Token mockToken;
    private Token mockCreated;
    private Options options;
    private ResultHandler<Token, ?> mockHandler;

    @BeforeMethod
    public void setup() throws DataLayerException {
        mockToken = mock(Token.class);
        mockCreated = mock(Token.class);
        mockAdapter = mock(LdapAdapter.class);
        options = Options.defaultOptions();
        mockHandler = mock(ResultHandler.class);
        given(mockAdapter.create(mockToken, options)).willReturn(mockCreated);

        task = new CreateTask(mockToken, options, mockHandler);
    }

    @Test
    public void shouldUseAdapterForCreate() throws Exception {
        task.execute(mockAdapter);
        verify(mockAdapter).create(eq(mockToken), eq(options));
    }

    @Test (expectedExceptions = DataLayerException.class)
    public void shouldHandleException() throws Exception {
        doThrow(new LdapOperationFailedException("test"))
                .when(mockAdapter).create(any(Token.class), eq(options));
        task.execute(mockAdapter);
        verify(mockHandler).processError(any(CoreTokenException.class));
    }

    @Test
    public void shouldUpdateHandlerOnSuccess() throws Exception {
        task.execute(mockAdapter);
        verify(mockHandler).processResults(mockCreated);
    }
}