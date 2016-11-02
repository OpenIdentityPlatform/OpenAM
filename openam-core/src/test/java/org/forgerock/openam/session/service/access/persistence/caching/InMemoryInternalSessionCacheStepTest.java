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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.session.service.access.persistence.caching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.utils.CollectionUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

public class InMemoryInternalSessionCacheStepTest {
    private static final int MAX_SESSIONS = 42;
    private static final SessionID SESSION_ID = new SessionID("test");

    @Mock
    private SessionServiceConfig mockSessionConfig;

    @Mock
    private InternalSession mockSession;

    @Mock
    private InternalSessionStore mockStore;

    @Mock
    private Debug mockDebug;

    private InMemoryInternalSessionCacheStep testCache;

    @BeforeMethod
    public void createTestCache() {
        MockitoAnnotations.initMocks(this);
        given(mockSessionConfig.getMaxSessionCacheSize()).willReturn(MAX_SESSIONS);
        given(mockSession.getID()).willReturn(SESSION_ID);
        testCache = new InMemoryInternalSessionCacheStep(mockSessionConfig, mockDebug);
    }

    @Test
    public void shouldNotStoreSessionsThatHaventBeenCached() throws Exception {
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isNull();
    }

    @Test
    public void shouldCacheSessionsWhenAsked() throws Exception {
        testCache.store(mockSession, mockStore);
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isEqualTo(mockSession);
    }

    @Test
    public void shouldTellLowerLayersToStoreSessionsWhenAsked() throws Exception {
        testCache.store(mockSession, mockStore);
        verify(mockStore).store(mockSession);
    }

    @Test
    public void shouldAskLowerLayersWhenSessionNotInCache() throws Exception {
        given(mockStore.getBySessionID(SESSION_ID)).willReturn(mockSession);
        InternalSession session = testCache.getBySessionID(SESSION_ID, mockStore);
        assertThat(session).isSameAs(mockSession);
        verify(mockStore).getBySessionID(SESSION_ID);
    }

    @Test
    public void shouldCacheSessionsBySessionHandleWhenPresent() throws Exception {
        String sessionHandle = "handle";
        given(mockSession.getSessionHandle()).willReturn(sessionHandle);
        testCache.store(mockSession, mockStore);
        assertThat(testCache.getByHandle(sessionHandle, mockStore)).isEqualTo(mockSession);
    }

    @Test
    public void shouldCacheSessionsByRestrictedTokensWhenPresent() throws Exception {
        Set<SessionID> restrictedTokens = CollectionUtils.asSet(new SessionID("one"), new SessionID("two"));
        given(mockSession.getRestrictedTokens()).willReturn(restrictedTokens);
        testCache.store(mockSession, mockStore);
        for (SessionID restrictedToken : restrictedTokens) {
            assertThat(testCache.getByRestrictedID(restrictedToken, mockStore)).isEqualTo(mockSession);
        }
    }

    @Test
    public void shouldAllowRemoval() throws Exception {
        testCache.store(mockSession, mockStore);
        testCache.remove(mockSession, mockStore);
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isNull();
    }

    @Test
    public void shouldRemoveAllReferencesToTheSessionWhenRemovingItByMasterSessionId() throws Exception {
        // Given
        InternalSession session = sessionWithHandleAndRestrictedTokens();
        testCache.store(session, mockStore);

        // When
        testCache.remove(session, mockStore);

        // Then
        verifyAllReferencesRemovedFromCache(session);
    }

    @Test
    public void shouldLimitSizeOfCache() throws Exception {
        for (int i = 0; i < MAX_SESSIONS * 2; ++i) {
            InternalSession session = mock(InternalSession.class);
            given(session.getID()).willReturn(new SessionID("Session" + i));
            testCache.store(session, mockStore);
        }

        assertThat(testCache.size()).isLessThanOrEqualTo(MAX_SESSIONS);
    }

    @Test
    public void shouldDetectCacheSizeHotSwap() throws Exception {
        for (int i = 0; i < MAX_SESSIONS * 2; ++i) {
            InternalSession session = mock(InternalSession.class);
            given(session.getID()).willReturn(new SessionID("Session" + i));
            testCache.store(session, mockStore);
        }

        assertThat(testCache.size()).as("Cache size before reconfiguration").isLessThanOrEqualTo(MAX_SESSIONS);

        // Update the cache size
        given(mockSessionConfig.getMaxSessionCacheSize()).willReturn(MAX_SESSIONS * 2);

        // Insert new sessions - the existing sessions should remain in the cache
        for (int i = MAX_SESSIONS; i < MAX_SESSIONS * 2; ++i) {
            InternalSession session = mock(InternalSession.class);
            given(session.getID()).willReturn(new SessionID("Session" + i));
            testCache.store(session, mockStore);
        }
        assertThat(testCache.size()).as("Cache size after reconfiguration").isLessThanOrEqualTo(MAX_SESSIONS * 2);
    }


    private InternalSession sessionWithHandleAndRestrictedTokens() {
        String sessionHandle = "sessionHandle";
        Set<SessionID> restrictedTokens = CollectionUtils.asSet(new SessionID("one"), new SessionID("two"));
        given(mockSession.getRestrictedTokens()).willReturn(restrictedTokens);
        given(mockSession.getSessionHandle()).willReturn(sessionHandle);
        return mockSession;
    }

    private void verifyAllReferencesRemovedFromCache(InternalSession session) throws Exception {
        assertThat(testCache.getBySessionID(session.getID(), mockStore)).isNull();
        assertThat(testCache.getByHandle(session.getSessionHandle(), mockStore)).isNull();
        for (SessionID restrictedToken : session.getRestrictedTokens()) {
            assertThat(testCache.getByRestrictedID(restrictedToken, mockStore)).isNull();
        }
    }

}