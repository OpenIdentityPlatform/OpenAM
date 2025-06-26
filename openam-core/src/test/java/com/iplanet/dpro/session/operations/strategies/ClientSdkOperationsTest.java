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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.util.Arrays;

import org.forgerock.openam.session.SessionServiceURLService;
import org.forgerock.openam.session.service.ServicesClusterMonitorHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.ClientSdkSessionRequests;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.dpro.session.share.SessionRequest;
import com.iplanet.dpro.session.share.SessionResponse;
import com.sun.identity.shared.debug.Debug;

public class ClientSdkOperationsTest {

    private ClientSdkOperations clientSdkOperations;

    @Mock
    private ClientSdkSessionRequests mockClientSdkSessionRequests;
    @Mock
    private Session mockRequester;
    @Mock
    private Session mockSession;
    @Mock
    private SessionID mockRequesterId;
    @Mock
    private SessionID mockSessionId;
    @Mock
    private SessionResponse mockResponse;
    @Mock
    private ServicesClusterMonitorHandler mockServicesClusterMonitorHandler;
    @Mock
    private SessionServiceURLService mockSessionServiceURLService;
    @Mock
    private SessionServerConfig mockServerConfig;

    @BeforeMethod
    public void setup() throws SessionException {
        MockitoAnnotations.initMocks(this);
        given(mockRequester.getID()).willReturn(mockRequesterId);
        given(mockSession.getID()).willReturn(mockSessionId);
        given(mockClientSdkSessionRequests.sendRequest(
                any(URL.class),
                any(SessionRequest.class),
                any(Session.class))).willReturn(mockResponse);

        clientSdkOperations = new ClientSdkOperations(mock(Debug.class), mockClientSdkSessionRequests,
                mockSessionServiceURLService);
    }

    @Test
    public void shouldUseSessionIDInRefreshRequest() throws SessionException {
        // Given
        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(mockResponse.getSessionInfo()).willReturn(Arrays.asList(mockSessionInfo));

        // When
        SessionInfo result = clientSdkOperations.refresh(mockSession, true);

        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldThrowExceptionIfRequestContainedOne() throws SessionException {
        // Given
        given(mockResponse.getException()).willReturn("Error");

        // When
        try {
            clientSdkOperations.refresh(mockSession, true);
        } catch (SessionException e) {
            // Then
            assertThat(e.getErrorCode()).isEqualTo(ClientSdkOperations.INVALID_SESSION_STATE);
        }
    }

    @Test
    public void shouldFailIfRequestDoesNotContainOneInfo() throws SessionException {
        // Given
        given(mockResponse.getSessionInfo()).willReturn(Arrays.asList(
                mock(SessionInfo.class),
                mock(SessionInfo.class)));

        // When
        try {
            clientSdkOperations.refresh(mockSession, true);
        } catch (SessionException e) {
            // Then
            assertThat(e.getErrorCode()).isEqualTo(ClientSdkOperations.UNEXPECTED_SESSION);
        }
    }

    @Test
    public void shouldUseRequestsForLogout() throws SessionException {
        // Given

        // When
        clientSdkOperations.logout(mockSession);

        // Then
        verify(mockClientSdkSessionRequests).sendRequest(any(URL.class), any(SessionRequest.class), eq(mockSession));
    }

    @Test
    public void shouldUseRequestsForDestroy() throws SessionException {
        // Given

        // When
        clientSdkOperations.destroy(mockRequester, mockSession);

        // Then
        verify(mockClientSdkSessionRequests).sendRequest(any(URL.class), any(SessionRequest.class), eq(mockSession));
    }

    @Test
    public void shouldUseRequestsForSetProperty() throws SessionException {
        // Given
        String name = "name";
        String value = "value";
        // When
        clientSdkOperations.setProperty(mockSession, name, value);

        // Then
        verify(mockClientSdkSessionRequests).sendRequest(any(URL.class), any(SessionRequest.class), eq(mockSession));
    }
}
