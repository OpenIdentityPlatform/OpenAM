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
 * $Id: EncInfo.java,v 1.2 2008/06/25 05:46:41 qcheng Exp $
 *
 */


package com.sun.identity.federation.key;

import java.security.Key;

/**
 * <code>EncInfo</code> is a class for keeping encryption information
 * such as the key-wrapping key, the data encryption algorithm, and
 * data encryption key strength.
 */ 
public class EncInfo {
    
    private Key wrappingKey = null;
    private String dataEncAlgorithm = null;
    private int dataEncStrength = 0;

    /**
     * Constructor for <code>EncInfo</code>.
     * @param wrappingKey Key-wrapping key
     * @param dataEncAlgorithm Data encryption algorithm
     * @param dataEncStrength Data encryption key size
     */
    public EncInfo(
        Key wrappingKey,
        String dataEncAlgorithm,
        int dataEncStrength) {
        
        this.wrappingKey = wrappingKey;
        this.dataEncAlgorithm = dataEncAlgorithm;
        this.dataEncStrength = dataEncStrength;
    }

    /**
     * Returns the key for encrypting the secret key.
     * @return <code>Key</code> for encrypting/wrapping the secretKey
     */
    public Key getWrappingKey() {
        return wrappingKey;
    }

    /**
     * Returns the algorithm for data encryption.
     * @return <code>String</code> for data encryption algorithm
     */
    public String getDataEncAlgorithm() {
        return dataEncAlgorithm;
    }
    
    /**
     * Returns the key strength for data encryption.
     * @return <code>int</code> for data encryption strength
     */
    public int getDataEncStrength() {
        return dataEncStrength;
    }
} 
