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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authz.modules;

import org.forgerock.authz.modules.oauth2.OAuth2Exception;
import org.forgerock.openam.oauth2.model.CoreToken;
import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.provider.Scope;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Service for getting user info for a OAuth2 Token.
 *
 * @since 12.0.0
 */
public class OAuth2UserInfoService {

    private final Logger logger = LoggerFactory.getLogger(OAuth2UserInfoService.class);

    /**
     * Retrieves user info for the given OAuth2 CoreToken.
     *
     * @param token The OAuth2 CoreToken.
     * @return A Map containing the user info.
     */
    public Map<String, Object> getUserInfo(final CoreToken token) throws OAuth2Exception {
        Scope scope = null;
        try {
            OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(token.getRealm());

            final String pluginClassName = settings.getScopeImplementationClass();
            if (pluginClassName != null && !pluginClassName.isEmpty()) {
                final Class<? extends Scope> scopeClass = Class.forName(pluginClassName).asSubclass(Scope.class);
                scope = scopeClass.newInstance();
            }
        } catch (Exception e) {
            logger.error("Could not get Scope implementation to get user info for token, %", token.getTokenID());
            throw new OAuth2Exception("Could not get Scope implementation to get user info for token.", e);
        }

        // Validate the granted scope
        if (scope != null) {
            return scope.getUserInfo(token);
        } else {
            logger.error("Could not get Scope implementation to get user info for token., %", token.getTokenID());
            throw new OAuth2Exception("Could not get Scope implementation to get user info for token.");
        }
    }
}
