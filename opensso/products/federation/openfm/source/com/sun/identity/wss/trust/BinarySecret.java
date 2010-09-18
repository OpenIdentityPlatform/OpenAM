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
 * $Id: BinarySecret.java,v 1.1 2009/08/29 03:05:59 mallas Exp $
 *
 */

package com.sun.identity.wss.trust;

import org.w3c.dom.Element;

/**
 * This class <code>BinarySecret</code> represents the binary secret
 * 
 */
public abstract class BinarySecret {
    
    protected static final String BINARY_SECRET = "BinarySecret";
    
    protected byte[] secret = null;
    
    /**
     * Returns the secret.
     * @return the secret.
     */
    public byte[] getSecret() {
        return secret;
    }
    
    /**
     * Sets the secret
     * @param secret the secret bytes
     */
    public  void setSecret(byte[] secret) {
        this.secret = secret; 
    }
    
    /**
     * Returns the DOM Element representation for the binary secret.
     * @return the DOM Element representation for the binary secret.
     * @throws com.sun.identity.wss.sts.protocol.WSTException
     */
    public abstract Element toDOMElement()  throws WSTException;
    
    /**
     * Converts into XML String
     * @return the XML String for <code>BinarySecret</code>.
     * @throws com.sun.identity.wss.trust.WSTException
     */
    public abstract String toXMLString() throws WSTException;
    
}