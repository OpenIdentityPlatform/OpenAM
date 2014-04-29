/**
 * Copyright 2013-2014 ForgeRock AS.
 *
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
 */
package com.iplanet.dpro.session.operations;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.operations.strategies.CTSOperations;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.RemoteOperations;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServerSessionOperationStrategyTest {

    private ServerSessionOperationStrategy strategy;
    private SessionService mockSessionService;
    private LocalOperations mockLocal;
    private RemoteOperations mockRemote;
    private CTSOperations mockCTS;
    private WebtopNamingQuery mockNamingQuery;
    private Session mockSession;

    @BeforeMethod
    public void setUp() throws Exception {
        // Required dependencies
        mockSessionService = mock(SessionService.class);
        mockNamingQuery = mock(WebtopNamingQuery.class);

        // Strategies
        mockLocal = mock(LocalOperations.class);
        mockRemote = mock(RemoteOperations.class);
        mockCTS = mock(CTSOperations.class);

        strategy = new ServerSessionOperationStrategy(
                mockSessionService,
                mockLocal,
                mockRemote,
                mockCTS,
                mockNamingQuery,
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
        assertThat(operation).isEqualTo(mockLocal);
    }

    @Test
    public void shouldUseRemoteForRemoteSessions() throws Exception {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(true);

        // The Session is a Site
        given(mockNamingQuery.isSite(anyString())).willReturn(true);

        // The Site is up.
        given(mockSessionService.checkSiteUp(anyString())).willReturn(true);

        // Cross-talk is enabled
        given(mockSessionService.isCrossTalkEnabled()).willReturn(true);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        verify(mockSessionService).checkSiteUp(anyString());
        assertThat(operation).isEqualTo(mockRemote);
    }

    @Test
    public void shouldUseCTSWhenRemoteIsDown() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(true);

        // The Session is a Site
        given(mockNamingQuery.isSite(anyString())).willReturn(true);

        // The Site is down.
        given(mockSessionService.checkSiteUp(anyString())).willReturn(false);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(mockCTS);
    }

    @Test
    public void shouldUseCTSWhenCrossTalkDisabledAndSessionIsRemote() throws SessionException {
        // Given
        given(mockSessionService.checkSessionLocal(any(SessionID.class))).willReturn(false);
        given(mockSessionService.isSessionFailoverEnabled()).willReturn(true);

        // Session is part of a site.
        given(mockNamingQuery.isSite(anyString())).willReturn(true);
        // The site is up
        given(mockSessionService.checkSiteUp(anyString())).willReturn(true);

        // Cross talk is disabled.
        given(mockSessionService.isCrossTalkEnabled()).willReturn(false);

        // When
        SessionOperations operation = strategy.getOperation(mockSession);

        // Then
        assertThat(operation).isEqualTo(mockCTS);
    }
}
