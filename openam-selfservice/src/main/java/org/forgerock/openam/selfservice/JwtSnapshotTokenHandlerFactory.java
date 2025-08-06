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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.selfservice;

import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;

import javax.crypto.SecretKey;
import jakarta.inject.Inject;
import java.security.KeyPair;

/**
 * Factory for providing snapshot token handlers.
 *
 * @since 13.0.0
 */
final class JwtSnapshotTokenHandlerFactory implements SnapshotTokenHandlerFactory {

    private static final JweAlgorithm DEFAULT_ENCRYPTION_ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final EncryptionMethod DEFAULT_ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;
    private static final JwsAlgorithm DEFAULT_SIGNING_ALGORITHM = JwsAlgorithm.HS256;

    private final AMKeyProvider keyProvider;

    @Inject
    JwtSnapshotTokenHandlerFactory(AMKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    public SnapshotTokenHandler get(SnapshotTokenConfig config) {
        if (config.getType().equals(KeyStoreJwtTokenConfig.TYPE)) {
            return configureJwtTokenHandler((KeyStoreJwtTokenConfig) config);
        }

        throw new StageConfigException("Unknown token type " + config.getType());
    }

    private SnapshotTokenHandler configureJwtTokenHandler(KeyStoreJwtTokenConfig config) {
        KeyPair encryptionKeyPair = keyProvider.getKeyPair(config.getEncryptionKeyPairAlias());

        if (encryptionKeyPair == null) {
            throw new StageConfigException("Unable to retrieve key pair for encryption key pair alias "
                    + config.getEncryptionKeyPairAlias());
        }

        SecretKey secretKey = keyProvider.getSecretKey(config.getSigningSecretKeyAlias());

        if (secretKey == null) {
            throw new StageConfigException("Unable to retrieve key for certificate alias "
                    + config.getSigningSecretKeyAlias());
        }

        SigningManager signingManager = new SigningManager();
        SigningHandler signingHandler = signingManager.newHmacSigningHandler(secretKey.getEncoded());

        return new JwtTokenHandler(
                DEFAULT_ENCRYPTION_ALGORITHM,
                DEFAULT_ENCRYPTION_METHOD,
                encryptionKeyPair,
                DEFAULT_SIGNING_ALGORITHM,
                signingHandler,
                config.getTokenLifeTimeInSeconds());
    }

}
