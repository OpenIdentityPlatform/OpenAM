/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SMSGateway.java,v 1.2 2009/06/03 20:46:51 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.authentication.modules.hotp;

import com.sun.identity.authentication.spi.AuthLoginException;
import java.util.Map;

public interface SMSGateway {

    /**
     * Sends a SMS message to the phone with the code
     * <p>
     *
     * @param from The address that sends the SMS message
     * @param to The address that the SMS message is sent
     * @param subject The SMS subject
     * @param message The content contained in the SMS message
     * @param code The code in the SMS message
     * @param options The SMS gateway options defined in the HOTP authentication
     * module
     * @throws AuthLoginException In case the module was unable to send the SMS
     */
    void sendSMSMessage(String from, String to, String subject,
        String message, String code, Map options) throws AuthLoginException;

    /**
     * Sends an email  message to the mail with the code
     * <p>
     *
     * @param from The address that sends the E-mail message
     * @param to The address that the E-mail message is sent 
     * @param subject The E-mail subject 
     * @param message The content contained in the E-mail message
     * @param code The code in the E-mail message
     * @param options The SMS gateway options defined in the HOTP authentication
     * module
     * @throws AuthLoginException In case the module was unable to send the e-mail
     */
    void sendEmail(String from, String to, String subject,
        String message, String code, Map options) throws AuthLoginException;
}

