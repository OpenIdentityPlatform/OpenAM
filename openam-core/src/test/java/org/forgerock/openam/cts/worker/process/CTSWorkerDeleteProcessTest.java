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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.cts.worker.process;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.forgerock.openam.cts.worker.CTSWorkerFilter;
import org.forgerock.openam.cts.worker.process.CTSWorkerDeleteProcess.TokenDeletion;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class CTSWorkerDeleteProcessTest {

    private CTSWorkerDeleteProcess process;
    private TokenDeletion mockTokenDeletion;
    private CTSReaperMonitoringStore monitoringStore;
    private CTSWorkerQuery mockQuery;
    private CTSWorkerFilter mockFilter = mock(CTSWorkerFilter.class);

    @BeforeMethod
    public void setUp() throws Exception {
        mockTokenDeletion = mock(TokenDeletion.class);
        monitoringStore = mock(CTSReaperMonitoringStore.class);
        mockQuery = mock(CTSWorkerQuery.class);

        process = new CTSWorkerDeleteProcess(mockTokenDeletion, monitoringStore, mock(Debug.class));
    }

    @AfterMethod
    public void tearDown() {
        // Clear the interrupt status.
        Thread.interrupted();
    }

    @Test
    public void shouldSignalTokensToTokenDeletion() throws CoreTokenException {
        // Given
        Collection<PartialToken> tokens = Arrays.asList(partialToken(), partialToken(), partialToken());
        given(mockFilter.filter(anyCollection())).willReturn(tokens);
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(null);
        given(mockTokenDeletion.deleteBatch(anyCollection())).willReturn(new CountDownLatch(0));

        // When
        process.handle(mockQuery, mockFilter);

        // Then
        verify(mockTokenDeletion).deleteBatch(tokens);
    }

    private PartialToken partialToken() {
        return mock(PartialToken.class);
    }

}
