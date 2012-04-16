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
 * $Id: EncryptionProvider.java,v 1.5 2009/08/29 07:30:38 mallas Exp $
 *
 */


package com.sun.identity.xmlenc;

import com.sun.identity.saml.xmlsig.KeyProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <code>EncryptionProvier</code> is an interface for encrypting and 
 * decrypting XML documents.
 */ 
public interface EncryptionProvider{
 
     /**
      * Initializes encryption provider.
      */
     public void initialize(KeyProvider keyProvider) 
     throws EncryptionException;

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keyStrength,
        java.lang.String certAlias,
        int kekStrength)
     throws EncryptionException;

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keyStrength,
        java.lang.String certAlias,
        int kekStrength,
        java.lang.String providerID)
     throws EncryptionException;

    /**
     * Encrypts the given ResourceID XML element in a given XML Context
     * document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength.
     * @return org.w3c.dom.Document EncryptedResourceID XML Document.
     */
    public org.w3c.dom.Document encryptAndReplaceResourceID(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keyStrength,
        java.lang.String certAlias,
        int kekStrength,
        java.lang.String providerID)
     throws EncryptionException;

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength
     * @param providerID Provider ID.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        String secretKeyAlg,
        int keyStrength,
        java.security.Key kek,
        int kekStrength,
        java.lang.String providerID)
     throws EncryptionException;

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength
     * @param providerID Provider ID.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplaceResourceID(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        String secretKeyAlg,
        int keyStrength,
        java.security.Key kek,
        int kekStrength,
        java.lang.String providerID)
     throws EncryptionException;

    /**
     * Encrypts the given WSS XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param elmMap Map of (Element, wsu_id) to be encrypted.
     * @param encDataEncAlg Encryption Key Algorithm.
     * @param encDataEncAlgStrength Encryption Key Strength.
     * @param certAlias Key Encryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength.
     * @param tokenType Security token type.     
     * @param providerID Provider ID.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplaceWSSElements(
        org.w3c.dom.Document doc,
        java.util.Map elmMap,
        java.lang.String encDataEncAlg,
        int encDataEncAlgStrength,
        String certAlias,
        int kekStrength,
        java.lang.String tokenType,
        java.lang.String providerID)
     throws EncryptionException;

    /**
     * Decrypts an XML Document that contains encrypted data.
     * @param encryptedDoc XML Document with encrypted data.
     * @param certAlias Private Key Certificate Alias.
     * @return org.w3c.dom.Document Decrypted XML Document.
     */
    public Document decryptAndReplace(
        Document encryptedDoc,
        java.lang.String certAlias)
     throws EncryptionException;

    /**
     * Decrypts an XML Document that contains encrypted data.
     * @param encryptedDoc XML Document with encrypted data.
     * @param privKey Key Encryption Key used for encryption.
     * @return org.w3c.dom.Document Decrypted XML Document.
     */
    public Document decryptAndReplace(
        Document encryptedDoc,
        java.security.Key privKey)
     throws EncryptionException;
    
    /**
     * Decrypt the given encrypted key.
     * @param encryptedKey the encrypted key element
     * @param certAlias the private key alias
     * @return the key associated with the decrypted key.
     */
    public java.security.Key decryptKey(Element encryptedKey, String certAlias);
} 
