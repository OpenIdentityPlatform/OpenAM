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

import static org.forgerock.openam.session.SessionConstants.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import org.forgerock.guice.core.InjectorHolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.sun.identity.shared.debug.Debug;

public class SessionAccessManagerTest {

    private SessionAccessManager sessionAccessManager;
    @Mock
    private SessionService mockSessionService;
    @Mock private SessionServiceConfig mockSessionServiceConfig;
    @Mock private SessionLogging mockSessionLogging;
    @Mock private SessionAuditor mockSessionAuditor;
    @Mock private Debug mockDebug;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sessionAccessManager = InjectorHolder.getInstance(SessionAccessManager.class);
    }

    @Test
    public void shouldNotUpdateIfAboutToDelete() {
        // Given
        final InternalSession session = mock(InternalSession.class);
        given(session.getState()).willReturn(VALID);
        given(session.isTimedOut()).willReturn(true);

        // When
        sessionAccessManager.update(session);

        // Then
//        verify(sessionAccessManager, never()).save(session); //TODO: how to unit test this?
    }
}
