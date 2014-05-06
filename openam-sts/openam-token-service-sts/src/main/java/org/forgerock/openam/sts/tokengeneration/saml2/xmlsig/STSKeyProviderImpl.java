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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2.xmlsig;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.CryptoType;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.KeystoreConfig;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProvider
 */
public class STSKeyProviderImpl implements STSKeyProvider {
    private final Crypto crypto;
    private final KeystoreConfig keystoreConfig;

    /*
    ctor not guice-injected as instance created by STSInstanceStateImpl
     */
    public STSKeyProviderImpl(KeystoreConfig keystoreConfig) {
        this.keystoreConfig = keystoreConfig;
        crypto = createCrypto();
    }

    //TODO: proper, checked exception, and clarify X509Certificate[] when signing. The CXF-STS just returns [0] - does
    //this make sense in all cases?
    public X509Certificate getX509Certificate(String certAlias) {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(certAlias);
        try {
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs == null) {
                throw new IllegalStateException("No certificates pulled from the keystore for alias "
                        + certAlias);
            } else if (certs.length > 1) {
                throw new IllegalStateException("The number of certificates pulled from the keystore for alias "
                        + certAlias + " is greater than 1: " + certs.length);
            } else {
                return certs[0];
            }
        } catch (WSSecurityException e) {
            throw new IllegalStateException("Exception caught pulling X509 cert from crypto for alias: "
                    + certAlias + ". Exception: " + e, e);
        }
    }

    public PrivateKey getPrivateKey(String keyAlias, String keyPassword) {
        try {
            return crypto.getPrivateKey(keyAlias, keyPassword);
        } catch (WSSecurityException e) {
            //TODO: proper exception
            throw new IllegalStateException("Exception pulling private key from crypto for alias " + keyAlias);
        }
    }

    private Crypto createCrypto() {
        try {
            return CryptoFactory.getInstance(getEncryptionProperties());
        } catch (WSSecurityException e) {
            String message = "Exception caught initializing the CryptoFactory: " + e;
            throw new IllegalStateException(message);
        }
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        String keystorePassword  = null;
        try {
            keystorePassword = new String(keystoreConfig.getKeystorePassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported string encoding for keystore password: " + e);
        }
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", keystorePassword);
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", keystoreConfig.getKeystoreFileName());
        properties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
        return properties;
    }
}
