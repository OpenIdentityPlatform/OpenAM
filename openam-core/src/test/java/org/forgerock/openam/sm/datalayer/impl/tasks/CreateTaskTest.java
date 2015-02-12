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
package org.forgerock.openam.sm.datalayer.impl.tasks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.LdapAdapter;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.LdapOperationFailedException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateTaskTest {

    private CreateTask task;
    private Connection mockConnection;
    private LdapAdapter mockAdapter;
    private Token mockToken;
    private ResultHandler<Token, ?> mockHandler;

    @BeforeMethod
    public void setup() {
        mockToken = mock(Token.class);
        mockAdapter = mock(LdapAdapter.class);
        mockConnection = mock(Connection.class);
        mockHandler = mock(ResultHandler.class);

        task = new CreateTask(mockToken, mockHandler);
    }

    @Test
    public void shouldUseAdapterForCreate() throws Exception {
        task.execute(mockConnection, mockAdapter);
        verify(mockAdapter).create(any(Connection.class), eq(mockToken));
    }

    @Test (expectedExceptions = DataLayerException.class)
    public void shouldHandleException() throws Exception {
        doThrow(new LdapOperationFailedException("test"))
                .when(mockAdapter).create(any(Connection.class), any(Token.class));
        task.execute(mockConnection, mockAdapter);
        verify(mockHandler).processError(any(CoreTokenException.class));
    }

    @Test
    public void shouldUpdateHandlerOnSuccess() throws Exception {
        task.execute(mockConnection, mockAdapter);
        verify(mockHandler).processResults(mockToken);
    }
}