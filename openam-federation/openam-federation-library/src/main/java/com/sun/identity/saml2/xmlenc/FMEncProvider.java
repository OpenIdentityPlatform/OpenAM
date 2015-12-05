/*
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
 * $Id: FMEncProvider.java,v 1.5 2008/06/25 05:48:03 qcheng Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.xmlenc;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLEncryptionException;

import java.security.PrivateKey;
import java.util.Hashtable;
import java.util.Set;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xmlenc.EncryptionConstants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Constants;

/**
 * <code>FMEncProvier</code> is a class for encrypting and 
 * decrypting XML documents, it implements <code>EncProvider</code>.
 */ 

public final class FMEncProvider implements EncProvider {
 
    /**
     * A static map contains the recipients' entity IDs as
     * the indices and symmetric keys as values. Symmetric key
     * generation each time is expensive operation. Using the
     * same key for each recipient is provided as an option
     * here.
     */ 
    static Hashtable cachedKeys = new Hashtable();

    /**
     * A hidden property to switch between two encryption formats.
     * If true, will have a ds:KeyInfo Element inside xenc:EncryptedData
     * which will include the xenc:EncryptedKey Element (as defined in
     * XML Encryption Specification). If false, will have xenc:EncryptedKey
     * Element parallels to xenc:EncryptedData (as defined in SAML2
     * profile specification). Default to true if not specified.
     */
    private static boolean encryptedKeyInKeyInfo = true;

    static {
        org.apache.xml.security.Init.init();
        String tmp = SystemConfigurationUtil.getProperty(
            "com.sun.identity.saml.xmlenc.encryptedKeyInKeyInfo");
        if ((tmp != null) && (tmp.equalsIgnoreCase("false"))) {
            encryptedKeyInKeyInfo = false;
        }
    }

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

	throws SAML2Exception {

        return encrypt(xmlString, recipientPublicKey, null, dataEncAlgorithm,
            dataEncStrength, recipientEntityID, outerElementName);
    }

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

	throws SAML2Exception {

	String classMethod = "FMEncProvider.encrypt: ";

	// checking the input parameters
	if (xmlString==null ||
	    xmlString.length()==0 ||
	    recipientPublicKey==null ||
	    dataEncAlgorithm==null ||
	    dataEncAlgorithm.length() == 0 ||
	    outerElementName==null ||
	    outerElementName.length() == 0) {
	    
            SAML2SDKUtils.debug.error(
		classMethod + "Null input parameter(s).");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("nullInput"));
        }
        if (!dataEncAlgorithm.equals(XMLCipher.AES_128) &&
	    !dataEncAlgorithm.equals(XMLCipher.AES_192) &&
	    !dataEncAlgorithm.equals(XMLCipher.AES_256) &&
	    !dataEncAlgorithm.equals(XMLCipher.TRIPLEDES)) {

            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
		"unsupportedKeyAlg"));
        }
	if ((dataEncAlgorithm.equals(XMLCipher.AES_128) &&
	     dataEncStrength != 128) ||
	    (dataEncAlgorithm.equals(XMLCipher.AES_192) &&
	     dataEncStrength != 192) ||
	    (dataEncAlgorithm.equals(XMLCipher.AES_256) &&
	     dataEncStrength != 256)) {

            SAML2SDKUtils.debug.error(
		classMethod +
		"Data encryption algorithm " + dataEncAlgorithm +
		"and strength " + dataEncStrength +
		" mismatch.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
		"algSizeMismatch"));
	}	    	    
	Document doc =
	    XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
	if (dataEncStrength <= 0) {
	    dataEncStrength = 128;
	}
	Element rootElement = doc.getDocumentElement();
	if (rootElement == null) {
            SAML2SDKUtils.debug.error(classMethod + "Empty document.");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("emptyDoc"));
	}
	// start of obtaining secret key
        if (secretKey == null) {
            if (recipientEntityID != null) {
	        if (cachedKeys.containsKey(recipientEntityID)) {
		    secretKey = (SecretKey)
		        cachedKeys.get(recipientEntityID);
                } else {
		    secretKey = generateSecretKey(
		        dataEncAlgorithm, dataEncStrength);
                    cachedKeys.put(recipientEntityID, secretKey);
                }
            } else {
                secretKey = generateSecretKey(
                   dataEncAlgorithm, dataEncStrength);
            }
            if (secretKey == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "errorGenerateKey"));
            }
        }
	// end of obtaining secret key

	XMLCipher cipher = null;
	// start of encrypting the secret key with public key
	String publicKeyEncAlg = recipientPublicKey.getAlgorithm();
	/* note that the public key encryption algorithm could only
	 * have three possible values here: "RSA", "AES", "DESede"
	 */
	try {	    
	    if (publicKeyEncAlg.equals(EncryptionConstants.RSA)) {
		cipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
		
	    } else if (publicKeyEncAlg.equals(EncryptionConstants.TRIPLEDES)) {
		cipher = XMLCipher.getInstance(XMLCipher.TRIPLEDES_KeyWrap);
		
	    } else if (publicKeyEncAlg.equals(EncryptionConstants.AES)) {
		
		cipher = XMLCipher.getInstance(XMLCipher.AES_128_KeyWrap);
	    } else {
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("unsupportedKeyAlg"));
	    }
	} catch (XMLEncryptionException xe1) {
            SAML2SDKUtils.debug.error(
		classMethod + 
                "Unable to obtain cipher with public key algorithm.", xe1);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("noCipherForPublicKeyAlg"));
	}
	try {
	    cipher.init(XMLCipher.WRAP_MODE, recipientPublicKey);
	} catch (XMLEncryptionException xe2) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to initialize cipher with public key",
		xe2);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedInitCipherWithPublicKey"));
	}
	EncryptedKey encryptedKey = null;
	try {
	    encryptedKey = cipher.encryptKey(doc, secretKey);
	} catch (XMLEncryptionException xe3) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to encrypt secret key with public key",
		xe3);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedEncryptingSecretKeyWithPublicKey"));
	}
	// end of encrypting the secret key with public key
	
	// start of doing data encryption
	try {
	    cipher = XMLCipher.getInstance(dataEncAlgorithm);
	} catch (XMLEncryptionException xe4) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to obtain a cipher for "+
		"data encryption algorithm" + dataEncAlgorithm,
		xe4);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "cipherNotAvailableForDataEncAlg"));
	}   
	try {
	    cipher.init(XMLCipher.ENCRYPT_MODE, secretKey);
	} catch (XMLEncryptionException xe5) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to initialize cipher with secret key.",
		xe5);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
                    "failedInitCipherWithSecretKey"));
	}   
	Document resultDoc = null;
	try {
	    resultDoc = cipher.doFinal(doc, rootElement);
	} catch (Exception e) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to do the final data encryption.", e);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("failedEncryptingData"));
	}	    
	// end of doing data encryption
	
	// add the EncryptedKey element
	Element ek = null;
	try {
	    ek = cipher.martial(doc, encryptedKey);
	} catch (Exception xe6) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to martial the encrypted key", xe6);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedMartialingEncryptedKey"));
	}

        String outerElemNS = SAML2Constants.ASSERTION_NAMESPACE_URI;
        String outerElemPrefix = "saml";
        if (outerElementName.equals("NewEncryptedID")) {
            outerElemNS = SAML2Constants.PROTOCOL_NAMESPACE;
            outerElemPrefix = "samlp";
        }
        Element outerElement = resultDoc.createElementNS(outerElemNS,
            outerElemPrefix + ":" + outerElementName);
        outerElement.setAttributeNS(SAML2Constants.NS_XML,
            "xmlns:" + outerElemPrefix, outerElemNS);
        Element ed = resultDoc.getDocumentElement();	
        resultDoc.replaceChild(outerElement, ed);
        outerElement.appendChild(ed);
        if (encryptedKeyInKeyInfo) {
            // create a ds:KeyInfo Element to include the EncryptionKey
            Element dsElement = resultDoc.createElementNS(
                SAML2Constants.NS_XMLSIG, "ds:KeyInfo");
            dsElement.setAttributeNS(SAML2Constants.NS_XML, "xmlns:ds",
                SAML2Constants.NS_XMLSIG);
            dsElement.appendChild(ek);
            // find the xenc:CipherData Element inside the encrypted data
            NodeList nl = ed.getElementsByTagNameNS(SAML2Constants.NS_XMLENC,
                "CipherData");
            if ((nl == null) || (nl.getLength() == 0)) {
                SAML2SDKUtils.debug.error(classMethod +
                    "Unable to find required xenc:CipherData Element.");
              throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                  "failedEncryptingData"));
            }
            Element cipherDataElement = (Element) nl.item(0);
            // insert the EncryptedKey before the xenc:CipherData Element
            ed.insertBefore(dsElement, cipherDataElement);
        } else {
            outerElement.appendChild(ek);
        }
	return resultDoc.getDocumentElement();
    }

    @Override
    public SecretKey getSecretKey(String xmlString, Set<PrivateKey> privateKeys) throws SAML2Exception {
	String classMethod = "FMEncProvider.getSecretKey: ";
        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message(classMethod + "Entering ...");
        }

        if (xmlString == null ||
	    xmlString.length() == 0 ||
	    privateKeys == null) {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Document doc = XMLUtils.toDOMDocument(
	    xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString(
		    "errorObtainingElement"));
        }
	Element rootElement = doc.getDocumentElement();
	if (rootElement == null) {
            SAML2SDKUtils.debug.error(classMethod + "Empty document.");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("emptyDoc"));
	}
	Element firstChild = getNextElementNode(rootElement.getFirstChild());
	if (firstChild == null) {
            SAML2SDKUtils.debug.error(
		classMethod + "Missing the EncryptedData element.");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "missingElementEncryptedData"));
	}
	Element secondChild = getNextElementNode(firstChild.getNextSibling());
	if (secondChild == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message(classMethod +
                    "looking for encrytion key inside first child.");
            }
            NodeList nl = firstChild.getElementsByTagNameNS(
                SAML2Constants.NS_XMLENC, "EncryptedKey");
            if ((nl == null) || (nl.getLength() == 0)) {
                SAML2SDKUtils.debug.error(
                    classMethod + "Missing the EncryptedKey element."); 
	        throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString(
		        "missingElementEncryptedKey"));
            } else {
                // use the first EncryptedKey found
                secondChild = (Element) nl.item(0);
            }
        }

        XMLCipher cipher = null;
	try {
	    cipher = XMLCipher.getInstance();
	} catch (XMLEncryptionException xe1) {
            SAML2SDKUtils.debug.error(
		classMethod + "Unable to get a cipher instance.", xe1);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("noCipher"));
	}
	try {
	    cipher.init(XMLCipher.DECRYPT_MODE, null);
	} catch (XMLEncryptionException xe2) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to initialize cipher for decryption mode",
		xe2);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedInitCipherForDecrypt"));
	}
	EncryptedData encryptedData = null;
	try {
	    encryptedData = cipher.loadEncryptedData(doc, firstChild);	
	} catch (XMLEncryptionException xe3) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to load encrypted data", xe3);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedLoadingEncryptedData"));
	}
	EncryptedKey encryptedKey = null;
	try {
	    encryptedKey = cipher.loadEncryptedKey(doc, secondChild);
	} catch (XMLEncryptionException xe4) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to load encrypted key", xe4);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedLoadingEncryptedKey"));
	}
        if ((encryptedKey != null) && (encryptedData != null)) {
            XMLCipher keyCipher;
            try {
                keyCipher = XMLCipher.getInstance();
            } catch (XMLEncryptionException xe5) {
                SAML2SDKUtils.debug.error(classMethod + "Failed to get a cipher instance for decrypting secret key.",
                        xe5);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
            }

            return (SecretKey) getEncryptionKey(keyCipher, privateKeys, encryptedKey,
                    encryptedData.getEncryptionMethod().getAlgorithm());
        }

        return null;
    }

    @Override
    public Element decrypt(String xmlString, Set<PrivateKey> privateKeys) throws SAML2Exception {

	String classMethod = "FMEncProvider.decrypt: ";
        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message(classMethod + "Entering ...");
        }
        if (StringUtils.isEmpty(xmlString) || CollectionUtils.isEmpty(privateKeys)) {
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Document doc = XMLUtils.toDOMDocument(
	    xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString(
		    "errorObtainingElement"));
        }
	Element rootElement = doc.getDocumentElement();
	if (rootElement == null) {
            SAML2SDKUtils.debug.error(classMethod + "Empty document.");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("emptyDoc"));
	}
	Element firstChild = getNextElementNode(rootElement.getFirstChild());
	if (firstChild == null) {
            SAML2SDKUtils.debug.error(
		classMethod + "Missing the EncryptedData element.");
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "missingElementEncryptedData"));
	}
	Element secondChild = getNextElementNode(firstChild.getNextSibling());
	if (secondChild == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message(classMethod +
                    "looking for encrytion key inside first child.");
            }
            NodeList nl = firstChild.getElementsByTagNameNS(
                SAML2Constants.NS_XMLENC, "EncryptedKey");
            if ((nl == null) || (nl.getLength() == 0)) {
                SAML2SDKUtils.debug.error(
                    classMethod + "Missing the EncryptedKey element."); 
	        throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString(
		        "missingElementEncryptedKey"));
            } else {
                // use the first EncryptedKey found
                secondChild = (Element) nl.item(0);
            }
        }
        XMLCipher cipher = null;
	try {
	    cipher = XMLCipher.getInstance();
	} catch (XMLEncryptionException xe1) {
            SAML2SDKUtils.debug.error(
		classMethod + "Unable to get a cipher instance.", xe1);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString("noCipher"));
	}
	try {
	    cipher.init(XMLCipher.DECRYPT_MODE, null);
	} catch (XMLEncryptionException xe2) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to initialize cipher for decryption mode",
		xe2);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedInitCipherForDecrypt"));
	}
	EncryptedData encryptedData = null;
	try {
	    encryptedData = cipher.loadEncryptedData(doc, firstChild);	
	} catch (XMLEncryptionException xe3) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to load encrypted data", xe3);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedLoadingEncryptedData"));
	}
	EncryptedKey encryptedKey = null;
	try {
	    encryptedKey = cipher.loadEncryptedKey(doc, secondChild);
	} catch (XMLEncryptionException xe4) {
            SAML2SDKUtils.debug.error(
		classMethod + "Failed to load encrypted key", xe4);
	    throw new SAML2Exception(
		SAML2SDKUtils.bundle.getString(
		    "failedLoadingEncryptedKey"));
	}
        Document decryptedDoc = null;
	if (encryptedKey != null && encryptedData != null) {
	    XMLCipher keyCipher = null;
	    try {
		keyCipher = XMLCipher.getInstance();
	    } catch (XMLEncryptionException xe5) {
		SAML2SDKUtils.debug.error(
		    classMethod +
		    "Failed to get a cipher instance "+
		    "for decrypting secret key.",
		    xe5);
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("noCipher"));
	    }

	    Key encryptionKey = getEncryptionKey(keyCipher, privateKeys, encryptedKey,
                encryptedData.getEncryptionMethod().getAlgorithm());

	    cipher = null;
	    try {
		cipher = XMLCipher.getInstance();
	    } catch (XMLEncryptionException xe8) {
		SAML2SDKUtils.debug.error(
		    classMethod +
		    "Failed to get cipher instance for " +
		    "final data decryption.", 
		    xe8);
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("noCipher"));
	    }
	    try {
		cipher.init(XMLCipher.DECRYPT_MODE, encryptionKey);
	    } catch (XMLEncryptionException xe9) {
		SAML2SDKUtils.debug.error(
		    classMethod +
		    "Failed to initialize cipher with secret key.", 
		    xe9);
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString(
                        "failedInitCipherForDecrypt"));
	    }
	    try {
		decryptedDoc = cipher.doFinal(doc, firstChild);
	    } catch (Exception e) {
		SAML2SDKUtils.debug.error(
		    classMethod + "Failed to decrypt data.", e);
		throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString(
			"failedDecryptingData"));
	    }
        }
        Element root = decryptedDoc.getDocumentElement();
        Element child = getNextElementNode(root.getFirstChild());
        if (child == null) {
            SAML2SDKUtils.debug.error(classMethod +
                "decrypted document contains empty element.");
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("failedDecryptingData"));
        }
	root.removeChild(child);
	decryptedDoc.replaceChild(child, root);
        return decryptedDoc.getDocumentElement();
    }

    /**
     * Returns the next Element node, return null if no such node exists.
     */
    private Element getNextElementNode(Node node) {
        while (true) {
            if (node == null) {
                return null;
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            } else {
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Generates secret key for a given algorithm and key strength.
     */  
    private SecretKey generateSecretKey(String algorithm, int keyStrength)
        throws SAML2Exception {
    	KeyGenerator keygen = null;
        try {
            if (algorithm.equals(XMLCipher.AES_128) ||
                algorithm.equals(XMLCipher.AES_192) ||
            	algorithm.equals(XMLCipher.AES_256)) {
                keygen = KeyGenerator.getInstance("AES");
            } else if (algorithm.equals(XMLCipher.TRIPLEDES)) {
                keygen = KeyGenerator.getInstance("TripleDES");
            } else {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
        		"unsupportedKeyAlg"));
            }
            
            if (keyStrength != 0) {
                keygen.init(keyStrength);
            }
        } catch (NoSuchAlgorithmException ne) {
            throw new SAML2Exception(ne);
        }
        
        return (keygen != null) ? keygen.generateKey() : null;
    }

    private Key getEncryptionKey(XMLCipher cipher, Set<PrivateKey> privateKeys, EncryptedKey encryptedKey,
            String algorithm) throws SAML2Exception {
        final String classMethod = "FMEncProvider.getEncryptionKey";
        String firstErrorCode = null;
        for (Key privateKey : privateKeys) {
            try {
                cipher.init(XMLCipher.UNWRAP_MODE, privateKey);
            } catch (XMLEncryptionException xee) {
                SAML2SDKUtils.debug.warning(classMethod + "Failed to initialize cipher in unwrap mode with private key",
                        xee);
                if (firstErrorCode == null) {
                    firstErrorCode = "noCipherForUnwrap";
                }
                continue;
            }
            try {
                return cipher.decryptKey(encryptedKey, algorithm);
            } catch (XMLEncryptionException xee) {
                SAML2SDKUtils.debug.error(classMethod + "Failed to decrypt the secret key", xee);
                if (firstErrorCode == null) {
                    firstErrorCode = "failedDecryptingSecretKey";
                }
            }
        }
        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(firstErrorCode));
    }
}
