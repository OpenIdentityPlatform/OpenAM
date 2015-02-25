/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package org.forgerock.openam.utils;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.SecurityDebug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;

public class AMKeyProvider implements KeyProvider {

    private Debug logger = SecurityDebug.debug;

    private KeyStore ks = null;
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

    /**
     * Constructor
     */
    public AMKeyProvider() {
        this(DEFAULT_KEYSTORE_FILE_PROP,DEFAULT_KEYSTORE_PASS_FILE_PROP,
                DEFAULT_KEYSTORE_TYPE_PROP, DEFAULT_PRIVATE_KEY_PASS_FILE_PROP);
    }

    /**
     * Constructor
     */
    public AMKeyProvider(
            String keyStoreFilePropName,String keyStorePassFilePropName,
            String keyStoreTypePropName, String privateKeyPassFilePropName) {
        initialize(keyStoreFilePropName, keyStorePassFilePropName,
                keyStoreTypePropName, privateKeyPassFilePropName);
        mapPk2Cert();
    }
    /**
     * Constructor
     * Already resolved is simply to give a different signature
     */
    public AMKeyProvider( boolean alreadyResolved,
            String keyStoreFile,String keyStorePass,
            String keyStoreType, String privateKeyPass) {
        this.keystoreFile = keyStoreFile;
        this.keystoreType = keyStoreType;
        this.keystorePass = keyStorePass;
        this.privateKeyPass = privateKeyPass;

        mapPk2Cert();
    }

    private void initialize(String keyStoreFilePropName, String keyStorePassFilePropName,
            String keyStoreTypePropName, String privateKeyPassFilePropName) {

        BufferedReader br = null;

        keystoreFile = SystemPropertiesManager.get(keyStoreFilePropName);

        if (keystoreFile == null || keystoreFile.length() == 0) {
            logger.error("JKSKeyProvider: keystore file does not exist");
        }

        String kspfile = SystemPropertiesManager.get(keyStorePassFilePropName);

        String tmp_ksType = SystemPropertiesManager.get(keyStoreTypePropName);
        if ( null != tmp_ksType ) {
            keystoreType = tmp_ksType.trim();
        }

        if (kspfile != null) {
            try {
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(kspfile)));
                    keystorePass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("JKSKeyProvider.initialize: Unable to read keystore password file " + kspfile);
            }
        } else {
            logger.error("JKSKeyProvider: keystore password is null");
        }

        String pkpfile = SystemPropertiesManager.get(privateKeyPassFilePropName);

        if (pkpfile != null) {
            try {
                try {
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(pkpfile)));
                    privateKeyPass = decodePassword(br.readLine());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("JKSKeyProvider.initialize: Unable to read privatekey password file " + kspfile);
            }
        }
    }

    /**
     * Decodes the given password and returns it. If decoding fails simply returns the same password parameter.
     *
     * @param password The password that requires decoding.
     * @return The decoded password or the same password parameter if the decoding failed.
     */
    public static String decodePassword(String password)  {
        String decodedPassword = AccessController.doPrivileged(new DecodeAction(password));

        return decodedPassword == null ? password : decodedPassword;
    }

    private void mapPk2Cert(){
        try {
            ks = KeyStore.getInstance(keystoreType);
            if ( (keystoreFile == null) || (keystoreFile.isEmpty()) ) {
                logger.error("mapPk2Cert.JKSKeyProvider: KeyStore FileName is null, "
                        + "unable to establish Mapping Public Keys to Certificates!");
                return;
            }
            FileInputStream fis = new FileInputStream(keystoreFile);
            ks.load(fis, keystorePass.toCharArray());
            // create publickey to Certificate mapping
            for(Enumeration e=ks.aliases();e.hasMoreElements();) {
                String alias = (String) e.nextElement ();
                // if this is not a Private or public Key,  then continue.
                if (ks.entryInstanceOf( alias, KeyStore.SecretKeyEntry.class)){
                    continue;
                }
                Certificate cert = getCertificate(alias);
                PublicKey pk = getPublicKey(alias);
                String key =
                        Base64.encode(pk.getEncoded());
                keyTable.put(key, cert);

            }
            logger.message("KeyTable size = " + keyTable.size());
        } catch (KeyStoreException e) {
            logger.error("mapPk2Cert.JKSKeyProvider:", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("mapPk2Cert.JKSKeyProvider:", e);
        } catch (CertificateException e) {
            logger.error("mapPk2Cert.JKSKeyProvider:", e);
        } catch (IOException e) {
            logger.error("mapPk2Cert.JKSKeyProvider:", e);
        }
    }

    public void setLogger(Debug logger) {
        this.logger = logger;
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
        } catch (KeyStoreException e) {
            logger.error("Unable to get cert alias:" + certAlias, e);
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
            X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
            pkey = cert.getPublicKey();
        } catch (KeyStoreException e) {
            logger.error("Unable to get public key:" + keyAlias, e);
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
        } catch (KeyStoreException e) {
            logger.error(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        } catch (UnrecoverableKeyException e) {
            logger.error(e.getMessage());
        }
        return key;
    }

    /**
     * Return the {@link java.security.PrivateKey} for the specified certAlias and encrypted private key password.
     * @param certAlias Certificate alias name
     * @param encryptedKeyPass The encrypted key password to use when getting the private certificate
     * @return PrivateKey which matches the certAlias, return null if the private key could not be found.
     */
    public PrivateKey getPrivateKey (String certAlias, String encryptedKeyPass) {

        PrivateKey key = null;

        String keyPass = decodePassword(encryptedKeyPass);
        if (keyPass != null) {
            try {
                key = (PrivateKey) ks.getKey(certAlias, keyPass.toCharArray());
            } catch (KeyStoreException e) {
                logger.error(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                logger.error(e.getMessage());
            } catch (UnrecoverableKeyException e) {
                logger.error(e.getMessage());
            }
        } else {
            logger.error("AMKeyProvider.getPrivateKey: " +
                    "null key password returned from decryption for certificate alias:" + certAlias +
                    " The password maybe incorrect.");
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
     * Gets the Keystore password.
     *
     * @return The Keystore password
     */
    public char[] getKeystorePass() {
        return keystorePass.toCharArray();
    }

    /**
     * Get the private key password
     * @return the private key password
     */
    public String getPrivateKeyPass() {
        return privateKeyPass;
    }

    /**
     * Gets the Keystore type.
     *
     * @return The Keystore type.
     */
    public String getKeystoreType() {
        return keystoreType;
    }

    /**
     * Gets the Keystore File path.
     *
     * @return The Keystore file path.
     */
    public String getKeystoreFilePath() {
        return keystoreFile;
    }

    /**
     * Get the keystore
     * @return the keystore
     */
    public KeyStore getKeyStore() {
        return ks;
    }

    /**
     * Set the Certificate with name certAlias in the leystore
     * @param certAlias Certificate's name Alias
     * @param cert Certificate
     */
    public void setCertificateEntry(String certAlias, Certificate cert) throws KeyStoreException {
        try {
            ks.setCertificateEntry(certAlias, cert);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Get the Certificate named certAlias.
     * @param certAlias Certificate's name Alias
     * @return the Certificate, If the keystore
     *       doesn't contain such certAlias, return null.
     */
    public Certificate getCertificate(String certAlias)  {
        try {
            return ks.getCertificate(certAlias);
        } catch (KeyStoreException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Store the keystore changes
     */
    public void store() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        try {
//            Save keystore to file.
            FileOutputStream keyStoreOStream =
                    new FileOutputStream(keystoreFile);
            ks.store(keyStoreOStream, keystorePass.toCharArray());
            keyStoreOStream.close();
            keyStoreOStream = null;
            if (logger.messageEnabled()) {
                logger.message("Keystore saved in " + keystoreFile);
            }
        } catch (KeyStoreException e) {
            logger.error(e.getMessage());
            throw e;
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
