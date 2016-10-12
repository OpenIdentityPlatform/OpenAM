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

package org.forgerock.openam.session.service;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.session.SessionCache;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.ForeignSessionHandler;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.InternalSessionStore;
import com.iplanet.dpro.session.service.MonitoringOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionState;
import com.sun.identity.shared.debug.Debug;

public class SessionAccessManagerTest {

    private SessionAccessManager sessionAccessManager;
    @Mock private Debug mockDebug;
    @Mock private SessionPersistentStore sessionPersistentStore;
    @Mock private Session mockSession;
    @Mock private SessionID mockSessionID;
    @Mock private InternalSession mockInternalSession;
    @Mock private InternalSessionStore internalSessionCache;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sessionAccessManager = new SessionAccessManager(mockDebug, mock(ForeignSessionHandler.class),
                mock(SessionCache.class), internalSessionCache, mock(SessionNotificationSender.class),
                mock(SessionLogging.class), mock(SessionAuditor.class), mock(MonitoringOperations.class),
                sessionPersistentStore);

        given(internalSessionCache.remove(mockSessionID)).willReturn(mockInternalSession);
        given(mockSession.getID()).willReturn(mockSessionID);

    }

    @Test
    public void shouldNotUpdateIfAboutToDelete() throws CoreTokenException {
        // Given
        final InternalSession session = mock(InternalSession.class);

        // set session up to be updated
        given(session.willExpire()).willReturn(true);
        given(session.isStored()).willReturn(true);
        given(session.isTimedOut()).willReturn(true);

        // now set it to be destroyed instead
        given(session.getState()).willReturn(SessionState.DESTROYED);

        // When
        sessionAccessManager.persistInternalSession(session);

        // Then
        verify(sessionPersistentStore, never()).save(any(InternalSession.class));
    }

    @Test
    public void shouldDeleteSessionTokenOnLogout() throws Exception {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getID()).willReturn(mockSessionID);

        given(mockInternalSession.isStored()).willReturn(true);
        given(mockInternalSession.getState()).willReturn(SessionState.DESTROYED);
        // When
        sessionAccessManager.removeInternalSession(mockSessionID);
        // Then
        verify(sessionPersistentStore).delete(mockInternalSession);
    }
}
