/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 *
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.services.email;

import javax.mail.MessagingException;
import java.util.Map;
import java.util.Set;

/**
 * Pluggable interface for all email sending in OpenAM. Can be configured on a per-realm or global basis via the
 * {@code forgerockMailServerImplClassName} attribute of the Email Service.
 * <p>
 * The implementation <strong>must</strong> provide a constructor taking a single String argument, which is the name
 * of the realm that service is being constructed for.
 *
 * @supported.all.api
 */
public interface MailServer {

    /**
     * Sends an email message, containing HTML, using default MailServer settings.
     *
     * @param to The address that the E-mail message is sent.
     * @param subject The E-mail subject.
     * @param message The content contained in the E-mail message.
     * @throws MessagingException in the case where the module was unable to send the e-mail.
     */
    void sendHtmlEmail(String to, String subject, String message) throws MessagingException;

    /**
     * Sends an email message, containing HTML, using specified options given
     * for the MailServer settings.
     *
     * @param from The address that sends the E-mail message.
     * @param to The address that the E-mail message is sent.
     * @param subject The E-mail subject.
     * @param message The content contained in the E-mail message.
     * @param options SMTPHostName, SMTPPort, SMTPUser, SMTPUserPassword.
     * @throws MessagingException in case where the module was unable to send the e-mail.
     */
    void sendHtmlEmail(String from, String to, String subject, String message, Map<String, Set<String>> options)
            throws MessagingException;

    /**
     * Sends an email  message using specified
     * options given for the MailServer settings
     *
     * @param from The address that sends the E-mail message
     * @param to The address that the E-mail message is sent
     * @param subject The E-mail subject
     * @param message The content contained in the E-mail message
     * @param options SMTPHostName, SMTPPort, SMTPUser, SMTPUserPassword
     * @throws MessagingException in case where the module was unable to send the e-mail
     */
    void sendEmail(String from, String to, String subject,
                          String message, Map<String, Set<String>> options) throws MessagingException;

    /**
     * Sends an email message using default MailServer settings
     * @param to The address that the E-mail message is sent
     * @param subject The E-mail subject
     * @param message The content contained in the E-mail message
     * @throws MessagingException in the case where the module was unable to send the e-mail
     */
    void sendEmail(String to, String subject, String message) throws MessagingException;

    /**
     * Sends an email message using default MailServer settings.
     *
     * @param to
     *         the address that the email message is sent
     * @param subject
     *         the E-mail subject
     * @param message
     *         the content contained in the email message
     * @param mimeType
     *         the mime type to be used for the email
     *
     * @throws MessagingException
     *         in the case where the module was unable to send the email
     */
    void sendEmail(String to, String subject, String message, String mimeType) throws MessagingException;

}