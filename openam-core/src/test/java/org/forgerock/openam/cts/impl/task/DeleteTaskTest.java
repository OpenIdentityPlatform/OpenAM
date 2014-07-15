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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.task;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.LDAPOperationFailedException;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.*;

public class DeleteTaskTest {
    private DeleteTask task;
    private Connection mockConnection;
    private LDAPAdapter mockAdapter;
    private String tokenId;
    private ResultHandler<String> mockResultHandler;

    @BeforeMethod
    public void setup() {
        tokenId = "badger";
        mockAdapter = mock(LDAPAdapter.class);
        mockConnection = mock(Connection.class);
        mockResultHandler = mock(ResultHandler.class);

        task = new DeleteTask(tokenId, mockResultHandler);
    }

    @Test
    public void shouldUseAdapterForDelete() throws CoreTokenException, ErrorResultException {
        task.execute(mockConnection, mockAdapter);
        verify(mockAdapter).delete(any(Connection.class), eq(tokenId));
    }

    @Test (expectedExceptions = CoreTokenException.class)
    public void shouldHandleException() throws ErrorResultException, CoreTokenException {
        doThrow(new LDAPOperationFailedException("test"))
                .when(mockAdapter).delete(any(Connection.class), anyString());
        task.execute(mockConnection, mockAdapter);
        verify(mockResultHandler).processError(any(CoreTokenException.class));
    }

    @Test
    public void shouldNotifyResultHandlerOnSuccess() throws CoreTokenException, ErrorResultException {
        task.execute(mockConnection, mockAdapter);
        verify(mockResultHandler).processResults(eq(tokenId));
    }
}