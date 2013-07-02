/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
 */
package org.forgerock.openam.services.email;

import java.util.Map;
import javax.mail.*;

public interface MailServer {

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
    public void sendEmail(String from, String to, String subject,
                          String message, Map options) throws MessagingException;

    /**
     * Sends an email message using default MailServer settings
     * @param to The address that the E-mail message is sent
     * @param subject The E-mail subject
     * @param message The content contained in the E-mail message
     * @throws MessagingException in the case where the module was unable to send the e-mail
     */
    public void sendEmail(String to, String subject, String message) throws MessagingException;
}