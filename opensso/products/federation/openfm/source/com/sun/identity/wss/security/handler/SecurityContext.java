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
 * $Id: SecurityContext.java,v 1.1 2009/08/29 03:05:58 mallas Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import java.security.Key;

/**
 * This class represents the security context for signing/verification
 * and encryption/decryption etc.
 */

public class SecurityContext {
    
    public static final String SYMMETRIC_KEY = "SymmetricKey";
    public static final String ASYMMETRIC_KEY = "AsymmetricKey";
            
    private Key signingKey = null;
    private Key verificationKey = null;
    private Key encryptionKey = null;
    private Key decryptionKey = null;
    private String keyType = null;
    private String signingCertAlias = null;
    private String verificationCertAlias = null;
    private String encryptionAlias = null;
    private String decryptionAlias = null;
    private String signingRef = null;
    
    /**
     * Returns the signing key.
     * @return the signing key.
     */
    public Key getSigningKey() {
        return signingKey;
    }
    
    /**
     * Sets the signing key
     * @param signingKey the signing key
     */
    public void setSigningKey(Key signingKey) {
        this.signingKey = signingKey;
    }
    
    /**
     * Returns the signature verification key.
     * 
     * @return the signature verification key.
     */
    public Key getVerificationKey() {
        return verificationKey;
    }
    
    /**
     * Sets the signature verification key
     * @param verificationKey signature verification key
     */
    public void setVerificationKey(Key verificationKey) {
        this.verificationKey = verificationKey;
    }
        
    /**
     * Returns the encryption key
     * @return the encryption key
     */
    public Key getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(Key encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    /**
     * Returns the decryption key.
     * return the decryption key.
     */
    public Key getDecryptionKey() {
        return decryptionKey;
    }

    /**
     * Sets the decryption key
     * @param decryptionKey the decryption key
     */
    public void setDecryptionKey(Key decryptionKey) {
        this.decryptionKey = decryptionKey;
    }
    
    /**
     * Returns the key type. The possible types are SYMMETRIC_KEY or
     * ASYMMETRIC_KEY.
     * @return the key type.
     */
    public String getKeyType() {
        return keyType;
    }
    
    /**
     * Sets the key type.
     * @param keyType the key type.
     */
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    /**
     * Return the signing certificate alias
     * @return the signing certificate alias
     */
    public String getSigningCertAlias() {
        return signingCertAlias;
    }
    
    /**
     * Sets the signing certificate alias.
     * @param alias the signature certificate alias.
     */
    public void setSigningCertAlias(String alias) {
        this.signingCertAlias = alias;
    }
    
    /**
     * Returns the signature verification certificate alias.
     * @return the signature verification certificate alias.
     */
    public String getVerificationCertAlias() {
        return verificationCertAlias;
    }
    
    /**
     * Sets the signature certificate alias
     * @param alias the signature certificate alias
     */
    public void setVerificationCertAlias(String alias) {
        this.verificationCertAlias = alias;
    }
    
    /**
     * Returns the encryption key alias.
     * @return the encryption key alias.
     */
    public String getEncryptionKeyAlias() {
        return encryptionAlias;
    }
    
    /**
     * Sets the encryption key alias.
     * @param alias the encryption key alias.
     */
    public void setEncryptionKeyAlias(String alias) {
        this.encryptionAlias = alias;
    }
    
    /**
     * Returns the certificate alias for the decryption.
     * @return the decryption certificate alias.
     */
    public String getDecryptionAlias() {
        return decryptionAlias;
    }
    
    /**
     * Sets the certificate alias for the decryption.
     * @param alias the certificate alias for the decryption.
     */
    public void setDecryptionAlias(String alias) {
        this.decryptionAlias  = alias;
    }
    
    /**
     * Returns the signing reference type.
     * @return the signing reference type.
     */
    public String getSigningRef() {
        return signingRef;
    }
    
    /**
     * Sets the signing reference type.
     * @param signingRef the signing reference type.
     */
    public void setSigningRef(String signingRef) {
        this.signingRef = signingRef;
    }
}
