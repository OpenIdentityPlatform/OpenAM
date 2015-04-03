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
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import org.forgerock.bloomfilter.BloomFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class BloomFilterSessionBlacklistTest {
    private static final long PURGE_DELAY = 1000l;

    @Mock
    private SessionBlacklist mockDelegate;

    @Mock
    private BloomFilter<BloomFilterSessionBlacklist.SessionBlacklistEntry> mockBloomFilter;

    @Mock
    private SessionServiceConfig mockServiceConfig;

    @Mock
    private Session mockSession;

    private BloomFilterSessionBlacklist testBlacklist;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testBlacklist = new BloomFilterSessionBlacklist(mockDelegate, mockServiceConfig, mockBloomFilter);

        given(mockServiceConfig.getSessionBlacklistPurgeDelay(any(TimeUnit.class))).willReturn(PURGE_DELAY);
    }

    @Test
    public void shouldSubscribeForUpdatesFromOtherServers() {
        verify(mockDelegate).subscribe(any(SessionBlacklist.Listener.class));
    }

    @Test
    public void shouldAddNotifiedBlacklistedSessionsToTheBloomFilter() {
        // Given
        ArgumentCaptor<SessionBlacklist.Listener> listenerArgumentCaptor
                = ArgumentCaptor.forClass(SessionBlacklist.Listener.class);
        willDoNothing().given(mockDelegate).subscribe(listenerArgumentCaptor.capture());
        testBlacklist = new BloomFilterSessionBlacklist(mockDelegate, mockServiceConfig, mockBloomFilter);
        SessionBlacklist.Listener listener = listenerArgumentCaptor.getValue();
        String id = "testSession";
        long expiryTime = 1234l;

        // When
        listener.onBlacklisted(id, expiryTime);

        // Then
        verify(mockBloomFilter).add(new BloomFilterSessionBlacklist.SessionBlacklistEntry(id, expiryTime));
    }

    @Test
    public void shouldDelegateBlacklistToDelegate() throws Exception {
        testBlacklist.blacklist(mockSession);
        verify(mockDelegate).blacklist(mockSession);
    }

    @Test
    public void shouldNotCheckDelegateIfSessionNotInBloomFilter() throws Exception {
        // Given
        String id = "testSession";
        long expiryTime = 1234l;
        given(mockSession.getStableStorageID()).willReturn(id);
        given(mockSession.getBlacklistExpiryTime(PURGE_DELAY)).willReturn(expiryTime);
        given(mockBloomFilter.mightContain(new BloomFilterSessionBlacklist.SessionBlacklistEntry(id, expiryTime)))
                .willReturn(false);

        // When
        boolean result = testBlacklist.isBlacklisted(mockSession);

        // Then
        assertThat(result).isFalse();
        verify(mockDelegate, never()).isBlacklisted(any(Session.class));
    }

    @Test
    public void shouldCheckDelegateIfSessionIsInBloomFilter() throws Exception {
        // Given
        String id = "testSession";
        long expiryTime = 1234l;
        given(mockSession.getStableStorageID()).willReturn(id);
        given(mockSession.getBlacklistExpiryTime(PURGE_DELAY)).willReturn(expiryTime);
        given(mockBloomFilter.mightContain(new BloomFilterSessionBlacklist.SessionBlacklistEntry(id, expiryTime)))
                .willReturn(true);
        given(mockDelegate.isBlacklisted(mockSession)).willReturn(true);

        // When
        boolean result = testBlacklist.isBlacklisted(mockSession);

        // Then
        assertThat(result).isTrue();
        verify(mockDelegate).isBlacklisted(mockSession);
    }

    @Test
    public void shouldDelegateSubscriptions() {
        // Given
        SessionBlacklist.Listener listener = mock(SessionBlacklist.Listener.class);
        // When
        testBlacklist.subscribe(listener);
        // Then
        mockDelegate.subscribe(listener);
    }
}