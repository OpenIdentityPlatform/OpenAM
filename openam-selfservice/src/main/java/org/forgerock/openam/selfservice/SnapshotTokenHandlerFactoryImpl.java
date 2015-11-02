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
package org.forgerock.openam.selfservice;

import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.selfservice.core.config.StageConfigException;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandler;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Factory for providing snapshot token handlers.
 *
 * @since 13.0.0
 */
final class SnapshotTokenHandlerFactoryImpl implements SnapshotTokenHandlerFactory {

    @Override
    public SnapshotTokenHandler get(SnapshotTokenConfig config) {
        if (config.getType().equals(JwtTokenHandlerConfig.TYPE)) {
            return configureJwtTokenHandler((JwtTokenHandlerConfig) config);
        }

        throw new StageConfigException("Unknown token type " + config.getType());
    }

    private SnapshotTokenHandler configureJwtTokenHandler(JwtTokenHandlerConfig config) {
        try {
            SigningManager signingManager = new SigningManager();
            SigningHandler signingHandler = signingManager.newHmacSigningHandler(config.getSharedKey());

            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(config.getKeyPairAlgorithm());
            keyPairGen.initialize(config.getKeyPairSize());

            return new JwtTokenHandler(
                    config.getJweAlgorithm(),
                    config.getEncryptionMethod(),
                    keyPairGen.generateKeyPair(),
                    config.getJwsAlgorithm(),
                    signingHandler,
                    config.getTokenLifeTimeInSeconds());

        } catch (NoSuchAlgorithmException nsaE) {
            throw new IllegalArgumentException("Unknown algorithm as defined in the config", nsaE);
        }
    }

}
