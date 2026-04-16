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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerQuery;
import org.forgerock.openam.cts.worker.CTSWorkerFilter;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSWorkerBaseProcessTest {

    private CTSWorkerQuery mockQuery;
    private CTSWorkerFilter mockFilter;
    /** Mock the abstract methods of CTSWorkerBaseProcess to allow the core logic to be tested */
    private CTSWorkerBaseProcess mockProcess;

    @BeforeMethod
    public void setUp() throws Exception {
        mockQuery = mock(CTSWorkerQuery.class);
        mockFilter = mock(CTSWorkerFilter.class);
        mockProcess = mock(CTSWorkerBaseProcess.class);
    }

    @AfterMethod
    public void tearDown() {
        // Clear the interrupt status.
        Thread.interrupted();
    }

    @Test
    public void shouldUseQueryForNextPage() throws CoreTokenException {
        // Given
        given(mockQuery.nextPage()).willReturn(null);

        // When
        mockProcess.handle(mockQuery, mockFilter);

        // Then
        verify(mockQuery).nextPage();
    }

    @Test
    public void shouldWaitForAllCountDownLatchesBeforeContinuing() throws CoreTokenException, InterruptedException {
        // Given
        CountDownLatch one = mock(CountDownLatch.class);
        CountDownLatch two = mock(CountDownLatch.class);
        CountDownLatch three = mock(CountDownLatch.class);

        Collection<PartialToken> tokens = Arrays.asList(partialToken(), partialToken(), partialToken());
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(tokens).willReturn(tokens).willReturn(null);
        given(mockProcess.handleBatch(anyCollection())).willReturn(one).willReturn(two).willReturn(three);

        // When
        mockProcess.handle(mockQuery, mockFilter);

        // Then
        verify(one).await();
        verify(two).await();
        verify(three).await();
    }

    @Test
    public void shouldRespondToInterruptSignal() throws CoreTokenException {
        // Given
        Collection<PartialToken> tokens = Arrays.asList(partialToken(), partialToken(), partialToken());
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(null);

        Thread.currentThread().interrupt();

        // When
        mockProcess.handle(mockQuery, mockFilter);

        // Then
        verify(mockProcess, times(0)).handleBatch(anyCollection());
    }

    private PartialToken partialToken() {
        return mock(PartialToken.class);
    }
}
