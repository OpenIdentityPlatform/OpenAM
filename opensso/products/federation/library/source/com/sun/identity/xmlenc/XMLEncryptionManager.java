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
 * $Id: XMLEncryptionManager.java,v 1.5 2009/08/29 07:30:38 mallas Exp $
 *
 */


package com.sun.identity.xmlenc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.Key;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;

public class XMLEncryptionManager {

    private EncryptionProvider ep = null;
    private static XMLEncryptionManager instance = new XMLEncryptionManager(); 

    /**
     * Constructor
     */ 
    protected XMLEncryptionManager() {
        try {
            String encryptClass = SystemPropertiesManager.get(
            EncryptionConstants.XML_ENCRYPTION_PROVIDER_KEY,
            "com.sun.identity.xmlenc.AMEncryptionProvider");
            ep = (EncryptionProvider) Class.forName(encryptClass).newInstance();
            String kprovider = SystemPropertiesManager.get(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                SAMLConstants.JKS_KEY_PROVIDER);
            ep.initialize((KeyProvider) Class.forName(kprovider).newInstance());
        } catch (Exception e) {
            EncryptionUtils.debug.error("XMLEncryptionManager:constructor" +
            " Exception in constructing xml encryption manager", e);
        }

    }

    /**
     * Constructor
     */
    protected XMLEncryptionManager (
        EncryptionProvider encProvider,
        KeyProvider keyProvider) {
        try {
            encProvider.initialize(keyProvider);
            ep = encProvider;
        } catch (Exception e) {
            EncryptionUtils.debug.error("XMLEncryptionManager:constructor" +
            " Exception in constructing xml encryption manager", e);
        }
    }
 
    /**
     * Gets the instance of <code>XMLEncryptionManager</code> with default
     * <code>KeyProvider</code> and <code>EncryptionProvider</code>. 
     * @return <code>XMLEncryptionManager</code>
     */
    public static XMLEncryptionManager getInstance() {
        return instance;
    }

    /**
     * Gets the instance of <code>XMLEncryptionManager</code> with given 
     * <code>KeyProvider</code> and <code>EncryptionProvider</code>. 
     * @return <code>XMLEncryptionManager</code>
     */
    public static XMLEncryptionManager getInstance(
           EncryptionProvider encProvider,
           KeyProvider keyProvider) {
        return new XMLEncryptionManager(encProvider, keyProvider);
    }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption key size
     * @param certAlias KeyEncryption Key cert alias.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.lang.String certAlias)
     throws EncryptionException {
        return ep.encryptAndReplace(doc, element, secretKeyAlg, keySize,
              certAlias, 0);
    }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption key size
     * @param certAlias KeyEncryption Key cert alias.
     * @param providerID Unique provider ID. 
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.lang.String certAlias,
        java.lang.String providerID)
     throws EncryptionException {
        return ep.encryptAndReplace(doc, element, secretKeyAlg, keySize,
              certAlias, 0, providerID);
     }

    /**
     * Encrypts the given ResourceID XML element in a given XML Context
     * document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption key size
     * @param certAlias KeyEncryption Key cert alias.
     * @param providerID Unique provider ID. 
     * @return org.w3c.dom.Document XML Document for EncryptedResourceID.
     */
    public org.w3c.dom.Document encryptAndReplaceResourceID(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.lang.String certAlias,
        java.lang.String providerID)
     throws EncryptionException {
        return ep.encryptAndReplaceResourceID(doc, element, secretKeyAlg,
		keySize, certAlias, 0, providerID);
     }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption key size
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength.
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.lang.String certAlias,
        int kekStrength)
     throws EncryptionException {
        return ep.encryptAndReplace(doc, element, secretKeyAlg, keySize,
           certAlias, kekStrength);
    }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *		for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.security.Key kek,
        int kekStrength)
     throws EncryptionException {
        return ep.encryptAndReplace(doc, element, secretKeyAlg, keySize,
           kek, kekStrength, null);
    }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength
     * @param providerID provider entityID
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *		for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.security.Key kek,
        int kekStrength,
        String providerID)
     throws EncryptionException {
        return ep.encryptAndReplace(
            doc, element, secretKeyAlg, keySize,
            kek, kekStrength, providerID);
    }

    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keySize Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *		for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplaceResourceID(
        org.w3c.dom.Document doc,
        org.w3c.dom.Element element,
        java.lang.String secretKeyAlg,
        int keySize,
        java.security.Key kek,
        int kekStrength,
        String providerID)
        throws EncryptionException 
    {
        return ep.encryptAndReplaceResourceID(
            doc, element, secretKeyAlg, keySize,
            kek, kekStrength, providerID);
    }

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
     throws EncryptionException {
        return ep.encryptAndReplaceWSSElements(doc,elmMap,
                                           encDataEncAlg,encDataEncAlgStrength,
                                           certAlias,kekStrength,tokenType,
                                           providerID);
    }

    /**
     * Decrypts and replaces the XML element in a given XML DOM Document.
     * @param encryptedDoc Encrypted XML Document.
     * @param kekAlias Key Encryption Key Cert Alias.
     *
     * @return org.w3.dom.Document Decrypted XML Document.
     * @exception XMLEncryptionException
     */
    public org.w3c.dom.Document decryptAndReplace(
        org.w3c.dom.Document encryptedDoc,
        java.lang.String kekAlias)
     throws EncryptionException {
        return ep.decryptAndReplace(encryptedDoc, kekAlias); 
    }

    /**
     * Decrypts an XML Document that contains encrypted data.
     * @param encryptedDoc XML Document with encrypted data.
     * @param privKey Key Encryption Key used for encryption.
     *
     * @return org.w3c.dom.Document Decrypted XML Document.
     */
    public Document decryptAndReplace(
        Document encryptedDoc,
        java.security.Key privKey)
      throws EncryptionException {
        return ep.decryptAndReplace(encryptedDoc, privKey); 
    }
    
    /**
     * Decrypts key and returns the associated secret key.
     * @param encryptedKey the encrypted key.
     * @param certAlias the private key alias that is used to decrypt the key.
     * @return the decrypted secret key.
     */
    public Key decryptKey(Element encryptedKey, String certAlias) {
        return ep.decryptKey(encryptedKey, certAlias);
    }

} 
