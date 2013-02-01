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

package com.sun.identity.saml.xmlsig;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Enumeration;
import com.sun.identity.shared.encode.Base64;

/**
 * The class <code>AMKeyProvider</code> is a class
 * that is implemented to retrieve X509Certificates and Private Keys from
 * user data store.  
 * <p>
 */

public class JKSKeyProvider implements KeyProvider {
    private KeyStore ks = null; 
    //TODO: move the below two password to AMConfig.properties
    private String privateKeyPass = null;
    private String keystorePass   = "";
    private String keystoreFile = "";
    private String keystoreType = "JKS";
    private final static String DEFAULT_KEYSTORE_FILE_PROP = 
            "com.sun.identity.saml.xmlsig.keystore";
    private final static String DEFAULT_KEYSTORE_PASS_FILE_PROP = 
            "com.sun.identity.saml.xmlsig.storepass";
    private final static String DEFAULT_KEYSTORE_TYPE_PROP = 
            "com.sun.identity.saml.xmlsig.storetype";
    private final static String DEFAULT_PRIVATE_KEY_PASS_FILE_PROP  = 
            "com.sun.identity.saml.xmlsig.keypass";
    
    HashMap keyTable = new HashMap();

    
 
    private void initialize(
            String keyStoreFilePropName, String keyStorePassFilePropName,
            String keyStoreTypePropName, String privateKeyPassFilePropName) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        keystoreFile = SystemConfigurationUtil.getProperty(
            keyStoreFilePropName);

        if (keystoreFile == null || keystoreFile.length() == 0) {
            SAMLUtilsCommon.debug.error(
		"JKSKeyProvider: keystore file does not exist"); 
        }

        String kspfile = SystemConfigurationUtil.getProperty(
            keyStorePassFilePropName);

        String tmp_ksType = SystemConfigurationUtil.getProperty(
            keyStoreTypePropName);
        if ( null != tmp_ksType ) {
            keystoreType = tmp_ksType.trim();
        }
        
        if (kspfile != null) {
            try {
                fis = new FileInputStream(kspfile);
                isr = new InputStreamReader(fis);
                br = new BufferedReader(isr);
                keystorePass = SAMLUtilsCommon.decodePassword(
                    br.readLine());  
                fis.close(); 
            } catch (Exception ex) {
                ex.printStackTrace();
                SAMLUtilsCommon.debug.error("JKSKeyProvider.initialize:"+
                    " Unable to read keystore password file " + kspfile);
            }
        } else {
            SAMLUtilsCommon.debug.error("JKSKeyProvider: keystore" +
                " password is null");
        }

        String pkpfile = SystemConfigurationUtil.getProperty(
            privateKeyPassFilePropName);

        if (pkpfile != null) {
            try {
                fis = new FileInputStream(pkpfile);
                isr = new InputStreamReader(fis);
                br = new BufferedReader(isr);
                privateKeyPass = SAMLUtilsCommon.decodePassword(
                    br.readLine());   
                fis.close(); 
            } catch (Exception ex) {
                ex.printStackTrace();
                SAMLUtilsCommon.debug.error("JKSKeyProvider.initialize: "+
                    "Unable to read privatekey password file " + kspfile);
            }
        } 
    }
    
    
    private void mapPk2Cert(){
        try {
            ks = KeyStore.getInstance(keystoreType);
            if ( (keystoreFile == null) || (keystoreFile.isEmpty()) ) {
                SAMLUtilsCommon.debug.error("mapPk2Cert.JKSKeyProvider: KeyStore FileName is null, " +
                        "unable to establish Mapping Public Keys to Certificates!");
                return;
            }
            FileInputStream fis = new FileInputStream(keystoreFile);
            ks.load(fis, keystorePass.toCharArray());
	    // create publickey to Certificate mapping
	    for(Enumeration e=ks.aliases();e.hasMoreElements();) {
		String alias = (String) e.nextElement ();
	        Certificate cert = getCertificate(alias);
		PublicKey pk = getPublicKey(alias);
		String key =
			Base64.encode(pk.getEncoded());
		keyTable.put(key, cert);

	    }
	    SAMLUtilsCommon.debug.message("KeyTable size = " +
                keyTable.size());
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("mapPk2Cert.JKSKeyProvider:", e);
        }        
    }
    
    /**
     * Constructor
     */
    public JKSKeyProvider() {
        this(DEFAULT_KEYSTORE_FILE_PROP,DEFAULT_KEYSTORE_PASS_FILE_PROP,
               DEFAULT_KEYSTORE_TYPE_PROP, DEFAULT_PRIVATE_KEY_PASS_FILE_PROP);

    }
    
    
       /**
     * Constructor
     */
    public JKSKeyProvider(
            String keyStoreFilePropName,String keyStorePassFilePropName,
            String keyStoreTypePropName, String privateKeyPassFilePropName) {
        initialize(keyStoreFilePropName, keyStorePassFilePropName,
                keyStoreTypePropName, privateKeyPassFilePropName); 
        mapPk2Cert();
    }
    
    
    /**
     * Set the key to access key store database. This method will only need to 
     * be calles once if the key could not be obtained by other means. 
     * @param storepass  password for the key store
     * @param keypass password for the certificate
     */
    public void setKey(String storepass, String keypass) {
        keystorePass = storepass; 
        privateKeyPass = keypass;
    }

    /**
     * Return java.security.cert.X509Certificate for the specified certAlias.
     * @param certAlias Certificate alias name 
     * @return X509Certificate which matches the certAlias, return null if
           the certificate could not be found.
     */
    public java.security.cert.X509Certificate getX509Certificate (
           String certAlias) {
        if (certAlias == null || certAlias.length() == 0) {
            return null;
        }
        java.security.cert.X509Certificate cert = null;
        try {
            cert = (X509Certificate) ks.getCertificate(certAlias);
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("Unable to get cert alias:" +
                certAlias, e); 
        }
        return cert; 
    }
    
    /**
     * Return java.security.PublicKey for the specified keyAlias
     * @param keyAlias Key alias name
     * @return PublicKey which matches the keyAlias, return null if
           the PublicKey could not be found.
     */
    public java.security.PublicKey getPublicKey (String keyAlias) {
        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }
        java.security.PublicKey pkey = null;
        try {
            java.security.cert.X509Certificate cert =
                                (X509Certificate) ks.getCertificate(keyAlias);
            pkey = cert.getPublicKey();
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("Unable to get public key:" +
                keyAlias, e); 
        }
        return pkey; 
    }

    /**
     * Return java.security.PrivateKey for the specified certAlias.
     * @param certAlias Certificate alias name  
     * @return PrivateKey which matches the certAlias, return null if
           the private key could not be found.
     */
    public java.security.PrivateKey getPrivateKey (String certAlias) {
        java.security.PrivateKey key = null;       
        try {
            key = (PrivateKey) ks.getKey(certAlias,
                                         privateKeyPass.toCharArray());
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error(e.getMessage());
        }
        return key;
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
        String certalias = null; 
        try {
            if (ks != null) {
                certalias = ks.getCertificateAlias(cert); 
            }
        } catch (KeyStoreException ke) {
            return null;
        }
        return certalias;
    }
    
    /**
     * Get the private key password
     * @return the private key password
     */
    public String getPrivateKeyPass() {
        return privateKeyPass;
    }

    /**
     * Get the keystore
     * @return the keystore
     */
    public KeyStore getKeyStore() {
        return ks;
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
    public void setCertificateEntry(String certAlias, Certificate cert)
        throws SAMLException {
        try {
            ks.setCertificateEntry(certAlias, cert); 
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error(e.getMessage()); 
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
        try {
            return ks.getCertificate(certAlias); 
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error(e.getMessage()); 
        }
        return null; 
    }
    
    /**
     * Store the keystore changes 
     */
    public void store() throws SAMLException {
        try { 
            // Save keystore to file.
            FileOutputStream keyStoreOStream =
                            new FileOutputStream(keystoreFile);
            ks.store(keyStoreOStream, keystorePass.toCharArray()); 
            keyStoreOStream.close();
            keyStoreOStream = null;
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Keystore saved in " +
                keystoreFile);
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error(e.getMessage()); 
            throw new SAMLException(e.getMessage()); 
        }
    }

    /**
     * Return Certificate for the specified PublicKey.
     * @param publicKey Certificate public key
     * @return Certificate which matches the PublicKey, return null if
           the Certificate could not be found.
     */
    public Certificate getCertificate (
            java.security.PublicKey publicKey) {
	String key = Base64.encode(publicKey.getEncoded());
        return (Certificate) keyTable.get (key);
    }
}
