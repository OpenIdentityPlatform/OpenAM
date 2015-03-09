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

package com.sun.identity.authentication.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultSessionActivatorTest {
    private static final String ORGDN = "testdn";

    @Mock
    private SessionService mockSessionService;

    @Mock
    private InternalSession mockAuthSession;

    @Mock
    private InternalSession mockNewSession;

    @Mock
    private LoginState mockState;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        given(mockState.getOrgDN()).willReturn(ORGDN);
        given(mockSessionService.newInternalSession(ORGDN, null)).willReturn(mockNewSession);
    }

    @Test
    public void shouldCreateNewInternalSession() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession);

        // Then
        verify(mockSessionService).newInternalSession(ORGDN, null);
    }

    @Test
    public void shouldRemoveAuthContext() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession);

        // Then
        verify(mockNewSession).removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);
    }

    @Test
    public void shouldCopySessionProperties() throws Exception {
        // Given
        final List<String> sessionProperties = Arrays.asList("one", "two", "three");
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(sessionProperties));
        given(mockAuthSession.getProperty("one")).willReturn("a");
        given(mockAuthSession.getProperty("two")).willReturn("b");
        given(mockAuthSession.getProperty("three")).willReturn("c");

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession);

        // Then
        verify(mockState).setSessionProperties(mockNewSession);
        verify(mockNewSession).putProperty("one", "a");
        verify(mockNewSession).putProperty("two", "b");
        verify(mockNewSession).putProperty("three", "c");
    }

    @Test
    public void shouldDestroyAuthSession() throws Exception {
        // Given
        final SessionID authSessionID = new SessionID();
        given(mockAuthSession.getID()).willReturn(authSessionID);
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService, mockAuthSession);

        // Then
        verify(mockSessionService).destroyInternalSession(authSessionID);
    }

    @Test
    public void shouldReturnNewSession() throws Exception {
        // Given
        given(mockAuthSession.getPropertyNames()).willReturn(Collections.enumeration(Collections.emptyList()));

        // When
        final InternalSession result = DefaultSessionActivator.INSTANCE.activateSession(mockState, mockSessionService,
                mockAuthSession);

        // Then
        assertEquals(result, mockNewSession);
    }
}