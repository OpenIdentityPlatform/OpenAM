/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RequestedProofToken.java,v 1.1 2009/08/29 03:06:00 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import org.w3c.dom.Element;

/**
 * This abstract class <code>RequestedProofToken</code> represents the
 * WS-Trust protocol element RequestedProofToken.
 */

public abstract class RequestedProofToken {
    
    protected static final String REQUESTED_PROOF_TOKEN = "RequestedProofToken";
    protected static final String BINARY_SECRET = "BinarySecret";
    protected static final String ENCRYPTED_KEY = "EncryptedKey";
    
    protected BinarySecret binarySecret = null;
    protected Element encryptedKey = null;
    
    
    
    /**
     * Returns the proof token element                     
     * @return the proof token element
     */
    public Object getProofToken() {
        return binarySecret;
    }
    
    /**
     * Sets the proof token.
     * @param proofToken the proof token element.
     */
    public void setProofToken(Object proofToken) {
        if(proofToken instanceof BinarySecret) {
           this.binarySecret = (BinarySecret)proofToken;
        } else if (proofToken instanceof Element) {
           this.encryptedKey = (Element)proofToken;
        }
    }
    
   
    
    /**
     * Returns the DOM Element representation for the requested proof token.
     * @return the DOM Element representation for the requested proof token.
     * @throws com.sun.identity.wss.sts.protocol.WSTException
     */
    public abstract Element toDOMElement()  throws WSTException;
    
    /**
     * Converts into XML String
     * @return the XML String for <code>RequestedProofToken</code>.
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract String toXMLString() throws WSTException;
        
    
}
