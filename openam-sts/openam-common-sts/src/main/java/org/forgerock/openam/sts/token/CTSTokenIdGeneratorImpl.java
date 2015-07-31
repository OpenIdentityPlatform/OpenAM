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
package org.forgerock.openam.sts.token;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @see CTSTokenIdGenerator
 */
public class CTSTokenIdGeneratorImpl implements CTSTokenIdGenerator {
    private static final String SHA1 = "SHA-1";
    private static final String ID_ATTRIBUTE = "ID";
    private static final String ASSERTION_LOCAL_NAME = "Assertion";
    private static final String ENCRYPTED_ASSERTION_LOCAL_NAME = "EncryptedAssertion";
    private static final String ENCRYPTED_DATA = "EncryptedData";
    private static final String CIPHER_DATA = "CipherData";
    private static final String CIPHER_VALUE = "CipherValue";

    private final XMLUtilities xmlUtilities;

    @Inject
    CTSTokenIdGeneratorImpl(XMLUtilities xmlUtilities) {
        this.xmlUtilities = xmlUtilities;
    }


    @Override
    public String generateTokenId(TokenType tokenType, String tokenString) throws TokenIdGenerationException {
        if (TokenType.SAML2.equals(tokenType)) {
            return generateSAML2AssertionId(tokenString);
        } else if (TokenType.OPENIDCONNECT.equals(tokenType)) {
            return generateOpenIdConnectTokenId(tokenString);
        } else {
            throw new TokenIdGenerationException(ResourceException.INTERNAL_ERROR,
                    "Illegal state: an id for tokens of type " + tokenType +
                            " cannot be generated. The token string: " + tokenString);
        }
    }

    private String generateOpenIdConnectTokenId(String openIdConnectToken) throws TokenIdGenerationException {
        try {
            byte[] digest = MessageDigest.getInstance(SHA1).digest(openIdConnectToken.getBytes(AMSTSConstants.UTF_8_CHARSET_ID));
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new TokenIdGenerationException(ResourceException.INTERNAL_ERROR,
                    "Unexpected error: no SHA-1 hash algorithm available while generating token id for OIDC token: "
                            + e.getMessage(), e);
        }
    }

    /**
     * Note that this method must handle both the case where the assertion is encrypted, and unencrypted. In the unencrypted
     * case, the ID attribute of the Assertion element can be used as the id. In the encrypted case no such ID is available,
     * as the assertion is encrypted. For encrypted assertions, the encrypted assertion data, a base64 string at
     * EncryptedAssertion->EncryptedData->CipherData->CipherValue, will be used as the input to generate a SHA-1 digest.
     */
    private String generateSAML2AssertionId(String saml2Assertion) throws TokenIdGenerationException {
        Element samlTokenElement = xmlUtilities.stringToDocumentConversion(saml2Assertion).getDocumentElement();
        final String localName = samlTokenElement.getLocalName();
        if (ASSERTION_LOCAL_NAME.equals(localName)) {
            return generateIdentifierFromUnencryptedSAML2Assertion(samlTokenElement);
        } else if (ENCRYPTED_ASSERTION_LOCAL_NAME.equals(localName)) {
            return generateIdentifierFromEncryptedSAML2Assertion(samlTokenElement);
        } else {
            throw new TokenIdGenerationException(ResourceException.BAD_REQUEST,
                    "Unexpected local name in to-be-validated SAML2 assertion: " + localName);
        }
    }

    private String generateIdentifierFromUnencryptedSAML2Assertion(Element samlTokenElement) throws TokenIdGenerationException {
        final String idAttribute = samlTokenElement.getAttribute(ID_ATTRIBUTE);
        if (StringUtils.isEmpty(idAttribute)) {
            throw new TokenIdGenerationException(ResourceException.INTERNAL_ERROR,
                    "ID attribute in to-be-validated SAML2 assertion null or empty.");
        } else {
            return idAttribute;
        }
    }

    private String generateIdentifierFromEncryptedSAML2Assertion(Element samlTokenElement) throws TokenIdGenerationException {
        final String encryptedAssertion = getCipherValueElement(samlTokenElement);
        try {
            byte[] digest = MessageDigest.getInstance(SHA1).digest(encryptedAssertion.getBytes(AMSTSConstants.UTF_8_CHARSET_ID));
            return DatatypeConverter.printHexBinary(digest);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new TokenIdGenerationException(ResourceException.INTERNAL_ERROR,
                "Unexpected error: no SHA-1 hash algorithm available while generating token id for encrypted " +
                        "SAML2 assertion: " + e.getMessage(), e);
        }
    }

    /*
    For an encrypted assertion, I need to get the contents of the CipherValue element, as that contains the base64 representations
    of the encrypted SAML2 assertion. That requires some work traversing down into the SAML2 assertion Element.
    See section 2.3.4 of http://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf and
    http://www.w3.org/TR/2002/REC-xmlenc-core-20021210/Overview.html#sec-eg-Symmetric-Key for details
     */
    private String getCipherValueElement(Element samlTokenElement) throws TokenIdGenerationException {
        Element child = getChildElement(samlTokenElement, ENCRYPTED_DATA);
        child = getChildElement(child, CIPHER_DATA);
        child = getChildElement(child, CIPHER_VALUE);
        return child.getTextContent();
    }

    private Element getChildElement(Element parent, String childLocalName) throws TokenIdGenerationException {
        NodeList children = parent.getChildNodes();
        for (int ndx = 0; ndx < children.getLength(); ndx++) {
            Node child = children.item(ndx);
            if (child instanceof Element) {
                if (childLocalName.equals(child.getLocalName())) {
                    return (Element)child;
                }
            }
        }
        throw new TokenIdGenerationException(ResourceException.INTERNAL_ERROR, "In CTSTokenIdGeneratorImpl, generating a " +
                "CTS token id for an encrypted " + "SAML2 assertion, could not find child element with local name: " + childLocalName);
    }
}
