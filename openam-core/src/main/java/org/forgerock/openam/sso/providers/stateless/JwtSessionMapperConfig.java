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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import java.security.Key;
import java.security.KeyPair;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jwe.JweAlgorithmType;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.StringUtils;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.encode.Base64;

/**
 * Responsible for loading SMS configuration options relating to JwtSessionMapper.
 */
public class JwtSessionMapperConfig {

    static final String SIGNING_ALGORITHM = "openam-session-stateless-signing-type";
    static final String SIGNING_HMAC_SHARED_SECRET = "openam-session-stateless-signing-hmac-shared-secret";
    static final String ENCRYPTION_ALGORITHM = "openam-session-stateless-encryption-type";
    static final String COMPRESSION_TYPE = "openam-session-stateless-compression-type";

    private static final String ASYMMETRIC_SIGNING_KEY_ALIAS = "openam-session-stateless-signing-rsa-certificate-alias";
    private static final String ENCRYPTION_RSA_KEY_ALIAS = "openam-session-stateless-encryption-rsa-certificate-alias";
    private static final String ENCRYPTION_AES_KEY = "openam-session-stateless-encryption-aes-key";

    private static final String NONE = "NONE";

    private final JwtSessionMapper jwtSessionMapper;

    /**
     * Loads JWT Session signing and encoding configuration options.
     *
     * Referenced KeyStore certificates are loaded once by this constructor now so that:
     *
     * 1) we fail-fast if the configuration is incorrect.
     * 2) we only perform this I/O once for the current configuration options.
     */
    public JwtSessionMapperConfig(Map attrs) {

        JwtSessionMapperBuilder builder = new JwtSessionMapperBuilder();

        // Configure encryption algorithm and parameters

        String encryptionAlgorithm = CollectionHelper.getMapAttr(attrs, ENCRYPTION_ALGORITHM, NONE);
        final boolean encryptionEnabled = !StringUtils.isEqualTo(NONE, encryptionAlgorithm);

        if (encryptionEnabled) {
            JweAlgorithmType jweAlgorithmType = JweAlgorithmType.valueOf(encryptionAlgorithm);
            switch (jweAlgorithmType) {
                case RSA:
                    builder.encryptedUsingKeyPair(getKeyPair(attrs, ENCRYPTION_RSA_KEY_ALIAS));
                    break;
                case AES_KEYWRAP:
                    builder.encryptedUsingKeyWrap(getSecretKey(attrs, ENCRYPTION_AES_KEY));
                    break;
                case DIRECT:
                    builder.encryptedUsingDirectKey(getSecretKey(attrs, ENCRYPTION_AES_KEY));
                    break;
            }
        }

        // Configure compression
        CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.parseAlgorithm(
                CollectionHelper.getMapAttr(attrs, COMPRESSION_TYPE, NONE));
        builder.compressedUsing(compressionAlgorithm);

        // Configure signing algorithm and parameters

        String signingAlgorithmValue = CollectionHelper.getMapAttr(attrs, SIGNING_ALGORITHM);
        JwsAlgorithm signingAlgorithm = signingAlgorithmValue == null ? JwsAlgorithm.NONE :
                JwsAlgorithm.valueOf(signingAlgorithmValue);
        switch (signingAlgorithm) {

            case RS256:
                builder.signedUsingRS256(getKeyPair(attrs, ASYMMETRIC_SIGNING_KEY_ALIAS));
                break;

            case HS256:
                builder.signedUsingHS256(CollectionHelper.getMapAttr(attrs, SIGNING_HMAC_SHARED_SECRET));
                break;

            case HS384:
                builder.signedUsingHS384(CollectionHelper.getMapAttr(attrs, SIGNING_HMAC_SHARED_SECRET));
                break;

            case HS512:
                builder.signedUsingHS512(CollectionHelper.getMapAttr(attrs, SIGNING_HMAC_SHARED_SECRET));
                break;

            case ES256:
                builder.signedUsingES256(getKeyPair(attrs, ASYMMETRIC_SIGNING_KEY_ALIAS));
                break;

            case ES384:
                builder.signedUsingES384(getKeyPair(attrs, ASYMMETRIC_SIGNING_KEY_ALIAS));
                break;

            case ES512:
                builder.signedUsingES512(getKeyPair(attrs, ASYMMETRIC_SIGNING_KEY_ALIAS));
                break;

            case NONE:
            default:
                // leave builder defaults unchanged
        }

        // Build and return the JwtSessionMapper

        jwtSessionMapper = builder.build();

    }

    @VisibleForTesting
    KeyPair getKeyPair(Map attrs, String key) {
        String keyAlias = CollectionHelper.getMapAttr(attrs, key);
        return AMKeyProviderHolder.INSTANCE.getKeyPair(keyAlias);
    }

    @VisibleForTesting
    Key getSecretKey(Map attrs, String key) {
        byte[] keyData = Base64.decode(CollectionHelper.getMapAttr(attrs, key));
        return new SecretKeySpec(keyData, "AES");
    }

    private static final class AMKeyProviderHolder {
        private static final AMKeyProvider INSTANCE = new AMKeyProvider();// TODO: Obtain from Guice
    }

    /**
     * @return JwtSessionMapper configured according to hot-swappable SMS settings.
     */
    public JwtSessionMapper getJwtSessionMapper() {
        return jwtSessionMapper;
    }

}
