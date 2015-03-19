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
 * Copyright 2013-2015 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.MonitoredOperations;
import com.iplanet.dpro.session.monitoring.SessionMonitorType;
import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.iplanet.dpro.session.operations.strategies.CTSOperations;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.RemoteOperations;
import com.iplanet.dpro.session.operations.strategies.StatelessOperations;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ServerSessionOperationStrategyTest {

    private ServerSessionOperationStrategy strategy;
    private SessionService mockSessionService;
    private LocalOperations mockLocal;
    private RemoteOperations mockRemote;
    private StatelessOperations mockStateless;
    private CTSOperations mockCTS;
    private WebtopNamingQuery mockNamingQuery;
    private Session mockSession;
    private SessionMonitoringStore mockStore;

    @BeforeMethod
    public void setUp() throws Exception {
        // Required dependencies
        mockSessionService = mock(SessionService.class);
        mockNamingQuery = mock(WebtopNamingQuery.class);

        // Strategies
        mockLocal = mock(LocalOperations.class);
        mockRemote = mock(RemoteOperations.class);
        mockStateless = mock(StatelessOperations.class);
        mockCTS = mock(CTSOperations.class);
        mockStore = mock(SessionMonitoringStore.class);

        strategy = new ServerSessionOperationStrategy(
                mockSessionService,
                mockStore,
                mockLocal,
                mockCTS,
                mockRemote,
                mockStateless,
                mockNamingQuery,
                mock(StatelessSessionFactory.class),
                mock(Debug.class));

        // test instances
        mockSession = mock(Session.class);
        SessionID mockSessionId = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionId);
        given(mockSessionId.getSessionServerID()).willReturn("TEST");
    }

    @Test
    public void shouldUseLocalForLocalSessions() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(true);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockLocal, SessionMonitorType.LOCAL, mockStore));
    }

    @Test
    public void shouldUseRemoteForRemoteSessionsWhenCrossTalkIsEnabled() throws Exception {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);

        // Cross-talk is enabled
        given(mockSessionService.isReducedCrossTalkEnabled()).willReturn(false);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockRemote, SessionMonitorType.REMOTE, mockStore));
    }

    @Test
    public void shouldUseCTSWhenRemoteIsDown() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(true);

        // Cross-talk is enabled
        given(mockSessionService.isReducedCrossTalkEnabled()).willReturn(false);

        // The Session is a Site
        given(mockNamingQuery.isSite(anyString())).willReturn(true);

        // The Site is down.
        given(mockSessionService.isSiteUp(anyString())).willReturn(false);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockCTS, SessionMonitorType.CTS, mockStore));
    }

    @Test
    public void shouldUseRemoteWhenFailoverIsDisabled() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(false);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockRemote, SessionMonitorType.REMOTE, mockStore));
    }

    @Test
    public void shouldUseCTSWhenCrossTalkDisabledCTSContainsSession() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(true);

        // Cross talk is disabled.
        given(mockSessionService.isReducedCrossTalkEnabled()).willReturn(true);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockCTS, SessionMonitorType.CTS, mockStore));
    }
}
