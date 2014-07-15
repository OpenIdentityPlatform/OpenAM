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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.reaper;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.reaper.ReaperQuery;
import org.forgerock.openam.cts.impl.query.reaper.ReaperQueryFactory;
import org.forgerock.openam.cts.monitoring.CTSReaperMonitoringStore;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class CTSReaperTest {
    private CTSReaper reaper;
    private TokenDeletion mockTokenDeletion;
    private CTSReaperMonitoringStore monitoringStore;
    private ReaperQueryFactory mockQueryFactory;
    private ReaperQuery mockQuery;

    @BeforeMethod
    public void setUp() throws Exception {
        mockTokenDeletion = mock(TokenDeletion.class);
        monitoringStore = mock(CTSReaperMonitoringStore.class);

        mockQuery = mock(ReaperQuery.class);
        mockQueryFactory = mock(ReaperQueryFactory.class);
        given(mockQueryFactory.getQuery()).willReturn(mockQuery);

        reaper = new CTSReaper(mockQueryFactory, mockTokenDeletion, monitoringStore, mock(Debug.class));
    }

    @AfterMethod
    public void tearDown() {
        // Clear the interrupt status.
        Thread.interrupted();
    }

    @Test
    public void shouldUseReaperQueryForNextPage() throws CoreTokenException {
        given(mockQuery.nextPage()).willReturn(null);
        reaper.run();
        verify(mockQuery).nextPage();
    }

    @Test
    public void shouldSignalTokensToTokenDeletion() throws CoreTokenException {
        // Given
        Collection<String> tokens = Arrays.asList("badger", "weasel", "ferret");
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(null);
        given(mockTokenDeletion.deleteBatch(anyCollection())).willReturn(new CountDownLatch(0));

        // When
        reaper.run();

        // Then
        verify(mockTokenDeletion).deleteBatch(eq(tokens));
    }

    @Test
    public void shouldWaitForAllCountDownLatchesBeforeContinuing() throws CoreTokenException, InterruptedException {
        // Given
        CountDownLatch one = mock(CountDownLatch.class);
        CountDownLatch two = mock(CountDownLatch.class);
        CountDownLatch three = mock(CountDownLatch.class);

        Collection<String> tokens = Arrays.asList("badger", "weasel", "ferret");
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(tokens).willReturn(tokens).willReturn(null);

        given(mockTokenDeletion.deleteBatch(anyCollection())).willReturn(one).willReturn(two).willReturn(three);

        // When
        reaper.run();

        // Then
        verify(one).await();
        verify(two).await();
        verify(three).await();
    }

    @Test
    public void shouldRespondToInterruptSignal() throws CoreTokenException {
        // Given
        Collection<String> tokens = Arrays.asList("badger", "weasel", "ferret");
        given(mockQuery.nextPage()).willReturn(tokens).willReturn(null);

        Thread.currentThread().interrupt();

        // When
        reaper.run();

        // Then
        verify(mockTokenDeletion, times(0)).deleteBatch(eq(tokens));
    }
}
