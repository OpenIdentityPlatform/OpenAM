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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertTrue;

public class STSKeyProviderImplTest {

    @Test
    public void testCreation() throws UnsupportedEncodingException {
        STSKeyProvider keyProvider = new STSKeyProviderImpl(createKeystoreConfig());
        assertTrue(keyProvider.getPrivateKey("test", "changeit") != null);
    }

    private KeystoreConfig createKeystoreConfig() throws UnsupportedEncodingException {
            return KeystoreConfig.builder()
                    .fileName("keystore.jks")
                    .password("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .encryptionKeyAlias("test")
                    .signatureKeyAlias("test")
                    .encryptionKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signatureKeyPassword("changeit".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .build();
    }
}
