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

package org.forgerock.openam.sts.tokengeneration.oidc;


import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @see org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenClaimMapperProvider
 */
public class OpenIdConnectTokenClaimMapperProviderImpl implements OpenIdConnectTokenClaimMapperProvider {
    private final Logger logger;
    private final DefaultOpenIdConnectTokenClaimMapper defaultOpenIdConnectTokenClaimMapper;
    @Inject
    OpenIdConnectTokenClaimMapperProviderImpl(Logger logger) {
        this.logger = logger;
        defaultOpenIdConnectTokenClaimMapper = new DefaultOpenIdConnectTokenClaimMapper();
    }

    @Override
    public OpenIdConnectTokenClaimMapper getClaimMapper(OpenIdConnectTokenConfig tokenConfig) {
        final String customClaimMapperClass = tokenConfig.getCustomClaimMapperClass();
        if (customClaimMapperClass != null) {
            try {
                return Class.forName(customClaimMapperClass).asSubclass(OpenIdConnectTokenClaimMapper.class).newInstance();
            } catch (Exception e) {
                logger.error("Could not instantiate custom OpenIdConnectTokenClaimMapper class "
                        + customClaimMapperClass +"; falling back to default implementation.");
            }
        }
        return defaultOpenIdConnectTokenClaimMapper;
    }
}
