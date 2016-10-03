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
package com.iplanet.dpro.session.operations.strategies;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.session.authorisation.SessionChangeAuthorizer;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.MonitoringOperations;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.sun.identity.shared.debug.Debug;

public class LocalOperationsTest {


    private LocalOperations local;
    @Mock
    private Session mockRequester;
    @Mock
    private Session mockSession;
    @Mock
    private SessionID mockSessionID;
    @Mock
    private InternalSession mockInternalSession;
    @Mock
    private SessionAccessManager sessionAccessManager;
    @Mock
    private SessionInfoFactory sessionInfoFactory;
    @Mock
    private SessionServerConfig serverConfig;
    @Mock
    private SessionServiceConfig serviceConfig;
    @Mock
    private MonitoringOperations monitoringOperations;
    @Mock
    private SessionCookies sessionCookies;
    @Mock
    private SessionChangeAuthorizer sessionChangeAuthorizer;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getID()).willReturn(mockSessionID);
        given(mockInternalSession.getSessionID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);
        given(sessionAccessManager.removeInternalSession(mockSessionID)).willReturn(mockInternalSession);
        given(mockSession.getID()).willReturn(mockSessionID);

        local = new LocalOperations(mock(Debug.class), sessionAccessManager, sessionInfoFactory, serverConfig,
                mock(SessionNotificationSender.class),
                mock(SessionLogging.class), mock(SessionAuditor.class), sessionChangeAuthorizer, serviceConfig);
    }

    @Test
    public void shouldNotSetLastAccessTimeWhenResetFlagIsFalse() throws SessionException {
        // Given
        boolean flag = false;
        // When
        local.refresh(mockSession, flag);
        // Then
        verify(mockInternalSession, times(0)).setLatestAccessTime();
    }

    @Test
    public void shouldSetLastAccessTimeWhenResetFlagIsTrue() throws SessionException {
        // Given
        boolean flag = true;
        // When
        local.refresh(mockSession, flag);
        // Then
        verify(mockInternalSession).setLatestAccessTime();
    }

    @Test
    public void shouldReturnSessionInfoOnRefresh() throws SessionException {
        // Given
        SessionInfo mockSessionInfo = mock(SessionInfo.class);

        given(sessionInfoFactory.getSessionInfo(mockInternalSession, mockInternalSession.getSessionID()))
                .willReturn(mockSessionInfo);
        // When
        SessionInfo result = local.refresh(mockSession, true);
        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldRemoveSessionFromSessionAccessManagerOnLogout() throws Exception {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(mockSession.getID()).willReturn(mockSessionID);
        // When
        local.logout(mockSession);
        // Then
        verify(sessionAccessManager).removeInternalSession(mockSessionID);
    }

    @Test
    public void shouldRemoveSessionFromSessionAccessManagerOnDestroy() throws SessionException {
        // Given
        given(mockSession.getSessionID()).willReturn(mockSessionID);
        given(sessionAccessManager.getInternalSession(mockSessionID)).willReturn(mockInternalSession);
        // When
        local.destroy(mockRequester, mockSession);
        // Then
        verify(sessionAccessManager).removeSessionId(eq(mockSessionID));
    }

}
