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

package com.iplanet.dpro.session.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionCookies;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class InternalSessionTest {

    @Mock
    private SessionService mockSessionService;

    @Mock
    private SessionServiceConfig mockSessionServiceConfig;

    @Mock
    private SessionLogging mockSessionLogging;

    @Mock
    private SessionCookies mockSessionCookies;

    private InternalSession session;

    @BeforeMethod
    public void createSession() {
        MockitoAnnotations.initMocks(this);
        session = new InternalSession();
        session.setSessionServiceDependencies(mockSessionService, mockSessionServiceConfig, mockSessionLogging,
                null, mockSessionCookies, null);
    }

    @AfterMethod
    public void restorePurgeDelay() {
        InternalSession.setPurgeDelay(Long.getLong("com.iplanet.am.session.purgedelay", 120));
    }

    @Test
    public void shouldNotUpdateForFailoverIfAboutToDelete() {
        // Given
        InternalSession.setPurgeDelay(0);
        session.setIsISStored(true);
        given(mockSessionServiceConfig.isSessionFailoverEnabled()).willReturn(true);
        session.setTimedOutAt(12345L);

        // When
        session.setState(SessionConstants.VALID);

        // Then
        // The session should *not* be saved for failover
        verify(mockSessionService, never()).saveForFailover(session);
    }

    @Test
    public void shouldUpdateForFailoverIfNotTimedOut() {
        // Given
        InternalSession.setPurgeDelay(0);
        session.setIsISStored(true);
        given(mockSessionServiceConfig.isSessionFailoverEnabled()).willReturn(true);
        session.setTimedOutAt(0);

        // When
        session.setState(SessionConstants.VALID);

        // Then
        verify(mockSessionService).saveForFailover(session);
    }

    @Test
    public void shouldUpdateForFailoverIfTimedOutButPurgeDelayExists() {
        // Given
        InternalSession.setPurgeDelay(120L);
        session.setIsISStored(true);
        given(mockSessionServiceConfig.isSessionFailoverEnabled()).willReturn(true);
        session.setTimedOutAt(12345L);

        // When
        session.setState(SessionConstants.VALID);

        // Then
        verify(mockSessionService).saveForFailover(session);
    }

    @Test
    public void shouldDeleteSessionIfNotValid() {
        // Given
        session.setIsISStored(true);
        given(mockSessionServiceConfig.isSessionFailoverEnabled()).willReturn(true);

        // When
        session.setState(SessionConstants.INVALID);

        // Then
        verify(mockSessionService).deleteFromRepository(session.getID());
    }
}