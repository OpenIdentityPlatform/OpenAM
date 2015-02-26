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
 * $Id: FSSignatureManager.java,v 1.2 2008/06/25 05:47:05 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.util;

import com.sun.identity.saml.common.*;
import com.sun.identity.saml.xmlsig.*;
import com.sun.identity.federation.common.*;

/**
 * Util class used to sign a string and verify signature on a string.
 */
public class FSSignatureManager {
    // Singleton instance of FSSignatureManager
    private static FSSignatureManager instance = null;
    private SignatureProviderSPI sp = null; 
    
    /**
     * Constructor.
     */
    protected FSSignatureManager() {
        sp = new FSSignatureProvider();
    }
    
    /** 
     * Constructor.
     */
    protected FSSignatureManager(KeyProvider keyProvider,
                                SignatureProviderSPI sigProvider)
    {
        sigProvider.initialize(keyProvider); 
        sp = sigProvider;
    }

    /**
     * Returns the singleton instance of <code>FSSignatureManager</code> with
     * default <code>KeyProvider</code> and <code>SignatureProvider</code>.
     * @return a <code>FSSignatureManager</code> instance
     */ 
    public static FSSignatureManager getInstance() {
        if (instance == null) {
            synchronized (FSSignatureManager.class) {
                if (instance == null) {
                    if (FSUtils.debug.messageEnabled() ) {
                        FSUtils.debug.message("Constructing a new instance"
                                + " of FSSignatureManager");
                    }
                    instance = new FSSignatureManager();
                }
            }
        }
        return (instance);
    }

    /**
     * Returns an instance of <code>FSSignatureManager</code> with specified 
     * <code>KeyProvider</code> and <code>SignatureProvider</code>.
     * @param keyProvider <code>KeyProvider</code> instance
     * @param sigProvider <code>SignatureProvider</code> instance
     * @return a <code>FSSignatureManager</code> instance
     */
    public static FSSignatureManager getInstance(KeyProvider keyProvider, 
                                         SignatureProviderSPI sigProvider)
    {
        return new FSSignatureManager(keyProvider, sigProvider);
    }

    /**
     * Signs a String using enveloped signatures and default signature
     * algorithm.
     * @param data string that needs to be signed
     * @param certAlias Signer's certificate alias name
     * @return byte array which contains signature object
     * @exception FSSignatureException if an error occurred during the signing
     *          process
     */
    public byte[] signBuffer(java.lang.String data, 
                             java.lang.String certAlias)
    throws FSSignatureException {
        return sp.signBuffer(data, certAlias);
    }

    /**
     * Signs a string using enveloped signatures.
     * @param data string that needs to be signed
     * @param certAlias Signer's certificate alias name
     * @param algorithm signing algorithm
     * @return byte array which contains signature Element object
     * @exception FSSignatureException if an error occurred during the signing
     *          process
     */
    public byte[] signBuffer(java.lang.String data, 
                             java.lang.String certAlias, 
                             java.lang.String algorithm)
    throws FSSignatureException {
        return sp.signBuffer(data, certAlias, algorithm);
    }
    
    /**
     * Verifies the signature of a signed string.
     * @param data string whose signature to be verified
     * @param signature signature in byte array
     * @param algorithm signing algorithm
     * @param cert Signer's certificate
     * @return <code>true</code> if the xml signature is verified;
     *                  <code>false</code> otherwise
     * @exception FSSignatureException if problem occurs during verification
     */
    public boolean verifySignature(java.lang.String data, 
                                   byte[] signature, 
                                   java.lang.String algorithm, 
                                   java.security.cert.X509Certificate cert)
    throws FSSignatureException {
        return sp.verifySignature(data, signature, algorithm, cert);
    }

    /**
     * Returns the key provider.
     * @return <code>KeyProvider</code> instance
     */
    public KeyProvider getKeyProvider() { 
        return sp.getKeyProvider();
    }
}
