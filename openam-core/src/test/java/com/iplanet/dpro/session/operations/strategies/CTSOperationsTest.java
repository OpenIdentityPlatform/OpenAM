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
import com.iplanet.dpro.session.utils.SessionInfoFactory;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.SessionAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.api.tokens.TokenIdFactory;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.exceptions.ReadFailedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class CTSOperationsTest {

    private CTSPersistentStore mockCTS;
    private SessionAdapter mockAdapter;
    private TokenIdFactory mockIdFactory;
    private SessionInfoFactory mockInfoFactory;
    private SessionService mockSessionService;
    private CTSOperations ctsOperations;
    private Session mockRequester;
    private Session mockSession;
    private RemoteOperations mockRemote;

    @BeforeMethod
    public void setUp() throws Exception {
        mockRequester = mock(Session.class);
        mockSession = mock(Session.class);
        mockCTS = mock(CTSPersistentStore.class);
        mockAdapter = mock(SessionAdapter.class);
        mockIdFactory = mock(TokenIdFactory.class);
        mockInfoFactory = mock(SessionInfoFactory.class);
        mockSessionService = mock(SessionService.class);
        mockRemote = mock(RemoteOperations.class);

        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);

        given(mockIdFactory.toSessionTokenId(any(SessionID.class))).willReturn("TEST");

        ctsOperations = new CTSOperations(mockCTS, mockAdapter, mockIdFactory, mockInfoFactory, mockSessionService,
                                            mockRemote, mock(Debug.class));
    }

    @Test
    public void shouldReadTokenFromCTS() throws CoreTokenException, SessionException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockCTS.read(anyString())).willReturn(mockToken);

        InternalSession mockInternalSession = mock(InternalSession.class);
        given(mockAdapter.fromToken(eq(mockToken))).willReturn(mockInternalSession);

        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(mockInfoFactory.getSessionInfo(eq(mockInternalSession), any(SessionID.class))).willReturn(mockSessionInfo);

        // When
        SessionInfo result = ctsOperations.refresh(mockSession, false);

        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldReadTokenFromRemoteWhenCTSFails() throws CoreTokenException, SessionException {
        // Given
        given(mockCTS.read(anyString())).willThrow(new ReadFailedException("id", new IOException()));

        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(mockRemote.refresh(mockSession, false)).willReturn(mockSessionInfo);

        // When
        SessionInfo result = ctsOperations.refresh(mockSession, false);

        // Then
        assertThat(result).isEqualTo(mockSessionInfo);
    }

    @Test
    public void shouldTriggerResetOfLastAccessTime() throws CoreTokenException, SessionException {
        // Given
        Token mockToken = mock(Token.class);
        given(mockCTS.read(anyString())).willReturn(mockToken);

        InternalSession mockInternalSession = mock(InternalSession.class);
        given(mockAdapter.fromToken(eq(mockToken))).willReturn(mockInternalSession);

        // When
        ctsOperations.refresh(mockSession, true);

        // Then
        verify(mockInternalSession).setLatestAccessTime();
    }

    @Test (expectedExceptions = SessionException.class)
    public void shouldThrowExceptionOnReadError() throws CoreTokenException, SessionException {
        // Given
        given(mockCTS.read(anyString())).willThrow(new CoreTokenException(""));

        // When / Then Throw
        ctsOperations.refresh(mockSession, false);
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldDThrowExceptionWhenGivenLocalSession() throws SessionException {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSessionService.checkSessionLocal(mockSessionID)).willReturn(true);

        // When
        ctsOperations.logout(mockSession);
    }

    @Test
    public void shouldDeleteRemoteTokenDuringLogout() throws SessionException, CoreTokenException {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSessionService.checkSessionLocal(mockSessionID)).willReturn(false);

        // When
        ctsOperations.logout(mockSession);

        // Then
        verify(mockRemote).logout(mockSession);
    }

    @Test(expectedExceptions = SessionException.class)
    public void shouldNotDeleteLocalTokenDuringLogout() throws SessionException, CoreTokenException {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSessionService.checkSessionLocal(mockSessionID)).willReturn(true);

        // When
        ctsOperations.logout(mockSession);
    }

    @Test
    public void shouldOnlyDeleteTokenRemotelyDuringDestroy() throws Exception {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSessionService.checkSessionLocal(mockSessionID)).willReturn(false);

        // When
        ctsOperations.destroy(mockRequester, mockSession);

        // Then
        verify(mockRemote).destroy(mockRequester, mockSession);
    }

    @Test (expectedExceptions = SessionException.class)
    public void shouldThrowExceptionWhenDeleteFails() throws CoreTokenException, SessionException {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);

        doThrow(new SessionException("")).when(mockRemote).logout(mockSession);

        // When / Then Throw
        ctsOperations.logout(mockSession);
    }

    @Test
    public void shouldInvokeRemoteActionDuringSetProperty() throws SessionException, CoreTokenException {
        // Given
        String name = "name";
        String value = "value";

        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);

        Token mockToken = mock(Token.class);
        given(mockCTS.read(anyString())).willReturn(mockToken);

        InternalSession mockInternalSession = mock(InternalSession.class);
        given(mockAdapter.fromToken(eq(mockToken))).willReturn(mockInternalSession);

        // When
        ctsOperations.setProperty(mockSession, name, value);

        // Then
        verify(mockRemote).setProperty(mockSession, name, value);
    }

    @Test
    public void shouldCallRemoteSetPropertyDuringSetProperty() throws SessionException {
        // Given
        SessionID mockSessionID = mock(SessionID.class);
        given(mockSession.getID()).willReturn(mockSessionID);

        // When / Then Throw
        ctsOperations.setProperty(mockSession, "a", "b");

        verify(mockRemote).setProperty(mockSession, "a", "b");
    }
}
