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

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2AssertionSigner
 *
 * Code modeled after the FMSigProvider, and the enveloped signature creation found here:
 * https://svn.apache.org/repos/asf/santuario/xml-security-java/trunk/samples/org/apache/xml/security/samples/signature/CreateSignature.java
 */
public class SAML2AssertionSignerImpl implements SAML2AssertionSigner {
    /*
        Used as constructor parameter to XMLSignature. The BaseURI is used for network identifiers for signatures.
       See https://santuario.apache.org/javafaq.html for details. We use an empty BaseURI as our signature is enveloped
       in the SAML assertion.
     */
    public static final String EMPTY_BASE_URI = "";
    public Element signSAML2Assertion(Document saml2Document, String assertionId, PrivateKey signingKey, X509Certificate certificate,
                                      String signatureAlgorithm, String canonicalizationAlgorithm) throws TokenCreationException {
        /*
        Sets the default xml namespace prefix for all signature elements
         */
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
        } catch (XMLSecurityException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught setting default namespace prefix for all signature elements in SAM2AssertionSignerImpl: " + e, e);
        }

        Element documentRoot = saml2Document.getDocumentElement();
        documentRoot.setIdAttribute(SAML2Constants.ID, true);
        XMLSignature xmlSignature = null;
        try {
            xmlSignature = new XMLSignature(
                        saml2Document, EMPTY_BASE_URI, signatureAlgorithm, canonicalizationAlgorithm);
        } catch (XMLSecurityException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught constructing XMLSignature instance in SAM2AssertionSignerImpl: " + e, e);
        }

        /*
        The following block of code adds the saml2:Signature element immediately after the Issuer element.
        The Signature, Conditions, Subject, Issuer, AttributeStatement, etc elements are all
        siblings of the root document. This while loop will go through the saml elements, exiting once the Issuer element
         is found, or the last sibling is traversed. If the Issuer element is encountered prior to the last element, the Signature element will
         be inserted directly thereafter. Otherwise, the Signature will simply be added as a child of the root.
         */
        Node samlElement = documentRoot.getFirstChild();
        while (samlElement != null &&
                (samlElement.getLocalName() == null ||
                        !samlElement.getLocalName().equals("Issuer"))) {
            samlElement = samlElement.getNextSibling();
        }
        Node nextSibling = null;
        if (samlElement != null) {
            nextSibling = samlElement.getNextSibling();
        }
        if (nextSibling == null) {
            documentRoot.appendChild(xmlSignature.getElement());
        } else {
            documentRoot.insertBefore(xmlSignature.getElement(), nextSibling);
        }

        /*
        Adding the Offline ResourceResolver so that that namespace url references don't incur network traffic.
         */
        xmlSignature.getSignedInfo().addResourceResolver(
                new com.sun.identity.saml.xmlsig.OfflineResolver());

        /*
        See section 5.4.4 of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf for the
        choice of transforms. The bottom line is that only
        the following transforms should be specified:
        http://www.w3.org/2000/09/xmldsig#enveloped-signature  - corresponds to TRANSFORM_ENVELOPED_SIGNATURE
        and
        (http://www.w3.org/2001/10/xml-exc-c14n# or http://www.w3.org/2001/10/xml-exc-c14n#WithComments)
        The latter two are simply the two supported canonicalization algorithms.
         */
        Transforms transforms = new Transforms(saml2Document);
        try {
            transforms.addTransform(
                    Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        } catch (TransformationException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught adding transform in SAM2AssertionSignerImpl: " + e, e);
        }
        /*
        Specify the xml canonicalization algorithm as one of the transforms, as that is one of the transforms which
        must be performed to create the digest-ready assertion document.
         */
        try {
            transforms.addTransform(canonicalizationAlgorithm);
        } catch (TransformationException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught adding transform in SAM2AssertionSignerImpl: " + e, e);
        }

        /*
        Adds the ds:Reference element, encapsulating the ds:Transforms, and ds:DigestMethod and ds:DigestValue
        to the encapsulating assertion.
        See section 5.4.2 of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf for the format of
        the reference.
         */
        String assertionRef = "#" + assertionId;
        try {
            xmlSignature.addDocument(
                    assertionRef,
                    transforms,
                    Constants.ALGO_ID_DIGEST_SHA1);
        } catch (XMLSignatureException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught adding reference element in SAM2AssertionSignerImpl: " + e, e);
        }
        if (certificate != null) {
            try {
                xmlSignature.addKeyInfo(certificate);
            } catch (XMLSecurityException e) {
                throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                        "Exception caught adding KeyInfo in SAM2AssertionSignerImpl: " + e, e);
            }
        }
        try {
            xmlSignature.sign(signingKey);
        } catch (XMLSignatureException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught signing assertion in SAM2AssertionSignerImpl: " + e, e);
        }
        return xmlSignature.getElement();
    }
}
