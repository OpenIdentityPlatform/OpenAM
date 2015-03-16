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

package org.forgerock.openam.sts.tokengeneration.xml;

import com.sun.identity.shared.xml.XMLUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.WSSecEncryptedKey;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;

/**
 * This class provides methods to generate santuario KeyInfo instances corresponding to symmetric and public keys.
 * TODO: all this needs to be tested and validated. Also look at SAML2Token.createKeyInfo
 */
public class KeyInfoFactory {
    /*
    Passed to XMLUtils.getSafeDocumentBuilder to get a non-validating parser
     */
    private static final boolean NON_VALIDATING_PARSER = false;
    public KeyInfo generateSymmetricKeyInfo(
                                        X509Certificate recipientCert,
                                        byte[] secret,
                                        int keyIdentifierType,
                                        String symmetricEncryptionAlgorithm,
                                        String keyWrapAlgorithm,
                                        Crypto encryptionCrypto) throws ParserConfigurationException, WSSecurityException {

        Document doc = XMLUtils.getSafeDocumentBuilder(NON_VALIDATING_PARSER).newDocument();
        WSSecEncryptedKey encrKey = new WSSecEncryptedKey();
        encrKey.setKeyIdentifierType(keyIdentifierType);
        encrKey.setEphemeralKey(secret);
        encrKey.setSymmetricEncAlgorithm(symmetricEncryptionAlgorithm);
        encrKey.setUseThisCert(recipientCert);
        encrKey.setKeyEncAlgo(keyWrapAlgorithm);
        encrKey.prepare(doc, encryptionCrypto);
        Element encryptedKeyElement = encrKey.getEncryptedKeyElement();

        Element keyInfoElement =
                doc.createElementNS(
                        WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN
                );
        keyInfoElement.setAttributeNS(
                WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS
        );
        keyInfoElement.appendChild(encryptedKeyElement);

        return new KeyInfo(doc);
    }

    public KeyInfo generatePublicKeyInfo(X509Certificate recipientCert) throws ParserConfigurationException, XMLSecurityException {
        X509Data x509Data = new X509Data(XMLUtils.getSafeDocumentBuilder(NON_VALIDATING_PARSER).newDocument());
        x509Data.addCertificate(recipientCert);
        KeyInfo keyInfo = new KeyInfo(XMLUtils.getSafeDocumentBuilder(NON_VALIDATING_PARSER).newDocument());
        keyInfo.add(x509Data);
        return keyInfo;
    }
}
