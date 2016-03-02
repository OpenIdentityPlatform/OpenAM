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

import javax.inject.Inject;
import java.security.KeyPair;

/**
 * Factory for providing snapshot token handlers.
 *
 * @since 13.0.0
 */
final class JwtSnapshotTokenHandlerFactory implements SnapshotTokenHandlerFactory {

    private static final JweAlgorithm DEFAULT_ENCRYPTION_ALOGIRTHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final EncryptionMethod DEFAULT_ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;

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

        SigningManager signingManager = new SigningManager();
        byte[] secret = config.getSigningSymmetricKey().getBytes();
        SigningHandler signingHandler = signingManager.newHmacSigningHandler(secret);
        JwsAlgorithm jwsAlgorithm = JwsAlgorithm.valueOf(config.getSigningAlgorithm());

        if (jwsAlgorithm == null) {
            throw new StageConfigException("Unsupported signing algorithm "
                    + config.getSigningAlgorithm());
        }

        return new JwtTokenHandler(
                DEFAULT_ENCRYPTION_ALOGIRTHM,
                DEFAULT_ENCRYPTION_METHOD,
                encryptionKeyPair,
                jwsAlgorithm,
                signingHandler,
                config.getTokenLifeTimeInSeconds());
    }

}
