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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap;

import javax.security.auth.callback.Callback;

/**
 * Callback class used to solicit and obtain the OpenAM session id from the OpenAMSessionTokenClientAssertionBuilder.
 * The same CallbackHandler instance (an instance of the SoapSTSConsumerCallbackHandler in these examples) passed as
 * a constructor parameter to the SoapSTSConsumer, will be set as the constructor parameter in the
 * OpenAMSessionTokenClientAssertionBuilder. The OpenAMSessionTokenClientAssertionBuilder will create an instance of
 * the OpenAMSessionTokenCallback class and pass it to the CallbackHandler to obtain the OpenAM session id.
 */
public class OpenAMSessionTokenCallback implements Callback {
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
