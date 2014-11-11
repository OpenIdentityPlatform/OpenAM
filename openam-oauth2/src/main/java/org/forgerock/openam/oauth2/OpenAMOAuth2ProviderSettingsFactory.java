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

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.PEMDecoder;
import org.restlet.Request;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating/retrieving OpenAMOAuth2ProviderSettings instances.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMOAuth2ProviderSettingsFactory implements OAuth2ProviderSettingsFactory {

    private final Map<String, OAuth2ProviderSettings> providerSettingsMap = new HashMap<String, OAuth2ProviderSettings>();
    private final RealmNormaliser realmNormaliser;
    private final CookieExtractor cookieExtractor;
    private final PEMDecoder pemDecoder;

    /**
     * Contructs a new OpenAMOAuth2ProviderSettingsFactory.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param cookieExtractor An instance of the CookieExtractor.
     * @param pemDecoder An instance of the PEMDecoder.
     */
    @Inject
    public OpenAMOAuth2ProviderSettingsFactory(RealmNormaliser realmNormaliser, CookieExtractor cookieExtractor,
            PEMDecoder pemDecoder) {
        this.realmNormaliser = realmNormaliser;
        this.cookieExtractor = cookieExtractor;
        this.pemDecoder = pemDecoder;
    }

    /**
     * {@inheritDoc}
     */
    public OAuth2ProviderSettings get(OAuth2Request request) {
        final String realm = request.getParameter("realm");
        final Request req = request.getRequest();
        final String deploymentUrl = req.getHostRef().toString() + "/" + req.getResourceRef().getSegments().get(0);
        return getInstance(realmNormaliser.normalise(realm), deploymentUrl);
    }

    /**
     * Only to be used internally by AM.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(String realm) {
        return getInstance(realmNormaliser.normalise(realm), null);
    }

    /**
     * Gets the instance of the OAuth2ProviderSettings.
     * <br/>
     * Cache each provider settings on the realm it was created for.
     *
     * @param realm The realm.
     * @param deploymentUrl The deployment url.
     * @return The OAuth2ProviderSettings instance.
     */
    private OAuth2ProviderSettings getInstance(String realm, String deploymentUrl) {
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                providerSettings = new OpenAMOAuth2ProviderSettings(realm, deploymentUrl, cookieExtractor, pemDecoder);
                providerSettingsMap.put(realm, providerSettings);
            }
            return providerSettings;
        }
    }
}
