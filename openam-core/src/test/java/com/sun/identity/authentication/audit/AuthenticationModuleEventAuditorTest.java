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
import org.forgerock.openam.audit.AuditConstants.*;

import com.sun.identity.authentication.service.LoginState;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.Principal;

public class AuthenticationModuleEventAuditorTest {

    private AuthenticationModuleEventAuditor auditor;

    @Mock
    private AuditEventPublisher eventPublisher;

    @Mock
    private AuditEventFactory eventFactory;

    @Mock
    private LoginState emptyState;

    @Mock
    private Principal emptyPrincipal;

    @Mock
    private AuthenticationAuditEntry emptyAuditEntryDetail;

    @BeforeMethod
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);

        when(eventPublisher.isAuditing(anyString(), anyString(), any(EventName.class))).thenReturn(true);
        when(eventFactory.authenticationEvent(anyString())).thenCallRealMethod();
        auditor = new AuthenticationModuleEventAuditor(eventPublisher, eventFactory);
    }

    @Test
    public void shouldNotFailAuditModuleSuccess() {
        // given

        // when
        auditor.auditModuleSuccess(null, null, null);
        auditor.auditModuleSuccess(emptyState, emptyPrincipal, emptyAuditEntryDetail);

        // then
        verify(eventPublisher, times(2)).tryPublish(anyString(), any(AuditEvent.class));
        // no exceptions expected
    }

    @Test
    public void shouldNotFailAuditModuleFailure() {
        // given

        // when
        auditor.auditModuleFailure(null, null, null);
        auditor.auditModuleFailure(emptyState, emptyPrincipal, emptyAuditEntryDetail);

        // then
        verify(eventPublisher, times(2)).tryPublish(anyString(), any(AuditEvent.class));
        // no exceptions expected
    }
}
