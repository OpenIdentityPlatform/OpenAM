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

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.openam.sts.soap.config.SoapSTSModule;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @see org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentCredentialsAccess
 * TODO: note that this class is currently a placeholder, and the correct implementation will occur as part of AME-5703 -
 * the installation of the soap-sts context. Currently, the config contains the password in plain-text.
 */
public class SoapSTSAgentCredentialsAccessImpl implements SoapSTSAgentCredentialsAccess {
    private final String agentUsername;
    private final String agentPassword;
    private final String encryptionKey;

    @Inject
    SoapSTSAgentCredentialsAccessImpl(@Named(SoapSTSModule.SOAP_STS_AGENT_USERNAME_PROPERTY_KEY) String agentUsername,
                                      @Named(SoapSTSModule.SOAP_STS_AGENT_PASSWORD_PROPERTY_KEY) String agentPassword,
                                      @Named(SoapSTSModule.SOAP_STS_AGENT_ENCRYPTION_KEY_PROPERTY_KEY) String encryptionKey) {
        this.agentUsername = agentUsername;
        this.agentPassword = agentPassword;
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String getAgentUsername() {
        return agentUsername;
    }

    @Override
    public String getAgentPassword() {
        return agentPassword;
    }
}
