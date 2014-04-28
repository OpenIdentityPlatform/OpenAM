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

package org.forgerock.oauth2;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.server.ConfigurationResource;
import org.restlet.Request;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @since 12.0.0
 */
@Singleton
public class OAuth2ProviderSettingsFactoryImpl implements OAuth2ProviderSettingsFactory {

    private final ConfigurationResource configurationResource;

    private OAuth2ProviderSettings providerSettings;

    @Inject
    public OAuth2ProviderSettingsFactoryImpl(final ConfigurationResource configurationResource) {
        this.configurationResource = configurationResource;
    }

    public synchronized OAuth2ProviderSettings get(OAuth2Request request) {
        if (providerSettings == null) {
            final Request req = request.getRequest();
            final String deploymentUrl = req.getHostRef().toString() + "/" + req.getResourceRef().getSegments().get(0);
            providerSettings = new OAuth2ProviderSettingsImpl(deploymentUrl, configurationResource);
        }
        return providerSettings;
    }
}
