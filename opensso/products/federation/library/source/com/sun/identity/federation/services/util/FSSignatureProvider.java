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
 * $Id: FSSignatureProvider.java,v 1.5 2009/06/08 23:41:51 madan_ranganath Exp $
 *
 */


package com.sun.identity.federation.services.util;


import java.io.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import com.sun.identity.saml.xmlsig.*;
import com.sun.identity.federation.common.*;
import com.sun.identity.federation.key.KeyUtil;

/**
 * This class implements interface <code>SignatureProviderSPI</code>.
 */
public class FSSignatureProvider implements SignatureProviderSPI {

    private KeyProvider keystore = null;
    private static String rsaProviderName = null;

    /**
     * Default Constructor.
     */
    public FSSignatureProvider() {        

        keystore = KeyUtil.getKeyProviderInstance();
    }
    
    /**
     * Initializes the provider. 
     * @param keyProvider <code>KeyProvider</code> object
     */
    public void initialize(KeyProvider keyProvider) {
        if (keyProvider == null) {
            FSUtils.debug.error("FSSignatureProvider.initialize: Key Provider "
                + "is null"); 
        } else {
            keystore = keyProvider;  
        }
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
    public byte[] signBuffer(String data, 
                             String certAlias)
    throws FSSignatureException {      
        return signBuffer(data, certAlias, IFSConstants.DEF_SIG_ALGO_JCA); 
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
    public byte[] signBuffer(String data, 
                             String certAlias, 
                             String algorithm)
    throws FSSignatureException {   
        if (data == null) {
            FSUtils.debug.error("FSSignatureProvider.signBuffer: data to be "
                + "signed is null.");
            throw new FSSignatureException(
                      FSUtils.bundle.getString("nullInput"));
        }        
        if (certAlias == null || certAlias.length() == 0) {
            FSUtils.debug.error("FSSignatureProvider.signBuffer: certAlias is "
                + "null.");
            throw new FSSignatureException(
                      FSUtils.bundle.getString("nullInput"));
        }    
        
        try{
            PrivateKey privateKey = 
                                (PrivateKey) keystore.getPrivateKey(certAlias);
            if (algorithm == null || algorithm.length() == 0) {
                algorithm = IFSConstants.DEF_SIG_ALGO_JCA; 
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSignatureProvider.signBuffer: "
                        + "algorithm is null assigning algorithm= "
                        + algorithm);
                }
            }
            if(!isValidAlgorithm(algorithm)){                
                FSUtils.debug.error("FSSignatureProvider.signBuffer: "
                    + "algorithm is invalid ");
                throw new FSSignatureException(
                    FSUtils.bundle.getString("invalidAlgorithm"));
            }

            Signature sig = Signature.getInstance(algorithm);
            sig.initSign(privateKey);
            sig.update(data.getBytes());
            return sig.sign();
        } catch (Exception ex) {
            String stackTrace = null;
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(bop));
            stackTrace = bop.toString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSignatureProvider.signBuffer: "
                    + stackTrace);
            }
            throw new FSSignatureException (ex.getMessage());
        }
    }

    private Signature getSignatureWithRSA()
        throws NoSuchAlgorithmException, NoSuchProviderException 
    {
        Signature sig = null;
        if (rsaProviderName == null) {
            Provider[] ps = Security.getProviders();
            for (int i=0; i<ps.length; i++) {
                try {
                    sig = Signature.getInstance(
                        IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA, ps[i]);
                    rsaProviderName = ps[i].getName();
                    break;
                } catch (NoSuchAlgorithmException nsa) {
                }
            }
        } else {
            sig = Signature.getInstance(
                IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA, rsaProviderName);
        }
        if (sig == null) {
            throw new NoSuchProviderException();
        }
        return sig;
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
    public boolean verifySignature(String data, 
                                   byte[] signature, 
                                   String algorithm, 
                                   X509Certificate cert)
    throws FSSignatureException {
        if (data == null || data.length() == 0) {
            FSUtils.debug.error("FSSignatureProvider.verifySignature: "
                + "data to be signed is null.");
            throw new FSSignatureException (
                      FSUtils.bundle.getString("nullInput"));
        }   
        try {
            if (algorithm == null || algorithm.length() == 0) {
                throw new FSSignatureException(
                    FSUtils.bundle.getString("invalidAlgorithm"));
            }
            if(!isValidAlgorithm(algorithm)){                
                throw new FSSignatureException(
                    FSUtils.bundle.getString("invalidAlgorithm"));
            }
            Signature sig = Signature.getInstance(algorithm);    
            if (cert != null) { 
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSignatureProvider.verifySignature:"
                        + " Certificate: " + cert.toString());
                }
                sig.initVerify(cert);
                sig.update(data.getBytes());
                return sig.verify(signature);  
            } else {
                return false; 
            }
        } catch (Exception ex) {
            String stackTrace = null;
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(bop));
            stackTrace = bop.toString();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSignatureProvider.verifySignature: "
                    + stackTrace);
            }
            throw new FSSignatureException (ex.getMessage());
        }            
   }
         
    /**
     * Returns the key provider.
     * @return <code>KeyProvider</code> instance
     */
    public KeyProvider getKeyProvider() {
        return keystore; 
    }
    
    private boolean isValidAlgorithm(String algorithm) {
        if (algorithm.equals(IFSConstants.ALGO_ID_SIGNATURE_DSA_JCA) ||
            algorithm.equals(IFSConstants.ALGO_ID_SIGNATURE_RSA_JCA)) { 
            return true;
        } else {
            return false; 
        }
    }
}
