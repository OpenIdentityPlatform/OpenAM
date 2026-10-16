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

import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.email.MailServerImpl;

import com.sun.identity.shared.debug.Debug;

/**
 * The subject and the body of the mails the anonymous self service actions of the users endpoint send.
 *
 * GHSA-mw38-8gr7-c4x2: both used to be read from the request, so an unauthenticated caller chose the wording of a
 * mail the server sent out under its own From address, and a CR or an LF in the subject reached the Subject header.
 * Both now come from the Email Service configuration of the realm, and the confirmation link stays the only thing
 * the notification adds.
 *
 * The class deliberately holds no injected state, so that the wording of these mails can be covered by a test that
 * needs no injector.
 */
final class SelfServiceNotification {

    private static final Debug debug = Debug.getInstance("frRest");

    /** The fields of the request body the two actions used to take the wording of the mail from. */
    static final String SUBJECT_FIELD = "subject";
    static final String MESSAGE_FIELD = "message";

    private SelfServiceNotification() {
    }

    /**
     * Returns the subject the self service notifications of a realm are sent with.
     *
     * @param mailAttributes The attributes of the Email Service of the realm
     * @param realm The realm the notification is sent for, for logging
     * @return The configured subject, never null; an empty string where the service carries none
     */
    static String notificationSubject(Map<String, Set<String>> mailAttributes, String realm) {
        String subject = firstValue(mailAttributes, MailServerImpl.SUBJECT);
        if (subject == null) {
            if (debug.warningEnabled()) {
                debug.warning("SelfServiceNotification.notificationSubject() :: the Email Service of realm {} "
                        + "carries no {}, sending with an empty subject", realm, MailServerImpl.SUBJECT);
            }
            return "";
        }
        return subject;
    }

    /**
     * Returns the body the self service notifications of a realm are sent with, with the confirmation link added.
     *
     * @param mailAttributes The attributes of the Email Service of the realm
     * @param realm The realm the notification is sent for, for logging
     * @param confirmationLink The link the recipient follows to confirm the request
     * @return The configured message followed by the confirmation link, or the link alone where the service
     *         carries no message
     */
    static String notificationMessage(Map<String, Set<String>> mailAttributes, String realm,
            String confirmationLink) {
        String message = firstValue(mailAttributes, MailServerImpl.MESSAGE);
        if (message == null) {
            if (debug.warningEnabled()) {
                debug.warning("SelfServiceNotification.notificationMessage() :: the Email Service of realm {} "
                        + "carries no {}, sending the confirmation link alone", realm, MailServerImpl.MESSAGE);
            }
            return confirmationLink;
        }
        return message + System.getProperty("line.separator") + confirmationLink;
    }

    /**
     * Logs that a subject or a message the request carried is being ignored.
     *
     * The fields are not rejected, because that would break a client that still sends them on what is a security
     * release; a deployment that customised the wording that way is told where its text went instead.
     *
     * @param requestBody The body of the action request, may be null
     * @param realm The realm the action was called in
     */
    static void warnIfRequestCarriesNotificationText(JsonValue requestBody, String realm) {
        if (requestBody == null || !debug.warningEnabled()) {
            return;
        }
        if (requestBody.isDefined(SUBJECT_FIELD) || requestBody.isDefined(MESSAGE_FIELD)) {
            debug.warning("SelfServiceNotification :: the {} or the {} of the request is ignored in realm {}; the "
                    + "wording of self service mail comes from the Email Service of the realm only",
                    SUBJECT_FIELD, MESSAGE_FIELD, realm);
        }
    }

    /**
     * Returns the first value of a single valued service attribute, or null where the attribute is absent, carries
     * no value, or carries a null one.
     */
    private static String firstValue(Map<String, Set<String>> attributes, String name) {
        if (attributes == null) {
            return null;
        }
        Set<String> values = attributes.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.iterator().next();
    }
}
