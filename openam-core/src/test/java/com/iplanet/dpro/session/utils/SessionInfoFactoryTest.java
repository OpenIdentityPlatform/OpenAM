/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package com.iplanet.dpro.session.utils;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.TokenRestriction;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.share.SessionInfo;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Hashtable;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class SessionInfoFactoryTest {

    private SessionInfoFactory factory;
    private InternalSession mockSession;
    private SessionID mockSessionID;

    @BeforeMethod
    public void setUp() throws Exception {
        factory = new SessionInfoFactory();
        mockSession = mock(InternalSession.class);
        mockSessionID = mock(SessionID.class);
    }

    @Test
    public void shouldFailForInvalidSessions() {
        // Given
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSession.getRestrictionForToken(any(SessionID.class))).willReturn(mock(TokenRestriction.class));

        given(mockSession.getState()).willReturn(Session.INVALID);
        // When / Then
        try {
            factory.getSessionInfo(mockSession, mockSessionID);
        } catch (SessionException e) {
            assertThat(e.getMessage()).containsIgnoringCase("invalid");
        }
    }

    @Test
    public void shouldFailForTimeoutSessions() {
        // Given
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSession.getRestrictionForToken(any(SessionID.class))).willReturn(mock(TokenRestriction.class));
        given(mockSession.getState()).willReturn(Session.VALID);

        given(mockSession.getState()).willReturn(Session.INVALID);
        given(mockSession.getTimeLeftBeforePurge()).willReturn(1l);
        // When / Then
        try {
            factory.getSessionInfo(mockSession, mockSessionID);
        } catch (SessionException e) {
            assertThat(e.getMessage()).containsIgnoringCase("timed out");
        }
    }

    @Test
    public void shouldGenerateSessionInfoFromInternalSession() throws SessionException {
        // Given
        given(mockSession.getID()).willReturn(mockSessionID);
        given(mockSession.getRestrictionForToken(any(SessionID.class))).willReturn(mock(TokenRestriction.class));
        given(mockSession.getState()).willReturn(Session.VALID);
        given(mockSession.getTimeLeftBeforePurge()).willReturn(0l);

        SessionInfo mockSessionInfo = mock(SessionInfo.class);
        given(mockSession.toSessionInfo()).willReturn(mockSessionInfo);
        Hashtable mockHashTable = mock(Hashtable.class);
        mockSessionInfo.properties = mockHashTable;

        given(mockSession.getRestrictionForToken(any(SessionID.class))).willReturn(new MockTokenRestriction());

        // When
        SessionInfo result = factory.getSessionInfo(mockSession, mockSessionID);

        // Then
        assertThat(result).isNotNull();
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldCatchInvalidSession() throws SessionException {
        // Given
        given(mockSession.getID()).willReturn(mock(SessionID.class));
        // When / Then throw
        factory.validateSession(mockSession, mockSessionID);
    }

    private static class MockTokenRestriction implements TokenRestriction {
        public boolean isSatisfied(Object context) throws Exception {
            return false;
        }
    }
}
