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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProvider
 * This interface was originally implemented using the wss4j Merlin class. Getting the wss4j dependencies out of OpenAM
 * excluded that approach. It is tempting to use OpenAM's AMKeyProvider or the JKSKeyProvider classes, but neither throw
 * exceptions upon initialization, and will log to a non-sts log file. I need to be able to throw exceptions if keystore
 * state cannot be initialized, as mis-configuring keystore locations and passwords is a commonly-encountered error
 * when publishing sts instances. In addition, the AMKeyProvider and JKSKeyProvider will only load a keystore from
 * the filesystem - it may be helpful to be able to load a keystore from the classpath, as Merlin supports this feature.
 */
public class STSKeyProviderImpl implements STSKeyProvider {
    private static final String FORWARD_SLASH = "/";
    private final KeyStore keyStore;
    private final SAML2Config saml2Config;
    private final Logger logger;

    /*
    ctor not guice-injected as instance created by STSInstanceStateImpl
     */
    public STSKeyProviderImpl(SAML2Config saml2Config, Logger logger) throws TokenCreationException {
        this.saml2Config = saml2Config;
        this.logger = logger;
        keyStore = loadKeystore();
    }

    /**
     *
     * @return The InputStream, if the keystore name can be resolved from the classpath, and null if not.
     */
    private InputStream getKeystoreFromClasspath() {
        final String keystoreLocation = saml2Config.getKeystoreFileName();
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
            return new FileInputStream(saml2Config.getKeystoreFileName());
        } else {
            return inputStream;
        }
    }

    private KeyStore loadKeystore() throws TokenCreationException {
        InputStream inputStream;
        try {
            inputStream = getKeystoreInputStream();
        } catch (FileNotFoundException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not find keystore file at location "
                    + saml2Config.getKeystoreFileName() + " neither on the filesystem, nor on the classpath.");
        }
        //TODO: do I want to support the specification of keystore type in the SAML2Config - now it is hard-coded,
        // as it was with the Merlin-based implementation
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Could not get JKS keystore: " + e.getMessage(), e);
        }
        try {
            keyStore.load(inputStream, new String(saml2Config.getKeystorePassword(), AMSTSConstants.UTF_8_CHARSET_ID).toCharArray());
            return keyStore;
        } catch (IOException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not load keystore at location "
                    + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Could not load keystore at location "
                    + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Could not load keystore at location "
                    + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        }
    }

    public X509Certificate getX509Certificate(String certAlias) throws TokenCreationException {
        try {
            return (X509Certificate)keyStore.getCertificate(certAlias);
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not find X509Certificate under alias "
                    + certAlias + " at keystore location " + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        }
    }

    public PrivateKey getPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        final KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(keyPassword.toCharArray());
        try {
            final KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, passwordProtection);
            return pkEntry.getPrivateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        } catch (UnrecoverableEntryException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        } catch (KeyStoreException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not obtain private key entry with alias "
                    + keyAlias + " from keystore at location " + saml2Config.getKeystoreFileName() + ": " + e.getMessage(), e);
        }
    }
}
