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
package com.sun.identity.authentication;

import org.forgerock.openam.audit.AMActivityAuditEventBuilder;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.ActivityAuditor;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuthenticationAuditor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * These are designed to test the interpretation and conversion of legacy authentication logging events to
 * audit events, via the {@link LegacyAuthenticationEventAuditor}.
 */
public class LegacyAuthenticationEventAuditorTest {

    LegacyAuthenticationEventAuditor auditor;

    AuthenticationAuditor authenticationAuditor;
    ActivityAuditor activityAuditor;
    AMAuthenticationAuditEventBuilder authenticationBuilder = mock(AMAuthenticationAuditEventBuilder.class);
    AMActivityAuditEventBuilder activityBuilder = mock(AMActivityAuditEventBuilder.class);

    private static final String LOGOUT_EVENT_1 = "LOGOUT";
    private static final String LOGOUT_EVENT_2 = "LOGOUT_USER";
    private static final String LOGOUT_EVENT_3 = "LOGOUT_ROLE";
    private static final String LOGOUT_EVENT_4 = "LOGOUT_SERVICE";
    private static final String LOGOUT_EVENT_5 = "LOGOUT_LEVEL";
    private static final String LOGOUT_EVENT_6 = "LOGOUT_MODULE_INSTANCE";

    private static final String TEST_REALM = "TEST_REALM";

    @BeforeMethod
    public void setUp() throws Exception {
        authenticationAuditor = mock(AuthenticationAuditor.class);
        activityAuditor = mock(ActivityAuditor.class);
        auditor = new LegacyAuthenticationEventAuditor(authenticationAuditor, activityAuditor);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent1() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_1);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent2() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_2);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent3() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_3);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent4() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_4);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent5() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_5);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForLegacyLogoutEvent6() throws Exception {
        boolean result = auditor.isLogoutEvent(LOGOUT_EVENT_6);

        assertEquals(true, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnFalseForNonLegacyLogoutEvent() throws Exception {
        boolean result = auditor.isLogoutEvent("Doesn't exist");

        assertEquals(false, result);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
        verifyNoMoreInteractions(authenticationBuilder);
        verifyNoMoreInteractions(activityBuilder);
    }

    @Test
    public void shouldReturnTrueForRealmAndAuthenticationTopicAuditing() throws Exception {
        when(authenticationAuditor.isAuditing(any(String.class), any(String.class))).thenReturn(true);

        boolean result = auditor.isAuditing(TEST_REALM, AuditConstants.AUTHENTICATION_TOPIC);

        assertEquals(true, result);
        verify(authenticationAuditor, times(1)).isAuditing(TEST_REALM, AuditConstants.AUTHENTICATION_TOPIC);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
    }

    @Test
    public void shouldReturnTrueForRealmAndActivityTopicAuditing() throws Exception {
        when(activityAuditor.isAuditing(any(String.class), any(String.class))).thenReturn(true);

        boolean result = auditor.isAuditing(TEST_REALM, AuditConstants.ACTIVITY_TOPIC);

        assertEquals(true, result);
        verify(activityAuditor, times(1)).isAuditing(TEST_REALM, AuditConstants.ACTIVITY_TOPIC);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
    }

    @Test
    public void shouldReturnFalseForRealmAndAuthenticationTopicAuditing() throws Exception {
        when(authenticationAuditor.isAuditing(any(String.class), any(String.class))).thenReturn(false);

        boolean result = auditor.isAuditing(TEST_REALM, AuditConstants.AUTHENTICATION_TOPIC);

        assertEquals(false, result);
        verify(authenticationAuditor, times(1)).isAuditing(TEST_REALM, AuditConstants.AUTHENTICATION_TOPIC);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
    }

    @Test
    public void shouldReturnFalseForRealmAndActivityTopicAuditing() throws Exception {
        when(activityAuditor.isAuditing(any(String.class), any(String.class))).thenReturn(false);

        boolean result = auditor.isAuditing(TEST_REALM, AuditConstants.ACTIVITY_TOPIC);

        assertEquals(false, result);
        verify(activityAuditor, times(1)).isAuditing(TEST_REALM, AuditConstants.ACTIVITY_TOPIC);
        verifyNoMoreInteractions(authenticationAuditor);
        verifyNoMoreInteractions(activityAuditor);
    }
}
