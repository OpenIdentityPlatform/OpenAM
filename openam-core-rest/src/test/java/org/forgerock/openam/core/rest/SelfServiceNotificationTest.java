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
package org.forgerock.openam.core.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.core.rest.SelfServiceNotification.notificationMessage;
import static org.forgerock.openam.core.rest.SelfServiceNotification.notificationSubject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.services.email.MailServerImpl;
import org.testng.annotations.Test;

/**
 * GHSA-mw38-8gr7-c4x2: the register and forgotPassword actions used to take the subject and the body of the
 * notification from the request, so an unauthenticated caller chose the wording of a mail the server sent out under
 * its own From address, and a CR or an LF in the subject reached the Subject header. Both now come from the Email
 * Service configuration of the realm.
 */
public class SelfServiceNotificationTest {

    /** What an administrator put in the Email Service of the realm. */
    private static final String CONFIGURED_SUBJECT = "Reset your password";
    private static final String CONFIGURED_MESSAGE = "Follow the link to reset your password.";

    private static final String REALM = "/";

    private static final String CONFIRMATION_LINK = "https://openam.example.com/openam/json/confirmation"
            + "/forgotPassword?confirmationId=abc&tokenId=def&username=demo&realm=/";

    @Test
    public void shouldTakeTheSubjectFromTheEmailService() {
        assertThat(notificationSubject(mailAttributes(), REALM)).isEqualTo(CONFIGURED_SUBJECT);
    }

    @Test
    public void shouldTakeTheMessageFromTheEmailService() {
        String message = notificationMessage(mailAttributes(), REALM, CONFIRMATION_LINK);

        assertThat(message).startsWith(CONFIGURED_MESSAGE);
        assertThat(message).endsWith(CONFIRMATION_LINK);
    }

    /** The confirmation link is the one thing the notification adds to the configured message. */
    @Test
    public void shouldAppendTheConfirmationLinkToTheConfiguredMessage() {
        assertThat(notificationMessage(mailAttributes(), REALM, CONFIRMATION_LINK))
                .isEqualTo(CONFIGURED_MESSAGE + System.getProperty("line.separator") + CONFIRMATION_LINK);
    }

    /**
     * A realm whose Email Service carries no subject used to fall back to the one in the request; there is nothing
     * to fall back to now, and an empty subject is better than one an anonymous caller chose.
     */
    @Test
    public void shouldFallBackToAnEmptySubjectWhenTheEmailServiceCarriesNone() {
        Map<String, Set<String>> attributes = mailAttributes();
        attributes.remove(MailServerImpl.SUBJECT);

        assertThat(notificationSubject(attributes, REALM)).isEmpty();
    }

    @Test
    public void shouldFallBackToTheConfirmationLinkWhenTheEmailServiceCarriesNoMessage() {
        Map<String, Set<String>> attributes = mailAttributes();
        attributes.remove(MailServerImpl.MESSAGE);

        assertThat(notificationMessage(attributes, REALM, CONFIRMATION_LINK)).isEqualTo(CONFIRMATION_LINK);
    }

    @Test
    public void shouldFallBackWhenTheEmailServiceCarriesNoAttributesAtAll() {
        Map<String, Set<String>> attributes = Collections.emptyMap();

        assertThat(notificationSubject(attributes, REALM)).isEmpty();
        assertThat(notificationMessage(attributes, REALM, CONFIRMATION_LINK)).isEqualTo(CONFIRMATION_LINK);
    }

    /** An attribute the service holds with no value at all, rather than one it does not hold. */
    @Test
    public void shouldFallBackWhenTheAttributesCarryNoValue() {
        Map<String, Set<String>> attributes = mailAttributes();
        attributes.put(MailServerImpl.SUBJECT, Collections.<String>emptySet());
        attributes.put(MailServerImpl.MESSAGE, Collections.<String>emptySet());

        assertThat(notificationSubject(attributes, REALM)).isEmpty();
        assertThat(notificationMessage(attributes, REALM, CONFIRMATION_LINK)).isEqualTo(CONFIRMATION_LINK);
    }

    /**
     * A null value would reach MimeMessage.setSubject(null), which removes the header rather than emptying it, so
     * the subject is emptied here instead.
     */
    @Test
    public void shouldNeverReturnANullSubjectOrMessage() {
        Map<String, Set<String>> attributes = mailAttributes();
        attributes.put(MailServerImpl.SUBJECT, valueSetOf(null));
        attributes.put(MailServerImpl.MESSAGE, valueSetOf(null));

        assertThat(notificationSubject(attributes, REALM)).isEmpty();
        assertThat(notificationMessage(attributes, REALM, CONFIRMATION_LINK)).isEqualTo(CONFIRMATION_LINK);
    }

    @Test
    public void shouldFallBackWhenThereIsNoEmailServiceConfiguration() {
        assertThat(notificationSubject(null, REALM)).isEmpty();
        assertThat(notificationMessage(null, REALM, CONFIRMATION_LINK)).isEqualTo(CONFIRMATION_LINK);
    }

    private static Map<String, Set<String>> mailAttributes() {
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(MailServerImpl.SUBJECT, Collections.singleton(CONFIGURED_SUBJECT));
        attributes.put(MailServerImpl.MESSAGE, Collections.singleton(CONFIGURED_MESSAGE));
        return attributes;
    }

    private static Set<String> valueSetOf(String value) {
        Set<String> values = new HashSet<>();
        values.add(value);
        return values;
    }
}
