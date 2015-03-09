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
package com.iplanet.dpro.session.service;

import com.iplanet.dpro.session.SessionID;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

public class InternalSessionCacheTest {
    private InternalSession session;
    private SessionID sessionId;
    private InternalSessionCache cache;

    @BeforeMethod
    public void setup() {
        session = mock(InternalSession.class);
        sessionId = mock(SessionID.class);
        given(session.getID()).willReturn(sessionId);

        SessionServiceConfig mockConfig = mock(SessionServiceConfig.class);
        given(mockConfig.getMaxSessions()).willReturn(10);
        cache = new InternalSessionCache(mockConfig);
    }

    @Test
    public void shouldCacheSession() {
        cache.put(session);
        assertThat(cache.getBySessionID(sessionId)).isEqualTo(session);
    }

    @Test
    public void shouldCacheRestrictedToken() {
        SessionID restrictedID = mock(SessionID.class);
        given(session.getRestrictedTokens()).willReturn(new HashSet<SessionID>(Arrays.asList(restrictedID)));
        cache.put(session);
        assertThat(cache.getByRestrictedID(restrictedID)).isEqualTo(session);
    }

    @Test
    public void shouldAllowAccessViaSessionHandle() {
        String handle = "session-handle";
        given(session.getSessionHandle()).willReturn(handle);
        cache.put(session);
        assertThat(cache.getByHandle(handle)).isEqualTo(session);
    }

    @Test
    public void shouldRemoveSession() {
        cache.put(session);
        cache.remove(sessionId);
        assertThat(cache.getBySessionID(sessionId)).isNull();
    }

    @Test
    public void shouldRemoveSessionHandle() {
        // Given
        String handle = "badger";
        given(session.getSessionHandle()).willReturn(handle);
        cache.put(session);

        // When
        cache.remove(session);

        // Then
        assertThat(cache.getByHandle(handle)).isNull();
    }

    @Test
    public void shouldRemoveRestrictedSession() {
        // Given
        SessionID restricted = mock(SessionID.class);
        given(session.getRestrictedTokens()).willReturn(new HashSet<SessionID>(Arrays.asList(restricted)));
        cache.put(session);

        // When
        cache.remove(session);

        // Then
        assertThat(cache.getByRestrictedID(restricted)).isNull();
    }

    @Test
    public void shouldRemovePreviousHandle() {
        // Given
        String oldHandle = "badger";
        given(session.getSessionHandle()).willReturn(oldHandle);

        cache.put(session);

        String newHandle = "ferret";
        given(session.getSessionHandle()).willReturn(newHandle);

        // When
        cache.put(session);

        // Then
        assertThat(cache.getByHandle(oldHandle)).isNull();
        assertThat(cache.getByHandle(newHandle)).isEqualTo(session);
    }

    @Test
    public void shouldRemovePreviousRestrictedSession() {
        // Given
        SessionID oldRestriction = mock(SessionID.class);
        given(session.getRestrictedTokens()).willReturn(new HashSet<SessionID>(Arrays.asList(oldRestriction)));

        cache.put(session);

        SessionID newRestriction = mock(SessionID.class);
        given(session.getRestrictedTokens()).willReturn(new HashSet<SessionID>(Arrays.asList(newRestriction)));

        // When
        cache.put(session);

        // Then
        assertThat(cache.getByRestrictedID(oldRestriction)).isNull();
        assertThat(cache.getByRestrictedID(newRestriction)).isEqualTo(session);
    }
}