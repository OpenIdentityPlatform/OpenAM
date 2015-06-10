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

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;

/**
 * Defines the concern of creating a OpenIdConnectTokenPKIProvider for a particular STS instance, driven by the
 * state in the OpenIdConnectTokenConfig associated with the STS instance.
 */
public interface OpenIdConnectTokenPKIProviderFactory {
    /**
     *
     * @param tokenConfig the OpenIdConnectTokenConfig corresponding the STS instance for whom the token service is generating
     *                    tokens
     * @return a non-null OpenIdConnectTokenPKIProvider
     * @throws TokenCreationException if the OpenIdConnectTokenPKIProvider could not be instantiated - usually because
     * of incorrect keystore configuration
     */
    OpenIdConnectTokenPKIProvider getOpenIdConnectTokenCryptoProvider(OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException;
}
