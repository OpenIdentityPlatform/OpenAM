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

import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class OpenIdConnectTokenPKIProviderFactoryImplTest {
    @Test
    public void testCryptoProviderLookup() throws TokenCreationException {
        OpenIdConnectTokenPKIProviderFactory cryptoProviderFactory = new OpenIdConnectTokenPKIProviderFactoryImpl(mock(Logger.class));
        assertTrue(cryptoProviderFactory.getOpenIdConnectTokenCryptoProvider(getConfig(JwsAlgorithm.RS256))
                instanceof OpenIdConnectTokenPKIProviderImpl);
        assertTrue(cryptoProviderFactory.getOpenIdConnectTokenCryptoProvider(getConfig(JwsAlgorithm.HS256))
                instanceof FauxOpenIdConnectTokenPKIProvider);
    }

    private OpenIdConnectTokenConfig getConfig(JwsAlgorithm jwsAlgorithm) {
        return OpenIdConnectTokenConfig.builder()
                .addAudience("audience")
                .issuer("issuer")
                .keystoreLocation("keystore.jks")
                .keystorePassword("changeit".getBytes())
                .clientSecret("bobo".getBytes())
                .signatureAlgorithm(jwsAlgorithm)
                .signatureKeyAlias("test")
                .signatureKeyPassword("changeit".getBytes())
                .build();
    }

}
