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

import org.forgerock.openam.sts.TokenCreationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Encapsulates concerns related to signing SAML2 Assertions.
 *  */
public interface SAML2AssertionSigner {
    /**
     * TODO: verification that the algorithm is indeed supported - does the caller do that, or does the implementation of this
     * interface - and what constitutes the supported list?
     * @param saml2Document The to-be-signed document
     * @param assertionId Used to refer the signature to the enveloping assertion
     * @param signingKey The PrivateKey used to sign the assertion
     * @param certificate The X509Certificate corresponding to the PrivateKey
     * @param signatureAlgorithm The to-be-used signature algorithm
     * @param canonicalizationAlgorithm The to-be-used canonicalization algorithm
     * @return The Element containing the signature, enveloped in the Element containing the assertion
     * @throws TokenCreationException
     */
    Element signSAML2Assertion(Document saml2Document, String assertionId, PrivateKey signingKey, X509Certificate certificate,
                               String signatureAlgorithm, String canonicalizationAlgorithm) throws TokenCreationException;
}
