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
 * $Id: NewIDImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;

import java.security.Key;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.NewID;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.xmlenc.EncManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** 
 * This class identifies the new ID entitie in an 
 * <code>ManageNameIDRequest</code> message.
 */
public class NewIDImpl implements NewID {
    public final String elementName = "NewID";
    private String newID;

    /**
     * Constructor to create the <code>NewID</code> Object.
     *
     * @param element Document Element of <code>NewID</code> Object.
     * @throws SAML2Exception if <code>NewID<code> cannot be created.
     */

    public NewIDImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }

    /**
     * Constructor to create the <code>NewID</code> Object.
     *
     * @param value of the <code>NewID<code>.
     * @throws SAML2Exception if <code>NewID<code> cannot be created.
     */
    public NewIDImpl(String value) throws SAML2Exception {
        newID = value;	
    }

    /** 
     * Returns the value of the <code>NewID</code> URI.
     *
     * @return value of the <code>NewID</code> URI.
     * @see #NewIDImpl(String)
     */
    public String getValue() {
        return newID;
    }
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a  String representation of this Object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /** 
     * Returns a String representation
     *
     * @param includeNSPrefix determines whether or not the namespace 
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @throws SAML2Exception if cannot convert to String.
     * @return a String representation of this Object.
     */
            
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	throws SAML2Exception {
        StringBuffer xml = new StringBuffer();
        
        if ((newID != null) && (newID.length() > 0)) {
        	String NS="";
        	String NSP="";
        	
        	if (declareNS) {
        	    NS = SAML2Constants.PROTOCOL_DECLARE_STR;
        	}
        	
        	if (includeNSPrefix) {
        	    NSP = SAML2Constants.PROTOCOL_PREFIX;
        	}

            xml.append("<").append(NSP).append(elementName);
            xml.append(NS).append(">");
            
            xml.append(newID);
            
            xml.append("</").append(NSP).append(elementName).append(">");
        }
        
        return xml.toString();
    }

    /**
     * Returns an <code>NewEncryptedID</code> object.
     *
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
     * @return <code>NewEncryptedID</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    public NewEncryptedID encrypt(
	Key recipientPublicKey,
	String dataEncAlgorithm,
	int dataEncStrength,
	String recipientEntityID)
	
	throws SAML2Exception {
	Element el = EncManager.getEncInstance().encrypt(
	    toXMLString(true, true),
	    recipientPublicKey,
	    dataEncAlgorithm,
	    dataEncStrength,
	    recipientEntityID,
	    "NewEncryptedID"
	);
	return ProtocolFactory.getInstance().
	    createNewEncryptedID(el);
    }
    
    void parseElement(Element element) {
        newID = XMLUtils.getValueOfValueNode((Node)element);
    }
}
