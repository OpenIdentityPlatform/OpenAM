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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.slf4j.Logger;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * An implementation of the OpenIdConnectTokenPKIProvider for STS instances which are not issuing OpenIdConnectTokens,
 * or are issuing OpenIdConnect tokens with a non-RSA-based signature mechanism (HMAC), and thus don't need public/private
 * key state.
 */
public class FauxOpenIdConnectTokenPKIProvider implements OpenIdConnectTokenPKIProvider {
    private final Logger logger;

    FauxOpenIdConnectTokenPKIProvider(Logger logger) {
        this.logger = logger;
    }

    @Override
    public PrivateKey getProviderPrivateKey(String keyAlias, String keyPassword) throws TokenCreationException {
        logger.error("getProviderPrivateKey called on an " +
                "instance of the FauxOpenIdConnectTokenPKIProvider, a crypto provider which should only be " +
                "instantiated for sts instances which signing OIDC tokens with hmac, or don't issue OIDC tokens at all, " +
                "and thus don't require PKI context. " +
                "Illegal state! This most-likely occurred due to a sts configuration state update, and the sts token " +
                "generation cache was not updated in time.");
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Illegal condition: FauxOpenIdConnectTokenPKIProvider#getProviderPrivateKey invoked.");
    }

    @Override
    public X509Certificate[] getProviderCertificateChain(String keyAlias) throws TokenCreationException {
        logger.error("getProviderCertificateChain called on an " +
                "instance of the FauxOpenIdConnectTokenPKIProvider, a crypto provider which should only be " +
                "instantiated for sts instances which signing OIDC tokens with hmac, or don't issue OIDC tokens at all, " +
                "and thus don't require PKI context. " +
                "Illegal state! This most-likely occurred due to a sts configuration state update, and the sts token " +
                "generation cache was not updated in time.");
        throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                "Illegal condition: FauxOpenIdConnectTokenPKIProvider#getProviderCertificateChain invoked.");
    }
}
