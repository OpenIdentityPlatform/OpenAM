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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.sts.tokengeneration.state;

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2CryptoProviderFactory;

import jakarta.inject.Inject;

/**
 * @see STSInstanceStateFactory
 */
public class RestSTSInstanceStateFactoryImpl implements STSInstanceStateFactory<RestSTSInstanceState, RestSTSInstanceConfig> {
    private final SAML2CryptoProviderFactory saml2CryptoProviderFactory;
    private final OpenIdConnectTokenPKIProviderFactory openIdConnectTokenPKIProviderFactory;

    @Inject
    RestSTSInstanceStateFactoryImpl(SAML2CryptoProviderFactory saml2CryptoProviderFactory,
                                    OpenIdConnectTokenPKIProviderFactory openIdConnectTokenPKIProviderFactory) {
        this.saml2CryptoProviderFactory = saml2CryptoProviderFactory;
        this.openIdConnectTokenPKIProviderFactory = openIdConnectTokenPKIProviderFactory;
    }

    public RestSTSInstanceState createSTSInstanceState(RestSTSInstanceConfig config) throws TokenCreationException {
        return new RestSTSInstanceState(config, saml2CryptoProviderFactory.createSAML2CryptoProvider(config.getSaml2Config()),
                openIdConnectTokenPKIProviderFactory.getOpenIdConnectTokenCryptoProvider(config.getOpenIdConnectTokenConfig()));
    }
}
