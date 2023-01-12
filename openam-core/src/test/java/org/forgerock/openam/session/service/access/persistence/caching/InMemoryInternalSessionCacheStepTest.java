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
 * 
 * Portions Copyrighted 2020 Open Identity Platform Community.
 */

package org.forgerock.openam.session.service.access.persistence.caching;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.watchers.SessionModificationListener;
import org.forgerock.openam.session.service.access.persistence.watchers.SessionModificationWatcher;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

public class InMemoryInternalSessionCacheStepTest {
    private static final int MAX_SESSIONS = 42;
    private static final long MAX_TIME = 1;
    private static final SessionID SESSION_ID = new SessionID("test");

    @Mock
    private SessionServiceConfig mockSessionConfig;

    @Mock
    private InternalSession mockSession;

    @Mock
    private InternalSessionStore mockStore;

    @Mock
    private SessionModificationWatcher mockSessionModificationWatcher;

    @Mock
    private Debug mockDebug;

    private InMemoryInternalSessionCacheStep testCache;
    private SessionModificationListener sessionModificationListener;

    @BeforeMethod
    public void createTestCache() throws Exception {
        MockitoAnnotations.initMocks(this);
        given(mockSessionConfig.getMaxSessionCacheSize()).willReturn(MAX_SESSIONS);
        given(mockSessionConfig.getMaxSessionCacheTime()).willReturn(MAX_TIME);
        given(mockSession.getID()).willReturn(SESSION_ID);

        setupMockCTSToCaptureQueryListener(mockSessionModificationWatcher);

        testCache = new InMemoryInternalSessionCacheStep(mockSessionConfig, mockDebug, mockSessionModificationWatcher);
    }

    @Test
    public void shouldNotStoreSessionsThatHaventBeenCached() throws Exception {
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isNull();
    }

    @Test
    public void shouldCacheSessionsWhenAsked() throws Exception {
        testCache.create(mockSession, mockStore);
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isEqualTo(mockSession);
    }

    @Test
    public void shouldTellLowerLayersToStoreSessionsWhenAsked() throws Exception {
        testCache.create(mockSession, mockStore);
        verify(mockStore).create(mockSession);
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
        testCache.create(mockSession, mockStore);
        assertThat(testCache.getByHandle(sessionHandle, mockStore)).isEqualTo(mockSession);
    }

    @Test
    public void shouldCacheSessionsByRestrictedTokensWhenPresent() throws Exception {
        Set<SessionID> restrictedTokens = CollectionUtils.asSet(new SessionID("one"), new SessionID("two"));
        given(mockSession.getRestrictedTokens()).willReturn(restrictedTokens);
        testCache.create(mockSession, mockStore);
        for (SessionID restrictedToken : restrictedTokens) {
            assertThat(testCache.getByRestrictedID(restrictedToken, mockStore)).isEqualTo(mockSession);
        }
    }

    @Test
    public void shouldAllowRemoval() throws Exception {
        testCache.create(mockSession, mockStore);
        testCache.remove(mockSession, mockStore);
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isNull();
    }

    @Test
    public void shouldRemoveOnSessionChangedEvent() throws Exception {
        testCache.create(mockSession, mockStore);

        sessionModificationListener.sessionChanged(SESSION_ID);
        assertThat(testCache.getBySessionID(SESSION_ID, mockStore)).isNull();
    }

    @Test
    public void shouldRemoveAllReferencesToTheSessionWhenRemovingItByMasterSessionId() throws Exception {
        // Given
        InternalSession session = sessionWithHandleAndRestrictedTokens();
        testCache.create(session, mockStore);

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
            testCache.create(session, mockStore);
        }

        assertThat(testCache.size()).isLessThanOrEqualTo(MAX_SESSIONS);
    }

    @Test
    public void shouldDetectCacheSizeHotSwap() throws Exception {
        for (int i = 0; i < MAX_SESSIONS * 2; ++i) {
            InternalSession session = mock(InternalSession.class);
            given(session.getID()).willReturn(new SessionID("Session" + i));
            testCache.create(session, mockStore);
        }

        assertThat(testCache.size()).as("Cache size before reconfiguration").isLessThanOrEqualTo(MAX_SESSIONS);

        // Update the cache size
        given(mockSessionConfig.getMaxSessionCacheSize()).willReturn(MAX_SESSIONS * 2);

        // Insert new sessions - the existing sessions should remain in the cache
        for (int i = MAX_SESSIONS; i < MAX_SESSIONS * 2; ++i) {
            InternalSession session = mock(InternalSession.class);
            given(session.getID()).willReturn(new SessionID("Session" + i));
            testCache.create(session, mockStore);
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

    private Map<String, Attribute> newChangeSetAffectingSessionId(SessionID sessionID) {
        Map<String, Attribute> changeSet = new HashMap<>();
        changeSet.put(SessionTokenField.SESSION_ID.getField().toString(),
                new LinkedAttribute("someDescription", Collections.singleton(SESSION_ID)));
        return changeSet;
    }

    /*
     * Populates the sessionModificationListener field with the value registered to the watcher
     */
    private void setupMockCTSToCaptureQueryListener(SessionModificationWatcher mockSessionModificationWatcher) throws CoreTokenException {
        doAnswer((new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                sessionModificationListener = (SessionModificationListener) invocationOnMock.getArguments()[0];
                return null;
            }
        })).when(mockSessionModificationWatcher).addListener(any(SessionModificationListener.class));
    }

}