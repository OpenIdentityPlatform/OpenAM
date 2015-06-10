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

package org.forgerock.openam.sts.tokengeneration.oidc.crypto;

import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @see OpenIdConnectTokenPKIProviderFactory
 */
public class OpenIdConnectTokenPKIProviderFactoryImpl implements OpenIdConnectTokenPKIProviderFactory {
    private final Logger logger;

    @Inject
    OpenIdConnectTokenPKIProviderFactoryImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public OpenIdConnectTokenPKIProvider getOpenIdConnectTokenCryptoProvider(OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
        if (realProviderRequired(tokenConfig)) {
            return new OpenIdConnectTokenPKIProviderImpl(tokenConfig);
        } else {
            return new FauxOpenIdConnectTokenPKIProvider(logger);
        }
    }

    /*
    if the sts instance is not configured to issue OpenIdConnect tokens, then the tokenConfig reference will be null, and
    a faux provider should be returned. Or if the issued OpenIdConnect tokens should be signed via HMAC, and thus do not
    require private/public key state, the faux provider will be returned.
     */
    private boolean realProviderRequired(OpenIdConnectTokenConfig tokenConfig) {
        return (tokenConfig != null) &&
                (JwsAlgorithmType.RSA.equals(tokenConfig.getSignatureAlgorithm().getAlgorithmType()));
    }
}
