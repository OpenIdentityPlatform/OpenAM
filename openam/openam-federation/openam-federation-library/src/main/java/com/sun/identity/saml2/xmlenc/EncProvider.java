/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: EncProvider.java,v 1.3 2008/06/25 05:48:03 qcheng Exp $
 *
 */


package com.sun.identity.saml2.xmlenc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.Key;
import javax.crypto.SecretKey;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * <code>EncProvider</code> is an interface for encrypting and 
 * decrypting SAML2 XML documents.
 */ 
public interface EncProvider {
 
    /**
     * Encrypts the root element of the given XML document.
     * @param xmlString String representing an XML document whose root
     *                  element is to be encrypted.
     * @param recipientPublicKey Public key used to encrypt the data encryption
     *                           (secret) key, it is the public key of the
     *                           recipient of the XML document to be encrypted.
     * @param dataEncAlgorithm Data encryption algorithm.
     * @param dataEncStrength Data encryption strength.
     * @param recipientEntityID Unique identifier of the recipient, it is used
     *                          as the index to the cached secret key so that
     *                          the key can be reused for the same recipient;
     *                          It can be null in which case the secret key will
     *                          be generated every time and will not be cached
     *                          and reused. Note that the generation of a secret
     *                          key is a relatively expensive operation.
     * @param outerElementName Name of the element that will wrap around the
     *                         encrypted data and encrypted key(s) sub-elements
     * @return org.w3c.dom.Element Root element of the encypted document; The
     *                             name of this root element is indicated by
     *                             the last input parameter
     * @exception SAML2Exception if there is an error during the encryption
     *                           process
     */
    public Element encrypt(
        String xmlString,
	Key recipientPublicKey,
        String dataEncAlgorithm,
        int dataEncStrength,
	String recipientEntityID,
	String outerElementName)
    throws SAML2Exception;

    /**
     * Encrypts the root element of the given XML document.
     * @param xmlString String representing an XML document whose root
     *                  element is to be encrypted.
     * @param recipientPublicKey Public key used to encrypt the data encryption
     *                           (secret) key, it is the public key of the
     *                           recipient of the XML document to be encrypted.
     * @param secretKey the secret key used to encrypted data.
     * @param dataEncAlgorithm Data encryption algorithm.
     * @param dataEncStrength Data encryption strength.
     * @param recipientEntityID Unique identifier of the recipient, it is used
     *                          as the index to the cached secret key so that
     *                          the key can be reused for the same recipient;
     *                          It can be null in which case the secret key will
     *                          be generated every time and will not be cached
     *                          and reused. Note that the generation of a secret
     *                          key is a relatively expensive operation.
     * @param outerElementName Name of the element that will wrap around the
     *                         encrypted data and encrypted key(s) sub-elements
     * @return org.w3c.dom.Element Root element of the encypted document; The
     *                             name of this root element is indicated by
     *                             the last input parameter
     * @exception SAML2Exception if there is an error during the encryption
     *                           process
     */
    public Element encrypt(
        String xmlString,
	Key recipientPublicKey,
        SecretKey secretKey,
        String dataEncAlgorithm,
        int dataEncStrength,
	String recipientEntityID,
	String outerElementName)

	throws SAML2Exception;

    /**
     * Returns the secret key that encrypts encrypted data and is encrypted
     * with recipient's public key in the XML document.
     * @param xmlString String representing an XML document with encrypted
     *     secret key.
     * @param recipientPrivateKey Private key used to decrypt the secret key
     * @return the secret key.
     * @exception SAML2Exception if there is an error during the decryption
     *     process
     */
    public SecretKey getSecretKey(
        String xmlString,
        Key recipientPrivateKey)
    throws SAML2Exception;

    /**
     * Decrypts an XML document that contains encrypted data.
     * @param xmlString String representing an XML document with encrypted
     *                  data.
     * @param recipientPrivateKey Private key used to decrypt the secret key
     * @return org.w3c.dom.Element Decrypted XML document. For example, if
     *                             the input document's root element is
     *                             EncryptedID, then the return element will
     *                             be NameID
     * @exception SAML2Exception if there is an error during the decryption
     *                           process
     */
    public Element decrypt(
        String xmlString,
        Key recipientPrivateKey)
     throws SAML2Exception;
} 
