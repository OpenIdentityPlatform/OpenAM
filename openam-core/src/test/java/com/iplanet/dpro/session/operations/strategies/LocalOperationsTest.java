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
 * Copyright 2013-2014 ForgeRock AS.
 */
package com.iplanet.dpro.session.operations.strategies;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

public class LocalOperationsTest {

    private LocalOperations local;
    private SessionService mockService;
    private Session mockRequester;
    private Session mockSession;
    private SessionID mockSessionID;
    private InternalSession mockInternalSession;

    @BeforeMethod
    public void setup() {
        mockSessionID = mock(SessionID.class);
        mockRequester = mock(Session.class);
        mockSession = mock(Session.class);
        given(mockSession.getID()).willReturn(mockSessionID);
        mockInternalSession = mock(InternalSession.class);
        given(mockInternalSession.getID()).willReturn(mockSessionID);

        mockService = mock(SessionService.class);

        local = new LocalOperations(mock(Debug.class), mockService);
    }

    @Test
    public void shouldUseSessionServiceForRefresh() throws SessionException {
        // Given
        boolean flag = true;
        // When
        local.refresh(mockSession, flag);
        // Then
        verify(mockService).getSessionInfo(eq(mockSessionID), eq(flag));
    }

    @Test
    public void shouldReturnSessionInfoOnRefresh() throws SessionException {
        // Given
        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(mockService.getSessionInfo(any(SessionID.class), anyBoolean())).willReturn(mockSessionInfo);
        // When
        SessionInfo result = local.refresh(mock(Session.class), true);
        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldUseSessionServiceForLogout() throws SessionException {
        // Given
        // When
        local.logout(mockSession);
        // Then
        verify(mockService).logout(eq(mockSessionID));
    }

    @Test
    public void shouldUseSessionServiceForDestroy() throws SessionException {
        // Given
        given(mockService.getInternalSession(mockSessionID)).willReturn(mockInternalSession);
        // When
        local.destroy(mockRequester, mockSession);
        // Then
        verify(mockService).destroySession(eq(mockRequester), eq(mockSessionID));
    }

    @Test
    public void shouldUseSessionServiceForSetProperty() throws SessionException {
        // Given
        String name = "name";
        String value = "value";
        // When
        local.setProperty(mockSession, name, value);
        // Then
        verify(mockService).setProperty(eq(mockSessionID), eq(name), eq(value));
    }
}
