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

import com.iplanet.services.util.JCEEncryption;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.soap.config.SoapSTSModule;
import org.forgerock.util.encode.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;

/**
 * @see org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentCredentialsAccess
 */
public class SoapSTSAgentCredentialsAccessImpl implements SoapSTSAgentCredentialsAccess {
    private final String agentUsername;
    private final String agentPassword;

    @Inject
    SoapSTSAgentCredentialsAccessImpl(@Named(SoapSTSModule.SOAP_STS_AGENT_USERNAME_PROPERTY_KEY) String agentUsername,
                                      @Named(SoapSTSModule.SOAP_STS_AGENT_PASSWORD_PROPERTY_KEY) String encryptedAgentPassword,
                                      KeyStore soapSTSInternalKeystore) {
        this.agentUsername = agentUsername;
        try {
            this.agentPassword = decryptAgentPassword(encryptedAgentPassword, soapSTSInternalKeystore);
        } catch (STSInitializationException e) {
            throw new RuntimeException("Unable to decrypt the agent password based on keystore state bundled in " +
                    "deployable soap-sts .war: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAgentUsername() {
        return agentUsername;
    }

    @Override
    public String getAgentPassword() throws STSInitializationException {
        return agentPassword;
    }

    private String decryptAgentPassword(String encryptedAgentPassword, KeyStore soapSTSInternalKeystore) throws STSInitializationException {
        try {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry)soapSTSInternalKeystore.getEntry(SharedSTSConstants.AM_INTERNAL_PEK_ALIAS,
                    new KeyStore.PasswordProtection(SharedSTSConstants.AM_INTERNAL_SOAP_STS_KEYSTORE_PW.toCharArray()));
            JCEEncryption jceEncryption = new JCEEncryption();
            final byte[] decodedPassword = Base64.decode(encryptedAgentPassword);
            try {
                jceEncryption.setPassword(new String(entry.getSecretKey().getEncoded(), StandardCharsets.UTF_8));
                final byte[] decryptedPassword = jceEncryption.decrypt(decodedPassword);
                return new String(decryptedPassword, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
            }
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
    }
}
