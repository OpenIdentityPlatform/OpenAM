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
package com.sun.identity.authentication.audit;

import static org.mockito.Mockito.*;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.LoginState;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractAuthenticationEventAuditorTest {

    private AbstractAuthenticationEventAuditor auditor;

    @Mock
    private AuditEventPublisher eventPublisher;

    @Mock
    private AuditEventFactory eventFactory;

    @Mock
    private LoginState emptyState;

    @Mock
    private SSOToken emptyToken;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        auditor = new AbstractAuthenticationEventAuditor(eventPublisher, eventFactory) {
            // no abstract implementation required
        };
    }

    @Test
    public void shouldNotFailGetUserId() {
        // given

        // when
        auditor.getUserId(null, null);

        // then
        // no exceptions expected
    }

    @Test
    public void shouldNotFailGetTrackingId() {
        // given
        LoginState withSessionState = mock(LoginState.class);
        when(withSessionState.getSession()).thenReturn(mock(InternalSession.class));

        // when
        auditor.getTrackingIds(null);
        auditor.getTrackingIds(emptyState);
        auditor.getTrackingIds(withSessionState);

        // then
        // no exceptions expected
    }

    @Test
    public void shouldNotFailGetRealmFromState() {
        // given

        // when
        auditor.getRealmFromState(null);
        auditor.getRealmFromState(emptyState);

        // then
        // no exceptions expected
    }

    @Test
    public void shouldNotFailGetRealmFromToken() {
        // given

        // when
        auditor.getRealmFromToken(null);
        auditor.getRealmFromToken(emptyToken);

        // then
        // no exceptions expected
    }
}
