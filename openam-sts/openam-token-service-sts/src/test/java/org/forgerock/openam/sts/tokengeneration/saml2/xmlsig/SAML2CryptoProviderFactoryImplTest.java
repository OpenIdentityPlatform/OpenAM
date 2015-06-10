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

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertTrue;

public class SAML2CryptoProviderFactoryImplTest {
    private static final boolean SIGN_ASSERTION = true;

    @Test
    public void testCryptoProviderType() throws TokenCreationException, UnsupportedEncodingException {
        SAML2CryptoProviderFactoryImpl saml2CryptoProviderFactory = new SAML2CryptoProviderFactoryImpl();
        assertTrue(saml2CryptoProviderFactory.createSAML2CryptoProvider(createSAML2Config(SIGN_ASSERTION)) instanceof SAML2CryptoProviderImpl);
        assertTrue(saml2CryptoProviderFactory.createSAML2CryptoProvider(createSAML2Config(!SIGN_ASSERTION)) instanceof FauxSAML2CryptoProvider);
        assertTrue(saml2CryptoProviderFactory.createSAML2CryptoProvider(null) instanceof FauxSAML2CryptoProvider);
    }

    private SAML2Config createSAML2Config(boolean signAssertion) throws UnsupportedEncodingException {

        return SAML2Config.builder()
                .nameIdFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")
                .spEntityId("http://host.com/sp/entity/id")
                .keystoreFile("keystore.jks")
                .keystorePassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .encryptionKeyAlias("test")
                .signatureKeyAlias("test")
                .signAssertion(signAssertion)
                .idpId("da_idp")
                .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
    }
}
