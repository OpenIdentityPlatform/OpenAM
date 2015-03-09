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
package com.iplanet.dpro.session;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SessionEventTest {
    @Test
    public void shouldInvokeAllListenersOnSession() {
        // Given
        Session mockSession = mock(Session.class);
        List<SessionListener> listeners = Arrays.asList(
                mock(SessionListener.class),
                mock(SessionListener.class),
                mock(SessionListener.class));
        given(mockSession.getLocalSessionEventListeners()).willReturn(new HashSet<SessionListener>(listeners));
        SessionEvent mockEvent = mock(SessionEvent.class);
        given(mockEvent.getSession()).willReturn(mockSession);

        // When
        SessionEvent.invokeListeners(mockEvent);

        // Then
        for (SessionListener mockListener : listeners) {
            verify(mockListener).sessionChanged(any(SessionEvent.class));
        }
    }
}