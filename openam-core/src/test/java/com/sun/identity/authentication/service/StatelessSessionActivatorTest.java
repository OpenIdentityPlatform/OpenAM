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
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.service;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;

public class StatelessSessionActivatorTest {

    private StatelessSessionActivator testActivator;

    @Mock
    private SessionService mockSessionService;

    @Mock
    private LoginState mockLoginState;

    @Mock
    private StatelessSessionManager mockSessionFactory;
    
    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        testActivator = new StatelessSessionActivator(mockSessionFactory);
    }
    
    @Test
    public void shouldCreateStatelessSessions() {
        // Given
        String orgDn = "/stateless";
        given(mockLoginState.getOrgDN()).willReturn(orgDn);

        // When
        testActivator.createSession(mockSessionService, mockLoginState);

        // When
        verify(mockSessionService).newInternalSession(orgDn, true);
    }

    @Test
    public void shouldActivateStatelessSessions() throws Exception {
        // Given
        InternalSession mockSession = mock(InternalSession.class);
        String userDn = "fred";
        given(mockLoginState.getUserDN()).willReturn(userDn);

        // When
        testActivator.activateSession(mockSession, mockLoginState);

        // Then
        verify(mockSession).activate(userDn);
    }

    @Test
    public void shouldGenerateStatelessSessionId() throws Exception {
        // Given
        InternalSession mockSession = mock(InternalSession.class);
        String userDn = "fred";
        given(mockLoginState.getUserDN()).willReturn(userDn);
        given(mockSession.activate(userDn)).willReturn(true);
        StatelessSession mockStatelessSession = mock(StatelessSession.class);
        given(mockSessionFactory.generate(mockSession)).willReturn(mockStatelessSession);
        SessionID statelessSessionId = new SessionID("stateless");
        given(mockStatelessSession.getID()).willReturn(statelessSessionId);

        // When
        testActivator.activateSession(mockSession, mockLoginState);

        // Then
        verify(mockSessionFactory).generate(mockSession);
        verify(mockLoginState).setSessionID(statelessSessionId);
    }

}