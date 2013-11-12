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
 * $Id: JKSKeyProvider.java,v 1.4 2008/06/25 05:47:38 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.saml.xmlsig;

import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import org.forgerock.openam.utils.AMKeyProvider;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * The class <code>JKSKeyProvider</code> is a class
 * that is implemented to retrieve X509Certificates and Private Keys from
 * user data store.  
 * <p>
 */
public class JKSKeyProvider implements KeyProvider {

    private final AMKeyProvider keyProvider;

    /**
     * Constructor
     */
    public JKSKeyProvider() {
        keyProvider = new AMKeyProvider();
        keyProvider.setLogger(SAMLUtilsCommon.debug);
    }

    /**
     * Constructor
     */
    public JKSKeyProvider(String keyStoreFilePropName,String keyStorePassFilePropName, String keyStoreTypePropName,
            String privateKeyPassFilePropName) {
        keyProvider = new AMKeyProvider(keyStoreFilePropName, keyStorePassFilePropName, keyStoreTypePropName,
                privateKeyPassFilePropName);
        keyProvider.setLogger(SAMLUtilsCommon.debug);
    }

    /**
     * Set the key to access key store database. This method will only need to 
     * be calles once if the key could not be obtained by other means. 
     * @param storepass  password for the key store
     * @param keypass password for the certificate
     */
    public void setKey(String storepass, String keypass) {
        keyProvider.setKey(storepass, keypass);
    }

    /**
     * Return java.security.cert.X509Certificate for the specified certAlias.
     * @param certAlias Certificate alias name 
     * @return X509Certificate which matches the certAlias, return null if
           the certificate could not be found.
     */
    public java.security.cert.X509Certificate getX509Certificate (String certAlias) {
        return keyProvider.getX509Certificate(certAlias);
    }
    
    /**
     * Return java.security.PublicKey for the specified keyAlias
     * @param keyAlias Key alias name
     * @return PublicKey which matches the keyAlias, return null if
           the PublicKey could not be found.
     */
    public java.security.PublicKey getPublicKey (String keyAlias) {
        return keyProvider.getPublicKey(keyAlias);
    }

    /**
     * Return java.security.PrivateKey for the specified certAlias.
     * @param certAlias Certificate alias name  
     * @return PrivateKey which matches the certAlias, return null if
           the private key could not be found.
     */
    public java.security.PrivateKey getPrivateKey (String certAlias) {
        return keyProvider.getPrivateKey(certAlias);
    }

    /**
     * Return the {@link java.security.PrivateKey} for the specified certAlias and encrypted private key password.
     * @param certAlias Certificate alias name
     * @param encryptedKeyPass The encrypted keypass to use when getting the private certificate
     * @return PrivateKey which matches the certAlias, return null if the private key could not be found.
     */
    public PrivateKey getPrivateKey (String certAlias, String encryptedKeyPass) {
        return keyProvider.getPrivateKey(certAlias, encryptedKeyPass);
    }

    /**
     * Get the alias name of the first keystore entry whose certificate matches 
     * the given certificate. 
     * @param cert Certificate 
     * @return the (alias) name of the first entry with matching certificate,
     *       or null if no such entry exists in this keystore. If the keystore 
     *       has not been loaded properly, return null as well. 
     */
    public String getCertificateAlias(Certificate cert) {
        return keyProvider.getCertificateAlias(cert);
    }
    
    /**
     * Get the private key password
     * @return the private key password
     */
    public String getPrivateKeyPass() {
        return keyProvider.getPrivateKeyPass();
    }

    /**
     * Get the keystore
     * @return the keystore
     */
    public KeyStore getKeyStore() {
        return keyProvider.getKeyStore();
    }

    /**
     * Return java.security.PrivateKey for the given X509Certificate.
     * @param cert X509Certificate
     * @return PrivateKey which matches the cert, return null if
           the private key could not be found.
     */
    //TODO:????? does not seem keystore support this 
    /*public java.security.PrivateKey getPrivateKey (
           java.security.cert.X509Certificate cert) {
        java.security.PrivateKey key = null; 
        if (SAMLUtilsCommon.debug.messageEnabled()) {
            SAMLUtilsCommon.debug.message("NOT implemented!");
        }
        return key;
    }*/
   
    /**
     * Set the Certificate with name certAlias in the leystore 
     * @param certAlias Certificate's name Alias
     * @param cert Certificate
     */
    public void setCertificateEntry(String certAlias, Certificate cert) throws SAMLException {
        try {
            keyProvider.setCertificateEntry(certAlias, cert);
        } catch (KeyStoreException e) {
            throw new SAMLException(e.getMessage());
        }
    }
    
    /**
     * Get the Certificate named certAlias. 
     * @param certAlias Certificate's name Alias
     * @return the Certificate, If the keystore 
     *       doesn't contain such certAlias, return null.
     */
    public Certificate getCertificate(String certAlias) {
        return keyProvider.getCertificate(certAlias);
    }
    
    /**
     * Store the keystore changes 
     */
    public void store() throws SAMLException {
        try {
            keyProvider.store();
        } catch (KeyStoreException e) {
            throw new SAMLException(e.getMessage());
        } catch (CertificateException e) {
            throw new SAMLException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new SAMLException(e.getMessage());
        } catch (IOException e) {
            throw new SAMLException(e.getMessage());
        }
    }

    /**
     * Return Certificate for the specified PublicKey.
     * @param publicKey Certificate public key
     * @return Certificate which matches the PublicKey, return null if
           the Certificate could not be found.
     */
    public Certificate getCertificate (java.security.PublicKey publicKey) {
        return keyProvider.getCertificate(publicKey);
    }
}
