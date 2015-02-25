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
package org.forgerock.openam.cts.monitoring.impl.queue;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.ResultHandler;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.eq;

public class DefaultMonitoringResultHandlerTest {

    private ResultHandler mockResultHandler;
    private CTSOperationsMonitoringStore mockStore;
    private DefaultMonitoringResultHandler handler;
    private CTSOperation operation;

    @BeforeMethod
    public void setup() {
        mockResultHandler = mock(ResultHandler.class);
        mockStore = mock(CTSOperationsMonitoringStore.class);
        operation = CTSOperation.LIST;

        handler = new DefaultMonitoringResultHandler(mockResultHandler, mockStore, operation);
    }

    @Test
    public void shouldDeferToHandlerForGetResults() throws CoreTokenException {
        handler.getResults();
        verify(mockResultHandler).getResults();
    }

    @Test
    public void shouldCallStoreOnProcessResults() {
        handler.processResults("");
        verify(mockStore).addTokenOperation((Token) eq(null), eq(operation), eq(true));
    }

    @Test
    public void shouldCallStoreOnProcessError() {
        handler.processError(mock(CoreTokenException.class));
        verify(mockStore).addTokenOperation((Token) eq(null), eq(operation), eq(false));
    }
}