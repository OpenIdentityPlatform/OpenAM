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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import com.sun.identity.shared.datastruct.CollectionHelper;
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.jose.jwe.JweAlgorithmType;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.utils.AMKeyProvider;

import java.security.KeyPair;
import java.util.Map;

/**
 * Responsible for loading SMS configuration options relating to JwtSessionMapper.
 */
public class JwtSessionMapperConfig {

    public static final String SIGNING_ALGORITHM = "openam-session-stateless-signing-type";
    public static final String SIGNING_HMAC_SHARED_SECRET = "openam-session-stateless-signing-hmac-shared-secret";
    public static final String SIGNING_RSA_KEY_ALIAS = "openam-session-stateless-signing-rsa-certificate-alias";
    public static final String ENCRYPTION_ALGORITHM = "openam-session-stateless-encryption-type";
    public static final String ENCRYPTION_RSA_KEY_ALIAS = "openam-session-stateless-encryption-rsa-certificate-alias";

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

        String encryptionAlgorithm = CollectionHelper.getMapAttr(attrs, ENCRYPTION_ALGORITHM);
        final boolean encryptionEnabled = JweAlgorithmType.RSA.toString().equalsIgnoreCase(encryptionAlgorithm);
        if (encryptionEnabled) {
            builder.encryptedUsingKeyPair(getKeyPair(attrs, ENCRYPTION_RSA_KEY_ALIAS));
        }

        // Configure signing algorithm and parameters

        JwsAlgorithm signingAlgorithm = JwsAlgorithm.valueOf(CollectionHelper.getMapAttr(attrs, SIGNING_ALGORITHM));
        switch (signingAlgorithm) {

            case RS256:
                builder.signedUsingRS256(getKeyPair(attrs, SIGNING_RSA_KEY_ALIAS));
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
