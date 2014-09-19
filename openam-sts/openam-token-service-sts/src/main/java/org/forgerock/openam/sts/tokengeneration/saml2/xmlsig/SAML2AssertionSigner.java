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
 * @deprecated In the standard consumption of the OpenAM SAML2 generation libraries (e.g. IDPSSOUtil), the AssertionImpl
 * class is consumed directly to sign the assertion. Assertion signing is ultimately delegated to the FMSigProvider class.
 * I initially chose not to go this path, but rather implement my own SAML2AssertionSigner interface and implementation,
 * because the signature algorithm and canonicalization algorithm are set globally in the FMSigProvider. However, it
 * seems that the signature algorithm is ultimately a function of the type of the private key (DSA or RSA), and the
 * customization of the canonicalization algorithm does not seem to be a particularly desirable feature.
 *
 * Furthermore, if an entire assertion is to be encrypted, this encryption must take place AFTER the assertion is signed.
 * Thus an assertion must be re-constituted from signing interface that generates an Element. This is certainly possible
 * using the AssertionImpl ctor, but this also requires that my token generation functionality deal with the AssertionImpl
 * class, rather than the Assertion interface. I would rather be constrained by a globally-defined canonicalization
 * algorithm, than have to code directly to the AssertionImpl interface. However, this interface, and the corresponding
 * implementation, will be kept around in case customer input indicates that canonicalization algorithm specification is
 * desirable. It is hard to imagine that any customer plugs in their own, custom Assertion implementation, but I'd rather
 * code to an interface, and thus will consume the assertion signing functionality encapsulated in the Assertion interface.
 *
 */
public interface SAML2AssertionSigner {
    /**
     * @param saml2Document The to-be-signed document
     * @param assertionId Used to refer the signature to the enveloping assertion
     * @param signingKey The PrivateKey used to sign the assertion
     * @param certificate The X509Certificate corresponding to the PrivateKey
     * @param signatureAlgorithm The to-be-used signature algorithm. Ultimately, the set of supported signature algorithms is determined
     *                           by a hash maintained in the org.apache.xml.security.signature.SignatureAlgorithm class. This interface
     *                           and corresponding implementation will not validate that the specified signature algorithm is
     *                           indeed supported, but rather rely on the XMLSecurityException thrown by the SignatureAlgorithm class.
     *                           Note however, that the SAML2Constants will ultimately originate this value, and this is where any
     *                           validation may take place.
     * @param canonicalizationAlgorithm The to-be-used canonicalization algorithm
     * @return The Element containing the signature, enveloped in the Element containing the assertion
     * @throws TokenCreationException
     */
    Element signSAML2Assertion(Document saml2Document, String assertionId, PrivateKey signingKey, X509Certificate certificate,
                               String signatureAlgorithm, String canonicalizationAlgorithm) throws TokenCreationException;
}
