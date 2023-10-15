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
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import static java.nio.charset.StandardCharsets.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyPair;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.forgerock.openam.keystore.KeyStoreConfig;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.SecurityDebug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

/**
 * Implementation of a {@code KeyProvider} interface for retrieving X509 Certificates and private
 * keys from the user data store.
 */
public class AMKeyProvider implements KeyProvider {

    private static final String DEFAULT_KEYSTORE_FILE_PROP = "com.sun.identity.saml.xmlsig.keystore";
    private static final String DEFAULT_KEYSTORE_PASS_FILE_PROP = "com.sun.identity.saml.xmlsig.storepass";
    private static final String DEFAULT_KEYSTORE_TYPE_PROP = "com.sun.identity.saml.xmlsig.storetype";
    private static final String DEFAULT_PRIVATE_KEY_PASS_FILE_PROP = "com.sun.identity.saml.xmlsig.keypass";

    private Debug logger = SecurityDebug.debug;

    private KeyStore ks = null;
    private String privateKeyPass = null;
    private String keystorePass   = "";
    private String keystoreFile = "";
    private String keystoreType = "JKS";
    private String storePassPath;
    private String keyPassPath;

    HashMap keyTable = new HashMap();

    /**
     * Constructor.
     */
    public AMKeyProvider() {
        this(DEFAULT_KEYSTORE_FILE_PROP, DEFAULT_KEYSTORE_PASS_FILE_PROP,
                DEFAULT_KEYSTORE_TYPE_PROP, DEFAULT_PRIVATE_KEY_PASS_FILE_PROP);
    }

    /**
     * Create a new instance of AMKeyProvider from a KeyStoreConfiguration object.
     *
     * @param kc The KeyStore configuration
     * @throws KeyStoreException if the keystore could not be opened or initialized
     * @throws IOException If the storepass or keypass files could not be opened
     */
    public AMKeyProvider(KeyStoreConfig kc) throws KeyStoreException, IOException {
        this.keystoreFile =     kc.getKeyStoreFile();
        this.keystorePass =     new String(kc.getKeyStorePassword());
        this.privateKeyPass =   new String(kc.getKeyPassword());
        this.keystoreType =     kc.getKeyStoreType();
        this.storePassPath =    kc.getKeyStorePasswordFile();
        this.keyPassPath =      kc.getKeyPasswordFile();
        mapPk2Cert();
    }

    /**
     * Constructor.
     *
     * @param keyStoreFilePropName The key store file property name.
     * @param keyStorePassFilePropName The key store password property name.
     * @param keyStoreTypePropName The key store type property name.
     * @param privateKeyPassFilePropName The key store private key password property name.
     */
    public AMKeyProvider(
            String keyStoreFilePropName, String keyStorePassFilePropName,
            String keyStoreTypePropName, String privateKeyPassFilePropName) {
        initialize(keyStoreFilePropName, keyStorePassFilePropName,
                keyStoreTypePropName, privateKeyPassFilePropName);
        mapPk2Cert();
    }

    /**
     * Constructor.
     * Already resolved is simply to give a different signature
     *
     * @param alreadyResolved {@code true} if already resolved.
     * @param keyStoreFile The key store file.
     * @param keyStorePass The key store password.
     * @param keyStoreType The key store type.
     * @param privateKeyPass The key store private key password.
     */
    public AMKeyProvider(boolean alreadyResolved,
            String keyStoreFile, String keyStorePass,
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

        this.storePassPath = SystemPropertiesManager.get(keyStorePassFilePropName);

        String tmpKsType = SystemPropertiesManager.get(keyStoreTypePropName);
        if (null != tmpKsType) {
            keystoreType = tmpKsType.trim();
        }

        if (storePassPath != null) {
            keystorePass = readPasswordFile(storePassPath);
        } else {
            logger.error("JKSKeyProvider: keystore password is null");
        }

        this.keyPassPath = SystemPropertiesManager.get(privateKeyPassFilePropName);

        if (keyPassPath != null) {
            privateKeyPass = readPasswordFile(keyPassPath);
        }
    }

    /**
     * Read a keystore password file (example: .storepass / .keypass ).
     *
     * @param filePath  Path to the password file
     * @return The password in clear text, or null if an IOException occurs.
     */
    protected String readPasswordFile(String filePath) {
        String p = null;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            p = br.readLine();
            // The password may or may not be encrypted.
            // If it is not, decodePassword will just return the plain text - which is what we want
            p = decodePassword(p);
        } catch (IOException e) {
            logger.error("Unable to read private key password file " + filePath, e);
        }
        return p;
    }

    /**
     * Decodes the given password and returns it. If decoding fails simply returns the same password parameter.
     *
     * @param password The password that requires decoding.
     * @return The decoded password or the same password parameter if the decoding failed.
     */
    public static String decodePassword(String password) {
        String decodedPassword = AccessController.doPrivileged(new DecodeAction(password));

        return decodedPassword == null ? password : decodedPassword;
    }

    private void mapPk2Cert() {
        try {
            ks = KeyStore.getInstance(keystoreType);
            if (keystoreFile == null || keystoreFile.isEmpty()) {
                logger.error("mapPk2Cert.JKSKeyProvider: KeyStore FileName is null, "
                        + "unable to establish Mapping Public Keys to Certificates!");
                return;
            }
            FileInputStream fis = new FileInputStream(keystoreFile);
            ks.load(fis, keystorePass.toCharArray());
            // create publickey to Certificate mapping
            for (Enumeration e = ks.aliases(); e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                // if this is not a Private or public Key,  then continue.
                if (ks.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
                    continue;
                }
                Certificate cert = getCertificate(alias);
                PublicKey pk = getPublicKey(alias);
                String key =
                        Base64.encode(pk.getEncoded());
                keyTable.put(key, cert);

            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            logger.error("mapPk2Cert.JKSKeyProvider:", e);
        }
    }

    /**
     * Sets the debug logger.
     *
     * @param logger The debug logger.
     */
    public void setLogger(Debug logger) {
        this.logger = logger;
    }

    /**
     * Set the key to access key store database. This method will only need to
     * be called once if the key could not be obtained by other means.
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
     * @return X509Certificate which matches the certAlias, return null if the certificate could not be found.
     */
    public java.security.cert.X509Certificate getX509Certificate(
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
     * Return java.security.PublicKey for the specified keyAlias.
     * @param keyAlias Key alias name
     * @return PublicKey which matches the keyAlias, return null if the PublicKey could not be found.
     */
    public java.security.PublicKey getPublicKey(String keyAlias) {
        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }
        java.security.PublicKey pkey = null;
        try {
            X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
            if (cert == null) {
                logger.error("Unable to retrieve certificate with alias '" + keyAlias + "' from keystore "
                        + "'" + this.keystoreFile + "'");
                return null;
            }
            pkey = cert.getPublicKey();
        } catch (KeyStoreException e) {
            logger.error("Unable to get public key:" + keyAlias, e);
        }
        return pkey;
    }

    /**
     * Return java.security.PrivateKey for the specified certAlias.
     * @param certAlias Certificate alias name
     * @return PrivateKey which matches the certAlias, return null if the private key could not be found.
     */
    public java.security.PrivateKey getPrivateKey(String certAlias) {
        java.security.PrivateKey key = null;
        try {
            key = (PrivateKey) ks.getKey(certAlias,
                    privateKeyPass.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            logger.error(e.getMessage());
        }
        return key;
    }

    @Override
    public SecretKey getSecretKey(String certAlias) {
        try {
            Key key = ks.getKey(certAlias, privateKeyPass.toCharArray());

            if (key instanceof SecretKey) {
                return (SecretKey) key;
            }
            if (key!=null)
            	logger.error("Expected a key of type javax.crypto.SecretKey but got " + key.getClass().getName());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            logger.error("Unable to get the secret key for certificate alias " + certAlias, e);
        }

        return null;
    }

    /**
     * Return the {@link java.security.PrivateKey} for the specified certAlias and encrypted private key password.
     * @param certAlias Certificate alias name
     * @param encryptedKeyPass The encrypted key password to use when getting the private certificate
     * @return PrivateKey which matches the certAlias, return null if the private key could not be found.
     */
    public PrivateKey getPrivateKey(String certAlias, String encryptedKeyPass) {

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
            logger.error("AMKeyProvider.getPrivateKey: "
                    + "null key password returned from decryption for certificate alias:" + certAlias
                    + " The password maybe incorrect.");
        }

        return key;
    }

    private static Map<String, PrivateKey> mapKey = new ConcurrentHashMap<>();
    /**
     * Return {@link KeyPair} containing {@link PublicKey} and {@link PrivateKey} for the specified certAlias.
     *
     * @param certAlias Certificate alias name
     *
     * @return KeyPair which matches the certAlias, return null if the PrivateKey or PublicKey could not be found.
     */
    public KeyPair getKeyPair(String certAlias) {

        PublicKey publicKey = getPublicKey(certAlias);
        PrivateKey privateKey=null;
        if (mapKey.containsKey(certAlias))
        {
        	privateKey=mapKey.get(certAlias);
        }
        else
        {
        	privateKey = getPrivateKey(certAlias);
		if (privateKey != null)
        		mapKey.putIfAbsent(certAlias, privateKey);
        }

        if (publicKey != null && privateKey != null) {
            return new KeyPair(publicKey, privateKey);
        } else {
            return null;
        }
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
     * Get the private key password.
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
     * Get the storepass path.
     *
     * @return the file path to .storepass
     */
    public String getKeystorePasswordFilePath() {
        return this.storePassPath;
    }

    /**
     * Get the .keypass path. This is the optional per key password, and is usually the same as the storepass.
     *
     * @return the file path to .keypass
     */
    public String getKeyPasswordFilePath() {
        return this.keyPassPath;
    }


    /**
     * Get the keystore.
     * @return the keystore
     */
    public KeyStore getKeyStore() {
        return ks;
    }

    /**
     * Set the Certificate with name certAlias in the keystore.
     * @param certAlias Certificate's name Alias
     * @param cert Certificate
     * @throws KeyStoreException If an error occurs when setting the certificate entry.
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
     * Store the keystore changes.
     *
     * @throws IOException If an error occurs when saving the keystore.
     * @throws CertificateException If an error occurs when saving the keystore.
     * @throws NoSuchAlgorithmException If an error occurs when saving the keystore.
     * @throws KeyStoreException If an error occurs when saving the keystore.
     */
    public void store() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        try {
            //  Save keystore to file.
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
    public Certificate getCertificate(
            java.security.PublicKey publicKey) {
        String key = Base64.encode(publicKey.getEncoded());
        return (Certificate) keyTable.get(key);
    }

    @Override
    public boolean containsKey(String alias) {
        try {
            return ks.containsAlias(alias);
        } catch (KeyStoreException ksE) {
            logger.error("Unable to determine key alias presence", ksE);
            return false;
        }
    }

    /**
     * Store a secret (typically a password) in the keystore
     *
     * The secret is protected with the same password as the keystore itself.
     *
     * If the alias already exists, the new secret will replace the old one
     *
     * @param alias  - the alias to store the password under
     * @param password - password or secret to store
     * @throws KeyStoreException if the password can not be stored in the keystore
     */
    public void setSecretKeyEntry(String alias, String password) throws KeyStoreException {
        SecretKeySpec keyspec =     new SecretKeySpec(password.getBytes(UTF_8), "RAW");
        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(keystorePass.toCharArray());

        try {
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias);
            }
            KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(keyspec);
            ks.setEntry(alias, entry, keyStorePP);
        } finally {
            try {
                keyStorePP.destroy();
            } catch (DestroyFailedException e) {
                // It's OK to swallow this  - means that we could not wipe the password, but this is never
                // guaranteed to work anyway.
            }
        }
    }

    /**
     * Retrieve store secret (usually a password).
     *
     * @param alias the alias of the secret
     * @return the plain text secret
     * @throws KeyStoreException if the password can not be read
     */
    public String getSecret(String alias) throws KeyStoreException  {

        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(keystorePass.toCharArray());
        try {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(alias, keyStorePP);
            return new String(entry.getSecretKey().getEncoded(), UTF_8);
        } catch (Exception e) {
            // to be nice we wrap and rethrows to a single exception type
            throw new KeyStoreException("Exception trying to fetch key with alias " + alias, e);
        } finally {
            try {
                keyStorePP.destroy();
            } catch (DestroyFailedException e) {
                // wrap and rethrow. This exception should never really happen...
                throw new KeyStoreException("Destroy failed", e);
            }
        }
    }
}
