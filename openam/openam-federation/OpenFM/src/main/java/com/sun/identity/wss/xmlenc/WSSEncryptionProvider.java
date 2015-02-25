/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSSEncryptionProvider.java,v 1.7 2009/08/29 03:06:01 mallas Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.xmlenc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.Key;
import java.security.PublicKey;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.ReferenceList;
import org.apache.xml.security.encryption.Reference;
import org.apache.xml.security.encryption.EncryptionMethod;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.X509Data;

import com.sun.identity.xmlenc.EncryptionException;
import com.sun.identity.xmlenc.EncryptionUtils;
import com.sun.identity.xmlenc.AMEncryptionProvider;
import com.sun.identity.xmlenc.EncryptionConstants;

import java.security.cert.X509Certificate;

import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;

/**
 * <code>WSSEncryptionProvider</code> is a class for encrypting and 
 * decrypting WSS XML Documents which implements 
 * <code>AMEncryptionProvider</code>.
 */ 
public class WSSEncryptionProvider extends AMEncryptionProvider {
    
    /** Creates a new instance of WSSEncryptionProvider */
    public WSSEncryptionProvider() {
        super();
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
        Document resultDoc = null;
        
        java.security.Key kek = keyProvider.getPublicKey(certAlias);
        
        if(doc == null || elmMap == null || kek == null) { 
           EncryptionUtils.debug.error("WSSEncryptionProvider.encryptAnd" +
           "ReplaceWSSElements: Null values for doc or elements map or public key");
           throw new EncryptionException(EncryptionUtils.bundle.getString(
            "nullValues"));
        }

        if(EncryptionUtils.debug.messageEnabled()) {
            EncryptionUtils.debug.message("WSSEncryptionProvider.encrypt" +
                "AndReplaceWSSElements: DOC input = " 
                + WSSUtils.print(doc));
        }

        org.w3c.dom.Element root = (Element) doc.getDocumentElement().
            getElementsByTagNameNS(WSSConstants.WSSE_NS,
            SAMLConstants.TAG_SECURITY).item(0);
       
        
        SecretKey secretKeyEncData = null;
        String cacheKey = providerID + encDataEncAlg + encDataEncAlgStrength;
        if(cacheKey != null) {
           if(keyMap.containsKey(cacheKey)) {
              secretKeyEncData = (SecretKey)keyMap.get(cacheKey);
              if(!secretKeyEncData.getAlgorithm().equals(encDataEncAlg)) {
                 secretKeyEncData = 
                       generateSecretKey(encDataEncAlg, encDataEncAlgStrength); 
              }
           } else {
              secretKeyEncData = 
                  generateSecretKey(encDataEncAlg, encDataEncAlgStrength);
              keyMap.put(cacheKey, secretKeyEncData);
           }
        } else {
           secretKeyEncData = 
               generateSecretKey(encDataEncAlg, encDataEncAlgStrength);
        }

        if(secretKeyEncData == null) {
           throw new EncryptionException(EncryptionUtils.bundle.getString(
           "generateKeyError"));
        }
       
        try {
            XMLCipher cipher = null;
            
            // ENCRYPTED KEY
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
            EncryptedKey encryptedKey = cipher.encryptKey(doc, 
                secretKeyEncData);
	        KeyInfo insideKi = new KeyInfo(doc);
            X509Data x509Data = new X509Data(doc);
            x509Data.addCertificate((X509Certificate)
			    keyProvider.getCertificate((PublicKey) kek));
            insideKi.add(x509Data);
	    
            // SecurityTokenReference   
            Element securityTokenRef = doc.createElementNS(WSSConstants.WSSE_NS,
                "wsse:" + SAMLConstants.TAG_SECURITYTOKENREFERENCE);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSSE, WSSConstants.WSSE_NS);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                WSSConstants.TAG_XML_WSU, WSSConstants.WSU_NS);
            String secRefId = SAMLUtils.generateID();
            securityTokenRef.setAttributeNS(WSSConstants.WSU_NS, 
                WSSConstants.WSU_ID, secRefId);            
            insideKi.addUnknownElement(securityTokenRef);
            securityTokenRef.setIdAttribute(secRefId, true);

            Element reference = doc.createElementNS(WSSConstants.WSSE_NS,
                SAMLConstants.TAG_REFERENCE);            
            reference.setPrefix(WSSConstants.WSSE_TAG); 
                        
            String searchType = null;
            if (SecurityToken.WSS_X509_TOKEN.equals(tokenType)) {                
                reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                    WSSConstants.WSSE_X509_NS + "#X509v3");
                searchType = SAMLConstants.BINARYSECURITYTOKEN;
            } else if (SecurityToken.WSS_USERNAME_TOKEN.equals(tokenType)) {
                reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                    WSSConstants.TAG_USERNAME_VALUE_TYPE);
                searchType = WSSConstants.TAG_USERNAME_TOKEN;
            } else if (SecurityToken.WSS_SAML_TOKEN.equals(tokenType)) {
                reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                    WSSConstants.ASSERTION_VALUE_TYPE);
                searchType = SAMLConstants.TAG_ASSERTION;
            } else if (SecurityToken.WSS_SAML2_TOKEN.equals(tokenType)) {
                reference.setAttributeNS(null, WSSConstants.TAG_VALUETYPE, 
                    WSSConstants.SAML2_ASSERTION_VALUE_TYPE);
                searchType = SAMLConstants.TAG_ASSERTION;
            }
            
            Element bsf = (Element)root.getElementsByTagNameNS(
                WSSConstants.WSSE_NS,searchType).item(0);

            String tokenId = null;
            if (bsf != null) {        
                tokenId = bsf.getAttributeNS(WSSConstants.WSU_NS,
                    SAMLConstants.TAG_ID);                
                reference.setAttributeNS(null, SAMLConstants.TAG_URI,"#"
                                         +tokenId);
            }
            
            securityTokenRef.appendChild(reference);
            encryptedKey.setKeyInfo(insideKi);
                        
            ReferenceList refList = 
                cipher.createReferenceList(ReferenceList.DATA_REFERENCE);
            if (refList != null) {
                Reference dataRef = null;
                Collection wsu_ids = elmMap.values();
                Set wsu_ids_set = 
                    (wsu_ids != null) ? new HashSet(wsu_ids) : Collections.EMPTY_SET;

                if (wsu_ids_set != null) {
                    for (Iterator it = wsu_ids_set.iterator(); it.hasNext(); ) {
	                String wsu_id = (String)it.next();
                        dataRef = refList.newDataReference("#" + wsu_id);
                        refList.add(dataRef);
                    }
                }

                encryptedKey.setReferenceList(refList);
            }            
            	    
            // ENCRYPTED KEY END
                        
            
            // ENCRYPTED DATA
            String encAlgorithm = 
                  getEncryptionAlgorithm(encDataEncAlg, encDataEncAlgStrength);
            
            for (Iterator elmIter = elmMap.entrySet().iterator();
                 elmIter.hasNext();) {
                Map.Entry me = (Map.Entry)elmIter.next();
                Element elm = (Element) me.getKey();
                String id = (String) me.getValue();

                cipher = XMLCipher.getInstance(encAlgorithm);            
                cipher.init(XMLCipher.ENCRYPT_MODE, secretKeyEncData);

                EncryptedData builder = cipher.getEncryptedData();            
                builder.setId(id);
            
                EncryptionMethod encMethod = 
                    cipher.createEncryptionMethod(encAlgorithm);
                builder.setEncryptionMethod(encMethod);
                doc = cipher.doFinal(doc, elm);
            }
            // ENCRYPTED DATA END

            root.appendChild(cipher.martial(doc, encryptedKey));
            resultDoc = doc;
           
            if(EncryptionUtils.debug.messageEnabled()) {
                EncryptionUtils.debug.message("WSSEncryptionProvider.encrypt" +
                    "AndReplaceWSSElements: Encrypted DOC = " 
                    + WSSUtils.print(resultDoc));
            }
	    	    
        } catch (Exception xe) {
            EncryptionUtils.debug.error("WSSEncryptionProvider.encryptAnd" +
            "ReplaceWSSElements: XML Encryption error : ", xe); 
            throw new EncryptionException(xe);
        }
        
        return resultDoc;
    }
    
    /**
     * Decrypt the given encrypted key.
     * @param encryptedKey the encrypted key element
     * @param certAlias the private key alias
     * @return the key associated with the decrypted key.
     */
    public Key decryptKey(Element encryptedKey, String certAlias) {
        
        Element encryptedElem = (Element)encryptedKey.getElementsByTagNameNS(
                EncryptionConstants.ENC_XML_NS, "EncryptedKey").item(0);
        if(encryptedElem == null) {
           return null; 
        }        
        try {
            XMLCipher cipher = XMLCipher.getInstance();
            cipher.init(XMLCipher.UNWRAP_MODE, 
                    keyProvider.getPrivateKey(certAlias));
            EncryptedKey encKey = cipher.loadEncryptedKey(encryptedKey);
            return cipher.decryptKey(encKey,
                    encKey.getEncryptionMethod().getAlgorithm());
        } catch (XMLEncryptionException xe) {
            EncryptionUtils.debug.error("WSSEncryptionProvider.decryptKey", xe);                  
            return null;
        }              
    }
    
}  

