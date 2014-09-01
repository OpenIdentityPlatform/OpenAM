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
 * $Id: SignatureProviderSPI.java,v 1.2 2008/06/25 05:47:05 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.util;

import com.sun.identity.saml.xmlsig.KeyProvider;
import java.security.cert.X509Certificate;

/**
 * This is an interface to be implemented to sign and verify signature.
 */
public interface SignatureProviderSPI {
    
    /**
     * Initializes the key provider.
     * @param keyProvider KeyProvider object 
     */
    public void initialize(KeyProvider keyProvider);
    
    /**
     * Signs a string using enveloped signatures and default signature
     * algorithm.
     * @param data string that needs to be signed
     * @param certAlias Signer's certificate alias name
     * @return byte array which contains signature Element object
     * @exception FSSignatureException if an error occurred during the signing
     *          process
     */
    public byte[] signBuffer(String data, 
        String certAlias)
        throws FSSignatureException ;
    
    /**
     * Signs a string using enveloped signatures.
     * @param data string that needs to be signed
     * @param certAlias Signer's certificate alias name
     * @param algorithm signing algorithm
     * @return byte array which contains signature Element object 
     * @exception FSSignatureException if an error occurred during the signing
     *          process
     */ 
    public byte[] signBuffer(String data, 
        String certAlias, 
        String algorithm)
        throws FSSignatureException ;
    
    
    /**
     * Verifies the signature of a signed string.
     * @param data string whose signature to be verified
     * @param signature signature in byte array
     * @param algorithm signing algorithm
     * @param cert certificate for Signer's certificate.
     * @return <code>true</code> if the xml signature is verified;
     *                  <code>false</code> otherwise
     * @exception FSSignatureException if problem occurs during verification
     */
    public boolean verifySignature(String data,
                                   byte[] signature,
                                   String algorithm,
                                   X509Certificate cert)
    throws FSSignatureException;
 
    /**
     * Returns the key provider.
     * @return <code>KeyProvider</code> instance
     */
    public KeyProvider getKeyProvider(); 
    
}
