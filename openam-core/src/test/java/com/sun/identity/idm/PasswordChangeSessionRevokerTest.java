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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.idm;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.session.service.access.SessionQueryManager;
import org.testng.annotations.Test;

public class PasswordChangeSessionRevokerTest {
    @Test
    public void revokesOtherSessionsAndKeepsCurrentSession() throws Exception {
        SessionQueryManager query = mock(SessionQueryManager.class);
        SessionCache cache = mock(SessionCache.class);
        Session other = mock(Session.class);
        Map<String, Long> sessions = new HashMap<>();
        sessions.put("current", 1L);
        sessions.put("other", 1L);
        when(query.getAllSessionsByUUID("user")).thenReturn(sessions);
        when(cache.getSession(any(), eq(true), eq(false))).thenReturn(other);

        new PasswordChangeSessionRevoker(query, cache).revoke("user", "current");

        verify(cache, times(1)).getSession(any(), eq(true), eq(false));
        verify(other).destroySession(other);
    }

    @Test
    public void continuesAfterOneSessionCannotBeDestroyed() throws Exception {
        SessionQueryManager query = mock(SessionQueryManager.class);
        SessionCache cache = mock(SessionCache.class);
        Session other = mock(Session.class);
        Map<String, Long> sessions = new HashMap<>();
        sessions.put("one", 1L);
        sessions.put("two", 1L);
        when(query.getAllSessionsByUUID("user")).thenReturn(sessions);
        when(cache.getSession(any(), eq(true), eq(false)))
                .thenThrow(new SessionException("failed")).thenReturn(other);

        new PasswordChangeSessionRevoker(query, cache).revoke("user", "current");

        verify(other).destroySession(other);
    }
}
