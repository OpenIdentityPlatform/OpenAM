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
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;
import org.forgerock.openam.sts.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory
 *
 * Note on the approach for generating the KeyInfo element implemented in this class:
 * The AssertionFactory.createKeyInfoConfirmationData methods never get called. KeyInfo objects are
 * created for holder-of-key assertions in the OpenAM code-base in two places: FAMSTSTokenProvider.createKeyInfo and
 * SAML2Token.createKeyInfo. The FAMSTSTokenProvider uses the org.apache.xml.security.keys.KeyInfo class. The SAML2Token
 * adopts a more low-level, direct XML manipulation approach. I will opt for delegating this complexity to the KeyInfo
 * class.
 */
public class KeyInfoFactoryImpl implements KeyInfoFactory {
    private final XMLUtilities xmlUtilities;

    @Inject
    public KeyInfoFactoryImpl(XMLUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }

    /*
    This method modeled after the example here:
    https://svn.apache.org/repos/asf/santuario/xml-security-java/trunk/samples/org/apache/xml/security/samples/keys/CreateKeyInfo.java
     */
    @Override
    public Element generatePublicKeyInfo(X509Certificate recipientCert) throws ParserConfigurationException, XMLSecurityException {
        Document sharedDocument = xmlUtilities.newSafeDocument(XMLUtils.isValidating());
        KeyInfo keyInfo = new KeyInfo(sharedDocument);
        sharedDocument.appendChild(keyInfo.getElement());
        X509Data x509Data = new X509Data(sharedDocument);
        keyInfo.add(x509Data);
        x509Data.addCertificate(recipientCert);
        return keyInfo.getElement();
    }
}
