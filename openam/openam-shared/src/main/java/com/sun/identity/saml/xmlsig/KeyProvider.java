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
 * $Id: KeyProvider.java,v 1.5 2009/07/02 21:53:26 madan_ranganath Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml.xmlsig;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * The class <code>KeyProvider</code> is an interface
 * that is implemented to retrieve X509Certificates and Private Keys from
 * user data store.  
 * <p>
 *
 * @supported.all.api
 */

public interface KeyProvider {

    /**
     * Set the key to access key store database. This method will only need to 
     * be called once if the key could not be obtained by other means. 
     * @param storepass  password for the key store
     * @param keypass password for the certificate
     */
    public void setKey(String storepass, String keypass); 

    /**
     * Return <code>java.security.cert.X509Certificate</code> for the specified
     * <code>certAlias</code>.
     * @param certAlias Certificate alias name 
     * @return <code>X509Certificate</code> which matches the
     *         <code>certAlias</code>, return null if the certificate could not
     *         be found.
     */
    public java.security.cert.X509Certificate getX509Certificate (
           String certAlias); 

    /**
     * Returns <code>java.security.PublicKey</code> for the specified
     * <code>keyAlias</code>
     *
     * @param keyAlias Key alias name
     * @return <code>PublicKey</code> which matches the <code>keyAlias</code>,
     *         return null if the <code>PublicKey</code> could not be found.
     */
    public java.security.PublicKey getPublicKey (String keyAlias);

    /**
     * Returns <code>java.security.PrivateKey</code> for the specified
     * <code>certAlias</code>.
     *
     * @param certAlias Certificate alias name  
     * @return <code>PrivateKey</code> which matches the <code>certAlias</code>,
     *         return null if the private key could not be found.
     */
    public java.security.PrivateKey getPrivateKey (String certAlias);

    /**
     * Return the {@link java.security.PrivateKey} for the specified certAlias and encrypted private key password.
     * @param certAlias Certificate alias name
     * @param encryptedKeyPass The encrypted keypass to use when getting the private certificate
     * @return PrivateKey which matches the certAlias, return null if
    the private key could not be found.
     */
    public PrivateKey getPrivateKey (String certAlias, String encryptedKeyPass);

     /**
     * Get the alias name of the first keystore entry whose certificate matches 
     * the given certificate. 
     * @param cert Certificate 
     * @return the (alias) name of the first entry with matching certificate,
     *       or null if no such entry exists in this keystore. If the keystore 
     *       has not been loaded properly, return null as well. 
     */
    public String getCertificateAlias(Certificate cert); 

    /**
     * Returns <code>java.security.PrivateKey</code> for the given
     * <code>X509Certificate</code>.
     *
     * @param cert <code>X509Certificate</code>
     * @return <code>PrivateKey</code> which matches the certificate,
     *         return null if the private key could not be found.
     */
    /* public java.security.PrivateKey getPrivateKey (
           java.security.cert.X509Certificate cert); */

    /**
     * Returns certificate corresponding to the specified
     * <code>PublicKey</code>.
     *
     * @param publicKey Certificate public key
     * @return Certificate which matches the <code>PublicKey</code>, return
     *         null if the Certificate could not be found.
     */
    public Certificate getCertificate (
	    java.security.PublicKey publicKey);

    /**
     * Returns the keystore instance.
     * @return the keystore instance.
     */
    public KeyStore getKeyStore();
}
