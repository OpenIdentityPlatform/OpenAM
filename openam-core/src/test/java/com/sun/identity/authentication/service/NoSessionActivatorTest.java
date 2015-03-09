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
import static org.testng.Assert.assertNull;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class NoSessionActivatorTest {

    @Mock
    private SessionService mockSessionService;

    @Mock
    private InternalSession mockSession;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldDestroyAuthSession() throws AuthException {
        // Given
        final SessionID sid = new SessionID();
        given(mockSession.getID()).willReturn(sid);

        // When
        NoSessionActivator.INSTANCE.activateSession(null, mockSessionService, mockSession);

        // Then
        verify(mockSessionService).destroyInternalSession(sid);
    }

    @Test
    public void shouldAlwaysReturnNull() throws Exception {
        assertNull(NoSessionActivator.INSTANCE.activateSession(null, mockSessionService, mockSession));
    }

}