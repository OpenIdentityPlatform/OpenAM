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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Base class which encapsulates keystore access to provide the crypto context for signing and encrypting SAML2 assertions
 * and OpenIdConnect tokens.
 * This functionality was originally implemented using the wss4j Merlin class. Getting the wss4j dependencies out of OpenAM
 * excluded that approach. It is tempting to use OpenAM's AMKeyProvider or the JKSKeyProvider classes, but neither throw
 * exceptions upon initialization, and will log to a non-sts log file. I need to be able to throw exceptions if keystore
 * state cannot be initialized, as mis-configuring keystore locations and passwords is a commonly-encountered error
 * when publishing sts instances. In addition, the AMKeyProvider and JKSKeyProvider will only load a keystore from
 * the filesystem - it may be helpful to be able to load a keystore from the classpath, as Merlin supports this feature.
 */
public abstract class STSCryptoProviderBase {
    protected static final String JKS_KEYSTORE = "JKS";
    private static final String FORWARD_SLASH = "/";
    private final KeyStore keystore;
    private final String keystoreLocation;
    private final byte[] keystorePassword;
    private final String keystoreType;

    /**
     *
     * @param keystoreLocation the location of the keystore, either on the filesystem, or on the classpath
     * @param keystorePassword the UTF-8 encoded representation of the keystore password.
     * @param keystoreType the type of keystore
     * @throws TokenCreationException if the keystore cannot be loaded at the specified location.
     */
    public STSCryptoProviderBase(String keystoreLocation, byte[] keystorePassword, String keystoreType) throws TokenCreationException {
        this.keystoreLocation = keystoreLocation;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        keystore = loadKeystore();

    }
    /**
     *
     * @return The InputStream, if the keystore name can be resolved from the classpath, and null if not.
     */
    private InputStream getKeystoreFromClasspath() {
        InputStream inputStream = getClass().getResourceAsStream(keystoreLocation);
        if (inputStream == null && !keystoreLocation.startsWith(FORWARD_SLASH)) {
            return getClass().getResourceAsStream(FORWARD_SLASH + keystoreLocation);
        } else {
            return inputStream;
        }
    }

    private InputStream getKeystoreInputStream() throws FileNotFoundException {
        InputStream inputStream = getKeystoreFromClasspath();
        if (inputStream == null) {
            return new BufferedInputStream(new FileInputStream(keystoreLocation));
        } else {
            return new BufferedInputStream(inputStream);
        }
    }

    private KeyStore loadKeystore() throws TokenCreationException {
        InputStream inputStream;
        try {
            inputStream = getKeystoreInputStream();
        } catch (FileNotFoundException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not find keystore file at location "
                    + keystoreLocation + " neither on the filesystem, nor on the classpath.");
        }
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(keystoreType);
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Could not get JKS keystore: " + e.getMessage(), e);
        }
        try {
            keyStore.load(inputStream, new String(keystorePassword, AMSTSConstants.UTF_8_CHARSET_ID).toCharArray());
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not load keystore at location "
                    + keystoreLocation + ": " + e.getMessage(), e);
        }
    }

    /**
     * This method will be called to obtain the SAML2 ServiceProvider's X509Certificate, whose public key should be used to encrypt
     * the generated symmetric key used to encrypt the SAML2 assertion
     * @param certAlias the alias referencing the SAML2 Service Provider's certificate
     * @return the X509Certificate - null will not be returned.
     * @throws TokenCreationException if the certificate could not be obtained
     */
    protected X509Certificate getX509Certificate(String certAlias) throws TokenCreationException {
        try {
            Certificate certificate = keystore.getCertificate(certAlias);
            if (!(certificate instanceof X509Certificate)) {
                throw new TokenCreationException(ResourceException.CONFLICT, "Could not find X509Certificate under alias "
                        + certAlias + " at keystore location " + keystoreLocation);
            }
            return (X509Certificate)certificate;
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not find X509Certificate under alias "
                    + certAlias + " at keystore location " + keystoreLocation + ": " + e.getMessage(), e);
        }
    }

    /**
     * This method will be called to obtain the Certificate chain corresponding to the OpenIdConnect Token provider's
     * PrivateKeyEntry. See the KeyStore javadocs for getCertificate and getCertificateChain for details. The bottom line
     * is that getCertificate will be called for a TrustedCertificateEntry, and getCertificateChain will be called to
     * obtain the X509Certificate[] corresponding to a PrivateKeyEntry. Trust in a PrivateKeyEntry might involve some
     * intermediate CAs - hence the X509Certificate[].
     * @param certAlias the alias referencing either the PrivateKeyEntry corresponding to the OpenIdConnect Token provider.
     * @return the X509Certificate[], with the leaf certificate the first element in the array. Null will not be returned.
     * @throws TokenCreationException if the certificate could not be obtained
     */
    protected X509Certificate[] getX509CertificateChain(String certAlias) throws TokenCreationException {
        try {
            Certificate[] certificateArray = keystore.getCertificateChain(certAlias);
            if (!(certificateArray instanceof X509Certificate[])) {
                throw new TokenCreationException(ResourceException.CONFLICT, "Could not find X509Certificate chain under alias "
                        + certAlias + " at keystore location " + keystoreLocation);
            }
            return (X509Certificate[])certificateArray;
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not find X509Certificate under alias "
                    + certAlias + " at keystore location " + keystoreLocation + ": " + e.getMessage(), e);
        }
    }

    /**
     * This method will be called to obtain the PrivateKey used to sign SAML2 assertions and OpenIdConnect Tokens
     * @param keyAlias the alias referencing the PrivateKeyEntry
     * @param keyPassword the password to the key
     * @return the PrivateKey entry used to sign the token
     * @throws TokenCreationException thrown if the alias/password does not reference an entry
     */
    protected PrivateKey getPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        final KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(keyPassword.toCharArray());
        try {
            final KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(keyAlias, passwordProtection);
            if (pkEntry == null) {
                throw new TokenCreationException(ResourceException.CONFLICT, "Could not obtain private key entry with alias "
                        + keyAlias + " from keystore at location " + keystoreLocation);
            }
            return pkEntry.getPrivateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + keystoreLocation + ": " + e.getMessage(), e);
        } catch (UnrecoverableEntryException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + keystoreLocation + ": " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.CONFLICT, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + keystoreLocation + ": " + e.getMessage(), e);
        }
    }
}
