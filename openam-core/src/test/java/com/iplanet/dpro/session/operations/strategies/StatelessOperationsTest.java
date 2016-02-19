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

package com.iplanet.dpro.session.operations.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionEvent;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionInfo;
import org.forgerock.openam.session.blacklist.SessionBlacklist;
import org.forgerock.openam.sso.providers.stateless.StatelessSSOProvider;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.sso.providers.stateless.StatelessSessionFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StatelessOperationsTest {

    @Mock
    private StatelessSSOProvider mockSsoProvider;

    @Mock
    private SessionService mockSessionService;

    @Mock
    private StatelessSessionFactory mockSessionFactory;

    @Mock
    private SessionBlacklist mockSessionBlacklist;

    @Mock
    private StatelessSession mockSession;

    @Mock
    private SessionLogging mockSessionLogging;

    private SessionID sid;

    private StatelessOperations statelessOperations;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sid = new SessionID("test");
        given(mockSession.getID()).willReturn(sid);
        statelessOperations = new StatelessOperations(
                null, mockSessionService, mockSessionFactory, mockSessionBlacklist, mockSessionLogging, null);
    }

    @Test
    public void shouldRefreshFromStatelessSessionFactory() throws Exception {
        // Given
        SessionInfo info = new SessionInfo();
        info.setExpiryTime(currentTimeMillis() + (1000 * 60 * 10));
        given(mockSessionFactory.getSessionInfo(sid)).willReturn(info);

        // When
        SessionInfo result = statelessOperations.refresh(mockSession, false);

        // Then
        verify(mockSessionFactory).getSessionInfo(sid);
        assertThat(result).isSameAs(info);
    }

    @Test(expectedExceptions = SessionTimedOutException.class)
    public void refreshShouldTimeoutFromStatelessSessionFactory() throws Exception {
        // Given
        SessionInfo info = new SessionInfo();
        given(mockSessionFactory.getSessionInfo(sid)).willReturn(info);

        // When
        SessionInfo result = statelessOperations.refresh(mockSession, false);

        // Then exception should be thrown, as session is timed-out
    }

    @Test
    public void shouldBlacklistSessionOnLogout() throws Exception {
        // Given

        // When
        statelessOperations.logout(mockSession);

        // Then
        verify(mockSessionLogging).logEvent(
                mockSessionFactory.getSessionInfo(mockSession.getSessionID()), SessionEvent.LOGOUT);
        verify(mockSessionBlacklist).blacklist(mockSession);
    }

    @Test
    public void shouldCheckPermissionToDestroySession() throws Exception {
        // Given
        Session requester = mock(Session.class);

        // When
        statelessOperations.destroy(requester, mockSession);

        // Then
        verify(mockSessionService).checkPermissionToDestroySession(requester, sid);
    }

    @Test
    public void shouldBlacklistSessionOnDestroyWhenAllowed() throws Exception {
        // Given
        Session requester = mock(Session.class);

        // When
        statelessOperations.destroy(requester, mockSession);

        // Then
        verify(mockSessionLogging).logEvent(
                mockSessionFactory.getSessionInfo(mockSession.getSessionID()), SessionEvent.DESTROY);
        verify(mockSessionBlacklist).blacklist(mockSession);
    }

    @Test(expectedExceptions = SessionException.class, expectedExceptionsMessageRegExp = "test")
    public void shouldNotBlacklistSessionOnDestroyIfNotAllowed() throws Exception {
        // Given
        Session requester = mock(Session.class);
        SessionException ex = new SessionException("test");
        willThrow(ex).given(mockSessionService).checkPermissionToDestroySession(requester, sid);

        // When
        try {
            statelessOperations.destroy(requester, mockSession);
        } finally {
            // Then
            verifyZeroInteractions(mockSessionBlacklist);
        }
    }
}