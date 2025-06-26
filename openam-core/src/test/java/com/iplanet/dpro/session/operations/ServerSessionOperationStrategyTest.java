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
package com.iplanet.dpro.session.operations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.monitoring.MonitoredOperations;
import com.iplanet.dpro.session.monitoring.SessionMonitorType;
import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.iplanet.dpro.session.operations.strategies.LocalOperations;
import com.iplanet.dpro.session.operations.strategies.StatelessOperations;
import com.sun.identity.shared.debug.Debug;

public class ServerSessionOperationStrategyTest {

    private ServerSessionOperationStrategy strategy;
    private LocalOperations mockLocal;
    private StatelessOperations mockStateless;
    private SessionID mockSessionID;
    private SessionMonitoringStore mockStore;

    @BeforeMethod
    public void setUp() throws Exception {
        // Strategies
        mockLocal = mock(LocalOperations.class);
        mockStateless = mock(StatelessOperations.class);
        mockStore = mock(SessionMonitoringStore.class);

        strategy = new ServerSessionOperationStrategy(
                mockStore,
                mockLocal,
                mockStateless,
                mock(StatelessSessionManager.class),
                mock(Debug.class));

        // test instances
        mockSessionID = mock(SessionID.class);
        SessionID mockSessionId = mock(SessionID.class);
        given(mockSessionId.getSessionServerID()).willReturn("TEST");
    }

    @Test
    public void shouldUseLocalForLocalSessions() throws SessionException {
        // Given
        given(mockLocal.checkSessionExists(any(SessionID.class))).willReturn(true);

        // When
        SessionOperations operation = strategy.getOperation(mockSessionID);

        // Then
        assertThat(operation).isEqualTo(new MonitoredOperations(mockLocal, SessionMonitorType.LOCAL, mockStore));
    }
}
