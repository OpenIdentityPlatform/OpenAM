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
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProvider
 */
public class STSKeyProviderImpl implements STSKeyProvider {
    private static final String MERLIN_KEYSTORE_FILE_KEY = "org.apache.ws.security.crypto.merlin.keystore.file";
    private final Crypto crypto;
    private final SAML2Config saml2Config;
    private final Logger logger;

    /*
    ctor not guice-injected as instance created by STSInstanceStateImpl
     */
    public STSKeyProviderImpl(SAML2Config saml2Config, Logger logger) throws TokenCreationException {
        this.saml2Config = saml2Config;
        this.logger = logger;
        crypto = createCrypto();
    }

    /*
    Note that there is a semantic impedance between the API exposed by the Crypto interface and signature x509Cert
    exigencies: the Crytpo.getX509Certificates method returns a X509Certificate[], presumably to support cert chains,
    whereas signature exigencies only require a single X509Certificate instance. The CXF-STS code follows the convention
    of simply using the first element in the array, and I will do the same. I will not throw an exception if more than
    a single X509Certificate is returned for a given alias, but rather log a warning.
     */
    public X509Certificate getX509Certificate(String certAlias) throws TokenCreationException {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(certAlias);
        try {
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            if (certs == null) {
                throw new TokenCreationException(ResourceException.BAD_REQUEST, "No certificates pulled from the keystore for alias "
                        + certAlias);
            } else if (certs.length > 1) {
                /*
                TODO: it may be that this warning can be removed - that having a cert chain, also for generating signatures,
                is nothing to warn about.
                The CXF-STS happily returns the first element in the chain, without logging a warning. Presumably the first
                element is the root of the trust chain, and thus the appropriate element to generate a signature with. I
                need to get more comfortable with this, before (possibly) removing this warning log.
                 */
                logger.warn("The number of certificates pulled from " +
                        "the keystore for alias " + certAlias + " is greater than 1: " + certs.length +
                        ". Returning the first cert in this array.");
                return certs[0];
            } else {
                return certs[0];
            }
        } catch (WSSecurityException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception caught pulling X509 cert from " +
                    "crypto for alias: " + certAlias + ". Exception: " + e, e);
        }
    }

    public PrivateKey getPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        try {
            return crypto.getPrivateKey(keyAlias, keyPassword);
        } catch (WSSecurityException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Exception pulling private key from " +
                    "crypto for alias " + keyAlias + "; Exception: " + e, e);
        }
    }

    private Crypto createCrypto() throws TokenCreationException {
        try {
            return CryptoFactory.getInstance(getEncryptionProperties());
        } catch (WSSecurityException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "Exception caught initializing the CryptoFactory - likely KeystoreConfig associated with published " +
                            "STS is incorrect. Exception: " + e + "; Keystore file: "
                            + getEncryptionProperties().getProperty(MERLIN_KEYSTORE_FILE_KEY));
        }
    }

    private Properties getEncryptionProperties() throws TokenCreationException {
        Properties properties = new Properties();
        /*
        It is not clear whether Merlin could support a hardware security module, nor the correct way of doing so. It is
        also not clear the customer demand for having STS crypto context stored in a HSM.
        It may well be that common HSMs are consumed via the KeyStore interface, and thus could integrate into Merlin
        transparently - i.e. the merlin.keystore.file could be the KeyStore pseudo-file providing access to HSM. It is
        also possible that this is not the case, and that a good way to support HSM would be to
        allow customers to specify the value corresponding to the org.apache.ws.security.crypto.provider, so that the
        CryptoFactory would instantiate the custom Crypto interface providing HSM access.

         If HSM support is required, and HSMs don't expose a KeyStore interface, then it is possible that a custom
         Crypto interface implementation may be the answer.
         */
        properties.put(
                "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        String keystorePassword;
        try {
            keystorePassword = new String(saml2Config.getKeystorePassword(), AMSTSConstants.UTF_8_CHARSET_ID);
        } catch (UnsupportedEncodingException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Unsupported string encoding for keystore password: " + e);
        }
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", keystorePassword);
        properties.put(MERLIN_KEYSTORE_FILE_KEY, saml2Config.getKeystoreFileName());
        properties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
        return properties;
    }
}
