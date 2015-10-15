package org.forgerock.openam.audit;

import org.forgerock.audit.events.AuditEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * These are designed to test the interpretation and conversion of legacy authentication logging events to
 * audit events, via the {@link LegacyAuthenticationEventAuditor}.
 */
public class LegacyAuthenticationEventAuditorTest {

    LegacyAuthenticationEventAuditor auditor;

    AuthenticationAuditor authenticationAuditor = mock(AuthenticationAuditor.class);
    ActivityAuditor activityAuditor = mock(ActivityAuditor.class);
    AMAuthenticationAuditEventBuilder authenticationBuilder = mock(AMAuthenticationAuditEventBuilder.class);
    AMActivityAuditEventBuilder activityBuilder = mock(AMActivityAuditEventBuilder.class);

    private static final String LOGOUT_EVENT_1 = "LOGOUT";
    private static final String LOGOUT_EVENT_2 = "LOGOUT_USER";
    private static final String LOGOUT_EVENT_3 = "LOGOUT_ROLE";
    private static final String LOGOUT_EVENT_4 = "LOGOUT_SERVICE";
    private static final String LOGOUT_EVENT_5 = "LOGOUT_LEVEL";
    private static final String LOGOUT_EVENT_6 = "LOGOUT_MODULE_INSTANCE";

    @BeforeMethod
    public void setUp() throws Exception {
        auditor = new LegacyAuthenticationEventAuditor(authenticationAuditor, activityAuditor);

//        when(authenticationBuilder.transactionId(any(String.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.authentication(any(String.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.timestamp(any(Long.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.component(any(AuditConstants.Component.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.eventName(any(String.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.realm(any(String.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.contexts(any(Map.class))).thenReturn(authenticationBuilder);
//        when(authenticationBuilder.entries(any(List.class))).thenReturn(authenticationBuilder);
//        when(authenticationAuditor.authenticationEvent()).thenReturn(authenticationBuilder);
//
//        when(activityBuilder.transactionId(any(String.class))).thenReturn(activityBuilder);
//        when(activityBuilder.authentication(any(String.class))).thenReturn(activityBuilder);
//        when(activityBuilder.timestamp(any(Long.class))).thenReturn(activityBuilder);
//        when(activityBuilder.component(any(AuditConstants.Component.class))).thenReturn(activityBuilder);
//        when(activityBuilder.eventName(any(String.class))).thenReturn(activityBuilder);
//        when(activityBuilder.realm(any(String.class))).thenReturn(activityBuilder);
//        when(activityBuilder.contexts(any(Map.class))).thenReturn(activityBuilder);
//        when(activityAuditor.activityEvent()).thenReturn(activityBuilder);
    }

    @Test
    public void shouldAuditEvent() throws Exception {

        String eventName = null;
        String eventDescription = null;
        String transactionId = "a";
        String authentication = "b";
        String realmName = null;
        long time = 1;
        Map<String, String> contexts = null;
        List<?> entries = null;
        auditor.audit(eventName, eventDescription, transactionId, authentication, realmName, time, contexts, entries);

        verify(authenticationAuditor, times(1)).authenticationEvent();
        verify(authenticationAuditor, times(1)).publish(any(AuditEvent.class));
        verifyNoMoreInteractions(authenticationAuditor);
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
    public void shouldReturnTrueForRealmAndTopicAuditing() throws Exception {

    }

    @Test
    public void shouldReturnFalseForRealmAndTopicAuditing() throws Exception {

    }

    @Test
    public void shouldReturnTrueForRealmAuditingWhenOneTopicIsBeingAudited() throws Exception {

    }

    @Test
    public void shouldReturnTrueForRealmAuditingWhenBothTopicsAreBeingAudited() throws Exception {

    }

    @Test
    public void shouldReturnFalseForRealmAuditing() throws Exception {

    }
}