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
 * $Id: NewID.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.NewIDImpl;
import java.security.Key;

/** 
 * This interface identifies the new identifier in an 
 * <code>ManageNameIDRequest</code> message.
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = NewIDImpl.class)
public interface NewID {
    /** 
     * Returns the value of the <code>NewID</code> URI.
     *
     * @return value of the <code>NewID</code> URI.
     */
    public String getValue();
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a  String representation of this Object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString() throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace 
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @throws SAML2Exception if cannot convert to String.
     * @return a String representation of this Object.
     **/
            
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	throws SAML2Exception;
    
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
	String recipientEntityID) throws SAML2Exception;
}
