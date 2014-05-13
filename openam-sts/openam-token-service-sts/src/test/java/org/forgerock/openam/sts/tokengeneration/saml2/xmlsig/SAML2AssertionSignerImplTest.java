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

import com.sun.identity.shared.xml.XMLUtils;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.slf4j.Logger;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;



public class SAML2AssertionSignerImplTest {
    private static final String RSA_DEFAULT_SIGNATURE_ALGORITHM = XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
    private static final String CANONICALIZATION_ALGORITHM = Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
    private static final String ASSERTION_ID = "s28cf6b911a0b8df238be3160fe680e1e39bd046e0";

    @Test
    public void testSignatureRoundTrip() throws Exception {
        //init santuario
        org.apache.xml.security.Init.init();
        STSKeyProvider keyProvider = getKeyProvider();
        SAML2AssertionSignerImpl signer = new SAML2AssertionSignerImpl();
        Element signedElement = signer.signSAML2Assertion(
                getSAMLDocument(),
                ASSERTION_ID,
                keyProvider.getPrivateKey("test", "changeit"),
                keyProvider.getX509Certificate("test"),
                RSA_DEFAULT_SIGNATURE_ALGORITHM,
                CANONICALIZATION_ALGORITHM
                );
        XMLSignature xmlSignature = new XMLSignature(signedElement, SAML2AssertionSignerImpl.EMPTY_BASE_URI);
        assertTrue(xmlSignature.checkSignatureValue(xmlSignature.getKeyInfo().getX509Certificate()));
    }

    private STSKeyProvider getKeyProvider() throws Exception {
        return new STSKeyProviderImpl(createKeystoreConfig(), mock(Logger.class));
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

    private Document getSAMLDocument() throws IOException {
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/assert.xml")));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return XMLUtils.toDOMDocument(stringBuilder.toString(), null);
    }
}
