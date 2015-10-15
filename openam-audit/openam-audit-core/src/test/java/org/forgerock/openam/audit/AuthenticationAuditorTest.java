package org.forgerock.openam.audit;

import org.forgerock.audit.events.AuditEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class AuthenticationAuditorTest {

    AuthenticationAuditor auditor;

    AuditEventPublisher auditEventPublisher;
    AuditEventFactory auditEventFactory;
    AMAuthenticationAuditEventBuilder builder = mock(AMAuthenticationAuditEventBuilder.class);
    AuditEvent auditEvent = mock(AuditEvent.class);

    @BeforeMethod
    public void setUp() throws Exception {
        auditEventPublisher = mock(AuditEventPublisher.class);
        auditEventFactory = mock(AuditEventFactory.class);

        auditor = new AuthenticationAuditor(auditEventPublisher, auditEventFactory);
    }

    @Test
    public void shouldGiveNewAuthenticationEventBuilder() throws Exception {
        when(auditor.authenticationEvent()).thenReturn(builder);

        AMAuthenticationAuditEventBuilder result = auditor.authenticationEvent();

        assertSame(builder, result, "Expected the same builder to be returned.");
        verify(auditEventFactory, times(1)).authenticationEvent();
        verifyNoMoreInteractions(auditEventFactory);
        verifyNoMoreInteractions(auditEventPublisher);
    }

    @Test
    public void shouldPublishEvent() throws Exception {
        auditor.publish(auditEvent);

        verify(auditEventPublisher, times(1)).publish(AuditConstants.AUTHENTICATION_TOPIC, auditEvent);
        verifyNoMoreInteractions(auditEventFactory);
        verifyNoMoreInteractions(auditEventPublisher);
    }

    @Test
    public void shouldReturnTrueForRealmAndTopicAuditing() throws Exception {
        String testRealm = "Any realm";
        String testTopic = "Any topic";

        when(auditEventPublisher.isAuditing(any(String.class), any(String.class))).thenReturn(true);

        boolean result = auditor.isAuditing(testRealm, testTopic);

        assertEquals(true, result);
        verify(auditEventPublisher, times(1)).isAuditing(testRealm, testTopic);
        verifyNoMoreInteractions(auditEventFactory);
        verifyNoMoreInteractions(auditEventPublisher);
    }

    @Test
    public void shouldReturnFalseForRealmAndTopicAuditing() throws Exception {
        String testRealm = "Any realm";
        String testTopic = "Any topic";

        when(auditEventPublisher.isAuditing(any(String.class), any(String.class))).thenReturn(false);

        boolean result = auditor.isAuditing(testRealm, testTopic);

        assertEquals(false, result);
        verify(auditEventPublisher, times(1)).isAuditing(testRealm, testTopic);
        verifyNoMoreInteractions(auditEventFactory);
        verifyNoMoreInteractions(auditEventPublisher);
    }
}