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
 * $Id: AMEncryptionProvider.java,v 1.7 2009/08/29 07:30:38 mallas Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.xmlenc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.HashMap;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.storage.StorageResolver;
import org.apache.xml.security.keys.storage.implementations.KeyStoreResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509CertificateResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509IssuerSerialResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509SKIResolver;
import org.apache.xml.security.keys.keyresolver.implementations.X509SubjectNameResolver;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * <code>AMEncryptionProvider</code> is a class for encrypting and
 * decrypting XML Documents which implements <code>EncryptionProvider</code>.
 */

public class AMEncryptionProvider implements EncryptionProvider {
    
    protected KeyProvider keyProvider = null;
    
    /**
     * A static map contains provider id and symmetric keys as key value pairs.
     * Key generation each time is an expensive operation and using the same
     * key for each provider should be okay.
     */
    protected static Map keyMap = new HashMap();
    
    static {
        org.apache.xml.security.Init.init();
    }
    
    /**
     * Initializes encryption provider.
     */
    public void initialize(KeyProvider keyprovider) throws EncryptionException {
        if(keyprovider == null) {
            EncryptionUtils.debug.error("AMSignatureProvider.initialize: "+
                    "keyprovider is null");
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "nullValues"));
        }
        this.keyProvider = keyprovider;
    }
    
    
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
            throws EncryptionException {
        
        return encryptAndReplace(doc, element, secretKeyAlg, keyStrength,
                keyProvider.getPublicKey(certAlias), kekStrength, null, false);
    }
    
    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength,
     * @param providerID Provider ID.
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
            throws EncryptionException {
        
        return encryptAndReplace(doc, element, secretKeyAlg, keyStrength,
                keyProvider.getPublicKey(certAlias), kekStrength, providerID, false);
    }
    
    /**
     * Encrypts the given ResourceID XML element in a given XML Context
     * document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param certAlias KeyEncryption Key cert alias.
     * @param kekStrength Key Encryption Key Strength,
     * @param providerID Provider ID.
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
            throws EncryptionException {
        
        return encryptAndReplace(doc, element, secretKeyAlg,
                keyStrength, keyProvider.getPublicKey(certAlias),
                kekStrength, providerID, true);
    }
    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength,
     * @param providerID Provider ID
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplace(
            org.w3c.dom.Document doc,
            org.w3c.dom.Element element,
            java.lang.String secretKeyAlg,
            int keyStrength,
            java.security.Key kek,
            int kekStrength,
            String providerID)
            throws EncryptionException {
        return encryptAndReplace(doc, element, secretKeyAlg, keyStrength,
                kek, kekStrength, providerID, false);
    }
    
    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength,
     * @param providerID Provider ID
     * @return org.w3c.dom.Document XML Document replaced with encrypted data
     *         for a given XML element.
     */
    public org.w3c.dom.Document encryptAndReplaceResourceID(
            org.w3c.dom.Document doc,
            org.w3c.dom.Element element,
            java.lang.String secretKeyAlg,
            int keyStrength,
            java.security.Key kek,
            int kekStrength,
            String providerID)
            throws EncryptionException {
        return encryptAndReplace(doc, element, secretKeyAlg, keyStrength,
                kek, kekStrength, providerID, true);
        
    }
    /**
     * Encrypts the given XML element in a given XML Context document.
     * @param doc the context XML Document.
     * @param element Element to be encrypted.
     * @param secretKeyAlg Encryption Key Algorithm.
     * @param keyStrength Encryption Key Strength.
     * @param kek Key Encryption Key.
     * @param kekStrength Key Encryption Key Strength,
     * @param providerID Provider ID
     * @param isEncryptResourceID A flag indicates whether it's to encrypt
     * 		ResourceID or not.
     * @return org.w3c.dom.Document EncryptedResourceID XML Document if
     * 		isEncryptResourceID is set. Otherwise, return the XML Document
     *		replaced with encrypted data for a given XML element.
     */
    private org.w3c.dom.Document encryptAndReplace(
            org.w3c.dom.Document doc,
            org.w3c.dom.Element element,
            java.lang.String secretKeyAlg,
            int keyStrength,
            java.security.Key kek,
            int kekStrength,
            String providerID,
            boolean isEncryptResourceID)
            throws EncryptionException {
        
        if(doc == null || element == null || kek == null) {
            EncryptionUtils.debug.error("AMEncryptionProvider.encryptAnd" +
                    "Replace: Null values");
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "nullValues"));
        }
        
        SecretKey secretKey = null;
        String secretKeyAlgShortName = getEncryptionAlgorithmShortName(secretKeyAlg);
        if(providerID != null) {
            if(keyMap.containsKey(providerID)) {
                secretKey = (SecretKey)keyMap.get(providerID);
            } else {
                secretKey = generateSecretKey(secretKeyAlgShortName, keyStrength);
                keyMap.put(providerID, secretKey);
            }
        } else {
            secretKey = generateSecretKey(secretKeyAlgShortName, keyStrength);
        }
        
        if(secretKey == null) {
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "generateKeyError"));
        }
        
        try {
            XMLCipher cipher = null;
            String keyEncAlg = kek.getAlgorithm();
            
            if(keyEncAlg.equals(EncryptionConstants.RSA)) {
                cipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
                
            } else if(keyEncAlg.equals(EncryptionConstants.TRIPLEDES)) {
                cipher = XMLCipher.getInstance(XMLCipher.TRIPLEDES_KeyWrap);
                
            } else if(keyEncAlg.equals(EncryptionConstants.AES)) {
                
                if (kekStrength == 0 || kekStrength == 128) {
                    cipher = XMLCipher.getInstance(XMLCipher.AES_128_KeyWrap);
                } else if(kekStrength == 192) {
                    cipher = XMLCipher.getInstance(XMLCipher.AES_192_KeyWrap);
                } else if(kekStrength == 256) {
                    cipher = XMLCipher.getInstance(XMLCipher.AES_256_KeyWrap);
                } else {
                    throw new EncryptionException(
                            EncryptionUtils.bundle.getString("invalidKeyStrength"));
                }
                
            } else {
                throw new EncryptionException(
                        EncryptionUtils.bundle.getString("unsupportedKeyAlg"));
            }
            
            // Encrypt the key with key encryption key
            cipher.init(XMLCipher.WRAP_MODE, kek);
            EncryptedKey encryptedKey = cipher.encryptKey(doc, secretKey);
            KeyInfo insideKi = new KeyInfo(doc);
            X509Data x509Data = new X509Data(doc);
            x509Data.addCertificate((X509Certificate)
            keyProvider.getCertificate((PublicKey) kek));
            insideKi.add(x509Data);
            encryptedKey.setKeyInfo(insideKi);
            String ekID = null;
            if (isEncryptResourceID) {
                ekID = com.sun.identity.saml.common.SAMLUtils.generateID();
                encryptedKey.setId(ekID);
            }
            
            if(EncryptionUtils.debug.messageEnabled()) {
                EncryptionUtils.debug.message("AMEncryptionProvider.encrypt" +
                        "AndReplace: Encrypted key = " + toString(cipher.martial(
                        doc, encryptedKey)));
            }
            String encAlgorithm =
                    getEncryptionAlgorithm(secretKeyAlgShortName, keyStrength);
            cipher = XMLCipher.getInstance(encAlgorithm);
            cipher.init(XMLCipher.ENCRYPT_MODE, secretKey);
            
            EncryptedData builder = cipher.getEncryptedData();
            KeyInfo builderKeyInfo = builder.getKeyInfo();
            if(builderKeyInfo == null) {
                builderKeyInfo = new KeyInfo(doc);
                builder.setKeyInfo(builderKeyInfo);
            }
            if (isEncryptResourceID) {
                builderKeyInfo.addKeyName(providerID);
                builderKeyInfo.addRetrievalMethod("#" + ekID, null,
                        "http://www.w3.org/2001/04/xmlenc#EncryptedKey");
            } else {
                builderKeyInfo.add(encryptedKey);
            }
            
            Document result = cipher.doFinal(doc, element);
            if (isEncryptResourceID) {
                Element ee = (Element) result.getElementsByTagNameNS(
                        "http://www.w3.org/2001/04/xmlenc#",
                        "EncryptedData").item(0);
                Node parentNode = ee.getParentNode();
                Element newone = result.createElementNS(
                        "urn:liberty:disco:2003-08",
                        "EncryptedResourceID");
                parentNode.replaceChild(newone, ee);
                newone.appendChild(ee);
                Element ek = cipher.martial(doc, encryptedKey);
                Element carriedName = doc.createElementNS(
                        "http://www.w3.org/2001/04/xmlenc#", "xenc:CarriedKeyName");
                carriedName.appendChild(doc.createTextNode(providerID));
                ek.appendChild(carriedName);
                newone.appendChild(ek);
            }
            return result;
        } catch (Exception xe) {
            EncryptionUtils.debug.error("AMEncryptionProvider.encryptAnd" +
                    "Replace: XML Encryption error", xe);
            throw new EncryptionException(xe);
        }
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
        return null;
    }
    
    /**
     * Decrypts an XML Document that contains encrypted data.
     * @param encryptedDoc XML Document with encrypted data.
     * @param certAlias Private Key Certificate Alias.
     * @return org.w3c.dom.Document Decrypted XML Document.
     */
    public Document decryptAndReplace(
            Document encryptedDoc,
            java.lang.String certAlias)
            throws EncryptionException {
        return decryptAndReplace(encryptedDoc,
                keyProvider.getPrivateKey(certAlias));
    }
    
    /**
     * Decrypts an XML Document that contains encrypted data.
     * @param encryptedDoc XML Document with encrypted data.
     * @param privKey Key Encryption Key used for encryption.
     * @return org.w3c.dom.Document Decrypted XML Document.
     */
    public Document decryptAndReplace(
            Document encryptedDoc,
            java.security.Key privKey)
            throws EncryptionException {
        EncryptionUtils.debug.message("************IN DECRYPT *************");
        if(encryptedDoc == null) {
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "null encrypted doc"));
        }
        
        if(EncryptionUtils.debug.messageEnabled()) {
            EncryptionUtils.debug.message("AMEncryptionProvider.decrypt" +
                    "AndReplace: input encrypted DOC = "
                    + XMLUtils.print(encryptedDoc));
        }
        
        Key encryptionKey = null;
        Document decryptedDoc = null;
        EncryptedKey encryptedKey = null;
        Element encryptedElementNext = null;
        XMLCipher cipher = null;
        
        NodeList nodes = encryptedDoc.getElementsByTagNameNS(
                EncryptionConstants.ENC_XML_NS, "EncryptedData");
        int length = nodes.getLength();
        if(nodes == null || length == 0) {
            return encryptedDoc;
        }
        
        /**
         * Check for the encrypted key after the encrypted data.
         * if found, use that symmetric key for the decryption., otherwise
         * check if there's one in the encrypted data.
         */
        Element encryptedElem = (Element)encryptedDoc.getElementsByTagNameNS(
                EncryptionConstants.ENC_XML_NS, "EncryptedKey").item(0);
        
        try {
            cipher = XMLCipher.getInstance();
            cipher.init(XMLCipher.DECRYPT_MODE, null);
        } catch (Exception xe) {
            EncryptionUtils.debug.error("AMEncryptionProvider.decrypt" +
                    "AndReplace: XML Decryption error for XMLCipher init :"
                    , xe);
            throw new EncryptionException(xe);
        }
        
        int i=0 ;
        Element encryptedElement = (Element)nodes.item(i);
        
        while (i < length) {
            try {
                if(EncryptionUtils.debug.messageEnabled()) {
                    EncryptionUtils.debug.message("AMEncryptionProvider.decrypt" +
                            "AndReplace: encrypted element (" + i + ") = "
                            + XMLUtils.print(encryptedElement));
                }
                
                EncryptedData encryptedData =
                        cipher.loadEncryptedData(encryptedDoc, encryptedElement);
                
                if(encryptedKey == null) {
                    encryptedKey =
                            cipher.loadEncryptedKey(encryptedDoc, encryptedElem);
                    if(encryptedKey == null) {
                        encryptedKey =
                                encryptedData.getKeyInfo().itemEncryptedKey(0);
                    }
                }
                
                if(EncryptionUtils.debug.messageEnabled()) {
                    EncryptionUtils.debug.message("AMEncryptionProvider.decrypt"
                            + "AndReplace: Encrypted key = " + toString(cipher.martial(
                            encryptedDoc, encryptedKey)));
                    
                    EncryptionUtils.debug.message("AMEncryptionProvider.decrypt"
                            + "AndReplace: Encrypted Data (" + i + ") = "
                            + toString(cipher.martial(encryptedDoc, encryptedData)));
                }
                
                if(encryptedKey != null) {
                    XMLCipher keyCipher = XMLCipher.getInstance();
                    if (privKey == null) {
                        privKey = getPrivateKey(encryptedKey.getKeyInfo());
                    }
                    keyCipher.init(XMLCipher.UNWRAP_MODE, privKey);
                    encryptionKey = keyCipher.decryptKey(encryptedKey,
                            encryptedData.getEncryptionMethod().getAlgorithm());
                }
                
                cipher = XMLCipher.getInstance();
                cipher.init(XMLCipher.DECRYPT_MODE, encryptionKey);
                
                i = i+1;
                if (i < length) {
                    encryptedElementNext = (Element)nodes.item(i);
                }
                
                decryptedDoc = cipher.doFinal(encryptedDoc, encryptedElement);
                
                encryptedElement = encryptedElementNext;
                
                if(EncryptionUtils.debug.messageEnabled()) {
                    EncryptionUtils.debug.message("AMEncryptionProvider.decrypt"
                            + "AndReplace: decryptedDoc (" + (i-1) + ") = " +
                            XMLUtils.print(decryptedDoc));
                }
                
            } catch (Exception xe) {
                EncryptionUtils.debug.error("AMEncryptionProvider.decrypt" +
                        "AndReplace: XML Decryption error.", xe);
                throw new EncryptionException(xe);
            }
        }
        
        if(EncryptionUtils.debug.messageEnabled()) {
            EncryptionUtils.debug.message("AMEncryptionProvider.decrypt"
                    + "AndReplace: FINAL decryptedDoc = " +
                    XMLUtils.print(decryptedDoc));
        }
        return decryptedDoc;
        
    }
    
    // converts the element to a string.
    private String toString(Element doc) {
        return XMLUtils.print(doc);
    }
    
    /**
     * Converts XML encryption algorithm string to a short name.
     * For example, http://www.w3.org/2001/04/xmlenc#aes128-cbc -> AES
     */
    private String getEncryptionAlgorithmShortName(String algorithmUri)
    throws EncryptionException {
        
        if (algorithmUri == null) {
            return null;
        } else if (algorithmUri.equals(XMLCipher.AES_128) ||
                algorithmUri.equals(XMLCipher.AES_192) ||
                algorithmUri.equals(XMLCipher.AES_256)) {
            
            return EncryptionConstants.AES;
        } else if (algorithmUri.equals(XMLCipher.TRIPLEDES)) {
            return EncryptionConstants.TRIPLEDES;
        } else {
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "unsupportedKeyAlg"));
        }
    }
    
    /**
     * Gets the equivalent XML encryption algorithm string for a given
     * algorithm and strength that is published by the provider.
     */
    protected String getEncryptionAlgorithm(String algorithm, int keyStrength)
    throws EncryptionException {
        
        if(algorithm == null) {
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "nullValues"));
        }
        
        if(algorithm.equals(EncryptionConstants.AES)) {
            
            if(keyStrength == 0 || keyStrength == 128) {
                return XMLCipher.AES_128;
            } else if (keyStrength == 192) {
                return XMLCipher.AES_192;
            } else if(keyStrength == 256) {
                return XMLCipher.AES_256;
            } else {
                throw new EncryptionException(EncryptionUtils.bundle.getString(
                        "invalidKeyStrength"));
            }
            
        } else if(algorithm.equals(EncryptionConstants.TRIPLEDES)) {
            return XMLCipher.TRIPLEDES;
        } else {
            throw new EncryptionException(EncryptionUtils.bundle.getString(
                    "unsupportedKeyAlg"));
        }
    }
    
    /**
     * Generates secret key for a given algorithm and key strength.
     */
    protected SecretKey generateSecretKey(String algorithm, int keyStrength)
    throws EncryptionException {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
            if(keyStrength != 0) {
                keygen.init(keyStrength);
            }
            return keygen.generateKey();
        } catch (NoSuchAlgorithmException ne) {
            throw new EncryptionException(ne);
        }
    }
    
    /**
     * Returns the private key for X509Certificate embedded in the KeyInfo
     * @param keyinfo KeyInfo
     * @return a private key for X509Certificate
     */
    protected java.security.PrivateKey getPrivateKey(KeyInfo keyinfo) {
        PrivateKey pk = null;
        try {
            if (keyinfo != null) {
                StorageResolver storageResolver = new StorageResolver(
                            new KeyStoreResolver(keyProvider.getKeyStore()));
                keyinfo.addStorageResolver(storageResolver);
                keyinfo.registerInternalKeyResolver(new
                            X509IssuerSerialResolver());
                keyinfo.registerInternalKeyResolver(new
                            X509CertificateResolver());
                keyinfo.registerInternalKeyResolver(new X509SKIResolver());
                keyinfo.registerInternalKeyResolver(new
                            X509SubjectNameResolver());
                if (keyinfo.containsX509Data()) {
                    if (EncryptionUtils.debug.messageEnabled()) {
                        EncryptionUtils.debug.message("Found X509Data" +
                                " element in the KeyInfo");
                    }
                    X509Certificate certificate = keyinfo.getX509Certificate();
                    String certAlias =
                            keyProvider.getCertificateAlias(certificate);
                    pk = keyProvider.getPrivateKey(certAlias);
                }
            }
        } catch (Exception e) {
            EncryptionUtils.debug.error("getPrivateKey(KeyInfo) Exception: "
                    , e);
        }
        
        return pk;
    }
    
    /**
     * Decrypt the given encrypted key.
     * @param encryptedKey the encrypted key element
     * @param certAlias the private key alias
     * @return the key associated with the decrypted key.
     */
    public Key decryptKey(Element encryptedKey, String certAlias) {
        return null;
    }
}
