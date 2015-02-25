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
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.rest.service.RestletRealmRouter;
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
    public OAuth2ProviderSettings get(OAuth2Request request) throws NotFoundException {
        final String realm = request.getParameter("realm");
        final Request req = request.getRequest();
        String urlPattern = (String) req.getAttributes().get(RestletRealmRouter.REALM_URL);
        if (urlPattern.endsWith("/")) {
            urlPattern = urlPattern.substring(0, urlPattern.length() - 1);
        }
        String queryRealm = req.getResourceRef().getQueryAsForm().getFirstValue("realm");
        if (queryRealm != null && !"/".equals(queryRealm) && urlPattern.endsWith("/oauth2")) {
            urlPattern += realmNormaliser.normalise(queryRealm);
        }
        return getInstance(realmNormaliser.normalise(realm), urlPattern);
    }

    /**
     * Only to be used internally by AM.
     *
     * @param realm The realm.
     * @return The OAuth2ProviderSettings instance.
     */
    public OAuth2ProviderSettings get(String realm) throws NotFoundException {
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
    private OAuth2ProviderSettings getInstance(String realm, String deploymentUrl) throws NotFoundException {
        synchronized (providerSettingsMap) {
            OAuth2ProviderSettings providerSettings = providerSettingsMap.get(realm);
            if (providerSettings == null) {
                providerSettings = new OpenAMOAuth2ProviderSettings(realm, deploymentUrl, cookieExtractor, pemDecoder);
                if (providerSettings.exists()) {
                    providerSettingsMap.put(realm, providerSettings);
                } else {
                    throw new NotFoundException("No OpenID Connect provider for realm " + realm);
                }
            }
            return providerSettings;
        }
    }
}
