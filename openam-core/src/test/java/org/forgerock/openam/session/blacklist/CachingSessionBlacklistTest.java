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

package org.forgerock.openam.session.blacklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.iplanet.dpro.session.Session;
import org.forgerock.util.time.TimeService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CachingSessionBlacklistTest {
    private static final String SID = "session1";
    private static final int CACHE_SIZE = 2;

    @Mock
    private TimeService mockClock;

    @Mock
    private SessionBlacklist mockDelegate;

    @Mock
    private Session mockSession;

    private CachingSessionBlacklist testBlacklist;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testBlacklist = new CachingSessionBlacklist(mockDelegate, CACHE_SIZE, 0, mockClock);

        given(mockSession.getStableStorageID()).willReturn(SID);
    }

    @Test
    public void shouldHitDelegateIfResultNotCached() throws Exception {
        // Given
        given(mockDelegate.isBlacklisted(mockSession)).willReturn(true);

        // When
        boolean result = testBlacklist.isBlacklisted(mockSession);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldCachePositiveResults() throws Exception {
        // Given
        given(mockSession.getTimeLeft()).willReturn(1L);
        given(mockClock.now()).willReturn(0L);
        given(mockDelegate.isBlacklisted(mockSession)).willReturn(true);

        // When
        testBlacklist.isBlacklisted(mockSession); // First call

        // Then
        verify(mockDelegate).isBlacklisted(mockSession);

        // When
        boolean result = testBlacklist.isBlacklisted(mockSession); // Second call - from cache

        // Then
        verifyNoMoreInteractions(mockDelegate);
        assertThat(result).isTrue();
    }

    @Test
    public void shouldNotCacheNegativeResults() throws Exception {
        // Given
        given(mockSession.getTimeLeft()).willReturn(1L);
        given(mockClock.now()).willReturn(0L);
        given(mockDelegate.isBlacklisted(mockSession)).willReturn(false);

        // When
        testBlacklist.isBlacklisted(mockSession); // First call
        boolean result = testBlacklist.isBlacklisted(mockSession); // Second call

        // Then
        verify(mockDelegate, times(2)).isBlacklisted(mockSession); // Neither call should be cached
        assertThat(result).isFalse();
    }
}